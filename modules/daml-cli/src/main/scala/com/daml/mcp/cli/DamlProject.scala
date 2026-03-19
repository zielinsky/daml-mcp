package com.daml.mcp.cli

import cats.effect.IO
import com.daml.mcp.cli.models.DamlProjectConfig
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class DamlProject(root: Path):

  def damlProjectYamlPaths: IO[Seq[Path]] = IO.blocking:
    val rootYaml = root.resolve("daml.yaml")
    val fromRoot = if Files.exists(rootYaml) then Seq(rootYaml) else Seq.empty
    val fromTree = Files
      .walk(root)
      .iterator()
      .asScala
      .filter(p => p.getFileName.toString == "daml.yaml" && p != rootYaml)
      .toSeq
    (fromRoot ++ fromTree).distinct

  def damlProjects: IO[Seq[DamlProjectConfig]] =
    damlProjectYamlPaths.flatMap: paths =>
      IO.traverse(paths): path =>
        IO.blocking:
          val content = Files.readString(path)
          DamlYamlParser.parseDamlProjectConfig(content)
      .map(_.flatten)

  def readFile(path: Path): IO[Option[String]] = IO.blocking:
    if Files.exists(path) && path.startsWith(root) then Some(Files.readString(path))
    else None
