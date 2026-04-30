package monitoring

import java.io.{File, PrintWriter}
import scala.util.{Try, Using}

/**
 * Generates CSV reports of hardware profiles that can be opened in Excel.
 * Uses semicolon separation for better Excel compatibility.
 */
object ExcelReporter:

  /**
   * Export hardware profile to CSV file (Excel-compatible).
   *
   * @param profile Hardware profile to export
   * @param outputPath Path where CSV file will be saved
   * @return Try containing the file path if successful
   */
  def exportToExcel(profile: HardwareProfile, outputPath: String = "hardware-profile.csv"): Try[String] =
    Try {
      val file = new File(outputPath)
      Option(file.getParentFile).foreach(_.mkdirs())

      Using.resource(new PrintWriter(file, "UTF-8")) { writer =>
        // Write BOM for Excel UTF-8 recognition
        writer.write('\ufeff')

        // Write header
        writer.println("Parametr;Wartość;Jednostka")

        // Write data rows
        val dataRows = prepareDataRows(profile)
        dataRows.foreach { row =>
          val formattedRow = row.map {
            case s: String => s""""${s.replace("\"", "\"\"")}""""
            case n: Number => n.toString
            case b: Boolean => if b then "Tak" else "Nie"
            case other => other.toString
          }
          writer.println(formattedRow.mkString(";"))
        }
      }

      outputPath
    }

  /**
   * Prepare data rows from hardware profile in format: (Parameter, Value, Unit).
   * Made public for access from BenchmarkExporter.
   */
  def prepareDataRows(profile: HardwareProfile): List[List[Any]] =
    List(
      // CPU Information
      List("Procesor - Model", profile.cpuModel, ""),
      List("Procesor - Producent", profile.cpuVendor, ""),
      List("Procesor - Rdzenie fizyczne", profile.physicalCores, "szt."),
      List("Procesor - Rdzenie logiczne", profile.logicalCores, "szt."),
      List("Procesor - Aktualna częstotliwość", f"${profile.currentFrequencyMhz}%.2f", "MHz"),
      List("Procesor - Maksymalna częstotliwość", f"${profile.maxFrequencyMhz}%.2f", "MHz"),
      List("Procesor - Identyfikator", profile.processorId, ""),
      List("Procesor - Cache L1", profile.l1CacheKb.getOrElse("N/A"), "KB"),
      List("Procesor - Cache L2", profile.l2CacheKb.getOrElse("N/A"), "KB"),
      List("Procesor - Cache L3", profile.l3CacheKb.getOrElse("N/A"), "KB"),
      List("Procesor - SMT/Hyper-Threading", profile.smtEnabled.map(b => if b then "Włączone" else "Wyłączone").getOrElse("N/A"), ""),

      List("", "", ""), // Spacer

      // RAM Information
      List("Pamięć RAM - Całkowita", f"${profile.totalRamGb}%.2f", "GB"),
      List("Pamięć RAM - Dostępna", f"${profile.availableRamGb}%.2f", "GB"),
      List("Pamięć RAM - Typ", profile.ramType, ""),
      List("Pamięć RAM - Prędkość", profile.ramSpeedMts.getOrElse("N/A"), "MT/s"),

      List("", "", ""), // Spacer

      // OS Information
      List("System operacyjny - Nazwa", profile.osName, ""),
      List("System operacyjny - Wersja", profile.osVersion, ""),
      List("System operacyjny - Architektura", profile.osArch, "bit"),

      List("", "", ""), // Spacer

      // JVM Information
      List("JVM - Producent", profile.jvmVendor, ""),
      List("JVM - Wersja", profile.jvmVersion, ""),
      List("JVM - Runtime", profile.jvmRuntime, ""),
      List("JVM - Parametr Xmx (max. pamięć)", profile.jvmXmxMb, "MB"),
      List("JVM - Parametr Xms (init. pamięć)", profile.jvmXmsMb, "MB"),
      List("JVM - Garbage Collector", profile.gcType, ""),

      List("", "", ""), // Spacer

      // BIOS/Motherboard Information
      List("BIOS - Producent", profile.biosManufacturer.getOrElse("N/A"), ""),
      List("BIOS - Model płyty głównej", profile.biosModel.getOrElse("N/A"), ""),
      List("BIOS - Wersja", profile.biosVersion.getOrElse("N/A"), ""),

      List("", "", ""), // Spacer

      // Storage Information
      List("Dyski - Liczba urządzeń", profile.storageDevices.length, "szt.")
    ) ++ profile.storageDevices.zipWithIndex.flatMap { (device, idx) =>
      List(
        List(s"Dysk ${idx + 1} - Model", device.model, ""),
        List(s"Dysk ${idx + 1} - Interfejs", device.interface, ""),
        List(s"Dysk ${idx + 1} - Pojemność", device.capacityGb, "GB")
      )
    } ++ List(
      List("", "", ""), // Spacer
      List("Data rejestracji", profile.capturedAt, "")
    )
