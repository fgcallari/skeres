package org.somelightprojections.skeres.examples

import com.google.ceres._
import org.somelightprojections.skeres._
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra._
import spire.implicits._

object HelloWorld {
  object HelloCostFunctor extends CostFunctor(1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] =
      Array(10.0 - x(0)(0))
  }

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("HelloWorld")
    val initialX = 0.5

    val x = new DoubleArray(1)
    x.set(0, initialX)

    val problemOptions = new Problem.Options
    problemOptions.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    problemOptions.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

    val problem = new Problem(problemOptions)

    val cost = HelloCostFunctor.toAutodiffCostFunction
    val loss = PredefinedLossFunctions.trivialLoss
    problem.addResidualBlock(cost, loss, x)

    val solverOptions = new Solver.Options
    solverOptions.setMinimizerProgressToStdout(true)

    val summary = new Solver.Summary

    ceres.solve(solverOptions, problem, summary)

    println(summary.briefReport)
  }
}
