package advice.engine

import play.api.libs.ws._
import com.ning.http.client.Realm._
import play.Logger
import play.api.Play

object RequestAuth {
    val usernameOption = Play.current.configuration.getString("github.username")
    val passwordOption = Play.current.configuration.getString("github.password")

    val username =  usernameOption.getOrElse { throw new IllegalArgumentException("Option 'github.username' not found") }
    val password =  passwordOption.getOrElse { throw new IllegalArgumentException("Option 'github.password' not found") }

    val defaultAuth: Option[RequestAuth] =
        if (!username.isEmpty && !password.isEmpty) {
            Some(new RequestAuth(username, password))
        } else {
            None
        }
}

class RequestAuth(val username: String, val password: String);

class RawRequest(url: String) {
    var auth = RequestAuth.defaultAuth

    def withAuth(username: String, password: String) {
        if (!username.isEmpty && !password.isEmpty) {
            auth = Some(new RequestAuth(username, password))
        }
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
    var auth: Option[RequestAuth] = RequestAuth.defaultAuth

    def changesUrl =  s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files"

    def withAuth(username: String, password: String) {
        if (!username.isEmpty && !password.isEmpty) {
            Logger.debug(s"Set auth: $username, $password")
            auth = Some(new RequestAuth(username, password))
        }
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