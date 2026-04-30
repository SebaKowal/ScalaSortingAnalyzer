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
      // Pobieranie ścieżek jarów JavaFX
      val jfxJars = (Compile / dependencyClasspath).value.files
        .filter(f => f.getName.startsWith("javafx") && f.getName.endsWith(".jar"))
      val modulePath = jfxJars.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator)

      Seq(
        // --- Ustawienia JavaFX ---
        s"--module-path=$modulePath",
        "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media",

        // --- Heap: give GC room to breathe — reduces GC frequency during benchmarks ---
        "-Xms512m",
        "-Xmx2g",

        // --- G1GC tuning for benchmark workload ---
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=10",     // aggressive pause target
        "-XX:G1HeapRegionSize=4m",     // larger regions = fewer GC events for short-lived arrays

        // UWAGA: Zakomentowane dla JMH - benchmarki potrzebują System.gc() między iteracjami
        // "-XX:+DisableExplicitGC",

        // --- JIT aggressiveness ---
        "-XX:+TieredCompilation",
        "-XX:CompileThreshold=1000",   // lower threshold = faster C2 activation
        "-XX:+OptimizeStringConcat",

        // --- Eliminate safepoint bias in timing ---
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:GuaranteedSafepointInterval=0"  // don't force safepoints on a timer

        // For diagnostics — remove in production
        // "-XX:+PrintGCDetails",
        // "-XX:+PrintGCDateStamps"
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