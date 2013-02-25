package caiiiycuk.github.com.controller

import scala.concurrent.ExecutionContext
import scala.concurrent.future
import scala.reflect.runtime.universe
import org.json4s.JArray
import org.json4s.JInt
import org.json4s.JString
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput
import caiiiycuk.github.com.api.Pull
import caiiiycuk.github.com.api.Status
import caiiiycuk.github.com.api.Statuses
import caiiiycuk.github.com.ws.WS
import xitrum.Controller
import org.slf4j.Logger
import caiiiycuk.github.com.engine.AdviceChecks
import caiiiycuk.github.com.engine.AdviceEngine
import caiiiycuk.github.com.api.Tree

class UnlintHook extends Controller {

  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  implicit val _logger: Logger = logger

  def githubHook = POST("github/hook/:token") {
    hook("", param("token"))
  }

  def hook(payload: String, token: String) {
    val json = parse(payload)

    val action = (json \ "action").toString()

    if (action != "JString(opened)" &&
      action != "JString(synchronize)") {
      return
    }

    val pull = (json \ "number")
    val owner = (json \ "repository" \ "owner" \ "login")
    val repo = (json \ "repository" \ "name")
    val sha = (json \ "pull_request" \ "head" \ "sha")

    (owner, repo, pull, sha) match {
      case (JString(owner), JString(repo), JInt(num), JString(sha)) =>
        val pull = new Pull(owner, repo, num.toInt, sha, token)
        future {
          check(pull)
        }
      case _ =>
      // nothing to do
    }
  }

  def check(pull: Pull) {
    Statuses.create(pull,
      Status.pending("http://unlint.github.com", "Checking pull request please wait..."))

    val changed = WS.downloadChanges(pull)

    val rest = changed.filter(change => {
      change("status") != "removed" &&
        !AdviceChecks.checksFor(change("file")).isEmpty
    })
    
    val tree = new Tree(pull)
    
    for (change <- rest) {
      val file = change("file")
      val sha = tree.blob(file)
      val contents: Option[String] = sha match {
        case Some(sha) =>
          WS.downloadBlob(pull, sha)
        case None =>
          None
      }
      
      contents match {
        case Some(source) =>
          val advices = AdviceEngine.analyze(file, source)
        case _ =>
          "not checked"
      }
    }
    
    Statuses.create(pull,
      Status.success("http://unlint.github.com", "Good"))
  }

}