package caiiiycuk.github.com.api


class Pull(owner: String, repo: String, pull: Int, sha: String) {
  
  def changes() = {
    s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files"
  }
  
}