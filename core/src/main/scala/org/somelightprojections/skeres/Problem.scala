package org.somelightprojections.skeres

import com.google.ceres._
import scala.collection.mutable

object Problem {
  type Options = CeresProblem.Options
}

final class Problem(opts: Problem.Options) extends CeresProblem(opts) {
  opts.setCostFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)
  opts.setLossFunctionOwnership(Ownership.DO_NOT_TAKE_OWNERSHIP)

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
