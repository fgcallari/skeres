package org.somelightprojections.skeres.examples

import com.google.ceres._
import org.somelightprojections.skeres._

object PowellAnalytic {

  // f1 = x1 + 10 * x2;
  object F1a extends SizedCostFunction(1, 1, 1) {
    override def evaluate(
      parameters: DoublePointerPointer,
      residuals: DoublePointer,
      jacobians: DoublePointerPointer
    ): Boolean = {
      val x1 = parameters.get(0, 0)
      val x2 = parameters.get(1, 0)
      residuals.set(0, x1 + 10 * x2)
      if (!(jacobians.isNull || jacobians.getRow(0).isNull)) {
        jacobians.set(0, 0, 1)
        jacobians.set(0, 1, 10)
      }
      true
    }
  }

  // f2 = sqrt(5) (x3 - x4)
  object F2a  extends SizedCostFunction(1, 1, 1) {
    val s5 = math.sqrt(5)

    override def evaluate(
      parameters: DoublePointerPointer,
      residuals: DoublePointer,
      jacobians: DoublePointerPointer
    ): Boolean = {
      val x3 = parameters.get(0, 0)
      val x4 = parameters.get(1, 0)
      residuals.set(0, s5 * (x3 - x4))
      if (!(jacobians.isNull || jacobians.getRow(0).isNull)) {
        jacobians.set(0, 0, s5)
        jacobians.set(0, 1, -s5)
      }
      true
    }
  }

  // f3 = (x2 - 2 x3)^2
  object F3a  extends SizedCostFunction(1, 1, 1) {
    override def evaluate(
      parameters: DoublePointerPointer,
      residuals: DoublePointer,
      jacobians: DoublePointerPointer
    ): Boolean = {
      val x2 = parameters.get(0, 0)
      val x3 = parameters.get(1, 0)
      val d = x2 - 2.0 * x3
      residuals.set(0, d * d)
      if (!(jacobians.isNull || jacobians.getRow(0).isNull)) {
        jacobians.set(0, 0, 2.0 * d)
        jacobians.set(0, 1, -4.0 * d)
      }
      true
    }
  }

  // f4 = sqrt(10) (x1 - x4)^2
  object F4a extends SizedCostFunction(1, 1, 1) {
    val s10 = math.sqrt(10.0)

    override def evaluate(
      parameters: DoublePointerPointer,
      residuals: DoublePointer,
      jacobians: DoublePointerPointer
    ): Boolean = {
      val x1 = parameters.get(0, 0)
      val x4 = parameters.get(1, 0)
      val d = x1 - x4
      residuals.set(0, s10 * d * d)
      if (!(jacobians.isNull || jacobians.getRow(0).isNull)) {
        val f = s10 * 2 * d
        jacobians.set(0, 0, f)
        jacobians.set(0, 1, -f)
      }
      true
    }
  }

  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("Powell")
    val initialX = Vector(3.0, -1.0, 0.0, 1.0)

    val x1 = new DoubleArray(1)
    x1.set(0, initialX(0))
    val x2 = new DoubleArray(1)
    x2.set(0, initialX(1))
    val x3 = new DoubleArray(1)
    x3.set(0, initialX(2))
    val x4 = new DoubleArray(1)
    x4.set(0, initialX(3))

    val problemOptions = new Problem.Options
    problemOptions.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    problemOptions.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

    val loss = PredefinedLossFunctions.trivialLoss
    val problem = new Problem(problemOptions)
    problem.addResidualBlock(F1a, loss, x1, x2)
    problem.addResidualBlock(F2a, loss, x3, x4)
    problem.addResidualBlock(F3a, loss, x2, x3)
    problem.addResidualBlock(F4a, loss, x1, x4)

    val options = new Solver.Options
    options.setMinimizerType(MinimizerType.TRUST_REGION)
    options.setMinimizerProgressToStdout(true)
    options.setMaxNumIterations(100)
    options.setLinearSolverType(LinearSolverType.DENSE_QR)

    def stateStr(xs: Seq[Double]) =
      xs.zipWithIndex.map { case (xi, i) => s"x${i + 1} = $xi" }.mkString(", ")

    println(s"Initial: ${stateStr(initialX)}")

    val summary = new Solver.Summary

    ceres.solve(options, problem, summary)

    val xout = Vector(x1, x2, x3, x4).flatMap(_.toArray(1))
    println(summary.briefReport)
    println(s"Final: ${stateStr(xout)}")
  }
}
