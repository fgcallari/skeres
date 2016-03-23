package org.somelightprojections.skeres

import com.google.ceres.{CostFunction, NumericDiffMethodType}
import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.{MustMatchers, WordSpec}
import spire.implicits._
import spire.math._

// This is a port to Scala of numeric_diff_cost_function_test.cc and numeric_diff_test_util.cc
// from the ceres-solver source distribution
//
object NumericDiffCostFunctionSpec {
  // Noise factor for randomized cost function.
  val kNoiseFactor = 0.01

  // Default random seed for randomized cost function.
  val kRandomSeed = 1234

  trait TestFunctor {
    def nearlyCorrect(method: NumericDiffMethodType): BeMatcher[CostFunction]
  }

  object EasyFunctor extends TestFunctor {
    private def failOrNone(e: => Boolean, fail: String, success: String): Option[MatchResult] =
      if (e) None else Some(MatchResult(e, fail, success))

    override def nearlyCorrect(method: NumericDiffMethodType) = new BeMatcher[CostFunction] {
      import NumericDiffMethodType._

      override def apply(left: CostFunction): MatchResult = {
        import TestUtil._

        // The x1[0] is made deliberately small to test the performance near
        // zero.
        val x1 = Array(1e-64, 2.0, 3.0, 4.0, 5.0)
        val x2 = Array(9.0, 9.0, 5.0, 5.0, 1.0)
        val parameters = RichDoubleMatrix.fromArrays(x1, x2)

        val jacobians = RichDoubleMatrix.ofSize(2, 15)

        val evaluatedResiduals = RichDoubleArray.ofSize(3)
        if (!left.evaluate(parameters, evaluatedResiduals, jacobians)) {
          MatchResult(false, "evaluate failed", "evaluate did not fail")
        } else {
          val residuals = evaluatedResiduals.toArray(3)
          val functor = new EasyFunctor
          val expectedResiduals = functor(x1, x2)
          failOrNone(expectedResiduals.zip(residuals).forall(p => p._1 == p._2),
            "inconsistent residuals", "consistent residuals"
          ).getOrElse {
            val tolerance = method match {
              case FORWARD => 2e-5
              case RIDDERS => 1e-13
              case CENTRAL | _ => 3e-9
            }

            val dydx1 = jacobians.getRow(0).toArray(15) // 3 x 5, row major.
            val dydx2 = jacobians.getRow(1).toArray(15) // 3 x 5, row major.

            MatchResult(
              (0 until 5).forall { i =>
                expectClose(x2(i),                    dydx1(5 * 0 + i), tolerance) &&
                expectClose(x1(i),                    dydx2(5 * 0 + i), tolerance) &&
                expectClose(2 * x2(i) * residuals(0), dydx1(5 * 1 + i), tolerance) &&
                expectClose(2 * x1(i) * residuals(0), dydx2(5 * 1 + i), tolerance) &&
                expectClose(0.0,                      dydx1(5 * 2 + i), tolerance) &&
                expectClose(2 * x2(i),                dydx2(5 * 2 + i), tolerance)
              },
              "inconsistent values", "consistent values"
            )
          }
        }
      }
    }
  }

  // y1 = x1'x2      -> dy1/dx1 = x2,               dy1/dx2 = x1
  // y2 = (x1'x2)^2  -> dy2/dx1 = 2 * x2 * (x1'x2), dy2/dx2 = 2 * x1 * (x1'x2)
  // y3 = x2'x2      -> dy3/dx1 = 0,                dy3/dx2 = 2 * x2
  class EasyFunctor extends NumericDiffCostFunctor(3, 5, 5) {
    override def apply(x: Array[Double]*): Array[Double] = {
      require(x.length == 2)
      require(x(0).length == 5)
      require(x(1).length == 5)
      val x1 = x(0)
      val x2 = x(1)
      val y = Array.fill[Double](3)(0.0)
      cforRange(0 until 5) { i =>
        y(0) += x1(i) * x2(i)
        y(2) += x2(i) * x2(i)
      }
      y(1) = y(0) * y(0)
      y
    }
  }

  object TranscendentalFunctor extends TestFunctor {
    private val kTests = Array(
      (Array(1.0, 2.0, 3.0, 4.0, 5.0), Array(9.0, 9.0, 5.0, 5.0, 1.0)), // No zeros.
      (Array(0.0, 2.0, 3.0, 0.0, 5.0), Array(9.0, 9.0, 5.0, 5.0, 1.0)), // Some zeros x1.
      (Array(1.0, 2.0, 3.0, 1.0, 5.0), Array(0.0, 9.0, 0.0, 5.0, 0.0)), // Some zeros x2.
      (Array(0.0, 0.0, 0.0, 0.0, 0.0), Array(9.0, 9.0, 5.0, 5.0, 1.0)), // All zeros x1.
      (Array(1.0, 2.0, 3.0, 4.0, 5.0), Array(0.0, 0.0, 0.0, 0.0, 0.0)), // All zeros x2.
      (Array(0.0, 0.0, 0.0, 0.0, 0.0), Array(0.0, 0.0, 0.0, 0.0, 0.0))  // All zeros.
    )

