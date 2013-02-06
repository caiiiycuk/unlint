package caiiiycuk.github.com.engine

import java.io.File
import scala.xml.XML
import org.apache.commons.io.FileUtils
import xitrum.Logger
import xitrum.util.Loader
import sys.process._
import java.util.concurrent.atomic.AtomicInteger
import java.nio.file.Files

object AdviceEngine extends Logger {
  val checks = Loader.jsonFromFile[Map[String, List[String]]]("etc/default.json")
  val marker = new AtomicInteger(0)

  def analyze(filename: String, extenstion: String, source: String) = {
    while (marker.getAndIncrement() > 0) {
      marker.decrementAndGet()
      Thread.sleep(50)
    }

    val directory = Files.createTempDirectory("unlint").toFile()
    val file = new File(directory, filename)

    try {
      val checkers = checks.getOrElse(extenstion, List())

      if (!checkers.isEmpty) {
        if (!new File(directory, ".git").mkdir()
          || !new File(directory, ".hg").mkdir()
          || !new File(directory, ".svn").mkdir()) {
          throw new IllegalStateException("Unable to create .git|.hg|.svn file")
        }

        if (!file.getParentFile().mkdirs()) {
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
    } catch {
      case e: java.lang.Throwable =>
        val message = e.getMessage()
        logger.debug(message)
        <error line="1" severity="critical" message={ s"$message" }></error>
    } finally {
      FileUtils.deleteQuietly(directory)
      marker.decrementAndGet()
    }
  }

}