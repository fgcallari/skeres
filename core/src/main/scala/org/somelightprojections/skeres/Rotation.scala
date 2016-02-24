package org.somelightprojections.skeres

import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra.{Field, InnerProductSpace, NRoot, Order, Trig}
import spire.implicits._
import spire.math._

/**
 * Trivial wrapper to index linear arrays as matrices, given a fixed
 * column and row stride. When an "array: Array[T]" is wrapped by a
 *   m: MatrixAdapter[T]
 *
 * the expression  m(i, j) is equivalent to
 *   array[i * m.rowStride + j * m.colStride]
 *
 * Conversion functions to and from rotation matrices accept
 * MatrixAdapters to permit using row-major and column-major layouts,
 * and rotation matrices embedded in larger matrices (such as a 3x4
 * projection matrix).
*/
trait MatrixAdapter[@sp(Double) T] {
  def data: Array[T]
  def rowStride: Int
  def colStride: Int
  def numRows: Int = if (isRowMajor) data.length / rowStride else data.length / numCols
  def numCols: Int = if (isColMajor) data.length / colStride else data.length / numRows

  def isRowMajor = colStride == 1
  def isColMajor = rowStride == 1

  def apply(i: Int, j: Int): T = data(i * rowStride + j * colStride)
  def set(i: Int, j: Int, x: T): Unit = { data(i * rowStride + j * colStride) = x }

  override def toString(): String =
    s"MatrixAdapter(data=[${data.mkString(", ")}], rowStride=$rowStride, colStride=$colStride)"
}

trait RowMajorMatrixAdapter[@sp(Double) T] extends MatrixAdapter[T] {
  override val colStride = 1
}

trait ColumnMajorMatrixAdapter[@sp(Double) T] extends MatrixAdapter[T] {
  override val rowStride = 1
}

case class RowMajorMatrixAdapter3x3[@sp(Double) T](override val data: Array[T])
  extends RowMajorMatrixAdapter[T] {
  override val rowStride = 3
}

case class ColumnMajorMatrixAdapter3x3[@sp(Double) T](override val data: Array[T])
  extends ColumnMajorMatrixAdapter[T] {
  override val colStride = 3
}

/**
 * Manipulation utils for rapresentations of 3D rotations: quaternions, Rodriguez angle-axis
 * vectors and rotation matrices.
 * This is a straight port from the rotation.h header file of ceres-solver, except that we make
 * use of spire's Quaternion type where appropriate.
 */
object Rotation {
  /**
   * Convert a value in combined axis-angle representation to a quaternion.
   * The value angleAxis is a triple whose norm is an angle in radians,
   * and whose direction is aligned with the axis of rotation,
   * and the return value is a 4-length array that will contain the resulting quaternion.
   * The implementation may be used with auto-differentiation up to the first
   * derivative, higher derivatives may have unexpected results near the origin.
  */
  def angleAxisToQuaternion[@sp(Double) T: Field: Trig: Order: NRoot: ClassTag](
    angleAxis: Array[T]
  ): Quaternion[T] = {
    val a0 = angleAxis(0)
    val a1 = angleAxis(1)
    val a2 = angleAxis(2)
    val thetaSquared = a0 * a0 + a1 * a1 + a2 * a2
    val fld = implicitly[Field[T]]
    // For points not at the origin, the full conversion is numerically stable.
    if (thetaSquared > fld.zero) {
      val theta = sqrt(thetaSquared)
      val halfTheta = theta * 0.5
      val k = sin(halfTheta) / theta
      Quaternion(cos(halfTheta), a0 * k, a1 * k, a2 * k)
    } else {
      // At the origin, sqrt() will produce NaN in the derivative since
      // the argument is zero.  By approximating with a Taylor series,
      // and truncating at one term, the value and first derivatives will be
      // computed correctly when Jets are used.
      val k = 0.5
      Quaternion(fld.one, a0 * k, a1 * k, a2 * k)
    }
  }

