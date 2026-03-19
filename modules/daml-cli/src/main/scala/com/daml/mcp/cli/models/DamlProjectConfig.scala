package com.daml.mcp.cli.models

/** Parsed DAML project configuration (daml.yaml).
  * Contains only: sdk-version, name, source, version, dependencies, data-dependencies.
  */
final case class DamlProjectConfig(
    sdkVersion: String,
    name: String,
    source: String,
    version: String,
    dependencies: Seq[String],
    dataDependencies: Seq[String]
)
