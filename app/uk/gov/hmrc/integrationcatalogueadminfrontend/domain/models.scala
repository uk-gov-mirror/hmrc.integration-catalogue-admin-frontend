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

package uk.gov.hmrc.integrationcatalogueadminfrontend.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.JsObject
import play.api.libs.json.Json


sealed trait PlatformType extends EnumEntry

object PlatformType extends Enum[PlatformType] with PlayJsonEnum[PlatformType] {

  val values = findValues

  case object CORE_IF   extends PlatformType
  case object API_PLATFORM extends PlatformType
  case object CORE_IF_FILE_TRANSFER_FLOW      extends PlatformType

}

sealed trait SpecificationType extends EnumEntry

object SpecificationType extends Enum[SpecificationType] with PlayJsonEnum[SpecificationType] {

  val values = findValues

  case object OAS_V3   extends SpecificationType


}

object ErrorCode extends Enumeration {
  type ErrorCode = Value
  val ACCEPT_HEADER_INVALID = Value("ACCEPT_HEADER_INVALID")
  val BAD_REQUEST = Value("BAD_REQUEST")
  val FORBIDDEN = Value("FORBIDDEN")
  val INVALID_ACCEPT_HEADER = Value("INVALID_ACCEPT_HEADER")
  val INVALID_CONTENT_TYPE =Value("INVALID_CONTENT_TYPE")
  val INVALID_REQUEST_PAYLOAD = Value("INVALID_REQUEST_PAYLOAD")
  val NOT_FOUND = Value("NOT_FOUND")
  val UNAUTHORISED = Value("UNAUTHORISED")
  val UNKNOWN_ERROR = Value("UNKNOWN_ERROR")

}

object JsErrorResponse {
  def apply(errorCode: ErrorCode.Value, message: JsValueWrapper): JsObject =
 Json.obj(
      "code" -> errorCode.toString,
      "message" -> message
    )
}
