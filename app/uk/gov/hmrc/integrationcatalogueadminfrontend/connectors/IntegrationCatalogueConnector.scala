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

package uk.gov.hmrc.integrationcatalogueadminfrontend.connectors

import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.http.BadRequestException

@Singleton
class IntegrationCatalogueConnector @Inject()(http: HttpClient, appConfig: AppConfig)
                                                    (implicit ec: ExecutionContext) extends Logging {

  private lazy val externalServiceUri = s"${appConfig.integrationCatalogueUrl}/integration-catalogue"

  def publishApis(publishRequest: ApiPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.PUT[ApiPublishRequest, PublishResult](s"$externalServiceUri/apis/publish", publishRequest))
  }

  def publishFileTransfer(publishRequest: FileTransferPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    handleResult(
      http.PUT[FileTransferPublishRequest, PublishResult](s"$externalServiceUri/filetransfer/publish", publishRequest))
  }

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType])
                     (implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
   val queryParamsValues = buildQueryParams(searchTerm, platformFilter: List[PlatformType])
    handleResult(
      http.GET[IntegrationResponse](s"$externalServiceUri/integrations", queryParams = queryParamsValues))
  }

  def findByIntegrationId(id: IntegrationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
    handleResult(http.GET[IntegrationDetail](s"$externalServiceUri/integrations/${id.value.toString}"))
  }

  def deleteByIntegrationId(integrationId: IntegrationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.DELETE(s"$externalServiceUri/integrations/${integrationId.value}")
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          false
      }
  }

  def deleteByPlatform(platform: PlatformType)(implicit hc: HeaderCarrier): Future[DeleteApiResult] = {
    http.DELETE[DeleteIntegrationsResponse](s"$externalServiceUri/integrations?platformFilter=${platform.toString}")
    .map(x => DeleteIntegrationsSuccess(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
         DeleteIntegrationsFailure(e.getMessage())
        }
  }

  private def buildQueryParams(searchTerm: List[String], platformFilter: List[PlatformType]): Seq[(String, String)] = {
    val searchTerms = searchTerm.map(x => ("searchTerm", x))
    val platformsFilters = platformFilter.map((x: PlatformType) => ("platformFilter", x.toString))
    searchTerms ++ platformsFilters

  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] ={
    result.map(x=> Right(x))
      .recover {
        case NonFatal(e) => logger.error(e.getMessage)
          Left(e)
      }
  }

}