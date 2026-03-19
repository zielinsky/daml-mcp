package com.daml.mcp.cli

import cats.effect.IO
import com.daml.mcp.cli.models.DamlProjectConfig
import java.nio.file.Path
import com.daml.mcp.cli.models.{DamlChoice, DamlTemplate}

/** High-level read-only queries over a DAML project. */
final class DamlProjectService(project: DamlProject):

  def listDamlProjects(): IO[Seq[DamlProjectConfig]] = project.damlProjects
  
