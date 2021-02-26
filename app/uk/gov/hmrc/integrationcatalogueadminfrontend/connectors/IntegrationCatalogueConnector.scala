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

@Singleton
class IntegrationCatalogueConnector @Inject()(http: HttpClient, appConfig: AppConfig)
                                                    (implicit ec: ExecutionContext) extends Logging {

  private lazy val externalServiceUri = s"${appConfig.integrationCatalogueUrl}/integration-catalogue"

  def publishApis(publishRequest: ApiPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    http.PUT[ApiPublishRequest, PublishResult](s"$externalServiceUri/apis/publish", publishRequest)
    .map(x=> Right(x))
    .recover {
      case NonFatal(e) => handleAndLogError(e)
    }
  }

  def publishFileTransfer(publishRequest: FileTransferPublishRequest)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResult]] = {
    http.PUT[FileTransferPublishRequest, PublishResult](s"$externalServiceUri/filetransfer/publish", publishRequest)
    .map(x=> Right(x))
    .recover {
      case NonFatal(e) => handleAndLogError(e)
    }
  }

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType])(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
   //queryParams: Seq[(String, String)]
   val queryParmsValues = buildQueryParams(searchTerm, platformFilter: List[PlatformType])
    http.GET[IntegrationResponse](s"$externalServiceUri/integrations/find-with-filter", queryParams = queryParmsValues)
    .map(x=> Right(x))
    .recover {
      case NonFatal(e) => handleAndLogError(e)
    }
  }

  private def buildQueryParams(searchTerm: List[String], platformFilter: List[PlatformType]): Seq[(String, String)] = {
    val searchTerms = searchTerm.map(x => ("searchTerm" , x)).toSeq
    val platformsFilters = platformFilter.map((x: PlatformType) => ("platformFilter" , x.toString())).toSeq
    searchTerms ++ platformsFilters
  
  } 

  def findByIntegrationId(id: IntegrationId)(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationDetail]] = {
    http.GET[IntegrationDetail](s"$externalServiceUri/integrations/${id.value.toString}")
    .map(x=> Right(x))
    .recover {
      case NonFatal(e) => handleAndLogError(e)
    }
  }

  def deleteByPublisherReference(publisherReference: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    http.DELETE(s"$externalServiceUri/integrations/$publisherReference")
      .map(_.status == NO_CONTENT)
      .recover {
        case NonFatal(e) => {
          logger.error(e.getMessage())
          false
        }
      }
  }

 private  def handleAndLogError(error: Throwable) = {
      logger.error(error.getMessage())
      Left(error)
  }
}