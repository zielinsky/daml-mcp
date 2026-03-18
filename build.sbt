ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "com.daml.mcp"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(ledgerClient, mcpTools, mcpResources, mcpPrompts, server)
  .settings(
    name    := "daml-mcp",
    publish := {},
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inProjects(ledgerClient, mcpTools, mcpResources, mcpPrompts, server),
  )

lazy val ledgerClient = (project in file("modules/ledger-client"))
  .settings(
    name := "daml-mcp-ledger-client",
    libraryDependencies ++= Seq(
      "com.daml" % "bindings-java" % "3.4.11",
    ),
  )

lazy val mcpTools = (project in file("modules/mcp-tools"))
  .dependsOn(ledgerClient)
  .settings(
    name := "daml-mcp-tools",
  )

lazy val mcpResources = (project in file("modules/mcp-resources"))
  .dependsOn(ledgerClient)
  .settings(
    name := "daml-mcp-resources",
  )

lazy val mcpPrompts = (project in file("modules/mcp-prompts"))
  .settings(
    name := "daml-mcp-prompts",
  )

lazy val server = (project in file("modules/server"))
  .dependsOn(mcpTools, mcpResources, mcpPrompts)
  .settings(
    name := "daml-mcp-server",
  )
