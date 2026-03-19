package com.daml.mcp.server

import com.daml.mcp.cli.{DamlProject, DamlProjectService}
import com.daml.mcp.prompts.PromptRegistry
import com.daml.mcp.resources.ResourceRegistry
import com.daml.mcp.tools.ToolRegistry

import io.modelcontextprotocol.json.McpJsonDefaults
import io.modelcontextprotocol.server.McpServer
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider

import org.apache.catalina.startup.Tomcat

import java.io.File
import java.nio.file.Path

@main def run(args: String*): Unit =
  val port = args.find(_.startsWith("-port=")).map(_.stripPrefix("-port=")).map(_.toInt).getOrElse(8080)
  val projectPath = args.find(_.startsWith("-path=")).map(_.stripPrefix("-path=")).orElse(Option(System.getenv("DAML_PROJECT_PATH"))).getOrElse(".")
  val project = DamlProject(Path.of(projectPath).toAbsolutePath.normalize)
  val projectService = DamlProjectService(project)

  val tools = ToolRegistry(projectService)
  val resources = ResourceRegistry(projectService)
  val prompts = PromptRegistry()

  val transport = HttpServletStreamableServerTransportProvider.builder()
    .jsonMapper(McpJsonDefaults.getMapper())
    .mcpEndpoint("/mcp")
    .build()

  val mcpServer = McpServer
    .sync(transport)
    .serverInfo("daml-mcp", "0.1.0")
    .capabilities(
      ServerCapabilities
        .builder()
        .tools(true)
        .resources(false, true)
        .prompts(true)
        .logging()
        .build()
    )
    .build()

  tools.all.foreach(mcpServer.addTool)
  resources.all.foreach(mcpServer.addResource)
  prompts.all.foreach(mcpServer.addPrompt)

  val tomcat = Tomcat()
  tomcat.setPort(port)
  tomcat.setBaseDir(System.getProperty("java.io.tmpdir"))
  tomcat.getConnector()

  val ctx = tomcat.addContext("", new File(".").getAbsolutePath)
  Tomcat.addServlet(ctx, "mcp", transport)
  ctx.addServletMappingDecoded("/mcp", "mcp")
  ctx.addServletMappingDecoded("/mcp/*", "mcp")

  tomcat.start()
  System.err.println(s"DAML MCP Server: http://localhost:$port/mcp")
  System.err.println(s"Project root: $projectPath (resolved: ${project.root})")
  tomcat.getServer.await()