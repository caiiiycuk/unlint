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

  private val marker = new AtomicInteger(0)
  
  private val PARALLEL_TASKS = 3;
    
  def analyze(filename: String, source: String) = {
    try {
      while (marker.getAndIncrement() > (PARALLEL_TASKS - 1)) {
        marker.decrementAndGet()
        Thread.sleep(50)
      }

      _analyze(filename, source)
    } catch {
      case e: java.lang.Throwable =>
        val message = e.getMessage()
        logger.debug(message)
        <error line="1" severity="critical" message={ s"$message" }></error>
    } finally {
      marker.decrementAndGet()
    }
  }

  def _analyze(filename: String, source: String) = {
    val temporalDirectory = new File(xitrum.Config.application.getString("xitrum.temporalDirectory"))
    val directory = new File(temporalDirectory, "unlint-" + System.nanoTime)

    if (!directory.mkdir()) {
      throw new IllegalStateException("Unable to create temp directory " + directory.getAbsoluteFile())
    }

    try {
      val file = new File(directory, filename)

      val checkers = AdviceChecks.checksFor(filename)

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
            val problem = s"$filename, Problem when executing '$executeString', cause: $message"
            throw new IllegalStateException(problem)
        }
      }
    } finally {
      FileUtils.deleteQuietly(directory)
    }
  }

}