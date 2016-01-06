package org.somelightprojections.skeres
import ceres._

package object skeres {
  // Readable renaming of SWIG-generated types.
  type DoublePointerPointer = SWIGTYPE_p_p_double
  type DoublePointer = SWIGTYPE_p_double
  type LossFunctionPointer = SWIGTYPE_p_ceres__LossFunction

  // Implicit conversions betwee pointer objects and
  // arrays or matrices.
  implicit def doublePointerPointerToBlockMatrix(p: DoublePointerPointer) = 
    BlockMatrix(p)

  implicit def doublePointerToRichDoubleArray(p: DoublePointer) = 
    RichDoubleArray(DoubleArray.frompointer(p))

  implicit def doubleArraytoRichDoubleArray(a: DoubleArray) = 
     RichDoubleArray(a)

  // Trivial loss function (we can't use null's directly).
  lazy val NullLossFunction: LossFunctionPointer = new TrivialLossFunction
}

import skeres._

// Enrichment of carrays.i's DoubleArray to
// get more convenient access and conversion
// back to pointer and to scala arrays. 
case class RichDoubleArray(a: DoubleArray) {
  def get(i: Int): Double = a.getitem(i)

  def set(i: Int, x: Double): Unit = a.setitem(i, x)

  def toPointer: DoublePointer = a.cast

  def toArray(length: Int): Array[Double] = {
    val aScala = Array.ofDim[Double](length)
    ArrayHelper.pointerToArray(a.toPointer, length, aScala)
    aScala
  }
}

// Convenient access to matrices represented as
// double**'s.
case class BlockMatrix(data: DoublePointerPointer) {
  def doubleArrayRow(i: Int) = DoubleArray.frompointer {
     BlockMatrixHelper.row(data, i)
  }
  def get(i: Int, j: Int) = doubleArrayRow(i).getitem(j)
  def set(i: Int, j: Int, x: Double) = doubleArrayRow(i).setitem(j, x)  
}
object BlockMatrix {
  @inline def isNull(data: DoublePointerPointer) =
    BlockMatrixHelper.isNull(data)
}

// Extension of LossFunction* that mimics a null pointer. 
class TrivialLossFunction extends LossFunctionPointer
