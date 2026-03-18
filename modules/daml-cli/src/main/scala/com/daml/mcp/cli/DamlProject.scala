package com.daml.mcp.cli

import cats.effect.IO
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

final case class DamlProject(root: Path):

  private val damlYamlPath: Path = root.resolve("daml.yaml")

  def exists: IO[Boolean] = IO.blocking(Files.exists(damlYamlPath))

  def damlYaml: IO[Option[String]] = IO.blocking:
    if Files.exists(damlYamlPath) then Some(Files.readString(damlYamlPath))
    else None

  def sourceDirectories: IO[Seq[Path]] =
    damlYaml.map:
      _.flatMap: content =>
        content.linesIterator
          .dropWhile(l => !l.startsWith("source:"))
          .take(1)
          .map(_.stripPrefix("source:").trim)
          .toList
          .headOption
      .map(src => Seq(root.resolve(src)))
        .getOrElse(Seq(root.resolve("daml")))

  def damlFiles: IO[Seq[Path]] =
    sourceDirectories.flatMap: dirs =>
      IO.blocking:
        dirs
          .filter(Files.exists(_))
          .flatMap: dir =>
            Files
              .walk(dir)
              .iterator()
              .asScala
              .filter(p => p.toString.endsWith(".daml"))
              .toSeq

  def readFile(path: Path): IO[Option[String]] = IO.blocking:
    if Files.exists(path) && path.startsWith(root) then Some(Files.readString(path))
    else None
