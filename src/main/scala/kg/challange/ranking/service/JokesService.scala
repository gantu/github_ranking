package kg.challange.ranking.service

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
import kg.challange.ranking.models.{Joke, JokeError}

trait JokesAlgebra[F[_]]{
    def get: F[Joke]
}

object JokesService {
    def apply[F[_]](implicit ev: JokesAlgebra[F]): JokesAlgebra[F] = ev

    def impl[F[_]: Sync](C: Client[F]): JokesAlgebra[F] = new JokesAlgebra[F]{
        val dsl = new Http4sClientDsl[F]{}
        import dsl._
        def get: F[Joke] = {
          C.expect[Joke](GET(uri"https://icanhazdadjoke.com/"))
            .adaptError{ case t => JokeError(t)} // Prevent Client Json Decoding Failure Leaking
        }
      }
}