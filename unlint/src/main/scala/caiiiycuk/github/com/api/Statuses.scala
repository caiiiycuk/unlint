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

class Status(val state: String, val target_url: String, var description: String) {
  if (description.length() >= 140) {
    description = description.substring(0, 136) + "..."
  }
}

object Statuses {

  implicit val ec: ExecutionContext =
    xitrum.Config.actorSystem.dispatcher

  def create(pull: Pull, status: Status) = {
    val post = WS.preparePost(pull.statuses())
    val data = Json.generate(status)
    
    post.setBody(data)
	post.execute().get()
  }

}