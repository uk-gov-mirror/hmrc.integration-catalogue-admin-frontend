/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common

import java.util.UUID
import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

case class IntegrationId(value: UUID) extends AnyVal

object IntegrationId {
  import play.api.libs.json.Json
  implicit val apiIdFormat = Json.valueFormat[IntegrationId]
}

case class ResourceId(value: UUID) extends AnyVal

object ResourceId {
  import play.api.libs.json.Json
  implicit val apiIdFormat = Json.valueFormat[ResourceId]
}

sealed trait PlatformType extends EnumEntry 

object PlatformType extends Enum[PlatformType] with PlayJsonEnum[PlatformType] {

  val values = findValues

  case object DES  extends PlatformType
  case object CORE_IF   extends PlatformType
  case object API_PLATFORM extends PlatformType
  case object CORE_IF_FILE_TRANSFER_FLOW     extends PlatformType

}

sealed trait SpecificationType extends EnumEntry

object SpecificationType extends Enum[SpecificationType] with PlayJsonEnum[SpecificationType] {

  val values = findValues

  case object OAS_V3   extends SpecificationType

}

case class ContactInformation(name: String, emailAddress: String)

case class Maintainer(name: String, slackChannel: String, contactInfo: List[ContactInformation] = List.empty)


sealed trait MessageType extends EnumEntry 

object MessageType extends Enum[MessageType] with PlayJsonEnum[MessageType] {

  val values = findValues

  case object JSON   extends MessageType
  case object XML extends MessageType

}

case class PublishResponse(id: IntegrationId, publisherReference: String, platformType: PlatformType)

case class ErrorResponseMessage(message: String)
case class ErrorResponse(errors: List[ErrorResponseMessage])
