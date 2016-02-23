package org.somelightprojections.skeres

import org.scalatest._
import org.scalatest.matchers.{MatchResult, Matcher}
import scala.util.Random
import spire.implicits._
import spire.math._

//
// All unit tests from ceres-library's rotation_test.cc ported to Scala here to unit-test their
// re-implementation in oject Rotation.
//
class RotationSpec extends WordSpec with MustMatchers {
  val kPi = math.Pi
  val kHalfSqrt2 = 0.5 * sqrt(2)
  val kEpsilon = 2.2204460492503131e-016
  val kNumTrials = 10000

  // A tolerance value for floating-point comparisons.
  val kTolerance = 10 * kEpsilon
  // Looser tolerance used for numerically unstable conversions.
  val kLooseTolerance = 1e-9

  // Transposes the data of a 3x3 matrix adapter.
  def transpose3x3(m: Array[Double]): Array[Double] = {
    def swap(x: Array[Double], a: Int, b: Int): Unit = { val tmp = x(a); x(a) = x(b); x(b) = tmp }
    val t = Array(m: _*)
    swap(t, 1, 3)
    swap(t, 2, 6)
    swap(t, 5, 7)
    t
  }

  // Convert Euler angles from radians to degrees in place.
  def toDegrees(ea: Array[Double]): Array[Double] = ea.map(x => x * 180.0 / kPi)

  // Some predicates and matchers used in the tests below

  def isNormalizedQuaternion(left: Quaternion[Double]): Boolean =
    abs(1 - left.abs.pow(2)) <= kTolerance

  val beNormalizedQuaternion: Matcher[Quaternion[Double]] =
    Matcher { left: Quaternion[Double] =>
      MatchResult(
        isNormalizedQuaternion(left),
        s"$left is not normalized",
        s"$left is normalized"
      )
    }

  def isNearQuaternion(expected: Quaternion[Double], left: Quaternion[Double]): Boolean = {
    // Quaternions are equivalent upto a sign change. So we will compare
    // both signs before declaring failure.
    val qLeft = Vector(left.r, left.i, left.j, left.k)
    val qExpected = Vector(expected.r, expected.i, expected.j, expected.k)
    qLeft.zip(qExpected).forall { case (l, e) => abs(l - e) <= kTolerance } ||
    qLeft.zip(qExpected).forall { case (l, e) => abs(l + e) <= kTolerance }
  }

  val beNearQuaternion: (Quaternion[Double]) => Matcher[Quaternion[Double]] =
    (expected: Quaternion[Double]) => Matcher { left: Quaternion[Double] =>
      MatchResult(
        isNearQuaternion(expected, left),
        s"$left, $expected are not near",
        s"$left, $expected are near"
      )
    }

  def isNearAngleAxis(expected: Array[Double], left: Array[Double]): Boolean = {
    require(expected.length == 3)
    require(left.length == 3)
    val eNorm = expected.norm
    val deltaNorm =
      if (eNorm > 0) {
        // Deal with the sign ambiguity near PI. Since the sign can flip,
        // we take the smaller of the two differences.
        if (abs(eNorm - kPi) < kLooseTolerance) {
          math.min((left - expected).norm, (left + expected).norm) / eNorm
        } else {
          (left - expected).norm / eNorm
        }
      } else {
        left.norm
      }
    deltaNorm <= kLooseTolerance
  }

  val beNearAngleAxis: (Array[Double]) => Matcher[Array[Double]] =
    (expected: Array[Double]) => Matcher { left: Array[Double] =>
      MatchResult(
        isNearAngleAxis(expected, left),
        s"$left, $expected are not near",
        s"$left, $expected are near"
      )
    }

  def isAngleAxisNearEulerAngles(expected: Array[Double], left: Array[Double]): Boolean = {
    require(expected.length == 3)
    require(left.length == 3)
    val aaColMajor = Rotation.angleAxisToRotationMatrix(left)
    val aaRowMajor = RowMajorMatrixAdapter3x3(transpose3x3(aaColMajor.data))
    val eaMatrix = Rotation.eulerAnglesToRotationMatrix(expected)
    isOrthonormal(aaRowMajor) && isOrthonormal(eaMatrix) && isNear3x3Matrix(eaMatrix, aaRowMajor)
  }

