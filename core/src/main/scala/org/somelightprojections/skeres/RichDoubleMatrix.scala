package org.somelightprojections.skeres

import com.google.ceres.{StdVectorDoublePointer, DoubleArray, DoubleMatrix}

// Convenient access to matrices represented as
// double**'s.
// This is intended for manipulation in Scala of memory blocks passed by
// the native code. Do not use in the wild for general matrix operations.
case class RichDoubleMatrix(data: DoublePointerPointer) {
  @inline def isNull: Boolean = DoubleMatrix.isNull(data)

  def hasRow(i: Int): Boolean = !getRow(i).isNull

  def getRow(i: Int): DoubleArray = DoubleArray.frompointer {
    DoubleMatrix.row(data, i)
  }

  def get(i: Int, j: Int): Double = getRow(i).getitem(j)

  def set(i: Int, j: Int, x: Double): Unit = getRow(i).setitem(j, x)

  def copyRowFrom(i: Int, a: Array[Double]): Unit = copyRowFrom(i, a, 0, a.length)

  def copyRowFrom(i: Int, a: Array[Double], begin: Int, end: Int): Unit =
    getRow(i).copyFrom(a.slice(begin, end))
}

object RichDoubleMatrix {
  def fromStdVector(vec: StdVectorDoublePointer): DoublePointerPointer =
    DoubleMatrix.toPointerPointer(vec)

  def fromArrays(a: Array[Double]*): DoublePointerPointer = {
    val v = new StdVectorDoublePointer()
    a.foreach(ai => v.add(new DoubleArray(ai.length).copyFrom(ai).toPointer))
    fromStdVector(v)
  }

  lazy val empty = ofSize(0, 0)

  def ofSize(numRows: Int, numColumns: Int): DoublePointerPointer = {
    if (numRows == 0 || numColumns == 0) {
      fromStdVector(null)
    } else {
      val pointerVector = new StdVectorDoublePointer(numRows)
      (0 until numRows).foreach(i => pointerVector.set(i, new DoubleArray(numColumns).toPointer))
      fromStdVector(pointerVector)
    }
  }
}