    override def nearlyCorrect(method: NumericDiffMethodType) = new BeMatcher[CostFunction] {
      import NumericDiffMethodType._
      import TestUtil._

      private val tolerance = method match {
        case RIDDERS => /* 3.0e-12 */ throw new IllegalArgumentException("RIDDERS not implemented yet")
        case FORWARD => 2.0e-5
        case CENTRAL | _ => 2.0e-7
      }

      private def runOneTest(costFunction: CostFunction, testIndex: Int): MatchResult = {
        val testData = kTests(testIndex)
        val x1 = testData._1
        val x2 = testData._2
        val parameters = RichDoubleMatrix.fromArrays(x1, x2)
        val jacobians = RichDoubleMatrix.ofSize(2, 10)
        val residuals = RichDoubleArray.ofSize(2)
        if (!costFunction.evaluate(parameters, residuals.toPointer, jacobians)) {
          MatchResult(
            false, s"cost function eval failed on example $testIndex", "cost function eval succeeded"
          )
        } else {
          val dydx1 = jacobians.getRow(0).toArray(10)
          val dydx2 = jacobians.getRow(1).toArray(10)
          val x1x2 = x1.zip(x2).map{ case (a, b) => a * b }.sum
          MatchResult(
            (0 until 5).forall { j =>
              expectClose( x2(j) * cos(x1x2), dydx1(5 * 0 + j), tolerance) &&
              expectClose( x1(j) * cos(x1x2), dydx2(5 * 0 + j), tolerance) &&
              expectClose(-x2(j) * exp(-x1x2 / 10.0) / 10.0, dydx1(5 * 1 + j), tolerance) &&
              expectClose(-x1(j) * exp(-x1x2 / 10.0) / 10.0, dydx2(5 * 1 + j), tolerance)
            },
            s"inconsistent jacobians on example $testIndex", "consistent jacobians"
          )
        }
      }

      override def apply(left: CostFunction): MatchResult = {
        val success = MatchResult(
          true, "failure on transcendental functor", "pass on transcendental functor "
        )
        kTests.indices.foldLeft(success) { case (m, i) =>
          if (m.matches) runOneTest(left, i) else m
        }
      }
    }
  }

  // y1 = sin(x1'x2)
  // y2 = exp(-x1'x2 / 10)
  //
  // dy1/dx1 =  x2 * cos(x1'x2),            dy1/dx2 =  x1 * cos(x1'x2)
  // dy2/dx1 = -x2 * exp(-x1'x2 / 10) / 10, dy2/dx2 = -x2 * exp(-x1'x2 / 10) / 10
  class TranscendentalFunctor extends NumericDiffCostFunctor(2, 5, 5) {
    override def apply(x: Array[Double]*): Array[Double] = {
      require(x.length == 2)
      require(x(0).length == 5)
      require(x(1).length == 5)
      val x1 = x(0)
      val x2 = x(1)
      var x1x2 = 0.0
      cforRange(0 until 5) { i =>
        x1x2 += x1(i) * x2(i)
      }
      Array(
        sin(x1x2),
        exp(-x1x2 / 10)
      )
    }
  }
}

class NumericDiffCostFunctionSpec extends WordSpec with MustMatchers {
  import NumericDiffMethodType._

  "NumericDiffCostFunction" when {
    "in the easy case" should {
      import NumericDiffCostFunctionSpec.EasyFunctor
      import EasyFunctor.nearlyCorrect
      "be nearly correct for FORWARD differences" in {
        val costFunction = new EasyFunctor().toNumericDiffCostFunction(FORWARD)
        costFunction must be(nearlyCorrect(FORWARD))
      }
      "be nearly correct for CENTRAL differences" in {
        val costFunction = new EasyFunctor().toNumericDiffCostFunction(CENTRAL)
        costFunction must be(nearlyCorrect(CENTRAL))
      }
    }
    "in the transcendental case" should {
      import NumericDiffCostFunctionSpec.TranscendentalFunctor
      import TranscendentalFunctor.nearlyCorrect
      "be nearly correct for FORWARD differences" in {
        val costFunction = new TranscendentalFunctor().toNumericDiffCostFunction(FORWARD)
        costFunction must be(nearlyCorrect(FORWARD))
      }
      "be nearly correct for CENTRAL differences" in {
        val costFunction = new TranscendentalFunctor().toNumericDiffCostFunction(CENTRAL)
        costFunction must be(nearlyCorrect(CENTRAL))
      }
    }
  }
}