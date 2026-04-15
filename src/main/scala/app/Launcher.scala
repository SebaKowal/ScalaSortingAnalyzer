package app

object Launcher {
  def main(args: Array[String]): Unit = {
    // jpackage requires a plain main-class entry point that does not extend JFXApp3.
    // This wrapper satisfies that requirement and delegates to the real application.
    Main.main(args)
  }
}