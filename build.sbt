scalaVersion := "2.11.7"

val spire = "org.spire-math" %% "spire" % "0.11.0"
val shapeless = "com.chuusai" %% "shapeless" % "2.2.5"
val scalaTest = "org.scalatest" %% "scalatest" % "2.2.6" % "test"

lazy val commonSettings = Seq(
  organization := "org.somelightprojections",
  version := "0.1.0",
  scalaVersion := "2.11.7",
  fork in run := true
)

lazy val root = project
  .settings(commonSettings: _*)
  .aggregate(core, examples)

lazy val core = project
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(spire, shapeless, scalaTest)
  )

lazy val examples = project
  .dependsOn(core)
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(spire)
  )
