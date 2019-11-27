package kg.challange.ranking.service

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe.{Encoder, Decoder, Json, HCursor}
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, Uri, Request}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.MediaType
import cats.data.EitherT
import kg.challange.ranking.models.{Repository, Error, GeneralError, BadRequestError, UnauthorizedError, UnrecognizedError}
import kg.challange.ranking.config.AppConfig

case class GithubRankingService[F[_]](C: Client[F], config: AppConfig, ghToken: Option[String])(implicit F: Sync[F]) {
  val ghApiUrl = config.githubApiUrl
  val customHeaders: Headers = ghToken.map { t => 
    Headers.of(
      Header("Authorization", s"token $t"),
      Header("Accept", "application/vnd.github.v3+json")
    )
  }.getOrElse(Headers.of(Header("Accept", "application/vnd.github.v3+json")))

  def getRepositories(orgName: String): F[List[Repository]] = {
    val dsl = new Http4sClientDsl[F]{}
    import dsl._
    println("token "+ghToken)
    println("url" + ghApiUrl) 
    val r = buildRequest(s"$ghApiUrl/orgs/$orgName/repos", customHeaders)
    
    C.expect[List[Repository]](r)
      .adaptError { case t => GeneralError(t)}
    }

    def buildRequest(url: String, customHeaders: Headers) = 
        Request[F](Method.GET, Uri.fromString(url).getOrElse(Uri()), headers = customHeaders)
}