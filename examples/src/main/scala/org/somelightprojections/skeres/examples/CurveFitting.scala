package org.somelightprojections.skeres.examples

import com.google.ceres._
import org.somelightprojections.skeres._
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra.{Order, NRoot, Field, Trig}
import spire.implicits._
import spire.math._

// Data generated using the following octave code.
//   randn('seed', 23497)
//   m = 0.3
//   c = 0.1
//   x=[0:0.075:5]
//   y = exp(m * x + c)
//   noise = randn(size(x)) * 0.2
//   y_observed = y + noise
//   data = [x', y_observed']

object CurveFitting {
  val Data = Vector(
    0.000000e+00, 1.133898e+00,
    7.500000e-02, 1.334902e+00,
    1.500000e-01, 1.213546e+00,
    2.250000e-01, 1.252016e+00,
    3.000000e-01, 1.392265e+00,
    3.750000e-01, 1.314458e+00,
    4.500000e-01, 1.472541e+00,
    5.250000e-01, 1.536218e+00,
    6.000000e-01, 1.355679e+00,
    6.750000e-01, 1.463566e+00,
    7.500000e-01, 1.490201e+00,
    8.250000e-01, 1.658699e+00,
    9.000000e-01, 1.067574e+00,
    9.750000e-01, 1.464629e+00,
    1.050000e+00, 1.402653e+00,
    1.125000e+00, 1.713141e+00,
    1.200000e+00, 1.527021e+00,
    1.275000e+00, 1.702632e+00,
    1.350000e+00, 1.423899e+00,
    1.425000e+00, 1.543078e+00,
    1.500000e+00, 1.664015e+00,
    1.575000e+00, 1.732484e+00,
    1.650000e+00, 1.543296e+00,
    1.725000e+00, 1.959523e+00,
    1.800000e+00, 1.685132e+00,
    1.875000e+00, 1.951791e+00,
    1.950000e+00, 2.095346e+00,
    2.025000e+00, 2.361460e+00,
    2.100000e+00, 2.169119e+00,
    2.175000e+00, 2.061745e+00,
    2.250000e+00, 2.178641e+00,
    2.325000e+00, 2.104346e+00,
    2.400000e+00, 2.584470e+00,
    2.475000e+00, 1.914158e+00,
    2.550000e+00, 2.368375e+00,
    2.625000e+00, 2.686125e+00,
    2.700000e+00, 2.712395e+00,
    2.775000e+00, 2.499511e+00,
    2.850000e+00, 2.558897e+00,
    2.925000e+00, 2.309154e+00,
    3.000000e+00, 2.869503e+00,
    3.075000e+00, 3.116645e+00,
    3.150000e+00, 3.094907e+00,
    3.225000e+00, 2.471759e+00,
    3.300000e+00, 3.017131e+00,
    3.375000e+00, 3.232381e+00,
    3.450000e+00, 2.944596e+00,
    3.525000e+00, 3.385343e+00,
    3.600000e+00, 3.199826e+00,
    3.675000e+00, 3.423039e+00,
    3.750000e+00, 3.621552e+00,
    3.825000e+00, 3.559255e+00,
    3.900000e+00, 3.530713e+00,
    3.975000e+00, 3.561766e+00,
    4.050000e+00, 3.544574e+00,
    4.125000e+00, 3.867945e+00,
    4.200000e+00, 4.049776e+00,
    4.275000e+00, 3.885601e+00,
    4.350000e+00, 4.110505e+00,
    4.425000e+00, 4.345320e+00,
    4.500000e+00, 4.161241e+00,
    4.575000e+00, 4.363407e+00,
    4.650000e+00, 4.161576e+00,
    4.725000e+00, 4.619728e+00,
    4.800000e+00, 4.737410e+00,
    4.875000e+00, 4.727863e+00,
    4.950000e+00, 4.669206e+00
  )

  case class ExponentialResidual(x: Double, y: Double) extends CostFunctor(1, 1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](p: Array[T]*): Array[T] = {
      val m = p(0)
      val c = p(1)
      Array(y - exp(m(0) * x + c(0)))
    }
  }

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("CurveFitting")

    val m = new DoubleArray(1)
    m.set(0, 0.0)
    val c = new DoubleArray(1)
    c.set(0, 0.0)

    val loss = PredefinedLossFunctions.trivialLoss

    val problemOptions = new Problem.Options
    problemOptions.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    problemOptions.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

    val problem = new Problem(problemOptions)
    Data.grouped(2).foreach { case Vector(x, y) =>
      problem.addResidualBlock(ExponentialResidual(x, y).toAutodiffCostFunction, loss, m, c)
    }

    val options = new Solver.Options
    options.setMaxNumIterations(25)
    options.setLinearSolverType(LinearSolverType.DENSE_QR)
    options.setMinimizerProgressToStdout(true)

    println(s"Initial: 0.0, 0.0")

    val summary = new Solver.Summary
    ceres.solve(options, problem, summary)

    val finalX = Vector(m, c).flatMap(_.toArray(1))

    println(summary.briefReport())
    println(s"Final: ${finalX.mkString(", ")}")
  }
}
