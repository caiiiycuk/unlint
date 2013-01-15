package advice.engine

import play.api.libs.ws._
import com.ning.http.client.Realm._

class AdviceRequestAuth(val username: String, val password: String);

class AdviceRequest(owner: String, repo: String, pull: String) {
    var auth: Option[AdviceRequestAuth] = None

    def changesUrl =  s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files"

    def withAuth(username: String, password: String) {
        auth = Some(new AdviceRequestAuth(username, password))
    }

    def getChanges() = {
        WS.url(changesUrl).withAuth("caiiiycuk", "pjjgfhr12", AuthScheme.BASIC).get()
    }
}

object AdviceRequest {
    val urlPattern = "https://github.com/(.*)/(.*)/pull/(.*)".r

    def fromUrl(url: String) = {
        url match {
            case urlPattern(owner, repo, pull) => {
                new AdviceRequest(owner, repo, pull)
            }

            case _ => {
                throw new IllegalArgumentException(s"Invalid url $url")
            }
        }
    }
}