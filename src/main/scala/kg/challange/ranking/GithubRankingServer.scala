package kg.challange.ranking

import cats.Parallel
import cats.effect._
import cats.syntax.functor._
import cats.syntax.option._

import monix.eval.{Task, TaskApp}
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.{Server => Http4sServer}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import pureconfig.generic.auto._ // required
import kg.challange.ranking.routes.GithubRankingRoutes
import kg.challange.ranking.config.AppConfig
import kg.challange.ranking.service.GithubRankingService
import scala.util.Try
import scala.concurrent.ExecutionContext
import scala.language.higherKinds
import java.util.concurrent.Executors

object Server extends TaskApp {

  def invokeServer[F[_] : ContextShift : ConcurrentEffect : Timer](implicit p: Parallel[F]): Resource[F, Http4sServer[F]] = {

    val config = pureconfig
      .loadConfig[AppConfig]
      .getOrElse(AppConfig())

    val userToken = Try(System.getenv("GH_TOKEN")).toOption.flatMap(t => if (t == null) None else t.some)

    for {
      client <- createClient[F](config)
      githubService = GithubRankingService[F](client, config,userToken)
      httpApp = GithubRankingRoutes.rankingRoutes[F](githubService).orNotFound
      server <-
      BlazeServerBuilder[F]
        .bindHttp(config.port, config.host)
        .withHttpApp(httpApp)
        .resource
    } yield server
  }

  def createClient[F[_] : ConcurrentEffect](config: AppConfig): Resource[F, Client[F]] = {
    val blockingEC = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(config.nThreadsBlocking))
    BlazeClientBuilder[F](blockingEC)
      .withMaxWaitQueueLimit(config.clientMaxWaitQueueLimit)
      .withMaxTotalConnections(config.totalClientConnections).resource
  }

  def run(args : List[String]) : Task[ExitCode] =
    invokeServer[Task]
      .use(_ => Task.never)
      .as(ExitCode.Success)

}