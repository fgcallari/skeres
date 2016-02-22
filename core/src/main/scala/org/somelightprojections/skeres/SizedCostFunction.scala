package org.somelightprojections.skeres

import com.google.ceres.CostFunction

//  Sized cost function
abstract class SizedCostFunction(val kNumResiduals: Int, val N: Int*) extends CostFunction {
  require(N.forall(_ >= 0), s"Negative block size detected. Block size are: ${N.mkString(", ")}")
  require(N.indices.tail.forall(i => N(i) == 0 || N(i - 1) > 0),
    "Zero block cannot precede a non-zero block. Block sizes are (ignore trailing 0's): " +
      N.mkString(", ")
  )
  setNumResiduals(kNumResiduals)
  N.foreach(mutableParameterBlockSizes.add)
}
