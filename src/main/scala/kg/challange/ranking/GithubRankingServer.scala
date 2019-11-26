package kg.challange.ranking

import cats.effect.{ConcurrentEffect, ContextShift, Timer}
import cats.implicits._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import scala.concurrent.ExecutionContext.global
import kg.challange.ranking.routes.GithubRankingRoutes
import kg.challange.ranking.config.AppConfig
import pureconfig.generic.auto._
import kg.challange.ranking.service.JokesService
import kg.challange.ranking.models.Joke

object GithubRankingServer {

  def stream[F[_]: ConcurrentEffect](implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {

    val config = pureconfig.loadConfig[AppConfig].getOrElse(AppConfig())

    val ghToken: Option[String] = sys.env.get("GH_TOKEN")

    for {
      client <- BlazeClientBuilder[F](global).stream
      jokeAlg = JokesService.impl[F](client)
  
      httpApp = (
        GithubRankingRoutes.jokeRoutes[F](jokeAlg)
      ).orNotFound

      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(config.port, config.host)
        .withHttpApp(finalHttpApp)
        .serve
    } yield exitCode
  }.drain
}
