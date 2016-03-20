package org.somelightprojections.skeres

import com.google.ceres.{NumericDiffOptions, NumericDiffMethodType}
import org.somelightprojections.skeres
import spire.implicits._
import spire.math._
import spire.algebra._

/**
  * Numerically-differentiable cost function
  */
case class NumericDiffCostFunction(
  method: NumericDiffMethodType,
  options: NumericDiffOptions,
  costFunctor: CostFunctor
) extends SizedCostFunction(costFunctor.kNumResiduals, costFunctor.N: _*) {

  override def evaluate(
    parameters: DoublePointerPointer,
    residuals: DoublePointer,
    jacobians: DoublePointerPointer
  ): Boolean = {
    val numParameterBlocks = costFunctor.N.length

    val minStepSize = {
      // It is not a good idea to make the step size arbitrarily
      // small. This will lead to problems with round off and numerical
      // instability when dividing by the step size. The general
      // recommendation is to not go down below sqrt(epsilon).
      val sqrtEpsilon = sqrt(skeres.EpsilonDouble)
      if (method == NumericDiffMethodType.RIDDERS) {
        max(sqrtEpsilon, options.getRiddersRelativeInitialStepSize)
      } else {
        sqrtEpsilon
      }
    }

    val x = Array.ofDim[Array[Double]](numParameterBlocks)
    cforRange(0 until numParameterBlocks) { i =>
      x(i) = parameters.getRow(i).toArray(costFunctor.N(i))
    }

    val yC: Array[Double] = costFunctor(x: _*)

    if (yC.isEmpty) {
      false
    } else if (jacobians.isNull) {
      residuals.copyFrom(yC)
      true
    } else {
      val stepScale =
        if (method == NumericDiffMethodType.RIDDERS) options.getRiddersRelativeInitialStepSize
        else options.getRelativeStepSize

      cforRange(0 until numParameterBlocks) { i =>
        val ni = costFunctor.N(i)
        val Ji: Array[Double] = Array.ofDim[Double](kNumResiduals * ni)
        val xi: Array[Double] = x(i)
        val steps = Array.ofDim[Double](ni)
        cforRange(0 until ni) { j =>
          steps(i) = max(abs(xi(j)) * stepScale, minStepSize)
        }
        cforRange(0 until ni) { col =>
          val xic = xi(col)
          val sc = steps(col)
          xi(col) = xic + sc
          val yF = costFunctor(x: _*)
          if (yF.isEmpty) {
            return false
          }
          if (method == NumericDiffMethodType.FORWARD) {
            val invSc = 1.0 / sc
            var offset = col
            cforRange(0 until kNumResiduals) { row =>
              Ji(offset) = (yF(row) - yC(row)) * invSc
              offset += ni
            }
          } else if (method == NumericDiffMethodType.CENTRAL) {
            val invSc = 0.5 * sc
            xi(col) = xic - sc
            val yB = costFunctor(x: _*)
            if (yB.isEmpty) {
              return false
            }
            var offset = col
            cforRange(0 until kNumResiduals) { row =>
              Ji(offset) = (yF(row) - yB(row)) * invSc
              offset += ni
            }
          }
          xi(col) = xic
          jacobians.copyRowFrom(i, Ji)
        }
      }
      true
    }
  }
}