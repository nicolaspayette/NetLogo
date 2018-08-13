package org.nlogo.app.github

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import java.util.concurrent.Executor
import javax.swing.SwingUtilities

import org.apache.commons.codec.binary.Base64

import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.libs.ws.ahc._
import play.api.libs.ws.JsonBodyReadables._

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ApiClient {
  implicit class RichResponse(r: StandaloneWSResponse) {
    def content = new String(Base64.decodeBase64((r.body[JsValue] \ "content").as[String]), "UTF-8")
    def nextPageUrl: Option[String] = r.header("Link").flatMap {
      _.split(", ") // separate the links
        .map(_.split("; ")) // split each one between url and rel
        .find(_.last == "rel=\"next\"") // find the link to the next page
        .map(_.head.tail.init) // and keep the url with the first ("<") and last (">") chars removed
    }
  }
  implicit val swingExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(new Executor {
      def execute(command: Runnable): Unit = SwingUtilities invokeLater command
    })
}

class ApiClient(oauthToken: () => String) {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import ApiClient.swingExecutionContext

  // TODO: close client when quitting dialog
  val client = StandaloneAhcWSClient()

  def get(uri: String, parameters: (String, String)*): Future[StandaloneWSResponse] = {
    val req = client
      .url(uri.toString)
      .withFollowRedirects(true)
      .addHttpHeaders(
        "Accept" -> "application/vnd.github.v3+json",
        "Authorization" -> s"token ${oauthToken()}"
      )
      .withQueryStringParameters(parameters: _*)

    req.get.map { res =>
      println(res.headers)
      res
    }
  }
}
