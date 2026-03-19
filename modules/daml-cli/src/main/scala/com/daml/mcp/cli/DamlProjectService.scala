package com.daml.mcp.cli

import cats.effect.IO
import com.daml.mcp.cli.models.{BuildResult, BuildStep, CleanResult, DamlProjectConfig}
import java.nio.file.{Files, Path}
import scala.jdk.CollectionConverters.*

/** High-level read-only queries over a DAML project. */
final class DamlProjectService(project: DamlProject):

  def listDamlProjects(): IO[Seq[DamlProjectConfig]] = project.damlProjects

  def mainDamlProject(): IO[DamlProjectConfig] = project.mainDamlProject

  /** Map of project name -> list of project names it depends on (via data-dependencies). */
  def dependencyGraph(): IO[Map[String, Seq[String]]] =
    listDamlProjects().map: projects =>
      projects.map: p =>
        val deps = p.dataDependencies.flatMap: depPath =>
          projects.find(other => depPath.startsWith(other.path.getParent))
            .map(_.name)
        p.name -> deps
      .toMap

  /** Build order with step levels and dependency references. */
  def buildOrder(): IO[Seq[BuildStep]] =
    dependencyGraph().map(computeBuildOrder)

  /** Run `daml build` for a single project by name. */
  def buildSingle(projectName: String): IO[BuildResult] =
    listDamlProjects().flatMap: projects =>
      projects.find(_.name == projectName) match
        case None =>
          IO.pure(BuildResult(
            project = projectName,
            step = 0,
            success = false,
            output = s"ERROR: Project '$projectName' not found. Available projects: ${projects.map(_.name).mkString(", ")}",
            durationMs = 0
          ))
        case Some(config) =>
          runDamlBuild(BuildStep(1, projectName, Seq.empty), config)

  /** Run `daml build` for all projects in dependency order. Stops on first failure. */
  def buildAll(): IO[Seq[BuildResult]] =
    for
      projects <- listDamlProjects()
      steps    <- buildOrder()
      byName    = projects.map(p => p.name -> p).toMap
      grouped   = steps.groupBy(_.step).toSeq.sortBy(_._1)
      results  <- runBuildSteps(grouped, byName, Vector.empty)
    yield results

  private def runBuildSteps(
      remaining: Seq[(Int, Seq[BuildStep])],
      byName: Map[String, DamlProjectConfig],
      acc: Vector[BuildResult]
  ): IO[Vector[BuildResult]] =
    remaining match
      case Seq() => IO.pure(acc)
      case (step, projects) +: rest =>
        IO.traverse(projects.toList)(bs => runDamlBuild(bs, byName(bs.project))).flatMap: results =>
          val newAcc = acc ++ results
          if results.exists(!_.success) then IO.pure(newAcc)
          else runBuildSteps(rest, byName, newAcc)

  private def runDamlBuild(step: BuildStep, config: DamlProjectConfig): IO[BuildResult] =
    IO.blocking:
      val projectDir = config.path.getParent
      val startTime = System.currentTimeMillis()
      val pb = ProcessBuilder("daml", "build", "-o", config.outputDar.getFileName.toString)
      pb.directory(projectDir.toFile)
      pb.redirectErrorStream(true)
      val process = pb.start()
      val output = String(process.getInputStream.readAllBytes())
      val exitCode = process.waitFor()
      val duration = System.currentTimeMillis() - startTime
      BuildResult(
        project = config.name,
        step = step.step,
        success = exitCode == 0,
        output = output.trim,
        durationMs = duration
      )

  /** Remove .daml/ directories and *.dar files from all sub-projects. */
  def cleanAll(): IO[Seq[CleanResult]] =
    listDamlProjects().flatMap: projects =>
      IO.traverse(projects.toList)(cleanProject)

  /** Remove .daml/ directory and *.dar files from a single project by name. */
  def cleanSingle(projectName: String): IO[CleanResult] =
    listDamlProjects().flatMap: projects =>
      projects.find(_.name == projectName) match
        case None =>
          IO.pure(CleanResult(projectName, removedFiles = -1))
        case Some(config) =>
          cleanProject(config)

  private def cleanProject(config: DamlProjectConfig): IO[CleanResult] =
    IO.blocking:
      val projectDir = config.path.getParent
      var removedFiles = 0

      val damlDir = projectDir.resolve(".daml")
      if Files.exists(damlDir) then
        Files.walk(damlDir).sorted(java.util.Comparator.reverseOrder())
          .forEach: p =>
            Files.delete(p)
            removedFiles += 1

      Files.list(projectDir).iterator().asScala
        .filter(_.toString.endsWith(".dar"))
        .foreach: p =>
          Files.delete(p)
          removedFiles += 1

      CleanResult(config.name, removedFiles)

  /** Scaffold a new DAML project using `daml new`. */
  def createProject(projectName: String, template: Option[String]): IO[String] =
    IO.blocking:
      val targetDir = project.root.resolve(projectName)
      if Files.exists(targetDir) then
        s"ERROR: Directory '$projectName' already exists at ${targetDir}."
      else
        val cmd = Seq("daml", "new", projectName) ++ template.toSeq
        val pb = ProcessBuilder(cmd*)
        pb.directory(project.root.toFile)
        pb.redirectErrorStream(true)
        val process = pb.start()
        val output = String(process.getInputStream.readAllBytes())
        val exitCode = process.waitFor()
        if exitCode == 0 then
          s"Project '$projectName' created successfully at ${targetDir}.\n$output".trim
        else
          s"ERROR: Failed to create project '$projectName' (exit code $exitCode).\n$output".trim

  /** Run `daml test` for a specific project by name. */
  def runDamlTest(projectName: String): IO[String] =
    listDamlProjects().flatMap: projects =>
      projects.find(_.name == projectName) match
        case None => 
          IO.pure(s"ERROR: Project '$projectName' not found. Available projects: ${projects.map(_.name).mkString(", ")}")
        case Some(config) =>
          runDamlTestForProject(config)

  private def runDamlTestForProject(config: DamlProjectConfig): IO[String] =
    IO.blocking:
      val projectDir = config.path.getParent
      val pb = ProcessBuilder("daml", "test", "--no-legacy-assistant-warning")
      pb.directory(projectDir.toFile)
      pb.redirectErrorStream(true)
      val process = pb.start()
      val output = String(process.getInputStream.readAllBytes())
      output.trim

  /** Inspect a DAML archive (.dar) file to show its API, templates, and data types. */
  def inspectDar(projectName: String): IO[String] =
      listDamlProjects().flatMap: projects =>
        projects.find(_.name == projectName) match
          case None =>
            IO.pure(s"ERROR: Project '$projectName' not found. Available projects: ${projects.map(_.name).mkString(", ")}")
          case Some(config) =>
            runInspectDarForProject(config)

  private def runInspectDarForProject(config: DamlProjectConfig): IO[String] =
    IO.blocking:
      val projectDir = config.path.getParent
      val pb = ProcessBuilder("daml", "inspect-dar", "--no-legacy-assistant-warning", config.outputDar.toString)
      pb.directory(projectDir.toFile)
      pb.redirectErrorStream(true)
      val process = pb.start()
      val output = String(process.getInputStream.readAllBytes())
      output.trim

  private[cli] def computeBuildOrder(graph: Map[String, Seq[String]]): Seq[BuildStep] =
    val stepOf = scala.collection.mutable.Map.empty[String, Int]

    def resolveStep(node: String): Int =
      stepOf.getOrElseUpdate(node, {
        val deps = graph.getOrElse(node, Seq.empty)
        if deps.isEmpty then 1
        else deps.map(resolveStep).max + 1
      })

    graph.keys.foreach(resolveStep)

    stepOf.toSeq
      .sortBy((name, step) => (step, name))
      .map: (name, step) =>
        val depSteps = graph.getOrElse(name, Seq.empty)
          .map(d => stepOf(d))
          .distinct.sorted
        BuildStep(step, name, depSteps)
