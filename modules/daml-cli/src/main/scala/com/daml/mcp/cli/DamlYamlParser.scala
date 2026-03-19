package com.daml.mcp.cli

import com.daml.mcp.cli.models.DamlProjectConfig

import org.yaml.snakeyaml.Yaml

import scala.jdk.CollectionConverters.*
import java.nio.file.Path

object DamlYamlParser:

  def parseDamlProjectConfig(rootPath: Path, path: Path, content: String): Option[DamlProjectConfig] =
    try
      val yaml = Yaml()
      val map  = yaml.load(content).asInstanceOf[java.util.Map[String, Any]]
      if map == null then None
      else
        val m = map.asScala
        Some(
          DamlProjectConfig(
            path = rootPath.resolve(path).normalize(),
            sdkVersion = getString(m, "sdk-version").getOrElse(""),
            name = getString(m, "name").getOrElse(""),
            source = getString(m, "source").getOrElse("daml"),
            sourcePath = path.getParent.resolve(getString(m, "source").getOrElse("daml")),
            version = getString(m, "version").getOrElse(""),
            dependencies = getStringList(m, "dependencies"),
            dataDependencies = getStringList(m, "data-dependencies").map(s => path.getParent.resolve(s).normalize())
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
