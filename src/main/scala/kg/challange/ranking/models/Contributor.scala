package kg.challange.ranking.models

import cats.effect._

import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.{EntityDecoder, EntityEncoder}
import scala.language.higherKinds
import cats.Applicative
import io.circe.generic.semiauto._

case class Contributor(login: String, contributions: Long)
object Contributor {
  implicit def contributorDecoder: Decoder[Contributor] = deriveDecoder[Contributor]
  implicit def contributorListDecoder[F[_]: Sync]: EntityDecoder[F, List[Contributor]] = jsonOf[F, List[Contributor]]
   
  implicit val contributorEncoder: Encoder[Contributor] = deriveEncoder[Contributor]
  implicit def contributorListEntityEncoder[F[_]: Applicative]: EntityEncoder[F, List[Contributor]] = jsonEncoderOf   


}