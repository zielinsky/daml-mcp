package com.daml.mcp.cli

import cats.effect.IO
import com.daml.mcp.cli.models.BuildStep
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

  private val mainDamlYaml =
    """sdk-version: 2.5.3
      |name: MyWorkspace
      |version: 1.0.0
      |source: daml
      |""".stripMargin

  private def subProjectYaml(name: String, dataDeps: Seq[String] = Seq.empty): String =
    val depsSection =
      if dataDeps.isEmpty then ""
      else dataDeps.map(d => s"  - $d").mkString("data-dependencies:\n", "\n", "\n")
    s"""sdk-version: 2.5.3
       |name: $name
       |version: 0.0.1
       |source: daml
       |dependencies:
       |  - daml-prim
       |  - daml-stdlib
       |$depsSection""".stripMargin

  // --- listDamlProjects ---

  tempDir.test("listDamlProjects returns sub-projects (not root)"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/Account/daml.yaml", subProjectYaml("Account", Seq("../Asset/asset.dar")))
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      assertEquals(projects.map(_.name).toSet, Set("Asset", "Account"))

  tempDir.test("listDamlProjects returns empty when no sub-projects"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      assertEquals(projects.length, 0)

  tempDir.test("listDamlProjects parses data-dependencies as resolved paths"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/Account/daml.yaml", subProjectYaml("Account", Seq("../Asset/asset.dar")))
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      val account = projects.find(_.name == "Account").get
      assertEquals(account.dataDependencies.length, 1)
      assert(account.dataDependencies.head.toString.contains("Asset"))

  tempDir.test("listDamlProjects includes damlFiles from source directory"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/Asset/daml/Asset.daml", "module Asset where")
      _ <- writeFile(dir, "main/Asset/daml/Helper.daml", "module Helper where")
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      val asset = projects.find(_.name == "Asset").get
      assertEquals(asset.damlFiles.length, 2)
      assert(asset.damlFiles.exists(_.toString.endsWith("Asset.daml")))
      assert(asset.damlFiles.exists(_.toString.endsWith("Helper.daml")))

  tempDir.test("listDamlProjects has empty damlFiles when source dir missing"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Empty/daml.yaml", subProjectYaml("Empty"))
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      val empty = projects.find(_.name == "Empty").get
      assertEquals(empty.damlFiles, Seq.empty)

  tempDir.test("listDamlProjects computes outputDar as lowercase name"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Account/daml.yaml", subProjectYaml("Account"))
      svc = DamlProjectService(DamlProject(dir))
      projects <- svc.listDamlProjects()
    yield
      val account = projects.find(_.name == "Account").get
      assert(account.outputDar.toString.endsWith("account.dar"))

  // --- mainDamlProject ---

  tempDir.test("mainDamlProject returns root project config"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      svc = DamlProjectService(DamlProject(dir))
      main <- svc.mainDamlProject()
    yield
      assertEquals(main.name, "MyWorkspace")
      assertEquals(main.sdkVersion, "2.5.3")

  tempDir.test("mainDamlProject fails when no root daml.yaml"): dir =>
    val svc = DamlProjectService(DamlProject(dir))
    interceptIO[RuntimeException](svc.mainDamlProject())

  // --- dependencyGraph ---

  tempDir.test("dependencyGraph with no dependencies"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/User/daml.yaml", subProjectYaml("User"))
      svc = DamlProjectService(DamlProject(dir))
      graph <- svc.dependencyGraph()
    yield
      assertEquals(graph("Asset"), Seq.empty)
      assertEquals(graph("User"), Seq.empty)

  tempDir.test("dependencyGraph resolves data-dependencies to project names"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/User/daml.yaml", subProjectYaml("User"))
      _ <- writeFile(dir, "main/Account/daml.yaml", subProjectYaml("Account", Seq("../Asset/asset.dar")))
      _ <- writeFile(dir, "main/Setup/daml.yaml", subProjectYaml("Setup", Seq("../Asset/asset.dar", "../Account/account.dar", "../User/user.dar")))
      svc = DamlProjectService(DamlProject(dir))
      graph <- svc.dependencyGraph()
    yield
      assertEquals(graph("Asset"), Seq.empty)
      assertEquals(graph("User"), Seq.empty)
      assertEquals(graph("Account"), Seq("Asset"))
      assertEquals(graph("Setup").toSet, Set("Asset", "Account", "User"))

  // --- buildOrder ---

  tempDir.test("buildOrder groups independent projects in same step"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/User/daml.yaml", subProjectYaml("User"))
      svc = DamlProjectService(DamlProject(dir))
      order <- svc.buildOrder()
    yield
      assertEquals(order.length, 2)
      assert(order.forall(_.step == 1))
      assert(order.forall(_.dependsOn.isEmpty))

  tempDir.test("buildOrder computes correct steps and dependsOn"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Asset/daml.yaml", subProjectYaml("Asset"))
      _ <- writeFile(dir, "main/User/daml.yaml", subProjectYaml("User"))
      _ <- writeFile(dir, "main/Account/daml.yaml", subProjectYaml("Account", Seq("../Asset/asset.dar")))
      _ <- writeFile(dir, "main/Setup/daml.yaml", subProjectYaml("Setup", Seq("../Asset/asset.dar", "../Account/account.dar", "../User/user.dar")))
      _ <- writeFile(dir, "main/Test/daml.yaml", subProjectYaml("Test", Seq("../Asset/asset.dar", "../Account/account.dar")))
      svc = DamlProjectService(DamlProject(dir))
      order <- svc.buildOrder()
    yield
      val byName = order.map(s => s.project -> s).toMap

      assertEquals(byName("Asset").step, 1)
      assertEquals(byName("Asset").dependsOn, Seq.empty)

      assertEquals(byName("User").step, 1)
      assertEquals(byName("User").dependsOn, Seq.empty)

      assertEquals(byName("Account").step, 2)
      assertEquals(byName("Account").dependsOn, Seq(1))

      assertEquals(byName("Setup").step, 3)
      assertEquals(byName("Setup").dependsOn, Seq(1, 2))

      assertEquals(byName("Test").step, 3)
      assertEquals(byName("Test").dependsOn, Seq(1, 2))

  tempDir.test("buildOrder returns sorted by step then name"): dir =>
    for
      _ <- writeFile(dir, "daml.yaml", mainDamlYaml)
      _ <- writeFile(dir, "main/Zebra/daml.yaml", subProjectYaml("Zebra"))
      _ <- writeFile(dir, "main/Alpha/daml.yaml", subProjectYaml("Alpha"))
      _ <- writeFile(dir, "main/Beta/daml.yaml", subProjectYaml("Beta", Seq("../Alpha/alpha.dar")))
      svc = DamlProjectService(DamlProject(dir))
      order <- svc.buildOrder()
    yield
      assertEquals(order.map(_.project), Seq("Alpha", "Zebra", "Beta"))
      assertEquals(order.map(_.step), Seq(1, 1, 2))
