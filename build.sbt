ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

val jfxVersion    = "21"
val jfxClassifier = System.getProperty("os.name").toLowerCase match {
  case n if n.contains("win") => "win"
  case n if n.contains("mac") => "mac"
  case _                      => "linux"
}
val jfxModules = Seq("base", "controls", "fxml", "graphics", "media", "swing")

lazy val root = (project in file("."))
  .settings(
    name := "ScalaSortingAnalyzer",
    fork := true,

    libraryDependencies ++= Seq(
      "org.scalafx"   %% "scalafx"   % "21.0.0-R32",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test
    ),

    libraryDependencies ++= jfxModules.map { m =>
      "org.openjfx" % s"javafx-$m" % jfxVersion classifier jfxClassifier
    },

    javaOptions ++= {
      val jfxJars = (Compile / dependencyClasspath).value.files
        .filter(f => f.getName.startsWith("javafx") && f.getName.endsWith(".jar"))
      val modulePath = jfxJars.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)
      Seq(
        s"--module-path=$modulePath",
        "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media"
      )
    },

    assembly / mainClass       := Some("app.Main"),
    assembly / assemblyJarName := "ScalaSortingAnalyzer.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", _*)            => MergeStrategy.discard
      case PathList("module-info.class")       => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    }
  )

enablePlugins(JmhPlugin)