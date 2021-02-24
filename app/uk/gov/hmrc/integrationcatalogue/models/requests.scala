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

import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, Maintainer, PlatformType, SpecificationType}

case class IntegrationResponse(count: Int, results: List[IntegrationDetail])

case class ApiPublishRequest(publisherReference: String, platformType: PlatformType, specificationType: SpecificationType, contents: String)

case class FileTransferPublishRequest(
    publisherReference: String,
    title: String,
    description: String,
    platformType: PlatformType,
    //    lastUpdated: DateTime,
    maintainer: Maintainer,
    flowId: String,
    sourceSystem: String,
    targetSystem: String,
    fileTransferPattern: String)

//TODO remove code from PublishError
case class PublishError(code: Int, message: String)

case class PublishDetails(isUpdate: Boolean, integrationId: IntegrationId, publisherReference: String, platformType: PlatformType)

object PublishDetails{
    def toPublishResponse(details: PublishDetails) = {
        PublishResponse(details.integrationId, details.publisherReference, details.platformType)
    }
}

case class PublishResult(isSuccess: Boolean, publishDetails: Option[PublishDetails] = None, errors: List[PublishError] = List.empty)

sealed trait DeleteApiResult

case object NotFoundDeleteApiResult extends DeleteApiResult
case object NoContentDeleteApiResult extends DeleteApiResult

case class PublishResponse(id: IntegrationId, publisherReference: String, platformType: PlatformType)

case class ErrorResponseMessage(message: String)
case class ErrorResponse(errors: List[ErrorResponseMessage])
