package com.daml.mcp.cli

import com.daml.mcp.cli.models.DamlProjectConfig

import org.yaml.snakeyaml.Yaml

import scala.jdk.CollectionConverters.*
import java.nio.file.Path

object DamlParser:

  def parseDamlProjectConfig(rootPath: Path, path: Path, content: String): Option[DamlProjectConfig] =
    try
      val yaml = Yaml()
      val map  = yaml.load(content).asInstanceOf[java.util.Map[String, Any]]
      if map == null then None
      else
        val m = map.asScala
        val name = getString(m, "name").getOrElse("")
        val projectDir = path.getParent
        Some(
          DamlProjectConfig(
            path = rootPath.resolve(path).normalize(),
            sdkVersion = getString(m, "sdk-version").getOrElse(""),
            name = name,
            source = getString(m, "source").getOrElse("daml"),
            sourcePath = projectDir.resolve(getString(m, "source").getOrElse("daml")),
            version = getString(m, "version").getOrElse(""),
            dependencies = getStringList(m, "dependencies"),
            dataDependencies = getStringList(m, "data-dependencies").map(s => projectDir.resolve(s).normalize()),
            damlFiles = Seq.empty,
            outputDar = projectDir.resolve(name.toLowerCase + ".dar")
          )
        )
    catch case _: Exception => None

  private def getString(m: scala.collection.mutable.Map[String, Any], key: String): Option[String] =
    m.get(key).collect { case s: String => s }

  private def getStringList(m: scala.collection.mutable.Map[String, Any], key: String): Seq[String] =
    m.get(key) match
      case Some(list: java.util.List[?]) =>
        list.asScala.toSeq.collect { case s: String => s }
      case Some(list: java.lang.Iterable[?]) =>
        list.asScala.toSeq.collect { case s: String => s }
      case _ => Seq.empty