  /**
   * Convert a quaternion to the equivalent combined axis-angle representation.
   * The value quaternion must be a unit quaternion - it is not normalized first,
   * and angle_axis will be filled with a value whose norm is the angle of
   * rotation in radians, and whose direction is the axis of rotation.
   * The implemention may be used with auto-differentiation up to the first
   * derivative, higher derivatives may have unexpected results near the origin.
   */
  def quaternionToAngleAxis[@sp(Double) T: Field: Trig: NRoot: Order: ClassTag](
    quaternion: Quaternion[T]
  ): Array[T] = {
    val q1 = quaternion.i
    val q2 = quaternion.j
    val q3 = quaternion.k
    val sinSquaredTheta = q1 * q1 + q2 * q2 + q3 * q3

    val fld = implicitly[Field[T]]
    val two = fld.fromDouble(2.0)

    // For quaternions representing non-zero rotation, the conversion
    // is numerically stable.
    if (sinSquaredTheta != fld.zero) {
      val sinTheta = sqrt(sinSquaredTheta)
      val cosTheta = quaternion.r

      // If cosTheta is negative, theta is greater than pi/2, which
      // means that angle for the angle_axis vector which is 2 * theta
      // would be greater than pi.
      //
      // While this will result in the correct rotation, it does not
      // result in a normalized angle-axis vector.
      //
      // In that case we observe that 2 * theta ~ 2 * theta - 2 * pi,
      // which is equivalent saying
      //
      //   theta - pi = atan(sin(theta - pi), cos(theta - pi))
      //              = atan(-sin(theta), -cos(theta))
      //
      val twoTheta =
        if (cosTheta < fld.zero) {
          two * atan2(-sinTheta, -cosTheta)
        } else {
          two * atan2(sinTheta, cosTheta)
        }
      val k = twoTheta / sinTheta
      Array(q1 * k, q2 * k, q3 * k)
    } else {
      // For zero rotation, sqrt() will produce NaN in the derivative since
      // the argument is zero.  By approximating with a Taylor series,
      // and truncating at one term, the value and first derivatives will be
      // computed correctly when Jets are used.
      Array(q1 * two, q2 * two, q3 * two)
    }
  }

  /**
   * Conversions between 3x3 rotation matrix (in column major order) and
   * quaternion rotation representations.  Templated for use with
   * autodifferentiation.
  */
  def rotationMatrixToQuaternion[T: Field: Trig: NRoot: Order: ClassTag](
    R: Array[T]
  ): Quaternion[T] = rotationMatrixToQuaternion(ColumnMajorMatrixAdapter3x3(R))

  // This algorithm comes from "Quaternion Calculus and Fast Animation",
  // Ken Shoemake, 1987 SIGGRAPH course notes
  def rotationMatrixToQuaternion[T: Field: Trig: NRoot: Order: ClassTag](
    R: MatrixAdapter[T]
  ): Quaternion[T] = {
    val fld = implicitly[Field[T]]
    val half = fld.fromDouble(0.5)
    val trace = R(0, 0) + R(1, 1) + R(2, 2)
    if (trace >= fld.zero) {
      var t = sqrt(trace + fld.one)
      val r = half * t
      t = half / t
      Quaternion(r, (R(2, 1) - R(1, 2)) * t, (R(0, 2) - R(2, 0)) * t, (R(1, 0) - R(0, 1)) * t)
    } else {
      var i: Int = 0
      if (R(1, 1) > R(0, 0)) i = 1
      if (R(2, 2) > R(i, i)) i = 2

      val j = (i + 1) % 3
      val k = (j + 1) % 3
      var t = sqrt(R(i, i) - R(j, j) - R(k, k) + fld.one)
      val q = Array.ofDim[T](4)
      q(i + 1) = half * t
      t = half / t
      q(0) = (R(k, j) - R(j, k)) * t
      q(j + 1) = (R(j, i) + R(i, j)) * t
      q(k + 1) = (R(k, i) + R(i, k)) * t
      Quaternion(q(0), q(1), q(2), q(3))
    }
  }

  /**
   * Conversions between 3x3 rotation matrix (in column major order) and axis-angle rotation
   * representations.  Templated for use with autodifferentiation.
   */
  def rotationMatrixToAngleAxis[T: Field: Trig: NRoot: Order: ClassTag](R: Array[T]): Array[T] =
    rotationMatrixToAngleAxis(ColumnMajorMatrixAdapter3x3(R))

  // The conversion of a rotation matrix to the angle-axis form is
  // numerically problematic when then rotation angle is close to zero
  // or to Pi. The following implementation detects when these two cases
  // occurs and deals with them by taking code paths that are guaranteed
  // to not perform division by a small number.
  def rotationMatrixToAngleAxis[T: Field: Trig: NRoot: Order: ClassTag](R: MatrixAdapter[T]): Array[T] =
    quaternionToAngleAxis(rotationMatrixToQuaternion(R))

