package monitoring

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.util.{Try, Using}
import benchmark.BenchmarkResult

object ExcelReporter:

  def exportToExcel(
                     results: Seq[BenchmarkResult],
                     hardware: HardwareProfile,
                     outputPath: String = "benchmark-report.xlsx"
                   ): Try[String] =
    val file = new File(outputPath)
    Option(file.getParentFile).foreach(_.mkdirs())
    Using(new ZipOutputStream(new FileOutputStream(file))) { zos =>
      // Required XLSX structure
      writeEntry(zos, "[Content_Types].xml", contentTypes)
      writeEntry(zos, "_rels/.rels", rels)
      writeEntry(zos, "xl/workbook.xml", workbookXml)
      writeEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels)
      writeEntry(zos, "xl/styles.xml", stylesXml)

      // Sheets
      writeEntry(zos, "xl/worksheets/sheet1.xml", benchmarkSheet(results))
      writeEntry(zos, "xl/worksheets/sheet2.xml", hardwareSheet(hardware))
      outputPath
    }

  def prepareHardwareRows(profile: HardwareProfile): Seq[Seq[Any]] =
    prepareHardwareData(profile)

  private def writeEntry(zos: ZipOutputStream, name: String, content: String): Unit =
    zos.putNextEntry(new ZipEntry(name))
    zos.write(content.getBytes("UTF-8"))
    zos.closeEntry()

  // ---------------------------
  // XML TEMPLATES
  // ---------------------------

  private val contentTypes =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
      |  <Default Extension="xml" ContentType="application/xml"/>
      |  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
      |  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
      |  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
      |  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
      |  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
      |</Types>
      |""".stripMargin

  private val rels =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      |  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
      |</Relationships>
      |""".stripMargin

  private val workbookXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
      |          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
      |  <sheets>
      |    <sheet name="Benchmark" sheetId="1" r:id="rId1"/>
      |    <sheet name="Hardware" sheetId="2" r:id="rId2"/>
      |  </sheets>
      |</workbook>
      |""".stripMargin

  private val workbookRels =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      |  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
      |  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
      |  <Relationship Id="styles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
      |</Relationships>
      |""".stripMargin

  private val stylesXml =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      |  <fonts count="1"><font><sz val="11"/></font></fonts>
      |  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
      |  <borders count="1"><border/></borders>
      |  <cellStyleXfs count="1"><xf/></cellStyleXfs>
      |  <cellXfs count="1"><xf xfId="0"/></cellXfs>
      |</styleSheet>
      |""".stripMargin

  // ---------------------------
  // SHEET 1: Benchmark
  // ---------------------------

  private def benchmarkSheet(results: Seq[BenchmarkResult]): String =
    val header = Seq(
      "Algorithm","Variant","Pattern","Size","Warm","Time Mean (ns)","Time Median (ns)",
      "Time P90 (ns)","Time P95 (ns)","Time P99 (ns)","Time Min (ns)","Time Max (ns)",
      "Time StdDev (ns)","Throughput (elem/ms)","Ops/sec","Comparisons","Swaps","Writes",
      "Heap Δ (MB)","Alloc Rate (MB/s)","GC Collections","GC Pause (ms)","CPU Time (ns)",
      "User Time (ns)","CPU %","Sorted","Failure Msg"
    )

    val rows =
      results.map { r =>
        Seq(
          r.algoName, r.variant, r.pattern, r.size, r.isWarm,
          r.timeMeanNs, r.timeMedianNs, r.timeP90Ns, r.timeP95Ns, r.timeP99Ns,
          r.timeMinNs, r.timeMaxNs, r.timeStdDevNs, r.throughput, r.opsPerSec,
          r.comparisons, r.swaps, r.writes, r.maxHeapMb, r.allocRateMbS,
          r.gcCollections, r.gcPauseMs, r.cpuTimeNs, r.userTimeNs,
          r.cpuPercent, r.isSorted, r.failureMsg
        )
      }

    xmlSheet(header, rows)

  // ---------------------------
  // SHEET 2: Hardware
  // ---------------------------

  private def hardwareSheet(hw: HardwareProfile): String =
    val rows = prepareHardwareData(hw)
    xmlSheet(Seq("Parameter", "Value", "Unit"), rows)

  // ---------------------------
  // XML ROW BUILDER
  // ---------------------------

  private def xmlSheet(header: Seq[Any], rows: Seq[Seq[Any]]): String =
    val headerRow = xmlRow(1, header)
    val dataRows = rows.zipWithIndex.map { case (r, i) => xmlRow(i + 2, r) }.mkString

    s"""<?xml version="1.0" encoding="UTF-8"?>
       |<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
       |  <sheetData>
       |    $headerRow
       |    $dataRows
       |  </sheetData>
       |</worksheet>
       |""".stripMargin

  private def xmlRow(rowNum: Int, values: Seq[Any]): String =
    val cells = values.zipWithIndex.map { case (v, col) =>
      val colName = columnName(col + 1)
      s"""<c r="$colName$rowNum" t="inlineStr"><is><t>${escape(v.toString)}</t></is></c>"""
    }.mkString
    s"<row r=\"$rowNum\">$cells</row>"

  private def columnName(n: Int): String =
    if n <= 26 then (('A' + n - 1).toChar).toString
    else columnName((n - 1) / 26) + columnName((n - 1) % 26 + 1)

  private def escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  // ---------------------------
  // HARDWARE DATA
  // ---------------------------

  private def prepareHardwareData(p: HardwareProfile): Seq[Seq[Any]] =
    Seq(
      Seq("CPU Model", p.cpuModel, ""),
      Seq("CPU Vendor", p.cpuVendor, ""),
      Seq("Physical Cores", p.physicalCores, "cores"),
      Seq("Logical Cores", p.logicalCores, "cores"),
      Seq("Current Frequency", p.currentFrequencyMhz, "MHz"),
      Seq("Max Frequency", p.maxFrequencyMhz, "MHz"),
      Seq("Processor ID", p.processorId, ""),
      Seq("L1 Cache", p.l1CacheKb.getOrElse("N/A"), "KB"),
      Seq("L2 Cache", p.l2CacheKb.getOrElse("N/A"), "KB"),
      Seq("L3 Cache", p.l3CacheKb.getOrElse("N/A"), "KB"),
      Seq("SMT/Hyper-Threading", p.smtEnabled.map(if _ then "Enabled" else "Disabled").getOrElse("Unknown"), ""),
      Seq("", "", ""),
      Seq("Total RAM", p.totalRamGb, "GB"),
      Seq("Available RAM", p.availableRamGb, "GB"),
      Seq("RAM Type", p.ramType, ""),
      Seq("RAM Speed", p.ramSpeedMts.getOrElse("Unknown"), "MT/s"),
      Seq("", "", ""),
      Seq("OS Name", p.osName, ""),
      Seq("OS Version", p.osVersion, ""),
      Seq("OS Architecture", p.osArch, "bit"),
      Seq("", "", ""),
      Seq("JVM Vendor", p.jvmVendor, ""),
      Seq("JVM Version", p.jvmVersion, ""),
      Seq("JVM Runtime", p.jvmRuntime, ""),
      Seq("JVM Max Memory (Xmx)", p.jvmXmxMb, "MB"),
      Seq("JVM Initial Memory (Xms)", p.jvmXmsMb, "MB"),
      Seq("Garbage Collector", p.gcType, ""),
      Seq("", "", ""),
      Seq("BIOS Manufacturer", p.biosManufacturer.getOrElse("Unknown"), ""),
      Seq("Motherboard Model", p.biosModel.getOrElse("Unknown"), ""),
      Seq("BIOS Version", p.biosVersion.getOrElse("Unknown"), ""),
      Seq("", "", ""),
      Seq("Storage Devices", p.storageDevices.length, "devices")
    ) ++ p.storageDevices.zipWithIndex.flatMap { case (d, i) =>
      Seq(
        Seq(s"Device ${i + 1} Model", d.model, ""),
        Seq(s"Device ${i + 1} Interface", d.interface, ""),
        Seq(s"Device ${i + 1} Capacity", d.capacityGb, "GB")
      )
    } ++ Seq(
      Seq("", "", ""),
      Seq("Snapshot Time", p.capturedAt, "")
    )