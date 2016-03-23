package org.somelightprojections.skeres

import com.google.ceres.{StdVectorDoublePointer, DoubleArray, DoubleMatrix}

/** An enrichment for convenient access to collections of blocks of double-precision floating
  * This is intended for manipulation in Scala of memory blocks passed by
  * native code in ceres-solver ONLY. Do NOT use in the wild for general matrix operations.
  *
  * Although the blocks in the collection are not restricted to have the same length, in the
  * following we refer to them as "rows", so they can be depicted, for example, as the following
  * tableau:
  *
  *   data -> [ [ ..... row 0 .....  ]
  *             [ .. row 1 ..]
  *             [ ....row 2 ....]
  *           ]
  *
  *  Here "data" is of type DoublePointerPointer, a SWIG-generated Java wrapper to a C double**
  *
  *  NOTE CAREFULLY:
  *    1 - Because the only interface to the native blocks is a double**, and this is
  *        often passed by native code, this class does NOT represent neither the size nor the
  *        number of the memory blocks in the collection. Therefore an indexing error may case
  *        a SIGSEGV or a heap corruption in native code. You have been warned.
  *    2 - Because the backing memory is owned by native code, reading from and writing to the
  *        blocks in the JVM causes a crossing of the JNI "boundary". Because of the performance
  *        implication of any such crossing, estensive manipulation will normally be done by
  *        first copying the blocks native -> JVM, operating, and finally writing back JVM -> native.
  *
  *  @param data: SWIG DoublePointerPointer wrapping a native double** .
  */
case class RichDoubleMatrix(data: DoublePointerPointer) {
  /** True iff the data pointer is null. */
  def isNull: Boolean = DoubleMatrix.isNull(data)

  /** True iff the i-th block is not null. No bound check is made on the index. */
  def hasRow(i: Int): Boolean = !getRow(i).isNull

  /** Retrieves the i-th block as a DoubleArray. */
  def getRow(i: Int): DoubleArray = DoubleArray.frompointer {
    DoubleMatrix.row(data, i)
  }

  /** Retrieves the j-th element of row i. */
  def get(i: Int, j: Int): Double = getRow(i).getitem(j)

  /** Assigns the j-th element of row i. */
  def set(i: Int, j: Int, x: Double): Unit = getRow(i).setitem(j, x)

  /** Assigns the leading a.length elements of the i-th row from the given Array.
    * WARNING - native heap corruption will ensue if the Array is longer than the block.
    */
  def copyRowFrom(i: Int, a: Array[Double]): Unit = copyRowFrom(i, a, 0, a.length)

  /** Assigns the leading (end - begin) elements of the i-th row from the slice of the given
    * Array starting at begin, inclusive, and ending at end, exclusive.
    * WARNING - native heap corruption will ensue if the slice is longer than the block.
    */
  def copyRowFrom(i: Int, a: Array[Double], begin: Int, end: Int): Unit =
    getRow(i).copyFrom(a.slice(begin, end))
}

/** The RichDoubleMatrix companion object defines some factory methods to simplify JVM-driven
  * allocation and initialization of native memory blocks.
  */
object RichDoubleMatrix {
  /** Allocates a collection of numRows native blocks of double, all of length numColumns.
    * There is no guarantee that the blocks are contiguous in native memory.
    */
  def ofSize(numRows: Int, numColumns: Int): DoublePointerPointer = {
    if (numRows == 0 || numColumns == 0) {
      fromStdVector(null)
    } else {
      val pointerVector = new StdVectorDoublePointer(numRows)
      (0 until numRows).foreach(i => pointerVector.set(i, new DoubleArray(numColumns).toPointer))
      fromStdVector(pointerVector)
    }
  }

  /** Allocates vec.size blocks of native memory and initializes them with the content of
    * other native memory blocks pointed to by vec's content.
    */
  def fromStdVector(vec: StdVectorDoublePointer): DoublePointerPointer =
    DoubleMatrix.toPointerPointer(vec)

  /** Allocates a.length native memory blocks, and initializes them with the content of the
    * given JVM arrays.
    * Note that, as the memory is held natively, it is OK for the passed JVM arrays to go
    * out of scope after this method returns.
    */
  def fromArrays(a: Array[Double]*): DoublePointerPointer = {
    val v = new StdVectorDoublePointer()
    a.foreach(ai => v.add(new DoubleArray(ai.length).copyFrom(ai).toPointer))
    fromStdVector(v)
  }

  /** Represents an empty collection of native memory blocks. */
  lazy val empty = ofSize(0, 0)
}
