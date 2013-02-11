package caiiiycuk.github.com.engine

import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.sys.process.stringToProcess
import scala.xml.XML

import org.apache.commons.io.FileUtils

import xitrum.Logger
import xitrum.util.Loader

object AdviceEngine extends Logger {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  val checksFileName = "etc/default.json"

  var checksChangedAt = 0l
  var checks: Map[String, List[String]] = Map()

  val marker = new AtomicInteger(0)

  xitrum.Config.actorSystem.scheduler.schedule(
    Duration.Zero,
    Duration.create(5000, TimeUnit.MILLISECONDS)) {
      val file = new File(checksFileName)
      if (checksChangedAt != file.lastModified()) {
        checksChangedAt = file.lastModified()
        checks = Loader.jsonFromFile[Map[String, List[String]]](checksFileName)

        logger.debug(s"Config '$checksFileName' reloaded...")
      }
    }

  def analyze(filename: String, extenstion: String, source: String) = {
    try {
      while (marker.getAndIncrement() > 0) {
        marker.decrementAndGet()
        Thread.sleep(50)
      }

      _analyze(filename, extenstion, source)
    } catch {
      case e: java.lang.Throwable =>
        val message = e.getMessage()
        logger.debug(message)
        <error line="1" severity="critical" message={ s"$message" }></error>
    } finally {
      marker.decrementAndGet()
    }
  }

  def _analyze(filename: String, extenstion: String, source: String) = {
    val temporalDirectory = new File(xitrum.Config.application.getString("xitrum.temporalDirectory"))
    val directory = new File(temporalDirectory, "unlint-" + System.nanoTime)

    if (!directory.mkdir()) {
      throw new IllegalStateException("Unable to create temp directory " + directory.getAbsoluteFile())
    }

    try {
      val file = new File(directory, filename)

      val checkers = checks.getOrElse(extenstion, List())

      if (!checkers.isEmpty) {
        if (!new File(directory, ".git").mkdir()
          || !new File(directory, ".hg").mkdir()
          || !new File(directory, ".svn").mkdir()) {
          throw new IllegalStateException("Unable to create .git|.hg|.svn file")
        }

        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
          throw new IllegalStateException("Unable to create source tree")
        }

        FileUtils.write(file, source, "utf8")
      }

      for (
        checker <- checkers;
        command = checker.toString
      ) yield {
        val executeString = command.replaceAll("\\$filename", file.getAbsoluteFile().toString)
        try {
          XML.loadString(executeString.!!)
        } catch {
          case e: java.lang.Throwable =>
            val message = e.getMessage()
            val problem = s"$filename, ($extenstion): Problem when executing '$executeString', cause: $message"
            throw new IllegalStateException(problem)
        }
      }
    } finally {
      FileUtils.deleteQuietly(directory)
    }
  }

}