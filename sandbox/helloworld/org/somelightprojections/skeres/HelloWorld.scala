package org.somelightprojections.skeres

import com.google.ceres._
import skeres._

// Concrete cost functor: cost(x) = 10.0 - x
class CostFunctor extends CostFunction {
  mutableParameterBlockSizes.add(1)
  setNumResiduals(1)

  override def evaluate(
    parameters: DoublePointerPointer,
    residuals: DoublePointer,
    jacobians: DoublePointerPointer
  ): Boolean = {
    val x = parameters.get(0, 0)
    residuals.set(0, 10.0 - x)
    if (!BlockMatrix.isNull(jacobians)) {
      jacobians.set(0, 0, -1.0)
    }
    true
  }
}

object HelloWorld {
  val numRuns = 1

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("HelloWorld")
    val initialX = 0.5
    (1 to numRuns).foreach {_ =>
      val x = solve(initialX)
      println(s"x : $initialX -> ${x(0)}")
    }
  }

  def solve(initialX: Double): Vector[Double] = {
    val x = new DoubleArray(1)
    x.set(0, initialX)

    val problemOptions = new Problem.Options
    problemOptions.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    problemOptions.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

    val problem = new Problem(problemOptions)

    val cost = new CostFunctor
    val loss = PredefinedLossFunctions.trivialLoss
    problem.addResidualBlock(cost, loss, x.toPointer)

    val solverOptions = new Solver.Options
    solverOptions.setMinimizerProgressToStdout(true)

    val summary = new Solver.Summary

    ceres.solve(solverOptions, problem, summary)

    println(summary.briefReport)
    x.toArray(1).toVector
  }
}