  def angleAxisToRotationMatrix[T: Field: Trig: NRoot: Order: ClassTag](
    angleAxis: Array[T]
  ): MatrixAdapter[T] =
    angleAxisToRotationMatrix(angleAxis, ColumnMajorMatrixAdapter3x3(Array.ofDim[T](9)))

  private[skeres] def angleAxisToRotationMatrix[T: Field: Trig: NRoot: Order: ClassTag](
    angleAxis: Array[T],
    R: MatrixAdapter[T]
  )(
    implicit ips: InnerProductSpace[Array[T], T]
  ): MatrixAdapter[T] = {
    val fld = implicitly[Field[T]]
    val kOne = fld.one
    val eps = fld.fromDouble(math.ulp(1.0))
    val theta2 = ips.dot(angleAxis, angleAxis)
    if (theta2 > eps) {
      // We want to be careful to only evaluate the square root if the
      // norm of the angleAxis vector is greater than zero. Otherwise
      // we get a division by zero.
      val theta = sqrt(theta2)
      val wx = angleAxis(0) / theta
      val wy = angleAxis(1) / theta
      val wz = angleAxis(2) / theta

      val costheta = cos(theta)
      val sintheta = sin(theta)

      R.set(0, 0,       costheta   + wx*wx*(kOne -      costheta))
      R.set(1, 0,  wz * sintheta   + wx*wy*(kOne -      costheta))
      R.set(2, 0, -wy * sintheta   + wx*wz*(kOne -      costheta))
      R.set(0, 1,  wx * wy * (kOne - costheta)   - wz * sintheta )
      R.set(1, 1,       costheta   + wy*wy*(kOne -      costheta))
      R.set(2, 1,  wx * sintheta   + wy*wz*(kOne -      costheta))
      R.set(0, 2,  wy * sintheta   + wx*wz*(kOne -      costheta))
      R.set(1, 2, -wx * sintheta   + wy*wz*(kOne -      costheta))
      R.set(2, 2,       costheta   + wz*wz*(kOne -      costheta))
    } else {
      // Near zero, we switch to using the first order Taylor expansion.
      R.set(0, 0,  kOne)
      R.set(1, 0,  angleAxis(2))
      R.set(2, 0, -angleAxis(1))
      R.set(0, 1, -angleAxis(2))
      R.set(1, 1,  kOne)
      R.set(2, 1,  angleAxis(0))
      R.set(0, 2,  angleAxis(1))
      R.set(1, 2, -angleAxis(0))
      R.set(2, 2,  kOne)
    }
    R
  }

  /**
   * Conversions between 3x3 rotation matrix (in ROW MAJOR order) and
   * Euler angle (in degrees) rotation representations.
   * The {pitch,roll,yaw} Euler angles are rotations around the {x,y,z}
   * axes, respectively.  They are applied in that same order, so the
   * total rotation R is Rz * Ry * Rx.
   */
  def eulerAnglesToRotationMatrix[T: Field: Trig: ClassTag](
    euler: Array[T]
  ): MatrixAdapter[T] =
    eulerAnglesToRotationMatrix(euler, RowMajorMatrixAdapter3x3(Array.ofDim[T](9)))

  def eulerAnglesToRotationMatrix[T: Field: Trig: ClassTag](
    euler: Array[T],
    R: MatrixAdapter[T]
  ): MatrixAdapter[T] = {
    val kPi = math.Pi
    val degreesToRadians = kPi / 180.0

    val pitch = euler(0) * degreesToRadians
    val roll = euler(1) * degreesToRadians
    val yaw = euler(2) * degreesToRadians

    val c1 = cos(yaw)
    val s1 = sin(yaw)
    val c2 = cos(roll)
    val s2 = sin(roll)
    val c3 = cos(pitch)
    val s3 = sin(pitch)

    R.set(0, 0, c1*c2)
    R.set(0, 1, -s1*c3 + c1*s2*s3)
    R.set(0, 2, s1*s3 + c1*s2*c3)

    R.set(1, 0, s1*c2)
    R.set(1, 1, c1*c3 + s1*s2*s3)
    R.set(1, 2, -c1*s3 + s1*s2*c3)

    R.set(2, 0, -s2)
    R.set(2, 1, c2*s3)
    R.set(2, 2, c2*c3)
    R
  }

