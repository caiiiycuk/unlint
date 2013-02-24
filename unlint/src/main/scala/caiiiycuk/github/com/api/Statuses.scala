package caiiiycuk.github.com.api

import scala.concurrent.ExecutionContext
import caiiiycuk.github.com.ws.WS
import xitrum.util.Json

object Status {
  def pending(target_url: String, description: String) = {
    new Status("pending", target_url, description)
  }

  def success(target_url: String, description: String) = {
    new Status("success", target_url, description)
  }

  def error(target_url: String, description: String) = {
    new Status("error", target_url, description)
  }

  def failure(target_url: String, description: String) = {
    new Status("failure", target_url, description)
  }
}

class Status(val state: String, val target_url: String, val description: String)

object Statuses {

  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  def create(commit: Commit, username: String, password: String, status: Status) = {
    val owner = commit.owner
    val repo = commit.repo
    val sha = commit.sha
    val post = WS.preparePost(s"https://api.github.com/repos/$owner/$repo/statuses/$sha")
    WS.basicAuth(post, username, password)

    val data = Json.generate(status)
    post.setBody(data)
    post.execute()
  }

}