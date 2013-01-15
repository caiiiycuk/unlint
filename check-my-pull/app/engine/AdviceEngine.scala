package advice.engine

import play.Logger

class SourceFile(filename: String, rawurl: String, source: String)

object AdviceEngine {
    val urlPattern = "https://github.com/(.*)/(.*)/pull/(.*)".r
    val filenamePattern = ".*\"filename\":\"([^\"]*)\".*".r
    val rawurlPattern = ".*\"raw_url\":\"([^\"]*)\".*".r

    def advice(url: String): Unit = {
        url match {
            case urlPattern(owner, repo, pull) => {
                advice(owner, repo, pull)
            }

            case _ => {
                throw new IllegalArgumentException(s"Invalid url $url")
            }
        }
    }

    def advice(owner: String, repo: String, pull: String): Unit = {
        Logger.debug(s"Generate advice for $owner, $repo, $pull")
        
        val apiUrl = s"https://api.github.com/repos/$owner/$repo/pulls/$pull/files"
        val response = readURL(apiUrl)

        val filenames = filenamePattern.findAllIn(response)
        for (m <- filenames) {
            m

            // println(m.group(1))
        }
        // val filenamePattern(filename) = response

        // response match {
        //     case filenamePattern(filename@_*) => 
        //         // Logger.debug(filename.fileContents)
        //         println(filename)
        // }
        



        // val rawurlPattern(rawurl) = response
        // val fileContents = readURL(rawurl)

        // val sourceFile = SourceFile(filename, rawurl, fileContents)



        Logger.debug("Well done...")
    }

    def readURL(url: String) = {
        import play.api.libs.ws._
        import com.ning.http.client.Realm._
        import play.api.libs.concurrent._
        import play.api.libs.concurrent.Execution.Implicits._

        val promise = WS.url(url).withAuth("caiiiycuk", "pjjgfhr12", AuthScheme.BASIC).get()
        
        promise.await(3000).get.body
        // val playPromise = new PlayPromise(promise).orTimeout("Timeout on github request", 1000)

        // val response = playPromise.map {
        //     either => 
        //         either.fold(
        //             response => response.body,
        //             timeout => throw new IllegalStateException(timeout)
        //         )
        // }
        // 
        // response.value
    }

}