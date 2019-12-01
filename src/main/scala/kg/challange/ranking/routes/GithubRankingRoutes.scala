package kg.challange.ranking.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import kg.challange.ranking.service.GithubRankingService
import org.http4s.Response
import kg.challange.ranking.models.{Error, ServiceResponseError, Contributor}

object GithubRankingRoutes {
  
  def rankingRoutes[F[_]: Sync](GS: GithubRankingService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "org" / orgName / "contributors" =>
        for {
          sorted <- GS.getCompleteData(orgName)
          resp <- Ok(sorted)
        } yield resp

      case GET -> Root / "org" / orgName => 
        for {
          repos <- GS.getRepositories(orgName)
          resp <- Ok(repos)
        } yield resp
    }
  }
}
