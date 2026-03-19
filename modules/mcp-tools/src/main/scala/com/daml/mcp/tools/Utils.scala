package com.daml.mcp.tools

import com.daml.mcp.cli.models.{BuildResult, CleanResult}

object Utils:

  def formatBuildResults(results: Seq[BuildResult]): String =
    if results.isEmpty then return "No projects to build."

    val totalMs = results.map(_.durationMs).sum
    val failed = results.filterNot(_.success)
    val header =
      if failed.isEmpty then s"BUILD SUCCESSFUL (${results.length} projects, ${totalMs}ms)"
      else s"BUILD FAILED (${failed.length}/${results.length} projects failed, ${totalMs}ms)"

    val details = results.map: r =>
      val status = if r.success then "OK" else "FAILED"
      val lines = Seq(
        s"[$status] Step ${r.step}: ${r.project} (${r.durationMs}ms)",
        r.output
      )
      lines.mkString("\n")

    (header +: details).mkString("\n\n")

  def formatCleanResults(results: Seq[CleanResult]): String =
    if results.isEmpty then return "No projects to clean."

    val totalRemoved = results.map(_.removedFiles).sum
    val header = s"CLEAN COMPLETE ($totalRemoved files removed from ${results.length} projects)"

    val details = results.map: r =>
      if r.removedFiles > 0 then s"  ${r.project}: ${r.removedFiles} files removed"
      else s"  ${r.project}: already clean"

    (header +: details).mkString("\n")