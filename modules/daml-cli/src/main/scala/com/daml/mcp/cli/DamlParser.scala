package com.daml.mcp.cli

/** Regex-based parser that extracts template metadata from .daml source files. */
object DamlParser:

  def parseFile(source: String, filePath: String): Seq[DamlTemplate] =
    val moduleName = extractModule(source)
    val templateBlocks = splitTemplateBlocks(source)
    templateBlocks.map(block => parseTemplate(block, moduleName, filePath))

  private def extractModule(source: String): String =
    source.linesIterator
      .map(_.trim)
      .find(_.startsWith("module "))
      .map(_.stripPrefix("module ").takeWhile(_ != ' ').trim)
      .getOrElse("Unknown")

  private val templateStartPattern = """^template\s+(\w+)""".r

  private def splitTemplateBlocks(source: String): Seq[String] =
    val lines = source.linesIterator.toVector
    val starts = lines.zipWithIndex.collect:
      case (line, idx) if templateStartPattern.findFirstIn(line.trim).isDefined => idx

    starts.zipWithIndex.map: (startIdx, i) =>
      val endIdx =
        if i + 1 < starts.length then starts(i + 1)
        else lines.length
      lines.slice(startIdx, endIdx).mkString("\n")

  private def parseTemplate(block: String, moduleName: String, filePath: String): DamlTemplate =
    val name = templateStartPattern
      .findFirstMatchIn(block.linesIterator.next().trim)
      .map(_.group(1))
      .getOrElse("Unknown")

    val fields = extractFields(block)
    val signatories = extractClause(block, "signatory")
    val observers = extractClause(block, "observer")
    val choices = extractChoices(block)

    DamlTemplate(
      name = name,
      module = moduleName,
      fields = fields,
      signatories = signatories,
      observers = observers,
      choices = choices,
      sourceFile = filePath
    )

  private def extractFields(block: String): Seq[DamlField] =
    val lines = block.linesIterator.toVector
    val withIdx =
      lines.indexWhere(_.trim == "with", lines.indexWhere(_.trim.startsWith("template")))
    if withIdx < 0 then return Seq.empty

    lines
      .drop(withIdx + 1)
      .takeWhile(l => l.trim.nonEmpty && !l.trim.startsWith("where") && l.startsWith("  "))
      .map(_.trim)
      .filter(_.contains(":"))
      .filterNot(_.startsWith("--"))
      .map: line =>
        val parts = line.split("\\s*:\\s*", 2)
        DamlField(parts(0).trim, if parts.length > 1 then parts(1).trim else "Unknown")

  private def extractClause(block: String, keyword: String): String =
    block.linesIterator
      .map(_.trim)
      .find(_.startsWith(s"$keyword "))
      .map(_.stripPrefix(s"$keyword ").trim)
      .getOrElse("")

  private val choicePattern = """^\s*choice\s+(\w+)\s*:\s*(.+)""".r

  private def extractChoices(block: String): Seq[DamlChoice] =
    val lines = block.linesIterator.toVector
    val choiceStarts = lines.zipWithIndex.collect:
      case (line, idx) if choicePattern.findFirstIn(line).isDefined => idx

    choiceStarts.map: startIdx =>
      val line = lines(startIdx)
      val (cName, cReturn) = choicePattern.findFirstMatchIn(line) match
        case Some(m) => (m.group(1), m.group(2).trim)
        case None    => ("Unknown", "Unknown")

      val choiceLines = lines.drop(startIdx + 1)
      val params = extractChoiceParams(choiceLines)
      val controller = choiceLines
        .map(_.trim)
        .find(_.startsWith("controller "))
        .map(_.stripPrefix("controller ").trim)
        .getOrElse("")

      DamlChoice(cName, cReturn, params, controller)

  private def extractChoiceParams(lines: Vector[String]): Seq[DamlField] =
    val withIdx = lines.indexWhere(_.trim == "with")
    if withIdx < 0 then return Seq.empty

    lines
      .drop(withIdx + 1)
      .takeWhile(l =>
        l.trim.nonEmpty && !l.trim.startsWith("controller") && !l.trim.startsWith("do")
      )
      .map(_.trim)
      .filter(_.contains(":"))
      .filterNot(_.startsWith("--"))
      .map: line =>
        val parts = line.split("\\s*:\\s*", 2)
        DamlField(parts(0).trim, if parts.length > 1 then parts(1).trim else "Unknown")
