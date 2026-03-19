package com.daml.mcp.cli.models

final case class BuildStep(
    step: Int,
    project: String,
    dependsOn: Seq[Int]
)
