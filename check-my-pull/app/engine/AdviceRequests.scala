package advice.engine

import play.api.libs.ws._
import com.ning.http.client.Realm._
import play.Logger

class RequestAuth(val username: String, val password: String);

class RawRequest(url: String) {
    var auth: Option[RequestAuth] = None

    def withAuth(username: String, password: String) {
        auth = Some(new RequestAuth(username, password))
    }

    def get() = {
        auth match {
            case Some(auth) => 
                WS.url(url).withAuth(auth.username, auth.password, AuthScheme.BASIC).get()
            case None =>
                WS.url(url).get()
        }
    }    
}

class ChangesRequest(owner: String, repo: String, pull: String) {
    var auth: Option[RequestAuth] = None

    def changesUrl =  s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files"

    def withAuth(username: String, password: String) {
        auth = Some(new RequestAuth(username, password))
    }

    def get() = {
        auth match {
            case Some(auth) => 
                WS.url(changesUrl).withAuth(auth.username, auth.password, AuthScheme.BASIC).get()
            case None =>
                WS.url(changesUrl).get()
        }
    }
}

object AdviceRequests {
    val changesPattern = "https://github.com/(.*)/(.*)/pull/(.*)".r

    def changes(url: String) = {
        url match {
            case changesPattern(owner, repo, pull) =>
                new ChangesRequest(owner, repo, pull)

            case _ =>
                throw new IllegalArgumentException(s"Invalid url $url")
        }
    }

    def raw(url: String) = {
        url.replaceAll("raw/", "")
        url.replaceAll("github\\.com", "raw.github.com")

        new RawRequest(url)
    }

}