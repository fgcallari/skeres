package org.somelightprojections.skeres.examples

import com.google.ceres.{NumericDiffMethodType, PredefinedLossFunctions, Solver, ceres}
import org.somelightprojections.skeres._

object HelloWorldNumericDiff {
  object HelloCostFunctor extends NumericDiffCostFunctor(1, 1) {
    override def apply(x: Array[Double]*): Array[Double] = Array(10.0 - x(0)(0))
  }

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("HelloWorld")
    val initialX = 0.5

    val x = RichDoubleArray.ofSize(1)
    x.set(0, initialX)

    val problem = new Problem

    val cost = HelloCostFunctor.toNumericDiffCostFunction(NumericDiffMethodType.CENTRAL)
    val loss = PredefinedLossFunctions.trivialLoss
    problem.addResidualBlock(cost, loss, x.toPointer)

    val solverOptions = new Solver.Options
    solverOptions.setMinimizerProgressToStdout(true)

    val summary = new Solver.Summary

    ceres.solve(solverOptions, problem, summary)

    println(summary.briefReport)
  }
}