  val beAngleAxisNearEulerAngles: (Array[Double]) => Matcher[Array[Double]] =
    (expected: Array[Double]) => Matcher { left: Array[Double] =>
      MatchResult(
        isAngleAxisNearEulerAngles(expected, left),
        s"$left, $expected are not near",
        s"$left, $expected are near"
      )
    }

  def isNear3x3Matrix(expected: MatrixAdapter[Double], left: MatrixAdapter[Double]): Boolean = {
    require(expected.numRows == 3)
    require(expected.numCols == 3)
    require(left.numRows == 3)
    require(left.numCols == 3)
    val diffs = for {
      i <- (0 until 3).toIterator
      j <- (0 until 3).toIterator
    } yield abs(left(i, j) - expected(i, j))
    diffs.forall(_ <= kTolerance)
  }

  val beNear3x3Matrix: (MatrixAdapter[Double]) => Matcher[MatrixAdapter[Double]] =
    (expected: MatrixAdapter[Double]) => Matcher { left: MatrixAdapter[Double] =>
      MatchResult(
        isNear3x3Matrix(expected, left),
        s"$left, $expected are not near",
        s"$left, $expected are near"
      )
    }

  def isOrthonormal(left: MatrixAdapter[Double]): Boolean = {
    require(left.numRows == 3)
    require(left.numCols == 3)
    val data = left.data
    val deltas = for {
      c1 <- (0 until 3).toIterator
      c2 <- (0 until 3).toIterator
    } yield {
      var v: Double = 0
      for (i <- 0 until 3) v += data(i + 3 * c1) * data(i + 3 * c2)
      val expected = if (c1 == c2) 1 else 0
      abs(expected - v)
    }
    deltas.forall(_ <= kTolerance)
  }

  val beOrthoNormal: Matcher[MatrixAdapter[Double]] = Matcher { left: MatrixAdapter[Double] =>
    MatchResult(
      isOrthonormal(left),
      s"$left is not orthonormal",
      s"$left is orthonormal"
    )
  }

