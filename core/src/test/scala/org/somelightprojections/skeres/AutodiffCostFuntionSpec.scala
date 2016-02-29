package org.somelightprojections.skeres

import com.google.ceres.{DoubleArray, StdVectorDoublePointer}
import org.scalatest.{MustMatchers, WordSpec}
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra._
import spire.implicits._

class AutodiffCostFuntionSpec extends WordSpec with MustMatchers {

  "AutodiffCostFunction" should {
    "evaluate cost and jacobians on a bilinear scalar cost function" in {
      case class BinaryScalarCost(a: Double) extends CostFunctor(1, 2, 2) {
        override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](
          p: Array[T]*
        ): Array[T] = {
          require(p.length == 2)
          require(p(0).length == 2)
          require(p(1).length == 2)
          val x = p(0)
          val y = p(1)
          val z = x(0) * y(0) + x(1) * y(1) - a
          Array(z)
        }
      }

      val parameters: DoublePointerPointer = RichDoubleMatrix.ofSize(2, 2)
      parameters.set(0, 0, 1.0)
      parameters.set(0, 1, 2.0)
      parameters.set(1, 0, 3.0)
      parameters.set(1, 1, 4.0)

      val noJacobians = RichDoubleMatrix.empty
      val residuals: DoublePointer = new DoubleArray(1).toPointer

      val costFunction = BinaryScalarCost(1.0).toAutodiffCostFunction

      costFunction.evaluate(parameters, residuals, noJacobians) must be(true)
      residuals.get(0) must be(10.0)
      residuals.set(0, 0.0)

      val jacobians: DoublePointerPointer = RichDoubleMatrix.ofDim(2, 2)
      costFunction.evaluate(parameters, residuals, jacobians) must be(true)

      residuals.get(0) must be(10.0)

      jacobians.get(0, 0) must be(3)
      jacobians.get(0, 1) must be(4)
      jacobians.get(1, 0) must be(1)
      jacobians.get(1, 1) must be(2)
    }
  }
  "evaluate cost and jacobians on a bilinear vector cost function" in {
    case class BinaryVector3Cost(a: Double) extends CostFunctor(3, 2, 2) {
      override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](
        p: Array[T]*
      ): Array[T] = {
        require(p.length == 2)
        require(p(0).length == 2)
        require(p(1).length == 2)
        val x = p(0)
        val y = p(1)
        val z0 = x(0) * y(0) + x(1) * y(1) - a
        val z1 = x(0) * y(0) - x(1) * y(1) + a
        val z2 = x(0) * x(1) + y(0) * y(1) + 10 * a
        Array(z0, z1, z2)
      }
    }

    val parameters: DoublePointerPointer = RichDoubleMatrix.ofSize(2, 2)
    parameters.set(0, 0, 1.0)
    parameters.set(0, 1, 2.0)
    parameters.set(1, 0, 3.0)
    parameters.set(1, 1, 4.0)

    val noJacobians = RichDoubleMatrix.empty
    val residuals: DoublePointer = new DoubleArray(3).toPointer

    val costFunction = BinaryVector3Cost(1.0).toAutodiffCostFunction

    costFunction.evaluate(parameters, residuals, noJacobians) must be(true)
    residuals.get(0) must be(10.0)
    residuals.get(1) must be(-4.0)
    residuals.get(2) must be(24.0)

    residuals.set(0, 0.0)
    residuals.set(1, 0.0)
    residuals.set(2, 0.0)

    val jacobians: DoublePointerPointer = RichDoubleMatrix.ofDim(2, 6)
    costFunction.evaluate(parameters, residuals, jacobians) must be(true)

    residuals.get(0) must be(10.0)

    jacobians.get(0, 0) must be(3)
    jacobians.get(0, 1) must be(4)
    jacobians.get(0, 2) must be(3)
    jacobians.get(0, 3) must be(-4)
    jacobians.get(0, 4) must be(2)
    jacobians.get(0, 5) must be(1)

    jacobians.get(1, 0) must be(1)
    jacobians.get(1, 1) must be(2)
    jacobians.get(1, 2) must be(1)
    jacobians.get(1, 3) must be(-2)
    jacobians.get(1, 4) must be(4)
    jacobians.get(1, 5) must be(3)
  }
  "evaluate cost and jacobians on a cost function with many parameter blocks" in {
    case class TenParameterCost() extends CostFunctor(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1) {
      override def apply[@sp(Double) T: Field : Trig : NRoot : Order : ClassTag](
        p: Array[T]*
      ): Array[T] = {
        require(p.length == 2)
        require(p.forall(_.length == 1))
        Array(p.map(_(0)).reduce(_ + _))
      }
    }

    val parameters: DoublePointerPointer = RichDoubleMatrix.ofSize(10, 1)
    (0 until 10).foreach(i => parameters.set(i, 0, i))

    val noJacobians = RichDoubleMatrix.empty
    val residuals: DoublePointer = new DoubleArray(1).toPointer

    val costFunction = TenParameterCost().toAutodiffCostFunction

    costFunction.evaluate(parameters, residuals, noJacobians) must be(true)
    residuals.get(0) must be(45.0)

    residuals.set(0, 0.0)

    val jacobians: DoublePointerPointer = RichDoubleMatrix.ofDim(10, 1)
    costFunction.evaluate(parameters, residuals, jacobians) must be(true)

    residuals.get(0) must be(45.0)
    (0 until 10).foreach(i => jacobians.get(i, 0) must be(1.0))
  }
}
