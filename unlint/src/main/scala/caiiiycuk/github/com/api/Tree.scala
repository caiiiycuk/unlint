package caiiiycuk.github.com.api

import scala.collection.mutable.Map

import org.json4s._
import org.json4s.native.JsonMethods.parse
import org.slf4j.Logger

import caiiiycuk.github.com.ws.WS

class Entry(val sha: String, var resolved: Boolean)

class Tree(pull: Pull)(implicit logger: Logger) {

  val parent = """^(.*)/.+$""".r
  val flatTree = Map[String, Entry]()

  loadTree(flatTree, pull.root(), "")

  def resolve(path: String) {
    path match {
      case parent(parentPath) =>
        resolve(parentPath)
        val entry = flatTree.get(parentPath)
        entry match {
          case Some(entry) if !entry.resolved =>
            loadTree(flatTree, entry.sha, parentPath + "/")
            entry.resolved = true
          case _ =>
        }
      case _ =>
    }
  }
  
  def blob(path: String): Option[String] = {
    resolve(path)
    
    flatTree.get(path) match {
      case Some(entity) =>
        Some(entity.sha)
      case None =>
        None
    }
  }

  private def loadTree(flat: Map[String, Entry], treeSha: String, parent: String) = {
    val data = WS.prepareGet(pull.tree(treeSha)).execute().get().getResponseBody()
    val json = parse(data)
    val paths = (json \ "tree" \ "path")
    val sha = (json \ "tree" \ "sha")

    (paths, sha) match {
      case (JArray(paths), JArray(sha)) =>
        for ((JString(path), JString(sha)) <- paths.zip(sha)) {
          flat.put(parent + path, new Entry(sha, false))
        }
      case (JString(path), JString(sha)) =>
        flat.put(parent + path, new Entry(sha, false))
      case _ =>
        logger.debug(s"Unmatched json '$data'")
    }

  }

}