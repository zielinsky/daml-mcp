package com.daml.mcp.tools

import cats.effect.unsafe.implicits.global
import com.daml.mcp.cli.{DamlProjectService, DamlTemplate, DamlChoice}
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{CallToolResult, JsonSchema, Tool}

import java.util as ju
import scala.jdk.CollectionConverters.*

class ToolRegistry(projectService: DamlProjectService):

  val listTemplates: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool
        .builder()
        .name("daml_list_templates")
        .description(
          "List all DAML templates in the project with their module, fields, signatories, observers, and choices."
        )
        .inputSchema(emptySchema)
        .build()
    )
    .callHandler: (_, _) =>
      val templates = projectService.listTemplates().unsafeRunSync()
      val text = templates.map(formatTemplate).mkString("\n---\n")
      textResult(if text.isEmpty then "No templates found." else text)
    .build()

  val getTemplate: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool
        .builder()
        .name("daml_get_template")
        .description(
          "Get detailed information about a specific DAML template by name, including fields, choices, signatories, and observers."
        )
        .inputSchema(schemaWithRequired("name" -> "string"))
        .build()
    )
    .callHandler: (_, request) =>
      val name = request.arguments().get("name").toString
      projectService.getTemplate(name).unsafeRunSync() match
        case Some(t) => textResult(formatTemplate(t))
        case None    => textResult(s"Template '$name' not found.")
    .build()

  val listFiles: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool
        .builder()
        .name("daml_list_files")
        .description("List all .daml source files in the project.")
        .inputSchema(emptySchema)
        .build()
    )
    .callHandler: (_, _) =>
      val files = projectService.listDamlFiles().unsafeRunSync()
      val text = files.map(_.toString).mkString("\n")
      textResult(if text.isEmpty then "No .daml files found." else text)
    .build()

  val readFile: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool
        .builder()
        .name("daml_read_file")
        .description("Read the contents of a .daml source file by its path.")
        .inputSchema(schemaWithRequired("path" -> "string"))
        .build()
    )
    .callHandler: (_, request) =>
      val path = java.nio.file.Path.of(request.arguments().get("path").toString)
      projectService.readDamlFile(path).unsafeRunSync() match
        case Some(content) => textResult(content)
        case None          => textResult(s"File not found: $path")
    .build()

  val readDamlYaml: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool
        .builder()
        .name("daml_read_config")
        .description("Read the daml.yaml project configuration file.")
        .inputSchema(emptySchema)
        .build()
    )
    .callHandler: (_, _) =>
      projectService.readDamlYaml().unsafeRunSync() match
        case Some(content) => textResult(content)
        case None          => textResult("No daml.yaml found.")
    .build()

  val all: Seq[SyncToolSpecification] =
    Seq(listTemplates, getTemplate, listFiles, readFile, readDamlYaml)

  private def textResult(text: String): CallToolResult =
    CallToolResult
      .builder()
      .content(ju.List.of(McpSchema.TextContent(text)))
      .build()

  private val emptySchema: JsonSchema =
    JsonSchema("object", ju.Map.of(), ju.List.of(), null, null, null)

  private def schemaWithRequired(props: (String, String)*): JsonSchema =
    val properties = new ju.HashMap[String, Object]()
    props.foreach: (name, tpe) =>
      properties.put(name, ju.Map.of[String, Object]("type", tpe))
    JsonSchema("object", properties, props.map(_._1).toList.asJava, null, null, null)

  private def formatTemplate(t: DamlTemplate): String =
    val fields = t.fields.map(f => s"    ${f.name} : ${f.fieldType}").mkString("\n")
    val choices = t.choices.map(formatChoice).mkString("\n")
    s"""template ${t.module}:${t.name}
       |  source: ${t.sourceFile}
       |  fields:
       |$fields
       |  signatories: ${t.signatories}
       |  observers: ${t.observers}
       |  choices:
       |$choices""".stripMargin

  private def formatChoice(c: DamlChoice): String =
    val params = c.params.map(p => s"${p.name}: ${p.fieldType}").mkString(", ")
    s"    ${c.name}($params) : ${c.returnType} [controller: ${c.controller}]"
