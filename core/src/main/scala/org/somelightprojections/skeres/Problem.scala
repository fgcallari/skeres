package org.somelightprojections.skeres

import com.google.ceres._
import scala.collection.mutable

object Problem {
  // Cost and loss functions are JVM objects, but the default behavior of Ceres's Problem class
  // is to take ownership of them and delete them upon deconstruction of the residual blocks.
  // Hence this bit of code.
  class Options extends CeresProblem.Options {
    setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
    setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
  }
}

class Problem(opts: Problem.Options) extends CeresProblem(opts) {

  def this() = this(new Problem.Options())

  def addResidualBlock(cost: CostFunction, loss: LossFunction, x: DoublePointer*): ResidualBlockId = {
    costs += cost
    losses += loss

    val xv = new StdVectorDoublePointer()
    x.foreach(xi => xv.add(xi))
    addResidualBlock(cost, loss, xv)
  }

  // These exist only so that the cost/loss function instances won't be GC'd before
  // the Problem instance is itself finalized.
  private val costs = mutable.ListBuffer.empty[CostFunction]
  private val losses = mutable.ListBuffer.empty[LossFunction]
}
