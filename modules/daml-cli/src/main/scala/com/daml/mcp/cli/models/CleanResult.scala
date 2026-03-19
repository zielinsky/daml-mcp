package com.daml.mcp.cli.models

final case class CleanResult(
    project: String,
    removedFiles: Int
)
