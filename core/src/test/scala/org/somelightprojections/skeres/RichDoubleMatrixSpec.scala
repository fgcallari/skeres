package org.somelightprojections.skeres

import com.google.ceres.{StdVectorDoublePointer, DoubleArray}
import org.scalatest.{OneInstancePerTest, MustMatchers, WordSpec}

class RichDoubleMatrixSpec extends WordSpec with MustMatchers {
  "RichDoubleMatrix" should {
    "construct from vector of pointers" in {
      val a0 = new DoubleArray(3)
      for (i <- 0 until 3) a0.set(i, 3 * i)
      val a1 = new DoubleArray(6)
      for (i <- 0 until 6) a1.set(i, 6 * i)

      val vec = new StdVectorDoublePointer
      vec.add(a0.toPointer)
      vec.add(a1.toPointer)
      val mat = RichDoubleMatrix.fromStdVector(vec)

      mat.hasRow(0) must be(true)
      mat.getRow(0).isNull must be(false)
      val r0 = mat.getRow(0)
      for (i <- 0 until 3) {
        r0.get(i) must be(3 * i)
        mat.set(0, i, 10 * i)
      }
      mat.getRow(1).isNull must be(false)
      val r1 = mat.getRow(1)
      for (i <- 0 until 6) {
        r1.get(i) must be(6 * i)
      }
      for (i <- 0 until 3) {
        mat.get(0, i) must be(10 * i)
      }
      mat.getRow(1).copyFrom((0 until 6).map(_ * 20.0).toArray)
      for (i <- 0 until 6) {
        mat.get(1, i) must be(20 * i)
      }
    }
    "construct with given dimension" in {
      val mat = RichDoubleMatrix.ofSize(2, 6)
      for (i <- 0 until 2) {
        for (j <- 0 until 6) {
          mat.set(i, j, 7 * i + 13 * j)
        }
      }
      for (i <- 0 until 2) {
        for (j <- 0 until 6) {
          mat.get(i, j) must be(7 * i + 13 * j)
        }
      }
    }
    "convert to/from DoublePointerPointer" in {
      val mat = RichDoubleMatrix.ofSize(2, 6)
      for (i <- 0 until 2) {
        for (j <- 0 until 6) {
          mat.set(i, j, 7 * i + 13 * j)
        }
      }
      val pp: DoublePointerPointer = mat.data
      for (i <- 0 until 2) {
        for (j <- 0 until 6) {
          pp.get(i, j) must be(7 * i + 13 * j)
        }
      }
    }
  }
}
