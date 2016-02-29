package org.somelightprojections.skeres

import com.google.ceres.{DoubleArraySlice, DoubleArray}
import org.scalatest.{MustMatchers, WordSpec}

class DoubleArraySliceSpec extends WordSpec with MustMatchers {
  "DoubleArraySlice" should {
    "get and manipulate slices of DoubleArray's" in {
      val buffer = new DoubleArray(10)
      for (i <- 0 until 10) {
        buffer.set(i, i)
      }
      val slice: DoublePointer = DoubleArraySlice.get(buffer.toPointer, 4)
      for (i <- 0 until 6) {
        val s = slice.get(i)
        slice.get(i) must be(4 + i)
        slice.set(i, 10 * (4 + i))
      }
      for (i <- 0 until 10) {
        if (i < 4) buffer.get(i) must be(i)
        else buffer.get(i) must be(10 * i)
      }
    }
  }
}
