package org.somelightprojections.skeres

import com.google.ceres.{StdVectorDoublePointer, DoubleArray, DoubleMatrix}

// Convenient access to matrices represented as
// double**'s.
case class RichDoubleMatrix(data: DoublePointerPointer) {
  def isNull: Boolean = DoubleMatrix.isNull(data)

  def hasRow(i: Int): Boolean = !getRow(i).isNull

  def getRow(i: Int): DoubleArray = DoubleArray.frompointer {
    DoubleMatrix.row(data, i)
  }

  def get(i: Int, j: Int): Double = getRow(i).getitem(j)

  def set(i: Int, j: Int, x: Double): Unit = getRow(i).setitem(j, x)

  def copyRowFrom(i: Int, a: Array[Double]): Unit = getRow(i).copyFrom(a)
}

object RichDoubleMatrix {
  def fromStdVector(vec: StdVectorDoublePointer): DoublePointerPointer =
    DoubleMatrix.toPointerPointer(vec)

  def ofSize(numRows: Int, numColumns: Int): DoublePointerPointer = {
    val pointerVector = new StdVectorDoublePointer(numRows)
    (0 until numColumns).foreach(i => pointerVector.set(i, new DoubleArray(numColumns).toPointer))
    fromStdVector(pointerVector)
  }
}
