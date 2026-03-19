package com.daml.mcp.resources

import com.daml.mcp.cli.models.{BuildStep, DamlProjectConfig}

object Utils:

  def formatProjectsAsJson(projects: Seq[DamlProjectConfig]): String =
    val projectJsons = projects.map(damlProjectConfigToJson)
    s"[\n${projectJsons.mkString(",\n")}\n]"

  def damlProjectConfigToJson(p: DamlProjectConfig): String =
    s"""  {
       |    "path": "${escapeJson(p.path.toString)}",
       |    "sdk-version": "${escapeJson(p.sdkVersion)}",
       |    "name": "${escapeJson(p.name)}",
       |    "source": "${escapeJson(p.source)}",
       |    "sourcePath": "${escapeJson(p.sourcePath.toString)}",
       |    "version": "${escapeJson(p.version)}",
       |    "outputDar": "${escapeJson(p.outputDar.toString)}",
       |    "dependencies": ${formatStringList(p.dependencies)},
       |    "data-dependencies": ${formatStringList(p.dataDependencies.map(_.toString))},
       |    "damlFiles": ${formatStringList(p.damlFiles.map(_.toString))}
       |  }""".stripMargin

  def dependencyGraphToJson(graph: Map[String, Seq[String]]): String =
    val entries = graph.toSeq.sortBy(_._1).map: (name, deps) =>
      s"""    "${escapeJson(name)}": ${formatStringList(deps)}"""
    s"{\n  \"graph\": {\n${entries.mkString(",\n")}\n  }\n}"

  def buildOrderToJson(steps: Seq[BuildStep]): String =
    val entries = steps.map: s =>
      val depsList = s.dependsOn.mkString("[", ", ", "]")
      s"""    { "step": ${s.step}, "project": "${escapeJson(s.project)}", "dependsOn": $depsList }"""
    s"{\n  \"buildOrder\": [\n${entries.mkString(",\n")}\n  ]\n}"

  private def formatStringList(list: Seq[String]): String =
    if list == null || list.isEmpty then "[]"
    else list.map(s => s""""${escapeJson(s)}"""").mkString("[\n      ", ",\n      ", "\n    ]")

  private def escapeJson(s: String): String =
    if s == null then ""
    else s.replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "\\r")
          .replace("\t", "\\t")