  /**
   * Convert a 4-vector to a 3x3 scaled rotation matrix.
   *
   * The choice of rotation is such that the quaternion [1 0 0 0] goes to an
   * identity matrix and for small a, b, c the quaternion [1 a b c] goes to
   * the matrix
   *
   *         [  0 -c  b ]
   *   I + 2 [  c  0 -a ] + higher order terms
   *         [ -b  a  0 ]
   *
   * which corresponds to a Rodrigues approximation, the last matrix being
   * the cross-product matrix of [a b c]. Together with the property that
   * R(q1 * q2) = R(q1) * R(q2) this uniquely defines the mapping from q to R.
   *
   * No normalization of the quaternion is performed, i.e.
   * R = ||q||^2 * Q, where Q is an orthonormal matrix
   * such that det(Q) = 1 and Q*Q' = I
   *
   * WARNING: The rotation matrix is ROW MAJOR
   */
  def quaternionToScaledRotation[T: Field: ClassTag](
    q: Quaternion[T])
  : MatrixAdapter[T] = quaternionToScaledRotation(q, RowMajorMatrixAdapter3x3(Array.ofDim[T](9)))

  def quaternionToScaledRotation[T: Field: ClassTag](
    q: Quaternion[T],
    R: MatrixAdapter[T]
  ): MatrixAdapter[T] = {
    val aa = q.r * q.r
    val ab = q.r * q.i
    val ac = q.r * q.j
    val ad = q.r * q.k
    val bb = q.i * q.i
    val bc = q.i * q.j
    val bd = q.i * q.k
    val cc = q.j * q.j
    val cd = q.j * q.k
    val dd = q.k * q.k
    val two = implicitly[Field[T]].fromDouble(2.0)

    R.set(0, 0, aa + bb - cc - dd)
    R.set(0, 1, two * (bc - ad))
    R.set(0, 2, two * (ac + bd))
    R.set(1, 0, two * (ad + bc))
    R.set(1, 1, aa - bb + cc - dd)
    R.set(1, 2, two * (cd - ab))
    R.set(2, 0, two * (bd - ac))
    R.set(2, 1, two * (ab + cd))
    R.set(2, 2, aa - bb - cc + dd)
    R
  }

  /**
   * Same as above except that the rotation matrix is normalized by the
   * Frobenius norm, so that R * R' = I (and det(R) = 1).
   *
   * WARNING: The rotation matrix is ROW MAJOR
   */
  def quaternionToRotation[T: Field: ClassTag](
    q: Quaternion[T]
  ): MatrixAdapter[T] = quaternionToRotation(q, RowMajorMatrixAdapter3x3(Array.ofDim[T](9)))

  def quaternionToRotation[T: Field: ClassTag](
    q: Quaternion[T],
    R: MatrixAdapter[T]
  ): MatrixAdapter[T] = {
    quaternionToScaledRotation(q, R)

    val norm = q.r * q.r + q.i * q.i + q.j * q.j + q.k * q.k
    val fld = implicitly[Field[T]]
    require(norm != fld.zero)
    val invNorm = fld.one / norm

    cforRange(0 until 3) { i =>
      cforRange(0 until 3) { j =>
        R.set(i, j, R(i, j) * invNorm)
      }
    }
    R
  }

  /**
   * Rotates a point pt by a quaternion q:
   *
   *   result = R(q) * pt
   *
   * Assumes the quaternion is unit norm. This assumption allows us to
   * write the transform as (something)*pt + pt, as is clear from the
   * formula below. If you pass in a quaternion with |q|^2 = 2 then you
   * WILL NOT get back 2 times the result you get for a unit quaternion.
   */
  def unitQuaternionRotatePoint[T: Field: ClassTag](
    q: Quaternion[T],
    pt: Array[T]
  ): Array[T] = {
    val t2 =  q.r * q.i
    val t3 =  q.r * q.j
    val t4 =  q.r * q.k
    val t5 = -q.i * q.i
    val t6 =  q.i * q.j
    val t7 =  q.i * q.k
    val t8 = -q.j * q.j
    val t9 =  q.j * q.k
    val t1 = -q.k * q.k
    val two = implicitly[Field[T]].fromDouble(2.0)
    Array(
      two * ((t8 + t1) * pt(0) + (t6 - t4) * pt(1) + (t3 + t7) * pt(2)) + pt(0),
      two * ((t4 + t6) * pt(0) + (t5 + t1) * pt(1) + (t9 - t2) * pt(2)) + pt(1),
      two * ((t7 - t3) * pt(0) + (t2 + t9) * pt(1) + (t5 + t8) * pt(2)) + pt(2)
    )
  }

