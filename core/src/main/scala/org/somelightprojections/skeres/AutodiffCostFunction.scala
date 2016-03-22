package org.somelightprojections.skeres

import com.google.ceres.DoubleArray
import spire.implicits._
import spire.math.{Jet, JetDim}

/**
  * A CostFunction with Jacobians computed via automatic differentiation. For more
  * information on automatic differentation, see the wikipedia article
  * at http://en.wikipedia.org/wiki/Automatic_differentiation
  *
  * To get an auto differentiated cost function, you must define an AutoDiffCostFunctor with
  * an override apply() method parametrized on a type T as:
  *   def apply[@specialized(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T].
  *
  * The autodiff framework substitutes appropriate "jet" objects for T in order to compute the
  * derivative when necessary, but this is hidden, and you should write the function as if T
  * were a scalar type (e.g. a double-precision floating point number).
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
  * To write an auto-differentiable cost function for the above model, first
  * define the object
  *
  *   // Dimension of y ----------------------------------------------------------+
  *   // Dimension of x -------------------------------------------------------+  |
  *   // Dimension of residual ---------------------------------------------+  |  |
  *   //                                                                    |  |  |
  *   case class MyScalarCostFunctor(k: Double) extends AutoDiffCostFunctor(1, 2, 2) {
  *     // Compute the residual: parameterBlocks are ordinately x, y.
  *     override def apply[@specialized(Double) T: Field: Trig: NRoot: Order: ClassTag](
  *       parameterBlocks: Array[T]*
  *     ): Array[T] = {
  *       val x = parameterBlocks(0)
  *       val y = parameterBlocks(1)
  *       Array(k - x(0) * y(0) + x(1) * y(1))
  *     }
  *   }
  *
  *
  * In the above declaration, the parent class parameters, "(1, 2, 2)", describe the functor
  * as computing a 1-dimensional output from two arguments, both 2-dimensional.
  * Since the residual is a scalar, the output Array of apply() has only one component.
  *
  * Given this class definition, the auto differentiated cost function for
  * it can be constructed as follows.
  *
  *   val costFunction: CostFunction = MyScalarCostFunctor(k = 1.0).toAutoDiffCostFunction
  *
  * WARNING #1: Since the functor will get instantiated with different types for
  * T, you must to convert from other numeric types to T before mixing
  * computations with other variables of type T. Import spire.implicits._ so that spire
  * can take care of this for you for all common scalar types.
  */
case class AutoDiffCostFunction(costFunctor: AutoDiffCostFunctor)
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
        cforRange(0 until kNumResiduals)(r => residuals.set(r, jy(r).real))

        var parBlockOffset = 0
        cforRange(0 until numParameterBlocks) { i =>
          val ni = costFunctor.N(i)
          if (jacobians.hasRow(i)) {
            val jacobianRow: DoubleArray = jacobians.getRow(i)
            var col = 0
            cforRange(0 until kNumResiduals) { r =>
              val derivatives: Array[Double] = jy(r).infinitesimal
              cforRange(0 until ni) { p =>
                jacobianRow.set(col, derivatives(parBlockOffset + p))
                col += 1
              }
            }
          }
          parBlockOffset += ni
        }
        true
      }
    }
  }
}
