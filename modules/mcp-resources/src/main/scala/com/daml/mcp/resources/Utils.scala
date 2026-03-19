package com.daml.mcp.resources

import com.daml.mcp.cli.models.DamlProjectConfig

object Utils:

  def formatProjectsAsJson(projects: Seq[DamlProjectConfig]): String =
    val projectJsons = projects.map(projectToJson)
    s"[\n${projectJsons.mkString(",\n")}\n]"

  private def projectToJson(p: DamlProjectConfig): String =
    s"""  {
       |    "sdk-version": "${escapeJson(p.sdkVersion)}",
       |    "name": "${escapeJson(p.name)}",
       |    "source": "${escapeJson(p.source)}",
       |    "version": "${escapeJson(p.version)}",
       |    "dependencies": ${formatList(p.dependencies)},
       |    "data-dependencies": ${formatList(p.dataDependencies)}
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