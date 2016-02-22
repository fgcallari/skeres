package org.somelightprojections.skeres

import org.scalatest.WordSpec
import org.scalatest._
import Matchers._
import org.scalatest.matchers.{MatchResult, Matcher}
import spire.implicits._
import spire.math._
import spire.algebra._
import spire.syntax._

class RotationSpec extends WordSpec with MustMatchers {
  val kHalfSqrt2 = 0.5 * sqrt(2.0)
  val kEpsilon = 2.2204460492503131e-016

  // A tolerance value for floating-point comparisons.
  val kTolerance = 10.0 * kEpsilon
  // Looser tolerance used for numerically unstable conversions.
  val kLooseTolerance = 1.0e-9

  val beNormalizedQuaternion = Matcher { left: Quaternion[Double] =>
    val norm2 = left.r.pow(2) + left.i.pow(2) + left.j.pow(2) + left.k.pow(2)
    MatchResult(
      abs(norm2 - 1.0) <= kTolerance,
      s"$left is not normalized",
      s"$left is normalized"
    )
  }

  def beNearQuaternion(expected: Quaternion[Double]) = Matcher { left: Quaternion[Double] =>
    // Quaternions are equivalent upto a sign change. So we will compare
    // both signs before declaring failure.
    val qLeft = Vector(left.r, left.i, left.j, left.k)
    val qExpected = Vector(expected.r, expected.i, expected.j, expected.k)
    MatchResult(
      qLeft.zip(qExpected).forall { case (l, e) => abs(l - e) <= kTolerance } ||
      qLeft.zip(qExpected).forall { case (l, e) => abs(l + e) <= kTolerance },
      s"$left, $expected are not near",
      s"$left, $expected are near"
    )
  }

  def beNearAngleAxis(expected: Array[Double]) = Matcher { left: Array[Double] =>
    require(expected.length == 3)
    require(left.length == 3)
    val eNorm = expected.norm
    val deltaNorm =
      if (eNorm > 0) {
        // Deal with the sign ambiguity near PI. Since the sign can flip,
        // we take the smaller of the two differences.
        if (abs(eNorm - math.Pi) < kLooseTolerance) {
          math.min((left - expected).norm, (left + expected).norm) / eNorm
        } else {
          (left - expected).norm / eNorm
        }
      } else {
        left.norm
      }

    MatchResult(
      deltaNorm <= kLooseTolerance,
      s"$left, $expected are not near",
      s"$left, $expected are near"
    )
  }

  "Rotation" should {
    "transform a zero axis/angle to a quaternion" in {
      val angleAxis = Array.fill(3)(0.0)
      val quat = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(1.0)
      quat must beNormalizedQuaternion
      quat must beNearQuaternion(expected)
    }
    "convert axis/angle to quaternion for small angles" in {
      val theta = 1.0e-2
      val angleAxis = Array(theta, 0.0, 0.0)
      val quat = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(cos(theta / 2), sin(theta / 2), 0.0, 0.0)
      quat must beNormalizedQuaternion
      quat must beNearQuaternion(expected)
    }
    "convert axis/angle to quaternion conversion for very small angles" in {
      val theta = Double.MinPositiveValue.pow(0.75)
      val angleAxis = Array(theta, 0.0, 0.0)
      val quat = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(cos(theta / 2), sin(theta / 2), 0.0, 0.0)
      quat must beNormalizedQuaternion
      quat must beNearQuaternion(expected)
    }
    "transform a rotation by pi/2 around X to a quaternion" in {
      val angleAxis = Array(math.Pi / 2, 0.0, 0.0)
      val quat = Rotation.angleAxisToQuaternion(angleAxis)
      val expected = Quaternion(kHalfSqrt2, kHalfSqrt2, 0.0, 0.0)
      quat must beNormalizedQuaternion
      quat must beNearQuaternion(expected)
    }
    "transform a unit quaternion to an axis angle" in {
      val quat = Quaternion(1.0)
      Rotation.quaternionToAngleAxis(quat) must beNearAngleAxis(Array(0.0, 0.0, 0.0))
    }
    "transform a quaternion that rotates by pi about the Y axis to an axis angle" in {
      val quat = Quaternion(0.0, 0.0, 1.0, 0.0)
      val expected = Array(0.0, math.Pi, 0.0)
    }
    "transform a quaternion that rotates by pi/3 about the Z axis to an axis angle" in {
      val quat = Quaternion(sqrt(3.0) / 2, 0, 0, 0.5)
      val expected = Array(0.0, 0.0, math.Pi / 3)
      Rotation.quaternionToAngleAxis(quat) must beNearAngleAxis(expected)
    }
    "exact quaternionToAngleAxis works for small angles" in {
      val theta = 1.0e-2
      val quat = Quaternion(cos(theta/2), sin(theta/2.0), 0, 0)
      val expected = Array(theta, 0.0, 0.0)
      Rotation.quaternionToAngleAxis(quat) must beNearAngleAxis(expected)
    }
    "approximate quaternionToAngleAxis works for very small angles" in {
      val theta = Double.MinPositiveValue.pow(0.75)
      val quat = Quaternion(cos(theta/2), sin(theta/2.0), 0, 0)
      val expected = Array(theta, 0.0, 0.0)
      Rotation.quaternionToAngleAxis(quat) must beNearAngleAxis(expected)
    }
  }
}
