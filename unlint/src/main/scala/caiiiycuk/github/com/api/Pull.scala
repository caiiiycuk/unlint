package caiiiycuk.github.com.api


class Pull(owner: String, repo: String, pull: Int, sha: String, token: String) {
  
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
  
  def applyToken(url: String) = {
    token match {
      case "" =>
        url
      case token =>
        s"$url?access_token=$token"
    }
  }
  
}