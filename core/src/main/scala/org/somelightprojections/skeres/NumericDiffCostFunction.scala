package org.somelightprojections.skeres

import com.google.ceres.{NumericDiffMethodType, NumericDiffOptions}
import org.somelightprojections.skeres
import spire.implicits._
import spire.math._

/**
  * A CostFunction with Jacobians computed via numeric (a.k.a. finite) differentiation.
  * For more details on numerical differentiation, see
  * http://en.wikipedia.org/wiki/Numerical_differentiation.
  *
  * To get a numerically differentiated cost function, you must define
  * a NumericDiffCostFunctor with an apply() method that computes the residuals.
  *
  * The method must return the residuals in an Array of size kNumResiduals, or an
  * empty one to indicate failure.
  * Please see the documentation for CostFunctor for details on how the return value
  * may be used to impose simple constraints on the parameter block.
  *
  * For example, consider a scalar error e = k - x'y, where both x and y are
  * two-dimensional column vector parameters, the prime sign indicates
  * transposition, and k is a constant. The form of this error, which is the
  * difference between a constant and an expression, is a common pattern in least
  * squares problems. For example, the value x'y might be the model expectation
  * for a series of measurements, where there is an instance of the cost function
  * for each measurement k.
  *
  * The actual cost added to the total problem is e^2, or (k - x'k)^2; however,
  * the squaring is implicitly done by the optimization framework.
  *
  * To write an numerically-differentiable cost function for the above model, first
  * define the object
  *
  *   // Dimension of y -------------------------------------------------------------+
  *   // Dimension of x ----------------------------------------------------------+  |
  *   // Dimension of residual ------------------------------------------------+  |  |
  *   //                                                                       |  |  |
  *   case class MyScalarCostFunctor(k: Double) extends NumericDiffCostFunctor(1, 2, 2) {
  *     // Compute the residual: parameterBlocks are ordinately x, y.
  *     override def apply(parameterBlocks: Array[Double]*): Array[Double] = {
  *       val x = parameterBlocks(0)
  *       val y = parameterBlocks(1)
  *       Array(k - x(0) * y(0) + x(1) * y(1))
  *     }
  *   }
  *
  * In the above declaration, the parent class parameters, "(1, 2, 2)", describe the functor
  * as computing a 1-dimensional output from two arguments, both 2-dimensional.
  * Since the residual is a scalar, the output Array of apply() has only one component.
  *
  * Given this class definition, the numerically differentiated
  * cost function with central differences used for computing the
  * derivative can be constructed as follows.
  *
  *   val method = NumericDiffMethodType.CENTRAL
  *   val options = new NumericDiffOptions
  *   val costFunction: CostFunction =
  *     MyScalarCostFunctor(k = 1.0).toNumericDiffCostFunction(method, Some(options))
  *
  * The options argument can be omitted if the default values sufice.
  *
  * The CENTRAL difference method is considerably more accurate than the FORWARD one,
  * at the cost of twice as many function evaluations. Consider using central differences
  * to begin with, and only after that works, trying forward difference to improve performance.
  *
  */
case class NumericDiffCostFunction(
  method: NumericDiffMethodType,
  options: NumericDiffOptions,
  costFunctor: NumericDiffCostFunctor
) extends SizedCostFunction(costFunctor.kNumResiduals, costFunctor.N: _*) {
  require(method != NumericDiffMethodType.RIDDERS, "RIDDERS method not yet implemented")

  override def evaluate(
    parameters: DoublePointerPointer,
    residuals: DoublePointer,
    jacobians: DoublePointerPointer
  ): Boolean = {
    import NumericDiffMethodType._

    val numParameterBlocks = costFunctor.N.length

    val minStepSize = {
      // It is not a good idea to make the step size arbitrarily
      // small. This will lead to problems with round off and numerical
      // instability when dividing by the step size. The general
      // recommendation is to not go down below sqrt(epsilon).
      val sqrtEpsilon = sqrt(skeres.EpsilonDouble)
      if (method == RIDDERS) {
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
      return false
    }

    residuals.copyFrom(yC)

    if (jacobians.isNull) {
      return true
    }

    val stepScale =
      if (method == RIDDERS) options.getRiddersRelativeInitialStepSize
      else options.getRelativeStepSize

    cforRange(0 until numParameterBlocks) { i =>
      val ni = costFunctor.N(i)
      // Jacobian for the i-th parameter block, stored row-major
      val Ji: Array[Double] = Array.ofDim[Double](kNumResiduals * ni)
      val xi: Array[Double] = x(i)
      val steps = Array.ofDim[Double](ni)
      cforRange(0 until ni) { j =>
        steps(j) = max(abs(xi(j)) * stepScale, minStepSize)
      }
      cforRange(0 until ni) { col =>
        val xic = xi(col)
        val sc = steps(col)
        xi(col) = xic + sc
        val yF = costFunctor(x: _*)
        xi(col) = xic
        if (yF.isEmpty) {
          return false
        }
        method match {
          case FORWARD =>
            val invSc = 1.0 / sc
            var offset = col
            cforRange(0 until kNumResiduals) { row =>
              Ji(offset) = (yF(row) - yC(row)) * invSc
              offset += ni
            }
          case CENTRAL =>
            val invSc = 0.5 / sc
            xi(col) = xic - sc
            val yB = costFunctor(x: _*)
            xi(col) = xic
            if (yB.isEmpty) {
              return false
            }
            var offset = col
            cforRange(0 until kNumResiduals) { row =>
              Ji(offset) = (yF(row) - yB(row)) * invSc
              offset += ni
            }
          case _ =>
            throw new IllegalArgumentException("RIDDERS not implemented yet")
        }
        jacobians.copyRowFrom(i, Ji)
      }
    }
    true
  }
}