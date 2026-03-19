package com.daml.mcp.cli

import cats.effect.IO
import com.daml.mcp.cli.models.DamlProjectConfig
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class DamlProject(root: Path):

  def damlProjectYamlPaths: IO[Seq[Path]] = IO.blocking:
    val rootYaml = root.resolve("daml.yaml")
    val fromTree = Files
      .walk(root)
      .iterator()
      .asScala
      .filter(p => p.getFileName.toString == "daml.yaml" && p != rootYaml)
      .toSeq
    fromTree.distinct

  def damlProjects: IO[Seq[DamlProjectConfig]] =
    damlProjectYamlPaths.flatMap: paths =>
      IO.traverse(paths): path =>
        IO.blocking:
          val content = Files.readString(path)
          DamlParser.parseDamlProjectConfig(root, path, content).map(enrichWithFiles)
      .map(_.flatten)

  def mainDamlProject: IO[DamlProjectConfig] = IO.blocking:
    val rootYaml = root.resolve("daml.yaml")
    if Files.exists(rootYaml) then
      val content = Files.readString(rootYaml)
      val config = DamlParser.parseDamlProjectConfig(root, rootYaml, content)
        .getOrElse(throw new RuntimeException("Failed to parse main DAML project configuration"))
      enrichWithFiles(config)
    else throw new RuntimeException("No main DAML project configuration found")

  private def enrichWithFiles(config: DamlProjectConfig): DamlProjectConfig =
    val sourceDir = config.sourcePath
    val damlFiles =
      if Files.exists(sourceDir) && Files.isDirectory(sourceDir) then
        Files.walk(sourceDir).iterator().asScala
          .filter(p => p.toString.endsWith(".daml"))
          .toSeq.sorted
      else Seq.empty
    config.copy(damlFiles = damlFiles)

  def readFile(path: Path): IO[Option[String]] = IO.blocking:
    if Files.exists(path) && path.startsWith(root) then Some(Files.readString(path))
    else None
