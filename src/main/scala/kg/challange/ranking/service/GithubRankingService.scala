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
import kg.challange.ranking.models.{Repository, Error, GeneralError, BadRequestError, UnauthorizedError, UnrecognizedError, Contributor}
import kg.challange.ranking.config.AppConfig
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.util.CaseInsensitiveString
import scala.util.Try
import scala.annotation.tailrec
import cats.Parallel

case class GithubRankingService[F[_]](C: Client[F], config: AppConfig, ghToken: Option[String])(implicit F: Sync[F], parallel: Parallel[F]) {
  val dsl = new Http4sClientDsl[F]{}
  import dsl._
  
  val ghApiUrl = config.githubApiUrl
  val customHeaders: Headers = ghToken.map { t => 
    Headers.of(
      Header("Authorization", s"token $t"),
      Header("Accept", "application/vnd.github.v3+json")
    )
  }.getOrElse(Headers.of(Header("Accept", "application/vnd.github.v3+json")))

  def getCompleteData(orgName: String): F[List[Contributor]] = {
    for {
      r <- getRepositories(orgName)
      c <- getContributors(orgName, r)
      d <- sortContributors(c)
    } yield d
  }

  def getRepositories(orgName: String): F[List[Repository]] = {
    makeRequest[Repository](s"$ghApiUrl/orgs/$orgName/repos", Some(1))
  }

  def getContributorsOfRepo(orgName: String, repo: Repository):F[List[Contributor]] = {  
    makeRequest[Contributor](s"$ghApiUrl/repos/$orgName/${repo.name}/contributors", Some(1))
  }

  def getContributors(orgName: String, repoList: List[Repository]):F[List[Contributor]] = {  
    for {
      rm <- repoList.parTraverse(getContributorsOfRepo(orgName,_)).map(_.combineAll)
    } yield rm
  }

  def sortContributors(list: List[Contributor]): F[List[Contributor]] = {
    F.pure(list.groupBy(_.login).map(a => Contributor(a._1, sum(a._2))).toList.sortWith(_.contributions > _.contributions))
  }

  def sum(l: List[Contributor]): Long = l.map(_.contributions).sum
  
  def getNextPageNumber(header: String): Option[Int] = {
    header.split(',').map{ part =>
      val section = part.split(';')
      val url = section(0).replace("<", "").replace(">", "")
      val page = section(1).replace(" rel=\"", "").replace("\"", "")
      (page, url)
    }.find(_._1 == "next")
      .map(_._2.split("="))
      .map {
        case Array(uri, pageNum) =>
          Try(pageNum.toInt).getOrElse(1)
      }
  }

  def buildRequest(url: String, customHeaders: Headers) = 
        Request[F](Method.GET, Uri.fromString(url).getOrElse(Uri()), headers = customHeaders)

  def makeRequest[A](uri: String, nextPage: Option[Int])(implicit decoder: EntityDecoder[F, List[A]]): F[List[A]] = {
    val request = buildRequest(uri+"?page="+nextPage.get, customHeaders)
    C.fetch[List[A]](request) { response => 
      val nextPage = response.headers.get(CaseInsensitiveString("Link")).flatMap(l => getNextPageNumber(l.value))
      val currentPageResult:F[List[A]] = response.attemptAs[List[A]].value.flatMap {
        case Right(value) => F.pure(value)
        case Left(err) => F.delay(List())
      }

      val nextPageResult = nextPage match {
        case Some(value) => makeRequest[A](uri, nextPage)
        case None => F.delay(List())
      }
      
     for { 
       c <- currentPageResult
       n <- nextPageResult
     } yield c ++ n
    } 
  }
}