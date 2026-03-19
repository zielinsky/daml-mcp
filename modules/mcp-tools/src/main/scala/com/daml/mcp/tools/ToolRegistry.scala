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

  private def textResult(text: String): CallToolResult =
    CallToolResult.builder()
      .content(ju.List.of(McpSchema.TextContent(text)))
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

  val all: Seq[SyncToolSpecification] = Seq(damlBuild, damlClean, damlTest)
