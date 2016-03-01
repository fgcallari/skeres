package org.somelightprojections.skeres

import com.google.ceres.DoubleArray
import org.scalatest.{MustMatchers, WordSpec}

class RichDoubleArraySpec extends WordSpec with MustMatchers {
  "RichDoubleArray" should {
    "manipulate DoubleArray with implicit conversion to/from DoublePointer" in {
      val buffer = new DoubleArray(10)
      for (i <- 0 until 10) {
        buffer.setitem(i, i)
      }
      val rich = RichDoubleArray(buffer)
      for (i <- 0 until 10) {
        rich.get(i) must be(buffer.get(i))
        buffer.set(i, 2 * i)
        buffer.getitem(i) must be(2 * i)
      }
    }
    "produce slices of arrays" in {
      val buffer = new DoubleArray(10)
      for (i <- 0 until 10) {
        buffer.setitem(i, i)
      }
      val slice: DoublePointer = buffer.slice(5)
      for (i <- 0 until 5) {
        slice.set(i, 2 * (i + 5))
      }
      for (i <- 0 until 10) {
        if (i < 5) buffer.get(i) must be(i)
        else buffer.get(i) must be(2 * i)
      }
    }
  }
}
