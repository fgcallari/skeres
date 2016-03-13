package org.somelightprojections.skeres.examples

/**
  * A port to Scala of the simple_bundle_adjuster.cc example from the ceres-solver
  * source distribution.
  */

import com.google.ceres._
import java.io.File
import java.util.Scanner
import org.somelightprojections.skeres._
import scala.reflect.ClassTag
import scala.{specialized => sp}
import spire.algebra.{Field, NRoot, Order, Trig}
import spire.implicits._


case class BalProblem(
  numCameras: Int,
  numPoints: Int,
  numObservations: Int,
  numParameters: Int,
  pointIndex: Vector[Int],
  cameraIndex: Vector[Int],
  observations: Vector[Double],
  parameters: DoubleArray  // DoubleArray because it is optimized.
) {
  def mutableCameras: DoublePointer = parameters.toPointer
  def mutablePoints: DoublePointer = parameters.slice(9 * numCameras)

  def mutableCameraForObservation(i: Int): DoublePointer = mutableCameras.slice(cameraIndex(i) * 9)

  def mutablePointForObservation(i: Int): DoublePointer = mutablePoints.slice(pointIndex(i) * 3)
}

object BalProblem {
  def fromFile(filePath: String): BalProblem = {
    print(s"Loading BalProblem from $filePath ...")
    val file = new File(filePath)
    val scanner = new Scanner(file)
    val numCameras = scanner.nextInt()
    val numPoints = scanner.nextInt()
    val numObservations = scanner.nextInt()

    val pointIndex = Array.ofDim[Int](numObservations)
    val cameraIndex = Array.ofDim[Int](numObservations)
    val observations = Array.ofDim[Double](2 * numObservations)

    val numParameters = 9 * numCameras + 3 * numPoints
    val parameters = new DoubleArray(numParameters)

    for (i <- 0 until numObservations) {
      cameraIndex(i) = scanner.nextInt()
      pointIndex(i) = scanner.nextInt()
      for (j <- 0 until 2) {
        observations(2 * i + j) = scanner.nextDouble()
      }
    }

    for (i <- 0 until numParameters) {
      parameters.set(i, scanner.nextDouble())
    }

    println(" done")

    BalProblem(
      numCameras,
      numPoints,
      numObservations,
      numParameters,
      pointIndex.toVector,
      cameraIndex.toVector,
      observations.toVector,
      parameters
    )
  }
}

class SnavelyReprojectionError(observedX: Double, observedY: Double) extends CostFunctor(2, 9, 3) {

  override def apply[@sp(Double) T: Field : Trig : NRoot : Order : ClassTag](
    params: Array[T]*
  ): Array[T] = {
    require(params.length == 2)
    val camera = params(0)
    require(camera.length == 9)
    val point = params(1)
    require(point.length == 3)

    // camera(0,1,2) are the angle-axis rotation.
    val angleAxis = camera.slice(0, 3)
    val p = Rotation.angleAxisRotatePoint(angleAxis, point)

    // camera(3,4,5) are the translation.
    p(0) += camera(3)
    p(1) += camera(4)
    p(2) += camera(5)

    // Compute the center of distortion. The sign change comes from
    // the camera model that Noah Snavely's Bundler assumes, whereby
    // the camera coordinate system has a negative z axis.
    val xp = - p(0) / p(2)
    val yp = - p(1) / p(2)

    // Apply second and fourth order radial distortion.
    val l1 = camera(7)
    val l2 = camera(8)
    val r2 = xp * xp + yp * yp
    val distortion = 1.0 + r2  * (l1 + l2  * r2)

    // Compute final projected point position.
    val focal = camera(6)
    val predictedX = focal * distortion * xp
    val predictedY = focal * distortion * yp

    // The error is the difference between the predicted and observed position.
    Array(predictedX - observedX, predictedY - observedY)
  }
}

object SnavelyReprojectionError {
  def apply(observedX: Double, observedY: Double): CostFunction =
    new SnavelyReprojectionError(observedX, observedY).toAutodiffCostFunction
}

object SimpleBundleAdjuster {
  def main(args: Array[String]): Unit = {
    ceres.initGoogleLogging("SimpleBundleAdjuster")
    if (args.isEmpty) {
      println("Usage: SimpleBundleAdjuster <data_file.txt>")
    }
    val balProblem = BalProblem.fromFile(args(0))
    val observations = balProblem.observations
    val problem = new Problem
    val lossFunction = PredefinedLossFunctions.trivialLoss()

    // Create residuals for each observation in the bundle adjustment problem. The
    // parameters for cameras and points are added automatically.
    for (i <- 0 until balProblem.numObservations) {
      val obsCost: CostFunction =
        SnavelyReprojectionError(observations(2 * i + 0), observations(2 * i + 1))
      val obsCamera = balProblem.mutableCameraForObservation(i)
      val obsPoint = balProblem.mutablePointForObservation(i)
      problem.addResidualBlock(obsCost, lossFunction, obsCamera, obsPoint)
    }

    val options = new Solver.Options
    options.setLinearSolverType(LinearSolverType.DENSE_SCHUR)
    options.setMinimizerProgressToStdout(true)

    val summary = new Solver.Summary
    ceres.solve(options, problem, summary)

    println(summary.fullReport())
  }
}
