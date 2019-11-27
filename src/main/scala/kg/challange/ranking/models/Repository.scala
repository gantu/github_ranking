package kg.challange.ranking.models

import cats.effect.Sync
import io.circe._
import io.circe.generic.semiauto._
import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
import io.circe.Decoder
import scala.language.higherKinds
import cats.Applicative


case class Repository(id: Long, name: String)

object Repository {
    implicit def repositoryDecoder: Decoder[Repository] = deriveDecoder[Repository]
    implicit def repositoryListDecoder[F[_]: Sync]: EntityDecoder[F, List[Repository]] =
    jsonOf[F, List[Repository]]
   
    implicit val repositoryEncoder: Encoder[Repository] = deriveEncoder[Repository]
    implicit def repositoryListEntityEncoder[F[_]: Applicative]: EntityEncoder[F, List[Repository]] =
        jsonEncoderOf    
}