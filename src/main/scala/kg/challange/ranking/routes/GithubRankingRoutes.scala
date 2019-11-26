package kg.challange.ranking.routes

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import kg.challange.ranking.service.JokesAlgebra

object GithubRankingRoutes {

  def jokeRoutes[F[_]: Sync](J: JokesAlgebra[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "joke" =>
        for {
          joke <- J.get
          resp <- Ok(joke)
        } yield resp
    }
  }
}
