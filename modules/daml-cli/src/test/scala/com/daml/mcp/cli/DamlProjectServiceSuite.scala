package com.daml.mcp.cli

import cats.effect.IO
import munit.CatsEffectSuite
import java.nio.file.{Files, Path}

class DamlProjectServiceSuite extends CatsEffectSuite:

  private val tempDir = ResourceFunFixture[Path](
    cats.effect.Resource.make(
      IO.blocking(Files.createTempDirectory("daml-mcp-test"))
    )(dir =>
      IO.blocking:
        Files.walk(dir).sorted(java.util.Comparator.reverseOrder()).forEach(Files.delete(_))
    )
  )

  private def writeFile(dir: Path, relativePath: String, content: String): IO[Path] =
    IO.blocking:
      val path = dir.resolve(relativePath)
      Files.createDirectories(path.getParent)
      Files.writeString(path, content)

  private val damlYaml =
    """sdk-version: 3.4.11
      |name: test-project
      |version: 1.0.0
      |source: daml
      |""".stripMargin

  private val assetDaml =
    """module Asset where
      |
      |template Asset
      |  with
      |    issuer : Party
      |    owner  : Party
      |    name   : Text
      |  where
      |    signatory issuer
      |    observer owner
      |    choice Give : ContractId Asset
      |      with
      |        newOwner : Party
      |      controller owner
      |      do create this with owner = newOwner
      |""".stripMargin

  private val tokenDaml =
    """module Token where
      |
      |template Token
      |  with
      |    owner : Party
      |    value : Int
      |  where
      |    signatory owner
      |""".stripMargin

  tempDir.test("listDamlFiles returns all .daml files"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      _ <- writeFile(dir, "daml/Asset.daml", assetDaml)
      _ <- writeFile(dir, "daml/Token.daml", tokenDaml)
      svc = DamlProjectService(DamlProject(dir))
      files <- svc.listDamlFiles()
    yield
      assertEquals(files.length, 2)
      assert(files.exists(_.toString.endsWith("Asset.daml")))
      assert(files.exists(_.toString.endsWith("Token.daml")))

  tempDir.test("listTemplates returns all templates across files"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      _ <- writeFile(dir, "daml/Asset.daml", assetDaml)
      _ <- writeFile(dir, "daml/Token.daml", tokenDaml)
      svc = DamlProjectService(DamlProject(dir))
      templates <- svc.listTemplates()
    yield
      assertEquals(templates.length, 2)
      assert(templates.exists(_.name == "Asset"))
      assert(templates.exists(_.name == "Token"))

  tempDir.test("listTemplateNames returns qualified names"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      _ <- writeFile(dir, "daml/Asset.daml", assetDaml)
      svc = DamlProjectService(DamlProject(dir))
      names <- svc.listTemplateNames()
    yield assertEquals(names, Seq("Asset:Asset"))

  tempDir.test("getTemplate finds by name"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      _ <- writeFile(dir, "daml/Asset.daml", assetDaml)
      svc = DamlProjectService(DamlProject(dir))
      found <- svc.getTemplate("Asset")
      notFound <- svc.getTemplate("Nonexistent")
    yield
      assert(found.isDefined)
      assertEquals(found.get.fields.length, 3)
      assert(notFound.isEmpty)

  tempDir.test("getTemplateChoices returns choices for template"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      _ <- writeFile(dir, "daml/Asset.daml", assetDaml)
      svc = DamlProjectService(DamlProject(dir))
      choices <- svc.getTemplateChoices("Asset")
    yield
      assertEquals(choices.length, 1)
      assertEquals(choices.head.name, "Give")

  tempDir.test("readDamlYaml returns project config"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", damlYaml)
      svc = DamlProjectService(DamlProject(dir))
      yaml <- svc.readDamlYaml()
    yield
      assert(yaml.isDefined)
      assert(yaml.get.contains("test-project"))

  tempDir.test("returns empty when no daml.yaml"): dir =>
    val svc = DamlProjectService(DamlProject(dir))
    for
      yaml <- svc.readDamlYaml()
      files <- svc.listDamlFiles()
    yield
      assert(yaml.isEmpty)
      assertEquals(files.length, 0)
