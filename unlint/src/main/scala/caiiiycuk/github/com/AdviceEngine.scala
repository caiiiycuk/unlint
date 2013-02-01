package caiiiycuk.github.com

import scala.io.Source
import sys.process._

import org.apache.commons.io.FileUtils
import java.io.File

import scala.xml._

object AdviceEngine {
//    val checksFileOption = Play.current.configuration.getString("checks.file")
//    val checksFile = checksFileOption.getOrElse { throw new IllegalArgumentException("Option 'checks.file' not found") }
//    val checksJson = Source.fromFile(checksFile).mkString
//    val checks = Json.parse(checksJson)
  
//	val checks = Map("java" -> "checkstyle -f xml -c etc/java.xml $filename")


    def analyze(filename: String, extenstion: String, source: String) = {
        val file = File.createTempFile("check-my-pull", "." + extenstion)

        try {
            val checkersJson = (checks \ extenstion)

            val checkers = checkersJson.asOpt[List[String]].getOrElse(List())

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
                    case e: Throwable =>
                        Logger.debug(s"$filename, ($extenstion): Problem when executing '$executeString'", e)
                    <xml></xml>
                }
            }
        } finally {
            file.delete()
        }
    }

}