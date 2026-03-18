package com.daml.mcp.cli

import cats.effect.IO
import java.nio.file.Path

/** High-level read-only queries over a DAML project. */
final class DamlProjectService(project: DamlProject):

  def listDamlFiles(): IO[Seq[Path]] = project.damlFiles

  def readDamlFile(path: Path): IO[Option[String]] = project.readFile(path)

  def readDamlYaml(): IO[Option[String]] = project.damlYaml

  def listTemplates(): IO[Seq[DamlTemplate]] =
    project.damlFiles.flatMap: paths =>
      IO:
        paths.flatMap: path =>
          val content = java.nio.file.Files.readString(path)
          DamlParser.parseFile(content, path.toString)

  def getTemplate(name: String): IO[Option[DamlTemplate]] =
    listTemplates().map(_.find(_.name == name))

  def listTemplateNames(): IO[Seq[String]] =
    listTemplates().map(_.map(t => s"${t.module}:${t.name}"))

  def getTemplateChoices(templateName: String): IO[Seq[DamlChoice]] =
    getTemplate(templateName).map(_.map(_.choices).getOrElse(Seq.empty))
