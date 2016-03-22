package org.somelightprojections.skeres

import com.google.ceres.{DoubleArraySlice, DoubleArray}
import spire.implicits._

/**
  * Enrichment of SWIG carrays.i's DoubleArray to get more convenient access and conversion
  * back to pointer and to scala arrays.
  * Memory is owned by the native code, and there is NO BOUND CHECKING: heap corruption
  * may occur if invalid indices are specified.
  *
  * @param a: Native array wrapped in a SWIG carray.
  */
case class RichDoubleArray(a: DoubleArray) {
  /**
    * Returns a native array element.
    * @param i Element index.
    * @return Native array element at i.
    */
  def get(i: Int): Double = a.getitem(i)

  /**
    * Assigns a native array element.
    * @param i Element index.
    * @param x Value to assign.
    */
  def set(i: Int, x: Double): Unit = a.setitem(i, x)

  /**
    * Assign the first from.length elements of a native array.
    * WARNING: heap corruption will occur if the array argument has length larger than
    * the native array.
    * @param from Array to copy from.
    * @return The underlying (native) DoubleArray.
    */
  def copyFrom(from: Array[Double]): DoubleArray = {
    cforRange(0 until from.length)(i => a.setitem(i, from(i)))
    a
  }

  /**
    * Test if the native array is NULL.
    * @return true if it is.
    */
  def isNull: Boolean = toPointer == null

  /**
    * Slice of a native array from a starting offset to the end.
    * @param start offset from the begining of the current array.
    * @return DoublePointer pointing to the beginning of the slice.
    */
  def slice(start: Int): DoublePointer = DoubleArraySlice.get(a.cast, start)

  /**
    * Conversion to DoublePointer.
    * @return DoublePointer pointing to the beginning of the array.
    */
  def toPointer: DoublePointer = a.cast

  /**
    * Conversion to Scala Array..
    * @param length Length of this native array.
    * @return Scala array containing a copy of the native one.
    */
  def toArray(length: Int): Array[Double] = {
    val array = Array.ofDim[Double](length)
    cforRange(0 to length - 1)(i => array(i) = a.getitem(i))
    array
  }
}

object RichDoubleArray {
  def ofSize(n: Int): DoublePointer = RichDoubleArray(new DoubleArray(n)).toPointer
  def fromArray(a: Array[Double]): DoublePointer = ofSize(a.length).copyFrom(a).toPointer
}