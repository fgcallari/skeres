package org.somelightprojections.skeres

import com.google.ceres.{CostFunction, DoubleArray, DoubleMatrix, StdVectorDoublePointer}
import scala.reflect.ClassTag
import scala.util.Try
import scala.{specialized => sp}
import shapeless.{TypeCase, Typeable}
import spire.algebra.{Order, NRoot, Field, Semiring, Trig}
import spire.implicits._
import spire.math.{Jet, JetDim}


object CostFunctionToFunctor {
  def apply(cost: CostFunction)(implicit d: JetDim): AutoDiffCostFunctor = CostFunctorAdapter(cost)
}

// This is only to preserve the ceres-solver class names - in Scala everything is dynamic.
object DynamicCostFunctionToFunctor {
  def apply(cost: CostFunction)(implicit d: JetDim): AutoDiffCostFunctor = CostFunctorAdapter(cost)
}

private[skeres] object CostFunctorAdapter {
  def N(cost: CostFunction): Array[Int] = {
    val n = cost.parameterBlockSizes.size.toInt
    (0 until n).map(i => cost.parameterBlockSizes.get(i)).toArray
  }

  implicit def jetTypeable[T](implicit castT: Typeable[T]): Typeable[Jet[T]] =
    new Typeable[Jet[T]] {
      override def cast(t: Any): Option[Jet[T]] = t match {
        case jt: Jet[_] =>
          castT.cast(jt.real) match {
            case None => None
            case _ => Some(t.asInstanceOf[Jet[T]])
          }
        case _ => None
      }

      override def describe = "Jet[T]"
    }

  val `Seq[Vector[Double]]` = TypeCase[Seq[Vector[Double]]]
  val `Seq[Vector[Jet[Double]]` = TypeCase[Seq[Vector[Jet[Double]]]]
}

private[skeres] case class CostFunctorAdapter(cost: CostFunction)(implicit d: JetDim)
    extends AutoDiffCostFunctor(cost.numResiduals(), CostFunctorAdapter.N(cost): _*) {
  import CostFunctorAdapter._
  private val numParameters = N.sum
  private val numParameterBlocks = N.length

  override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] = {
    // See https://github.com/milessabin/shapeless/issues/465 for why this rather inefficient
    // Array->Vector->Array song ad dance is needed.
    val out = x.map(_.toVector) match {
      case `Seq[Vector[Double]]`(xDouble) => applyForDouble(xDouble.map(_.toArray): _*)
      case `Seq[Vector[Jet[Double]]`(xJet) => applyForJet(xJet.map(_.toArray): _*)
      case _ => throw new IllegalArgumentException("Invalid type fox x")
    }
    out.asInstanceOf[Array[T]]
  }

  private def applyForDouble(x: Array[Double]*): Array[Double] = {
    val xa = new StdVectorDoublePointer(x.length)
    cforRange(0 until x.length) { i =>
      xa.set(i, new DoubleArray(N(i)).copyFrom(x(i)).toPointer)
    }
    val parameters = DoubleMatrix.toPointerPointer(xa)
    val residuals = new DoubleArray(kNumResiduals)
    if (!cost.evaluate(parameters, residuals.toPointer, null)) {
      Array.empty
    } else {
      residuals.toArray(kNumResiduals)
    }
  }

  private def applyForJet(input: Array[Jet[Double]]*): Array[Jet[Double]] = {
    val xa = new StdVectorDoublePointer(input.length)
    cforRange(0 until input.length) { i =>
      xa.set(i, new DoubleArray(N(i)).copyFrom(input(i).map(_.real)).toPointer)
    }
    val parameters = DoubleMatrix.toPointerPointer(xa)
    val ja = new StdVectorDoublePointer(kNumResiduals * numParameters)
    val jacobians = DoubleMatrix.toPointerPointer(ja)
    val residuals = new DoubleArray(kNumResiduals)
    if (!cost.evaluate(parameters, residuals.toPointer, jacobians)) {
      Array.empty
    } else {
      // Now that we have the incoming Jets, which are carrying the
      // partial derivatives of each of the inputs w.r.t to some other
      // underlying parameters. The derivative of the outputs of the
      // cost function w.r.t to the same underlying parameters can now
      // be computed by applying the chain rule.
      //
      //  d output[i]               d output[i]   d input[j]
      //  --------------  = sum_j   ----------- * ------------
      //  d parameter[k]            d input[j]    d parameter[k]
      //
      // d input[j]
      // --------------  = inputs[j], so
      // d parameter[k]
      //
      //  outputJet[i]  = sum_k jacobian[i][k] * inputJet[k]
      //
      // The following loop, iterates over the residuals, computing one
      // output jet at a time.
      val out = Array.ofDim[Jet[Double]](kNumResiduals)
      cforRange(0 until out.length) { i =>
        val real = residuals.get(i)
        out(i) = Jet(real)
        cforRange(0 until numParameterBlocks) { j =>
          val blockSize = N(j)
          val jrow: DoubleArray = jacobians.getRow(j)
          val inj: Array[Jet[Double]] = input(j)
          cforRange(0 until blockSize) { k =>
            out(i) += jrow.get(i * blockSize + k) * Jet[Double](0, inj(k).infinitesimal)
          }
        }
      }
      out
    }
  }
}
