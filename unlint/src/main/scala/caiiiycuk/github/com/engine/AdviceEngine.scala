package caiiiycuk.github.com.engine

import java.io.File
import scala.xml.XML
import org.apache.commons.io.FileUtils
import xitrum.Logger
import xitrum.util.Loader
import sys.process._

object AdviceEngine extends Logger {
  val checks = Loader.jsonFromFile[Map[String, List[String]]]("etc/default.json")

  def analyze(filename: String, extenstion: String, source: String) = {
    val file = File.createTempFile("check-my-pull", "." + extenstion)

    try {
      val checkers = checks.getOrElse(extenstion, List())

      if (!checkers.isEmpty) {
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
            val problem = e.getMessage()
            logger.debug(s"$filename, ($extenstion): Problem when executing '$executeString', cause: $problem")
            <xml></xml>
        }
      }
    } finally {
      file.delete()
    }
  }

}