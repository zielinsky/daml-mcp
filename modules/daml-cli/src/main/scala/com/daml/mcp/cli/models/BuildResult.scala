package com.daml.mcp.cli.models

final case class BuildResult(
    project: String,
    step: Int,
    success: Boolean,
    output: String,
    durationMs: Long
)
