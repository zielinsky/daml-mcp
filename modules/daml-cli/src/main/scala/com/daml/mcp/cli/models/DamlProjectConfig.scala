package com.daml.mcp.cli.models

import java.nio.file.Path

/** Parsed DAML project configuration (daml.yaml).
  * Contains only: sdk-version, name, source, version, dependencies, data-dependencies.
  */
final case class DamlProjectConfig(
    sdkVersion: String,
    name: String,
    path: Path,
    source: String,
    sourcePath: Path,
    version: String,
    dependencies: Seq[String],
    dataDependencies: Seq[Path]
)
