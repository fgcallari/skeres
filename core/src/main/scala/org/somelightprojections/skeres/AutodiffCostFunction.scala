package org.somelightprojections.skeres

import spire.implicits._
import spire.math.{Jet, JetDim}

/**
  * Auto-differentiable cost function
  */
case class AutodiffCostFunction(costFunctor: CostFunctor)
  extends SizedCostFunction(costFunctor.kNumResiduals, costFunctor.N: _*) {

  implicit val jetDimension = JetDim(costFunctor.N.sum)

  override def evaluate(
    parameters: DoublePointerPointer,
    residuals: DoublePointer,
    jacobians: DoublePointerPointer
  ): Boolean = {
    val numParameterBlocks = costFunctor.N.length
    if (jacobians.isNull) {
      val x = Array.ofDim[Array[Double]](numParameterBlocks)
      cforRange(0 until numParameterBlocks) { i =>
        x(i) = parameters.getRow(i).toArray(costFunctor.N(i))
      }

      val y: Array[Double] = costFunctor(x: _*)

      if (y.isEmpty) {
        false
      } else {
        residuals.copyFrom(y)
        true
      }
    } else {
      val jx = Array.fill[Array[Jet[Double]]](numParameterBlocks)(null)
      var k = 0
      cforRange(0 until numParameterBlocks) { i =>
        val n = costFunctor.N(i)
        val xi = parameters.getRow(i)
        val jxi = Array.ofDim[Jet[Double]](n)
        cforRange(0 until n) { j =>
          jxi(j) = Jet[Double](xi.get(j), k)
          k += 1
        }
        jx(i) = jxi
      }

      val jy: Array[Jet[Double]] = costFunctor(jx: _*)

      if (jy.isEmpty) {
        false
      } else {
        cforRange(0 until costFunctor.kNumResiduals) { i =>
          val jyi = jy(i)
          residuals.set(i, jyi.real)
          if (!jacobians.isNull && jacobians.hasRow(i)) jacobians.copyRowFrom(i, jyi.infinitesimal)
        }
        true
      }
    }
  }
}