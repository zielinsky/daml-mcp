package com.daml.mcp.resources

import cats.effect.unsafe.implicits.global
import com.daml.mcp.cli.DamlProjectService
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
      .uri("daml://config")
      .name("daml.yaml")
      .description("DAML project configuration (daml.yaml)")
      .mimeType("text/yaml")
      .build(),
    (_, _) =>
      val content = projectService.readDamlYaml().unsafeRunSync().getOrElse("No daml.yaml found.")
      ReadResourceResult(
        ju.List.of(McpSchema.TextResourceContents("daml://config", "text/yaml", content))
      )
  )

  val templateListResource: SyncResourceSpecification = SyncResourceSpecification(
    Resource
      .builder()
      .uri("daml://templates")
      .name("DAML Templates")
      .description("List of all DAML templates in the project with qualified names")
      .mimeType("text/plain")
      .build(),
    (_, _) =>
      val names = projectService.listTemplateNames().unsafeRunSync().mkString("\n")
      val text = if names.isEmpty then "No templates found." else names
      ReadResourceResult(
        ju.List.of(McpSchema.TextResourceContents("daml://templates", "text/plain", text))
      )
  )

  val sourceFileTemplate: SyncResourceTemplateSpecification = SyncResourceTemplateSpecification(
    ResourceTemplate
      .builder()
      .uriTemplate("daml://source/{path}")
      .name("DAML Source File")
      .description("Read a .daml source file by path")
      .mimeType("text/plain")
      .build(),
    (_, request) =>
      val uri = request.uri()
      val path = uri.stripPrefix("daml://source/")
      val filePath = java.nio.file.Path.of(path)
      val content =
        projectService.readDamlFile(filePath).unsafeRunSync().getOrElse(s"File not found: $path")
      ReadResourceResult(ju.List.of(McpSchema.TextResourceContents(uri, "text/plain", content)))
  )

  val allResources: Seq[SyncResourceSpecification] =
    Seq(damlYamlResource, templateListResource)

  val allResourceTemplates: Seq[SyncResourceTemplateSpecification] =
    Seq(sourceFileTemplate)
