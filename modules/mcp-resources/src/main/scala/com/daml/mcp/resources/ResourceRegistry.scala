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

  val all: Seq[SyncResourceSpecification] =
    Seq(damlMainProjectResource, damlProjectsResource)
