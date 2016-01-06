package org.somelightprojections.skeres
import ceres._
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
  // Load the swig-wrapped C++ DLL.
  System.loadLibrary("skeres");

  val numRuns = 1

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("HelloWorld")
    (1 to numRuns).foreach {_ => 
      val x = solve
    }
  }

  def solve: Vector[Double] = {
    val initialX = 0.5
    val x = new DoubleArray(1)
    x.set(0, initialX)

    val problemOptions = new Problem.Options
    problemOptions.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    problemOptions.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

    val problem = new Problem(problemOptions)

    val cost = new CostFunctor
    problem.addResidualBlock(cost, NullLossFunction, x.toPointer)

    val solverOptions = new Solver.Options
    solverOptions.setMinimizerProgressToStdout(true)

    val summary = new Solver.Summary

    ceres.solve(solverOptions, problem, summary)

    println(summary.briefReport)
    println(s"x : $initialX -> ${x.get(0)}")
    x.toArray(1).toVector
  }
}
