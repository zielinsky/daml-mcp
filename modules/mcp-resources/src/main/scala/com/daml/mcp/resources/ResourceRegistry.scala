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

  val damlYamlResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://projects")
      .name("DAML Projects")
      .description("List of all DAML projects in the workspace")
      .mimeType("text/yaml")
      .build(),
    (_, _) =>
      val projects = projectService.listDamlProjects().unsafeRunSync()
      val content = if projects.isEmpty then "[]" else Utils.formatProjectsAsJson(projects)
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents("daml://projects", "application/json", content)))
  )

  val all: Seq[SyncResourceSpecification] =
    Seq(damlYamlResource)
