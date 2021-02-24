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

package uk.gov.hmrc.integrationcatalogue.models

import play.api.libs.json._
import uk.gov.hmrc.integrationcatalogue.models.common._

object JsonFormatters {

  val dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
  implicit val JodaDateReads: Reads[org.joda.time.DateTime] = JodaReads.jodaDateReads(dateFormat)
  implicit val JodaDateWrites: Writes[org.joda.time.DateTime] = JodaWrites.jodaDateWrites(dateFormat)
  implicit val JodaDateTimeFormat: Format[org.joda.time.DateTime] = Format(JodaDateReads, JodaDateWrites)

  implicit val formatContactInformation : Format[ContactInformation] = Json.format[ContactInformation]
  
  implicit val formatMaintainer : Format[Maintainer] = Json.format[Maintainer]

  implicit val formatIntegrationDetail : OFormat[IntegrationDetail] = Json.format[IntegrationDetail]

  implicit val exampleFormats: OFormat[Example] = Json.format[Example]

  implicit val endpointFormats: OFormat[Endpoint] = Json.format[Endpoint]

  implicit val formatApiDetailParsed : Format[ApiDetail] = Json.format[ApiDetail]

  implicit val formatFileTransferDetail : Format[FileTransferDetail] = Json.format[FileTransferDetail]

  implicit val formatPublishRequest : Format[ApiPublishRequest] = Json.format[ApiPublishRequest]

  implicit val formatFileTransferPublishRequest : Format[FileTransferPublishRequest] = Json.format[FileTransferPublishRequest]

  implicit val formatPublishError : Format[PublishError] = Json.format[PublishError]

  implicit val formatPublishDetails : Format[PublishDetails] = Json.format[PublishDetails]

  implicit val formatPublishResult : Format[PublishResult] = Json.format[PublishResult]

  implicit val publishResponseFormat = Json.format[PublishResponse]

  implicit val errorResponseMessageFormat = Json.format[ErrorResponseMessage]
  
  implicit val errorResponseFormat = Json.format[ErrorResponse]


  implicit val formatIntegrationResponse : Format[IntegrationResponse] = Json.format[IntegrationResponse]

}