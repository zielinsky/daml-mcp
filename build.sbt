ThisBuild / scalaVersion := "3.3.7"
ThisBuild / organization := "com.daml.mcp"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

val damlVersion       = "3.4.11"
val catsEffectVersion = "3.7.0"
val munitCEVersion    = "2.2.0"
val mcpSdkVersion     = "1.1.0"

val mcpDeps = Seq(
  "io.modelcontextprotocol.sdk" % "mcp-core"          % mcpSdkVersion,
  "io.modelcontextprotocol.sdk" % "mcp-json-jackson2" % mcpSdkVersion % Runtime,
)

lazy val root = (project in file("."))
  .enablePlugins(ScalaUnidocPlugin)
  .aggregate(damlCli, ledgerClient, mcpTools, mcpResources, mcpPrompts, server)
  .settings(
    name    := "daml-mcp",
    publish := {},
    ScalaUnidoc / unidoc / unidocProjectFilter :=
      inProjects(damlCli, ledgerClient, mcpTools, mcpResources, mcpPrompts, server),
  )

lazy val damlCli = (project in file("modules/daml-cli"))
  .settings(
    name := "daml-mcp-daml-cli",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect"       % catsEffectVersion,
      "org.typelevel" %% "munit-cats-effect" % munitCEVersion % Test,
    ),
  )

lazy val ledgerClient = (project in file("modules/ledger-client"))
  .settings(
    name := "daml-mcp-ledger-client",
    libraryDependencies ++= Seq(
      "com.daml"      %  "bindings-java"     % damlVersion,
      "org.typelevel" %% "cats-effect"       % catsEffectVersion,
      "org.typelevel" %% "munit-cats-effect" % munitCEVersion % Test,
    ),
  )

lazy val mcpTools = (project in file("modules/mcp-tools"))
  .dependsOn(damlCli, ledgerClient)
  .settings(
    name := "daml-mcp-tools",
    libraryDependencies ++= mcpDeps,
  )

lazy val mcpResources = (project in file("modules/mcp-resources"))
  .dependsOn(damlCli, ledgerClient)
  .settings(
    name := "daml-mcp-resources",
    libraryDependencies ++= mcpDeps,
  )

lazy val mcpPrompts = (project in file("modules/mcp-prompts"))
  .settings(
    name := "daml-mcp-prompts",
    libraryDependencies ++= mcpDeps,
  )

lazy val server = (project in file("modules/server"))
  .dependsOn(mcpTools, mcpResources, mcpPrompts)
  .settings(
    name := "daml-mcp-server",
    libraryDependencies ++= mcpDeps ++ Seq(
      "jakarta.servlet" % "jakarta.servlet-api" % "6.0.0",
      "org.apache.tomcat.embed" % "tomcat-embed-core" % "10.1.34",
    ),
  )
