package com.daml.mcp.tools

import cats.effect.unsafe.implicits.global
import com.daml.mcp.cli.DamlProjectService
import com.daml.mcp.cli.models.{DamlTemplate, DamlChoice}
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification
import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.{CallToolResult, JsonSchema, Tool}

import java.util as ju
import scala.jdk.CollectionConverters.*

class ToolRegistry(projectService: DamlProjectService):


    val all: Seq[SyncToolSpecification] = Seq()