scalaVersion := "2.11.7"

val spire = "org.spire-math" %% "spire" % "0.11.0"
val shapeless = "com.chuusai" %% "shapeless" % "2.2.5"

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
    libraryDependencies ++= Seq(spire, shapeless)
  )

lazy val examples = project
  .dependsOn(core % "compile->compile;test->compile")
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(spire)
  )
