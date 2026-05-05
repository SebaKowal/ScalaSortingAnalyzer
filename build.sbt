ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.7"

// 1. Wykrywanie systemu operacyjnego (uproszczone)
val osName = System.getProperty("os.name").toLowerCase
val jfxClassifier = osName match {
  case n if n.contains("win") => "win"
  case n if n.contains("mac") => "mac"
  case _                      => "linux"
}

val jfxVersion = "21"
val jfxModules = Seq("base", "controls", "fxml", "graphics", "media", "swing")

lazy val root = (project in file("."))
  .enablePlugins(JmhPlugin)
  .settings(
    name := "ScalaSortingAnalyzer",
    fork := true,

    Compile / run / javaOptions ++= jfxOptions((Compile / dependencyClasspath).value),
    Test / run / javaOptions    ++= jfxOptions((Test / dependencyClasspath).value),

    libraryDependencies ++= Seq(
      "org.scalafx"   %% "scalafx"   % "21.0.0-R32",
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "org.apache.poi" % "poi-ooxml" % "5.2.5"
    ),

    libraryDependencies ++= jfxModules.map { m =>
      "org.openjfx" % s"javafx-$m" % jfxVersion classifier jfxClassifier
    },

    assembly / mainClass             := Some("app.Main"),
    assembly / assemblyJarName       := "ScalaSortingAnalyzer.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", _*)            => MergeStrategy.discard
      case PathList("module-info.class")       => MergeStrategy.discard
      case _                                   => MergeStrategy.first
    }
  )

// Funkcja pomocnicza wyodrębniona z głównego nurtu settingsów
def jfxOptions(cp: Classpath): Seq[String] = {
  val jfxJars = cp.files.filter(f => f.getName.startsWith("javafx") && f.getName.endsWith(".jar"))
  val modulePath = jfxJars.map(_.getAbsolutePath).distinct.mkString(java.io.File.pathSeparator)

  Seq(
    s"--module-path=$modulePath",
    "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media",
    "-Xms512m",
    "-Xmx2g",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=10",
    "-XX:G1HeapRegionSize=4m",
    "-XX:+TieredCompilation",
    "-XX:CompileThreshold=1000",
    "-XX:+OptimizeStringConcat",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:GuaranteedSafepointInterval=0"
  )
}