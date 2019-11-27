package kg.challange.ranking.models

sealed trait Error

case object UnauthorizedError extends Error
case object BadRequestError extends Error
case object UnprocessableEntityError extends Error
case class UnrecognizedError(statusCode: Int) extends Error

final case class GeneralError(e: Throwable) extends RuntimeException
