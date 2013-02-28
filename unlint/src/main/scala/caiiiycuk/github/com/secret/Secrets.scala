package caiiiycuk.github.com.secret

import com.hazelcast.core.IMap

import caiiiycuk.github.com.api.Pull
import xitrum.Config

object Secrets {
  val secrets = Config.hazelcastInstance.getMap("unlint/secrets").asInstanceOf[IMap[String, Any]]

  def secretOfPull(pull: Pull) = {
    val secret = pull.toString
    secrets.put(secret, pull)
    secret
  }

  def pullOfSecret(secret: String) = {
    Option(secrets.get(secret)).map(_.asInstanceOf[Pull])
  }
}