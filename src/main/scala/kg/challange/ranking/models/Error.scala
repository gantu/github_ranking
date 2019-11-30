package kg.challange.ranking.models

import cats.Applicative
import cats.effect.Sync
import org.http4s.{EntityDecoder, EntityEncoder, Method, Uri, Request}
import io.circe.{Encoder, Decoder, Json, HCursor}
import io.circe.generic.semiauto._
import org.http4s.circe._

sealed trait Error

case object UnauthorizedError extends Error
case object BadRequestError extends Error
case object UnprocessableEntityError extends Error
case class UnrecognizedError(statusCode: Int) extends Error

final case class GeneralError(e: Throwable) extends RuntimeException


case class ServiceResponseError(code: Int, message: String)

object ServiceResponseError {
    implicit val responseErrorDecoder: Decoder[ServiceResponseError] = deriveDecoder[ServiceResponseError]
    implicit def responseErrorEntityDecoder[F[_]: Sync]: EntityDecoder[F, ServiceResponseError] =
        jsonOf[F, ServiceResponseError]

  implicit val responseErrorEncoder: Encoder[ServiceResponseError] = deriveEncoder[ServiceResponseError]
  implicit def responseErrorEntityEncoder[F[_]: Applicative]: EntityEncoder[F, ServiceResponseError] =
    jsonEncoderOf[F, ServiceResponseError]
}
