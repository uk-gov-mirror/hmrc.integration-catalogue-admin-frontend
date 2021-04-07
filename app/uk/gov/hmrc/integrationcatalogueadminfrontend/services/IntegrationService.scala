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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models.IntegrationResponse
import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType


@Singleton
class IntegrationService @Inject()(integrationCatalogueConnector: IntegrationCatalogueConnector){

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit hc: HeaderCarrier) : Future[Boolean] = {
    integrationCatalogueConnector.deleteByIntegrationId(integrationId)
  }
  
  def deleteByPlatform(platform: PlatformType)(implicit hc: HeaderCarrier) : Future[Unit] = {
    integrationCatalogueConnector.deleteByPlatform(platform)
  }

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType])
  (implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
        integrationCatalogueConnector.findWithFilters(searchTerm, platformFilter)
  }

  def findByIntegrationId(integrationId: IntegrationId)
    (implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
         integrationCatalogueConnector.findByIntegrationId(integrationId)
  }
}


