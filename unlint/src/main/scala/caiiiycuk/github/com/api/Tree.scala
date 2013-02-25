package caiiiycuk.github.com.api

import scala.collection.mutable.Map

import org.json4s._
import org.json4s.native.JsonMethods.parse
import org.slf4j.Logger

import caiiiycuk.github.com.ws.WS

class BlobEntry(val sha: String, val size: Option[Long], var resolved: Boolean)

class Tree(pull: Pull)(implicit logger: Logger) {

  val parent = """^(.*)/.+$""".r
  val flatTree = Map[String, BlobEntry]()

  loadTree(flatTree, pull.root(), "")

  def resolve(path: String) {
    path match {
      case parent(parentPath) =>
        resolve(parentPath)
        val blobEntry = flatTree.get(parentPath)
        blobEntry match {
          case Some(blobEntry) if !blobEntry.resolved =>
            loadTree(flatTree, blobEntry.sha, parentPath + "/")
            blobEntry.resolved = true
          case _ =>
        }
      case _ =>
    }
  }

  def blob(path: String): Option[BlobEntry] = {
    resolve(path)

    flatTree.get(path) match {
      case Some(entity) =>
        Some(entity)
      case None =>
        None
    }
  }

  private def loadTree(flat: Map[String, BlobEntry], treeSha: String, parent: String) = {
    val request = pull.tree(treeSha)
    val data = WS.prepareGet(request).execute().get().getResponseBody()
    val json = parse(data)

    val trees =
      (json \ "tree") match {
        case JArray(trees) =>
          trees
        case tree =>
          List(tree)
      }

    for (tree <- trees) {
      val path = (tree \ "path")
      val sha = (tree \ "sha")
      val size = (tree \ "size")

      (path, sha, size) match {
        case (JString(path), JString(sha), JInt(size)) =>
          flat.put(parent + path, new BlobEntry(sha, Some(size.longValue), false))
        case (JString(path), JString(sha), JNothing) =>
          flat.put(parent + path, new BlobEntry(sha, None, false))
        case _ =>
          logger.error(s"Unknown blob type '$data'")
      }
    }
  }

}