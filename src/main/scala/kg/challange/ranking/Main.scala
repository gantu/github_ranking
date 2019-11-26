package kg.challange.ranking

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    GithubRankingServer.stream[IO].compile.drain.as(ExitCode.Success)
}
