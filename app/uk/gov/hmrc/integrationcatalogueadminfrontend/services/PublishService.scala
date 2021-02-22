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

package uk.gov.hmrc.integrationcatalogueadminfrontend.services

import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.PublishRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common.PlatformType
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common.SpecificationType
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.PublishResult


@Singleton
class PublishService @Inject()(integrationCatalogueConnector: IntegrationCatalogueConnector)(implicit ec: ExecutionContext){
    
    def publishApi(publisherRef: String, platformType: PlatformType, specType: SpecificationType, contents: String)
    (implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]]  ={
         integrationCatalogueConnector.publish(PublishRequest(publisherRef, platformType, specType, contents))
    }
}


