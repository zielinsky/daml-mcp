package com.daml.mcp.resources

import cats.effect.unsafe.implicits.global

import com.daml.mcp.cli.DamlProjectService
import com.daml.mcp.cli.models.DamlProjectConfig

import io.modelcontextprotocol.server.McpServerFeatures.{
  SyncResourceSpecification,
  SyncResourceTemplateSpecification
}
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{ReadResourceResult, Resource, ResourceTemplate}

import java.util as ju

class ResourceRegistry(projectService: DamlProjectService):

  val damlMainProjectResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://mainProjectConfig")
      .name("DAML Main Project Configuration")
      .description("The configuration of the main DAML project in the workspace")
      .mimeType("application/json")
      .build(),
    (_, _) =>
      val project = projectService.mainDamlProject().unsafeRunSync()
      val content = Utils.damlProjectConfigToJson(project)
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://mainProjectConfig", "application/json", content)))
  )

  val damlProjectsResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://projects")
      .name("DAML Projects")
      .description("List of all DAML projects in the workspace")
      .mimeType("application/json")
      .build(),
    (_, _) =>
      val projects = projectService.listDamlProjects().unsafeRunSync()
      val content = if projects.isEmpty then "[]" else Utils.formatProjectsAsJson(projects)
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://projects", "application/json", content)))
  )

  val dependencyGraphResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://dependencyGraph")
      .name("DAML Dependency Graph")
      .description("Dependency graph showing which DAML projects depend on which (via data-dependencies)")
      .mimeType("application/json")
      .build(),
    (_, _) =>
      val graph = projectService.dependencyGraph().unsafeRunSync()
      val content = Utils.dependencyGraphToJson(graph)
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://dependencyGraph", "application/json", content)))
  )

  val buildOrderResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://buildOrder")
      .name("DAML Build Order")
      .description("Correct topological build order for DAML projects based on their dependencies")
      .mimeType("application/json")
      .build(),
    (_, _) =>
      val steps = projectService.buildOrder().unsafeRunSync()
      val content = Utils.buildOrderToJson(steps)
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://buildOrder", "application/json", content)))
  )

  val allTemplatesResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://allTemplates")
      .name("DAML All Templates")
      .description(
        "Structured summary of all DAML templates and their choices across all projects in the workspace. " +
        "Shows each project's templates with fields, and each choice with its parameters."
      )
      .mimeType("text/plain")
      .build(),
    (_, _) =>
      val content = projectService.allTemplatesSummary().unsafeRunSync()
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://allTemplates", "text/plain", content)))
  )

  val all: Seq[SyncResourceSpecification] =
    Seq(damlMainProjectResource, damlProjectsResource, dependencyGraphResource, buildOrderResource, allTemplatesResource)
