package com.daml.mcp.tools

import cats.effect.unsafe.implicits.global
import com.daml.mcp.cli.DamlProjectService
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{CallToolResult, JsonSchema, Tool}

import java.util as ju
import scala.jdk.CollectionConverters.*

class ToolRegistry(projectService: DamlProjectService):

  private val emptySchema: JsonSchema =
    JsonSchema("object", ju.Map.of(), ju.List.of(), null, null, null)

  private val projectNameSchema: JsonSchema =
    val properties = new ju.HashMap[String, Object]()
    properties.put("projectName", ju.Map.of[String, Object]("type", "string"))
    JsonSchema("object", properties, ju.List.of("projectName"), null, null, null)

  private val createProjectSchema: JsonSchema =
    val properties = new ju.HashMap[String, Object]()
    properties.put("projectName", ju.Map.of[String, Object]("type", "string", "description", "Name of the new project (will also be the directory name)"))
    properties.put("template", ju.Map.of[String, Object]("type", "string", "description", "Optional daml new template name (e.g. empty-skeleton)"))
    JsonSchema("object", properties, ju.List.of("projectName"), null, null, null)

  private def textResult(text: String): CallToolResult =
    CallToolResult.builder()
      .content(ju.List.of(McpSchema.TextContent(text)))
      .build()

  val damlBuildSingle: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_build_single")
        .description(
          "Build a single DAML project by name. " +
          "Runs `daml build` only for the specified sub-project. " +
          "Use daml://projects resource to discover available project names."
        )
        .inputSchema(projectNameSchema)
        .build()
    )
    .callHandler: (_, request) =>
      val projectName = request.arguments().get("projectName").toString
      val result = projectService.buildSingle(projectName).unsafeRunSync()
      textResult(Utils.formatBuildResults(Seq(result)))
    .build()

  val damlCleanSingle: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_clean_single")
        .description(
          "Clean a single DAML project by name. " +
          "Removes .daml/ directory and *.dar files only for the specified sub-project. " +
          "Use daml://projects resource to discover available project names."
        )
        .inputSchema(projectNameSchema)
        .build()
    )
    .callHandler: (_, request) =>
      val projectName = request.arguments().get("projectName").toString
      val result = projectService.cleanSingle(projectName).unsafeRunSync()
      if result.removedFiles == -1 then
        textResult(s"ERROR: Project '$projectName' not found. Use daml://projects to list available projects.")
      else
        textResult(Utils.formatCleanResults(Seq(result)))
    .build()

  val damlCreateProject: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_create_project")
        .description(
          "Scaffold a new DAML project in the workspace using `daml new`. " +
          "Creates a new sub-project directory with a daml.yaml and starter source files. " +
          "Optionally accepts a template name (e.g. empty-skeleton)."
        )
        .inputSchema(createProjectSchema)
        .build()
    )
    .callHandler: (_, request) =>
      val projectName = request.arguments().get("projectName").toString
      val template = Option(request.arguments().get("template")).map(_.toString)
      val result = projectService.createProject(projectName, template).unsafeRunSync()
      textResult(result)
    .build()

  val damlBuild: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_build")
        .description(
          "Build all DAML projects in the workspace in correct dependency order. " +
          "Runs `daml build` for each sub-project following the topological build graph. " +
          "Stops on first failure."
        )
        .inputSchema(emptySchema)
        .build()
    )
    .callHandler: (_, _) =>
      val results = projectService.buildAll().unsafeRunSync()
      textResult(Utils.formatBuildResults(results))
    .build()

  val damlClean: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_clean")
        .description(
          "Clean all DAML projects in the workspace. " +
          "Removes .daml/ directories and *.dar files from each sub-project."
        )
        .inputSchema(emptySchema)
        .build()
    )
    .callHandler: (_, _) =>
      val results = projectService.cleanAll().unsafeRunSync()
      textResult(Utils.formatCleanResults(results))
    .build()

  val damlTest: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_test")
        .description(
          "Runs daml test for a specific DAML project to verify syntax, typecheck, " +
          "and execute business logic scripts. Returns the compiler and test runner output."
        )
        .inputSchema(projectNameSchema)
        .build()
    )
    .callHandler: (_, request) =>
      val projectName = request.arguments().get("projectName").toString
      val result = projectService.runDamlTest(projectName).unsafeRunSync()
      textResult(result)
    .build()

  val damlInspectDar: SyncToolSpecification = SyncToolSpecification
    .builder()
    .tool(
      Tool.builder()
        .name("daml_inspect_dar")
        .description(
          "Inspects a compiled DAML archive (.dar) and returns its exposed API, templates, " +
          "interfaces, and data types. Useful for understanding external dependencies before " +
          "integrating with them."
        )
        .inputSchema(projectNameSchema)
        .build()
    )
    .callHandler: (_, request) =>
      val projectName = request.arguments().get("projectName").toString
      val result = projectService.inspectDar(projectName).unsafeRunSync()
      textResult(result)
    .build()

  val all: Seq[SyncToolSpecification] = Seq(
    damlBuild, damlBuildSingle,
    damlClean, damlCleanSingle,
    damlTest, damlInspectDar,
    damlCreateProject
  )
