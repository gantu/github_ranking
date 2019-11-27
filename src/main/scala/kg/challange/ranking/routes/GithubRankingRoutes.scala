package kg.challange.ranking.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

import kg.challange.ranking.service.GithubRankingService

object GithubRankingRoutes {

  def rankingRoutes[F[_]: Sync](G: GithubRankingService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "org" / orgName / "repos" =>
        for {
          repos <- G.getRepositories(orgName)
          resp <- Ok(repos)
        } yield resp
    }
  }
}
