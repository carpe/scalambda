package io.carpe.scalambda.fixtures

import io.circe.{Decoder, Encoder}

object TestModels {

  import io.circe.generic.semiauto._

  case class Car(wheels: Seq[Wheel], hp: Int)

  implicit val carEncoder: Encoder[Car] = deriveEncoder[Car]
  implicit val carDecoder: Decoder[Car] = deriveDecoder[Car]

  case class Wheel(brandName: Option[String], width: Int, diameter: Int)

  implicit val wheelEncoder: Encoder[Wheel] = deriveEncoder[Wheel]
  implicit val wheelDecoder: Decoder[Wheel] = deriveDecoder[Wheel]


  lazy val validCar: Car = Car(
    wheels = Seq(Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40)),
    hp = 1337
  )

  lazy val validCarWithNulls: Car = Car(
    wheels = Seq(Wheel(None, 20, 40), Wheel(None, 20, 40), Wheel(None, 20, 40), Wheel(None, 20, 40)),
    hp = 1337
  )

  lazy val lowHorsepowerCar: Car = Car(
    wheels = Seq(Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40), Wheel(Some("Pirelli"), 20, 40)),
    hp = 8
  )
}
