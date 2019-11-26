package kg.challange.ranking.models

import cats.Applicative
import cats.effect.Sync
import cats.implicits._
import io.circe.{Encoder, Decoder, Json, HCursor}
import io.circe.generic.semiauto._
import org.http4s._
import org.http4s.implicits._
import org.http4s.{EntityDecoder, EntityEncoder, Method, Uri, Request}
import org.http4s.client.Client
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.Method._
import org.http4s.circe._


case class Joke(joke: String) extends AnyVal
object Joke {
    implicit val jokeDecoder: Decoder[Joke] = deriveDecoder[Joke]
    implicit def jokeEntityDecoder[F[_]: Sync]: EntityDecoder[F, Joke] =
      jsonOf
    implicit val jokeEncoder: Encoder[Joke] = deriveEncoder[Joke]
    implicit def jokeEntityEncoder[F[_]: Applicative]: EntityEncoder[F, Joke] =
      jsonEncoderOf
}

final case class JokeError(e: Throwable) extends RuntimeException