  /**
   * Rotates a point pt by a quaternion q:
   *
   *   result = R(q) * pt
   *
   * With this function you do not need to assume that q has unit norm.
   * It does assume that the norm is non-zero.
   */
  def quaternionRotatePoint[T: Field: NRoot: ClassTag](
    q: Quaternion[T],
    pt: Array[T]
  ): Array[T] = {
    // 'scale' is 1 / norm(q).
    val fld = implicitly[Field[T]]
    val scale = fld.one / sqrt(q.r * q.r + q.i * q.i + q.j * q.j + q.k * q.k)
    // Make unit-norm version of q.
    val unit = q * scale
    unitQuaternionRotatePoint(unit, pt)
  }

  /** zw = z * w, where * is the Quaternion product between 4 vectors. */
  def quaternionProduct[T: Field: Trig: ClassTag](
    z: Quaternion[T],
    w: Quaternion[T]
  ) = z * w

  /** xy = x cross y */
  def crossProduct[T: Field: ClassTag](x: Array[T], y: Array[T]): Array[T] =
    Array(x(1) * y(2) - y(2) * x(1), x(2) * y(0) - x(0) * y(2), x(0) * y(1) - x(1) * y(0))

  /** xy = x dot y */
  def dotProduct[T: Field: ClassTag](x: Array[T], y: Array[T])(
    implicit ips: InnerProductSpace[Array[T], T]): T = ips.dot(x, y)

  /** y = R(angle_axis) * x */
  def angleAxisRotatePoint[T: Field: Trig: NRoot: Order: ClassTag](
    angleAxis: Array[T],
    pt: Array[T]
  )(implicit
    ips: InnerProductSpace[Array[T], T]
  ): Array[T] = {
    val fld = implicitly[Field[T]]
    val theta2 = dotProduct(angleAxis, angleAxis)
    val eps = fld.fromDouble(Math.ulp(1.0))
    if (theta2 > eps) {
      // Away from zero, use the rodriguez formula
      //
      //   result = pt cosTheta +
      //            (w x pt) * sinTheta +
      //            w (w . pt) (1 - cosTheta)
      //
      // We want to be careful to only evaluate the square root if the
      // norm of the angle_axis vector is greater than zero. Otherwise
      // we get a division by zero.
      //
      val theta = sqrt(theta2)
      val cosTheta = cos(theta)
      val sinTheta = sin(theta)
      val thetaInverse = 1.0 / theta

      val w = Array(
        angleAxis(0) * thetaInverse,
        angleAxis(1) * thetaInverse,
        angleAxis(2) * thetaInverse
      )

      // Explicitly inlined evaluation of the cross product for
      // performance reasons.
      val wCrossPt = Array(
        w(1) * pt(2) - w(2) * pt(1),
        w(2) * pt(0) - w(0) * pt(2),
        w(0) * pt(1) - w(1) * pt(0)
      )
      val tmp = (w(0) * pt(0) + w(1) * pt(1) + w(2) * pt(2)) * (fld.one - cosTheta)
      Array(
        pt(0) * cosTheta + wCrossPt(0) * sinTheta + w(0) * tmp,
        pt(1) * cosTheta + wCrossPt(1) * sinTheta + w(1) * tmp,
        pt(2) * cosTheta + wCrossPt(2) * sinTheta + w(2) * tmp
      )
    } else {
      // Near zero, the first order Taylor approximation of the rotation
      // matrix R corresponding to a vector w and angle w is
      //
      //   R = I + hat(w) * sin(theta)
      //
      // But sintheta ~ theta and theta * w = angle_axis, which gives us
      //
      //  R = I + hat(w)
      //
      // and actually performing multiplication with the point pt, gives us
      // R * pt = pt + w x pt.
      //
      // Switching to the Taylor expansion near zero provides meaningful
      // derivatives when evaluated using Jets.
      //
      // Explicitly inlined evaluation of the cross product for
      // performance reasons.
      val w_cross_pt = Array(
        angleAxis(1) * pt(2) - angleAxis(2) * pt(1),
        angleAxis(2) * pt(0) - angleAxis(0) * pt(2),
        angleAxis(0) * pt(1) - angleAxis(1) * pt(0)
      )
      Array(
        pt(0) + w_cross_pt(0),
        pt(1) + w_cross_pt(1),
        pt(2) + w_cross_pt(2)
      )
    }
  }
}
