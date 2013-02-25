package caiiiycuk.github.com.engine

import java.io.File
import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import xitrum.util.Loader

object AdviceChecks {
  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  private val extensionPattern = """^.+\.([^.]*)$""".r

  private val checksFileName = "etc/default.json"

  private var checksChangedAt = 0l

  private var checks: Map[String, List[String]] = Map()

  def checksFor(filename: String) = {
    val extension = filename match {
      case url if url.endsWith(".min.js") || url.endsWith(".min.css") =>
        ""
      case extensionPattern(extension) =>
        extension
      case _ =>
        ""
    }

    AdviceChecks.checks.getOrElse(extension, List())
  }

  def update() = {
    val file = new File(checksFileName)
    if (checksChangedAt != file.lastModified()) {
      checksChangedAt = file.lastModified()
      checks = Loader.jsonFromFile[Map[String, List[String]]](checksFileName)
    }
  }

  xitrum.Config.actorSystem.scheduler.schedule(
    Duration.Zero,
    Duration.create(5000, TimeUnit.MILLISECONDS)) {
      update()
    }

}