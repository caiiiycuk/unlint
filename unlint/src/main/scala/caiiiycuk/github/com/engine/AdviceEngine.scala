package caiiiycuk.github.com.engine

import org.slf4j.Logger
import caiiiycuk.github.com.api.Pull
import caiiiycuk.github.com.api.Tree
import caiiiycuk.github.com.ws.WS
import caiiiycuk.github.com.api.Change
import scala.xml.Node

trait AnalyzeProgress {
  def notifyDownload(): Unit
  def notifyAnalyze(): Unit
}

object NOOPAnalyzeProgress extends AnalyzeProgress {
  def notifyDownload(): Unit = {}
  def notifyAnalyze(): Unit = {}
}

class SourceWithAdvice(val advice: String, val source: String)

class AdviceEngine(pull: Pull)(implicit logger: Logger) {
  private val MAX_SIZE = 1024 * 70 /* 70Kb */

  val changes = WS.downloadChanges(pull)

  private val tree = new Tree(pull)

  def analyze(change: Change, progress: AnalyzeProgress = NOOPAnalyzeProgress): SourceWithAdvice = {
    val file = change.file
    val status = change.status

    val advices =
      if (status == "removed") {
        (List(<skip line="1" severity="info" message={ s"Sikpped: (Removed)" }></skip>), "")
      } else if (AdviceChecks.checksFor(file).isEmpty) {
        (List(<skip line="1" severity="info" message={ s"Skipped: (No checks)" }></skip>), "")
      } else {
        tree.blob(file) match {
          case Some(blob) =>
            val size = blob.size.getOrElse(0l)
            val sha = blob.sha

            if (size > MAX_SIZE) {
              (List(<skip line="1" severity="info" message={ s"Skipped: (File too big)" }></skip>), "")
            } else {
              progress.notifyDownload()
              WS.downloadBlob(pull, sha) match {
                case Some(source) =>
                  progress.notifyAnalyze
                  (AdviceProcess.analyze(file, source), source)
                case None =>
                  (List(<error line="1" severity="critical" message={ s"File not found, sha '$sha'" }></error>), "")
              }
            }
          case None =>
            (List(<error line="1" severity="critical" message={ s"File not found" }></error>), "")
        }
      }

    new SourceWithAdvice(<advice>{ advices._1.map(a => a) }</advice>.toString, advices._2)
  }
}