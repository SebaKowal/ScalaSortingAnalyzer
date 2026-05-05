package monitoring

import java.io.{File, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.util.{Try, Using}
import benchmark.pure.PureResult
import benchmark.full.FullResult

object ExcelReporter:

  def exportPureToExcel(results: Seq[PureResult], hw: HardwareProfile, path: String): Try[String] =
    val headers = Seq("Algorithm", "Pattern", "Size", "Mean (ns)", "Median (ns)", "StdDev (ns)", "Min (ns)", "Max (ns)", "Throughput", "Comparisons", "Swaps", "Writes", "Sorted")
    val rows = results.map(r => Seq(r.algoName, r.pattern, r.size, r.meanNs, r.medianNs, r.stdDevNs, r.minNs, r.maxNs, r.throughputElemsPerMs, r.comparisons, r.swaps, r.writes, r.isSorted))
    generate(headers, rows, hw, path)

  def exportFullToExcel(results: Seq[FullResult], hw: HardwareProfile, path: String): Try[String] =
    val headers = Seq("Algorithm", "Pattern", "Size", "Mean (ns)", "Heap Δ (MB)", "Alloc Rate (MB/s)", "GC Colls", "GC Pause (ms)", "CPU Time (ns)", "CPU %", "Sorted")
    val rows = results.map { r =>
      val p = r.pure
      Seq(p.algoName, p.pattern, p.size, p.meanNs, r.heapDeltaMb, r.allocRateMbS, r.gcCollections, r.gcPauseMs, r.cpuTimeNs, r.cpuPercent, p.isSorted)
    }
    generate(headers, rows, hw, path)

  private def generate(header: Seq[String], rows: Seq[Seq[Any]], hardware: HardwareProfile, outputPath: String): Try[String] =
    val file = new File(outputPath)
    Option(file.getParentFile).foreach(_.mkdirs())
    Using(new ZipOutputStream(new FileOutputStream(file))) { zos =>
      writeEntry(zos, "[Content_Types].xml", contentTypes)
      writeEntry(zos, "_rels/.rels", rels)
      writeEntry(zos, "xl/workbook.xml", workbookXml)
      writeEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels)
      writeEntry(zos, "xl/styles.xml", stylesXml)
      writeEntry(zos, "xl/worksheets/sheet1.xml", xmlSheet(header, rows))
      writeEntry(zos, "xl/worksheets/sheet2.xml", hardwareSheet(hardware))
      outputPath
    }

  private def writeEntry(zos: ZipOutputStream, name: String, content: String): Unit =
    zos.putNextEntry(new ZipEntry(name));
    zos.write(content.getBytes("UTF-8"));
    zos.closeEntry()

  private def hardwareSheet(hw: HardwareProfile): String =
    xmlSheet(Seq("Parameter", "Value", "Unit"), prepareHardwareData(hw))

  private def xmlSheet(header: Seq[Any], rows: Seq[Seq[Any]]): String =
    val headerRow = xmlRow(1, header)
    val dataRows = rows.zipWithIndex.map { case (r, i) => xmlRow(i + 2, r) }.mkString
    s"""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$headerRow$dataRows</sheetData></worksheet>"""

  private def xmlRow(rowNum: Int, values: Seq[Any]): String =
    val cells = values.zipWithIndex.map { case (v, col) =>
      s"""<c r="${columnName(col + 1)}$rowNum" t="inlineStr"><is><t>${escape(v.toString)}</t></is></c>"""
    }.mkString
    s"<row r=\"$rowNum\">$cells</row>"

  private def columnName(n: Int): String =
    if n <= 26 then (('A' + n - 1).toChar).toString else columnName((n - 1) / 26) + columnName((n - 1) % 26 + 1)

  private def escape(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  private val contentTypes = """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>"""
  private val rels = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
  private val workbookXml = """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Results" sheetId="1" r:id="rId1"/><sheet name="Hardware" sheetId="2" r:id="rId2"/></sheets></workbook>"""
  private val workbookRels = """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="styles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
  private val stylesXml = """<?xml version="1.0" encoding="UTF-8"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="1"><font><sz val="11"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf/></cellStyleXfs><cellXfs count="1"><xf xfId="0"/></cellXfs></styleSheet>"""

  private def prepareHardwareData(p: HardwareProfile): Seq[Seq[Any]] =
    Seq(
      Seq("CPU Model", p.cpuModel, ""), Seq("CPU Vendor", p.cpuVendor, ""), Seq("Physical Cores", p.physicalCores, "cores"),
      Seq("Logical Cores", p.logicalCores, "cores"), Seq("Current Frequency", p.currentFrequencyMhz, "MHz"),
      Seq("Max Frequency", p.maxFrequencyMhz, "MHz"), Seq("Processor ID", p.processorId, ""),
      Seq("L1 Cache", p.l1CacheKb.getOrElse("N/A"), "KB"), Seq("L2 Cache", p.l2CacheKb.getOrElse("N/A"), "KB"),
      Seq("L3 Cache", p.l3CacheKb.getOrElse("N/A"), "KB"), Seq("SMT", p.smtEnabled.map(if _ then "Enabled" else "Disabled").getOrElse("N/A"), ""),
      Seq("Total RAM", p.totalRamGb, "GB"), Seq("OS", p.osName, p.osVersion), Seq("JVM", p.jvmVendor, p.jvmVersion)
    )