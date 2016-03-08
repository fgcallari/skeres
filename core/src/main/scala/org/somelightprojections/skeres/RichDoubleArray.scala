package org.somelightprojections.skeres

import com.google.ceres.{DoubleArraySlice, DoubleArray}
import spire.implicits._

/**
  * Enrichment of SWIG carrays.i's DoubleArray to get more convenient access and conversion
  * back to pointer and to scala arrays.
  * Memory is owned by the native code, and there is NO BOUND CHECKING: heap corruption
  * may occur if invalid indices are specified.
  */
case class RichDoubleArray(a: DoubleArray) {
  /**
    * Returns a native array element.
    * @param i element index.
    * @return Native array element at i.
    */
  def get(i: Int): Double = a.getitem(i)

  /**
    * Assigns a native array element.
    * @param i element index.
    * @param x value to assign.
    */
  def set(i: Int, x: Double): Unit = a.setitem(i, x)

  /**
    * Assign the first from.length elements of a native array.
    * WARNING: heap corruption will occurr if the array argument has length larger than
    * the native array.
    * @param from Array to copy from.
    * @return The underlying (native) DoubleArray.
    */
  def copyFrom(from: Array[Double]): DoubleArray = {
    cforRange(0 until from.length)(i => a.setitem(i, from(i)))
    a
  }

  def isNull: Boolean = toPointer == null

  def slice(start: Int): DoublePointer = DoubleArraySlice.get(a.cast, start)

  def toPointer: DoublePointer = a.cast

  def toArray(length: Int): Array[Double] = {
    val array = Array.ofDim[Double](length)
    cforRange(0 to length - 1)(i => array(i) = a.getitem(i))
    array
  }
}

