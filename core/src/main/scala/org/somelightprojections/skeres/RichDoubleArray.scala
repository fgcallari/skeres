package org.somelightprojections.skeres

import com.google.ceres.DoubleArray
import spire.implicits._

// Enrichment of SWIG carrays.i's DoubleArray to
// get more convenient access and conversion
// back to pointer and to scala arrays.
case class RichDoubleArray(a: DoubleArray) {
  def get(i: Int): Double = a.getitem(i)

  def set(i: Int, x: Double): Unit = a.setitem(i, x)

  def copyFrom(from: Array[Double]): DoubleArray = {
    cforRange(0 until from.length)(i => a.setitem(i, from(i)))
    a
  }

  def toPointer: DoublePointer = a.cast

  def isNull: Boolean = toPointer == null

  def toArray(length: Int): Array[Double] = {
    val aScala = Array.ofDim[Double](length)
    cforRange(0 to length - 1)(i => aScala(i) = a.getitem(i))
    aScala
  }
}

