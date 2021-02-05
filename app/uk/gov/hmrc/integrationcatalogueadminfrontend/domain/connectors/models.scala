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

package uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors

import uk.gov.hmrc.integrationcatalogueadminfrontend.domain._




case class PublishRequest(publisherReference: String, platformType: PlatformType, fileName: String, specificationType: SpecificationType, contents: String)

case class PublishError(code: Int, message: String)

case class PublishDetails(isUpdate: Boolean, integrationId: IntegrationId, publisherReference: String, platformType: PlatformType)

case class PublishResult(isSuccess: Boolean, publishDetails: Option[PublishDetails], errors: List[PublishError] = List.empty)

object PublishDetails{
    def toPublishResponse(details: PublishDetails) = {
        PublishResponse(details.integrationId, details.publisherReference, details.platformType)
    }
}