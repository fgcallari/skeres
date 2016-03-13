package org.somelightprojections.skeres.examples

import com.google.ceres._
import org.somelightprojections.skeres._
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra._
import spire.implicits._
import spire.math._

object Powell {

  // f1 = x1 + 10 * x2;
  object F1 extends CostFunctor(1, 1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] = {
      val x1 = x(0)
      val x2 = x(1)
      val y = x1(0) + 10.0 * x2(0)
      Array(y)
    }
  }

  // f2 = sqrt(5) (x3 - x4)
  object F2 extends CostFunctor(1, 1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] = {
      val x3 = x(0)
      val x4 = x(1)
      val y = sqrt(5.0) * x3(0) - x4(0)
      Array(y)
    }
  }

  // f3 = (x2 - 2 x3)^2
  object F3 extends CostFunctor(1, 1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] = {
      val x2 = x(0)
      val x3 = x(1)
      val d = x2(0) - 2.0 * x3(0)
      val y = d * d
      Array(y)
    }
  }

  // f4 = sqrt(10) (x1 - x4)^2
  object F4 extends CostFunctor(1, 1, 1) {
    override def apply[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](x: Array[T]*): Array[T] = {
      val x1 = x(0)
      val x4 = x(1)
      val d = x1(0) - x4(0)
      val y = sqrt(10) * d * d
      Array(y)
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

    val loss = PredefinedLossFunctions.trivialLoss
    val problem = new Problem

    problem.addResidualBlock(F1.toAutodiffCostFunction, loss, x1.toPointer, x2.toPointer)
    problem.addResidualBlock(F2.toAutodiffCostFunction, loss, x3.toPointer, x4.toPointer)
    problem.addResidualBlock(F3.toAutodiffCostFunction, loss, x2.toPointer, x3.toPointer)
    problem.addResidualBlock(F4.toAutodiffCostFunction, loss, x1.toPointer, x4.toPointer)

    val options = new Solver.Options
    options.setMinimizerType(MinimizerType.TRUST_REGION)
    options.setMinimizerProgressToStdout(true)
    options.setMaxNumIterations(100)
    options.setLinearSolverType(LinearSolverType.DENSE_QR)

    println(s"Initial: ${initialX.mkString(", ")}")

    val summary = new Solver.Summary

    ceres.solve(options, problem, summary)

    val xout = Vector(x1, x2, x3, x4).flatMap(_.toArray(1))
    println(summary.briefReport)
    println(s"Final: ${xout.map(x => "%.3g".format(x)).mkString(", ")}")
  }
}
