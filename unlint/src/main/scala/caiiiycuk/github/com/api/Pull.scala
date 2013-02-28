package caiiiycuk.github.com.api

import org.json4s.JInt
import org.json4s.JNothing
import org.json4s.JString
import org.json4s.JValue
import org.json4s.jvalue2monadic
import org.json4s.native.JsonMethods.parse
import org.json4s.string2JsonInput

import caiiiycuk.github.com.ws.WS

object Pull {
  val urlRegex = """https://github.com/(.*)/(.*)/pull/(\d+).*""".r

  def apply(json: JValue, token: String): Option[Pull] = {
    val pullRequest = (json \ "pull_request") match {
      case JNothing =>
        json
      case json =>
        json
    }
    
    val pull = (pullRequest \ "number")
    val owner = (pullRequest \ "base" \ "user" \ "login" )
    val repo = (pullRequest \ "base" \ "repo" \"name")
    val sha = (pullRequest \ "head" \ "sha")

    (owner, repo, pull, sha) match {
      case (JString(owner), JString(repo), JInt(num), JString(sha)) =>
        Some(new Pull(owner, repo, num.toInt, sha, token))
      case _ =>
        None
    }
  }

  def apply(url: String, token: String): Option[Pull] = {
    url match {
      case urlRegex(owner, repo, pull) =>
        val pullUrl =
          token match {
            case "" =>
              s"https://api.github.com/repos/$owner/$repo/pulls/$pull"
            case token =>
              s"https://api.github.com/repos/$owner/$repo/pulls/$pull?access_token=$token"
          }

        val data = WS.prepareGet(pullUrl).execute().get().getResponseBody()
        apply(parse(data), token)
      case _ =>
        None
    }
  }
}

class Pull(val owner: String, val repo: String, val pull: Int, val sha: String, val token: String) extends Serializable {

  def changes() = {
    applyToken(s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files")
  }

  def statuses() = {
    applyToken(s"https://api.github.com/repos/$owner/$repo/statuses/$sha")
  }

  def root(): String = {
    sha
  }

  def tree(sha: String): String = {
    applyToken(s"https://api.github.com/repos/$owner/$repo/git/trees/$sha")
  }

  def blob(sha: String): String = {
    applyToken(s"https://api.github.com/repos/$owner/$repo/git/blobs/$sha")
  }

  override def toString(): String = {
    return s"$owner.$repo.$pull.$sha"
  }
  
  def applyToken(url: String) = {
    token match {
      case "" =>
        url
      case token =>
        s"$url?access_token=$token"
    }
  }

}