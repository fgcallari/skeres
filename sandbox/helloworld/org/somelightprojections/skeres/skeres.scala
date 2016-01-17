package org.somelightprojections.skeres

import com.google.ceres._

package object skeres {
  // Readable renaming of SWIG-generated types.
  type DoublePointerPointer = SWIGTYPE_p_p_double
  type DoublePointer = SWIGTYPE_p_double

  // Implicit conversions betwee pointer objects and
  // arrays or matrices.
  implicit def doublePointerPointerToRichDoubleMatrix(p: DoublePointerPointer) =
    RichDoubleMatrix(p)

  implicit def doublePointerToRichDoubleArray(p: DoublePointer) =
    RichDoubleArray(DoubleArray.frompointer(p))

  implicit def doubleArraytoRichDoubleArray(a: DoubleArray) =
     RichDoubleArray(a)
}

import skeres._

// Enrichment of carrays.i's DoubleArray to
// get more convenient access and conversion
// back to pointer and to scala arrays.
case class RichDoubleArray(a: DoubleArray) {
  def get(i: Int): Double = a.getitem(i)

  def set(i: Int, x: Double): Unit = a.setitem(i, x)

  def toPointer: DoublePointer = a.cast

  // This method is very inefficient. Prefer using the get/set methods above in cost function
  // callbacks.
  // Use of this method is recommended only to return to Scala the result of the optimization.
  def toArray(length: Int): Array[Double] = {
    val aScala = Array.ofDim[Double](length)
    aScala.indices.foreach(i => aScala(i) = a.getitem(i))
    aScala
  }
}

// Convenient access to matrices represented as
// double**'s.
case class RichDoubleMatrix(data: DoublePointerPointer) {
  def isNull: Boolean = DoubleMatrix.isNull(data)

  def doubleArrayRow(i: Int) = DoubleArray.frompointer {
     DoubleMatrix.row(data, i)
  }

  def get(i: Int, j: Int) = doubleArrayRow(i).getitem(j)

  def set(i: Int, j: Int, x: Double) = doubleArrayRow(i).setitem(j, x)
}
