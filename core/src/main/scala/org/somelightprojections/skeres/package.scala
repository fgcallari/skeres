package org.somelightprojections

import com.google.ceres._
import scala.language.implicitConversions
import spire.algebra.Order
import spire.math.Jet
import spire.implicits._

package object skeres {
  // Readable renaming of SWIG-generated types.
  type DoublePointerPointer = SWIGTYPE_p_p_double
  type DoublePointer = SWIGTYPE_p_double
  type ResidualBlockId = SWIGTYPE_p_ceres__internal__ResidualBlock

  // Implicit conversions between pointer objects and
  // arrays or matrices.
  implicit def doublePointerPointerToRichDoubleMatrix(p: DoublePointerPointer): RichDoubleMatrix =
    RichDoubleMatrix(p)

  implicit def doublePointerToRichDoubleArray(p: DoublePointer): RichDoubleArray =
    RichDoubleArray(DoubleArray.frompointer(p))

  implicit def doubleArraytoRichDoubleArray(a: DoubleArray): RichDoubleArray = RichDoubleArray(a)

  implicit val jetDoubleOrder = Order.by[Jet[Double], Double](_.real)
}


