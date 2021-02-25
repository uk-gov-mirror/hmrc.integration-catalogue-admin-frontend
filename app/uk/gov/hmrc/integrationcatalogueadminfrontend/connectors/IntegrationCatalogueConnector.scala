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

  def getAll()(implicit hc: HeaderCarrier): Future[Either[Throwable, IntegrationResponse]] = {
    http.GET[IntegrationResponse](s"$externalServiceUri/integrations")
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