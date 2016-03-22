package org.somelightprojections.skeres

import com.google.ceres.{NumericDiffOptions, NumericDiffMethodType}
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra.{Order, NRoot, Trig, Field}

/**
  * Abstract base class for functors that compute residuals (an possibly Jacobians) for the least
  * squares framework.
  *
  * Subclasses define an apply() method that takes the parameter blocks ordinately as
  * Array's, and returns the residuals in an Array.
  *
  * By convention, returning an empy Array indicates that the computation of the residuals
  * and/or jacobians was not successful.
  *
  * This can be used to communicate numerical failures in jacobian
  * computations for instance.
  *
  * A more interesting and common use is to impose constraints on the
  * parameters. If the initial values of the parameter blocks satisfy
  * the constraints, then returning an empty array whenever the constraints
  * are not satisfied will prevent the solver from moving into the
  * infeasible region. This is not a very sophisticated mechanism for
  * enforcing constraints, but is often good enough.
  *
  * @param kNumResiduals number of residuals, i.e. dimension of the functor output.
  * @param N sequence of integers, each ordinately equal to the dimension of a parameter block.
  */
abstract class CostFunctor(val kNumResiduals: Int, val N: Int*) {
  require(kNumResiduals > 0, s"Nonpositive number of residuals specified: $kNumResiduals")
  require(N.forall(_ > 0), s"Nonpositive parameter block sizes specified: ${N.mkString(", ")}")
}

/**
  * Auto-differentiable cost functor
  * See documentation for AutoDiffCostFunction.
  */
abstract class AutoDiffCostFunctor(kNumResiduals: Int, N: Int*)
  extends CostFunctor(kNumResiduals, N: _*) {

  /** Returns an auto-differentiable cost function computed through this functor. */
  def toAutoDiffCostFunction = AutoDiffCostFunction(this)

  /**
    * Evaluates the functor on a sequence of N.length parameter blocks, ordinately of N(k) size,
    * for k = 0, 1, ..., N.length.
    */
  def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T]
}

/**
  * Numerically-differentiable cost functor.
  * See documentation for NumericDiffCostFunction.
  */
abstract class NumericDiffCostFunctor(kNumResiduals: Int, N: Int*)
  extends CostFunctor(kNumResiduals, N: _*) {

  /** Returns a numerically differentiable cost function computed through this functor. */
  def toNumericDiffCostFunction(
    method: NumericDiffMethodType,
    options: Option[NumericDiffOptions] = None
  ) = NumericDiffCostFunction(method, options.getOrElse(new NumericDiffOptions), this)

  /**
    * Evaluates the functor on a sequence of N.length parameter blocks, ordinately of N(k) size,
    * for k = 0, 1, ..., N.length.
    */
  def apply(x: Array[Double]*): Array[Double]
}
