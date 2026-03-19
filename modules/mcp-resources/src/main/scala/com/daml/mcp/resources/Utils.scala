package com.daml.mcp.resources

import com.daml.mcp.cli.models.DamlProjectConfig

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
       |    "dependencies": ${formatList(p.dependencies)},
       |    "data-dependencies": ${formatList(p.dataDependencies.map(_.toString))}
       |  }""".stripMargin

  private def formatList(list: Seq[String]): String =
    if (list == null || list.isEmpty) "[]"
    else list.map(s => s""""${escapeJson(s)}"""").mkString("[\n      ", ",\n      ", "\n    ]")

  private def escapeJson(s: String): String =
    if (s == null) ""
    else s.replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "\\r")
          .replace("\t", "\\t")