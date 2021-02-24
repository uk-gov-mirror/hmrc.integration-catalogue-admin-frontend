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

package uk.gov.hmrc.integrationcatalogueadminfrontend.controllers

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.IntegrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class IntegrationController @Inject()(appConfig: AppConfig, integrationService: IntegrationService, mcc: MessagesControllerComponents)
                                 (implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  implicit val config: AppConfig = appConfig

 def getAllIntegrations: Action[AnyContent] = Action.async { implicit request =>
    integrationService.getAllIntegrations() map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(error: Throwable)  =>  BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"error integration-catalogue ${error.getMessage}")))))
    }
 }

  def deleteByPublisherReference(publisherReference: String) : Action[AnyContent] = Action.async { implicit request =>
    integrationService.deleteByPublisherReference(publisherReference).map {
      case true => NoContent
      case false => NotFound(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"The requested resource could not be found.")))))
    }
  }
}