  "Rotation" should {
    "transform a zero axis/angle to a real unit quaternion" in {
      val angleAxis = Array[Double](0, 0, 0)
      val quaternion = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(1)
      quaternion must beNormalizedQuaternion
      quaternion.isReal must be(true)
    }
    "transform axis/angle to quaternion for small angles" in {
      val theta = 1e-2
      val angleAxis = Array(theta, 0, 0)
      val quaternion = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(cos(theta / 2), sin(theta / 2), 0, 0)
      quaternion must beNormalizedQuaternion
      quaternion must beNearQuaternion(expected)
    }
    "transform axis/angle to quaternion conversion for very small angles" in {
      val theta = Double.MinPositiveValue.pow(0.75)
      val angleAxis = Array(theta, 0, 0)
      val quaternion = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(cos(theta / 2), sin(theta / 2), 0, 0)
      quaternion must beNormalizedQuaternion
      quaternion must beNearQuaternion(expected)
    }
    "transform a rotation by pi/2 around X to a quaternion" in {
      val angleAxis = Array(kPi / 2, 0, 0)
      val quaternion = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(kHalfSqrt2, kHalfSqrt2, 0, 0)
      quaternion must beNormalizedQuaternion
      quaternion must beNearQuaternion(expected)
    }
    "transform a real unit quaternion to a zero angle-axis" in {
      val quaternion = Quaternion[Double](1)
      val expected = Array[Double](0, 0, 0)
      Rotation.quaternionToAngleAxis(quaternion) must beNearAngleAxis(expected)
    }
    "transform a quaternion that rotates by pi about the Y axis to an angle-axis" in {
      val quaternion = Quaternion[Double](0, 0, 1, 0)
      val expected = Array(0, kPi, 0)
      Rotation.quaternionToAngleAxis(quaternion) must beNearAngleAxis(expected)
    }
    "transform a quaternion that rotates by pi/3 about the Z axis to an angle-axis" in {
      val quaternion = Quaternion(sqrt(3) / 2, 0, 0, 0.5)
      val expected = Array(0, 0, kPi / 3)
      Rotation.quaternionToAngleAxis(quaternion) must beNearAngleAxis(expected)
    }
    "transform exactly quaternions to angle-axis for small angles" in {
      val theta = 1e-2
      val quaternion = Quaternion(cos(theta / 2), sin(theta / 2), 0, 0)
      val expected = Array(theta, 0, 0)
      Rotation.quaternionToAngleAxis(quaternion) must beNearAngleAxis(expected)
    }
    "transform approximately quaternions to angle-axis for very small angles" in {
      val theta = Double.MinPositiveValue.pow(0.75)
      val quaternion = Quaternion(cos(theta / 2), sin(theta / 2), 0, 0)
      val expected = Array(theta, 0, 0)
      Rotation.quaternionToAngleAxis(quaternion) must beNearAngleAxis(expected)
    }
    "transform quaternions to angle-axis with angles no greater than pi" in {
      val halfTheta = 0.75 * kPi
      val quaternion = Quaternion(cos(halfTheta), sin(halfTheta), 0, 0)
      val angleAxis = Rotation.quaternionToAngleAxis(quaternion)
      val angle = angleAxis.norm
      angle must be <= kPi
    }
    "transform consistenly angle-axis => quaternion => angle-axis" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        // Make an axis by choosing three random numbers in [-1, 1) and
        // normalizing.
        val tmp = (0 until 3).map(_ => random.nextDouble() * 2 - 1).toArray
        val norm = tmp.norm
        val theta = kPi * (2 * random.nextDouble() - 1)
        val angleAxis = tmp :* (theta / norm)
        val quaternion = Rotation.angleAxisToQuaternion(angleAxis)
        quaternion must beNormalizedQuaternion
        val roundTrip = Rotation.quaternionToAngleAxis(quaternion)
        roundTrip must beNearAngleAxis(angleAxis)
      }
    }
    "transform consistenly  quaternion => angle-axis => quaternion" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        // Make a quaternion by choosing four random numbers in [-1, 1) and
        // normalizing.
        val tmp = (0 until 4).map(_ => random.nextDouble() * 2 - 1)
        val quaternion = Quaternion(tmp(0), tmp(1), tmp(2), tmp(3)) / tmp.norm
        quaternion must beNormalizedQuaternion
        val angleAxis = Rotation.quaternionToAngleAxis(quaternion)
        val roundTrip = Rotation.angleAxisToQuaternion(angleAxis)
        roundTrip must beNearQuaternion(quaternion)
      }
    }
    "transform a zero angle-axis to an identity rotation matrix" in {
      val angleAxis = Array[Double](0, 0, 0)
      val expected = ColumnMajorMatrixAdapter3x3(Array[Double](1, 0, 0, 0, 1, 0, 0, 0, 1))
      val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
      rotationMatrix must beOrthoNormal
      rotationMatrix must beNear3x3Matrix(expected)
    }
    "transform a near-zero angle-axis to an identity rotation matrix" in {
      val angleAxis = Array(1e-24, 2e-24, 3e-24)
      val expected = ColumnMajorMatrixAdapter3x3(Array[Double](1, 0, 0, 0, 1, 0, 0, 0, 1))
      val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
      rotationMatrix must beOrthoNormal
      rotationMatrix must beNear3x3Matrix(expected)
    }
    "transforms a rotation by pi/2 around X to a rotation matrix and back" in {
      val angleAxis = Array(kPi / 2, 0, 0)
      val expected = ColumnMajorMatrixAdapter3x3(
        Array[Double](1, 0, 0, 0, 0, 1, 0, -1, 0))
      val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
      rotationMatrix must beOrthoNormal
      rotationMatrix must beNear3x3Matrix(expected)
      val roundTrip = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
      roundTrip must beNearAngleAxis(angleAxis)
    }
    "transforms an angle-axis that rotates by pi about the Y axis to a rotation matrix and back" in {
      val angleAxis = Array(0, kPi, 0)
      val expected = ColumnMajorMatrixAdapter3x3(Array[Double](-1, 0, 0, 0, 1, 0, 0, 0, -1))
      val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
      rotationMatrix must beOrthoNormal
      rotationMatrix must beNear3x3Matrix(expected)
      val roundTrip = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
      roundTrip must beNearAngleAxis(angleAxis)
    }
    "transforms an angle-axis near pi to a rotation matrix and back" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        // Make an axis by choosing three random numbers in [-1, 1) and
        // normalizing.
        val tmp = (0 until 3).map(_ => random.nextDouble() * 2 - 1).toArray
        val norm = tmp.norm
        // Angle in [pi - kMaxSmallAngle, pi).
        val kMaxSmallAngle = 1e-8
        val theta = kPi - kMaxSmallAngle * random.nextDouble()
        val inAngleAxis = tmp :* (theta / norm)
        val rotationMatrix = Rotation.angleAxisToRotationMatrix(inAngleAxis)
        val outAngleAxis = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
        outAngleAxis must beNearAngleAxis(inAngleAxis)
      }
    }
    "transforms a rotation matrix to an angle-axis at pi to and back" in {
      // A rotation of kPi about the X axis;
      val inMatrix = ColumnMajorMatrixAdapter3x3(Array[Double](1, 0, 0, 0, -1, 0, 0, 0, -1))
      val angleAxis = Rotation.rotationMatrixToAngleAxis(inMatrix)
      val expected = Array(kPi, 0, 0)
      angleAxis must beNearAngleAxis(expected)
      val roundTrip = Rotation.angleAxisToRotationMatrix(angleAxis)
      roundTrip must beOrthoNormal
      roundTrip must beNear3x3Matrix(inMatrix)
    }
    "transforms an angle-axis that rotates by pi/3 about the Z axis to a rotation matrix" in {
      val angleAxis = Array(0, 0, kPi / 3)
      val expected = ColumnMajorMatrixAdapter3x3(
        Array(0.5, sqrt(3) / 2, 0, -sqrt(3) / 2, 0.5, 0, 0, 0, 1)
      )
      val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
      rotationMatrix must beOrthoNormal
      rotationMatrix must beNear3x3Matrix(expected)
      val roundTrip = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
      roundTrip must beNearAngleAxis(angleAxis)
    }
    "transform random angle-axis to rotation matrices and back consistently" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        val tmp = (0 until 3).map(_ => random.nextDouble() * 2 - 1).toArray
        val norm = tmp.norm
        // Angle in [-pi, pi).
        val theta = kPi * 2 * random.nextDouble() - kPi
        val angleAxis = tmp :* (theta / norm)
        val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
        rotationMatrix must beOrthoNormal
        val roundTrip = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
        roundTrip must beNearAngleAxis(angleAxis)
      }
    }
    "transform random angle-axis near zero to rotation matrices and back consistently" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        val tmp = (0 until 3).map(_ => random.nextDouble() * 2 - 1).toArray
        val norm = tmp.norm
        // Tiny theta.
        val theta = 1e-16 * (kPi * 2 * random.nextDouble() - kPi)
        val angleAxis = tmp :* (theta / norm)
        val rotationMatrix = Rotation.angleAxisToRotationMatrix(angleAxis)
        rotationMatrix must beOrthoNormal
        val roundTrip = Rotation.rotationMatrixToAngleAxis(rotationMatrix)
        roundTrip must beNearAngleAxis(angleAxis)
      }
    }
    "transform to rotation matrix from angle-axis and Euler angles consistently" in {
      for {
        x <- -1 to 1
        y <- -1 to 1
        z <- -1 to 1
        nonZeros = Seq(x, y, z).count(_ != 0)
        if nonZeros <= 1
      } {
        val angleAxis = Array[Double](x, y, z)
        val eulerAngles = toDegrees(Array[Double](x, y, z))
        angleAxis must beAngleAxisNearEulerAngles(eulerAngles)
      }
    }
    "transform random rotations specified as Euler angles to orthonormal rotation matrix" in {
      val random = new Random(5)
      for (i <- 1 to kNumTrials) {
        val ea = (0 until 3).map(_ => 360.0 * (random.nextDouble() * 2.0 - 1.0)).toArray
        val rotationMatrix = Rotation.eulerAnglesToRotationMatrix(ea)
        rotationMatrix must beOrthoNormal
      }
    }
  }
}
