package caiiiycuk.github.com.controller

import xitrum.Controller
import xitrum.util.Json
import caiiiycuk.github.com.api.Owner
import caiiiycuk.github.com.api.Auth
import caiiiycuk.github.com.api.Commit
import caiiiycuk.github.com.api.Pull

class UnlintHook extends Controller {

  def hookWithAuth = POST("hook/:username/:password") {

  }

  def hookWithoutAuth = POST("hook") {

  }

  def hook(payload: String, auth: Auth) {
    import org.json4s._
    import org.json4s.native.JsonMethods._

    val json = parse(payload)

    val opened = (json \ "action").toString() == "JString(opened)"

    if (!opened) {
      return
    }

    val pull = (json \ "number")
    val owner = (json \ "repository" \ "owner" \ "login" )
    val repo = (json \ "repository" \ "name" )
    val sha = (json \ "pull_request" \ "head" \ "sha")
    
    println(pull)
    
    (owner, repo, pull, sha) match {
      case (JString(owner), JString(repo), JInt(num), JString(sha)) =>
        val pull = new Pull(owner, repo, num.toInt, sha)
        check(pull, auth)
      case _ =>
        // nothing to do
    }
  }
  
  def check(pull: Pull, auth: Auth) {
    
  }

}