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
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders.ValidateQueryParamKeyAction

@Singleton
class IntegrationController @Inject()(appConfig: AppConfig,
                                      integrationService: IntegrationService,
                                      validateQueryParamKeyAction: ValidateQueryParamKeyAction,
                                      mcc: MessagesControllerComponents)
                                 (implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  implicit val config: AppConfig = appConfig

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType]) : Action[AnyContent] =
    (Action andThen validateQueryParamKeyAction).async { implicit request =>
    integrationService.findWithFilters(searchTerm, platformFilter)
     .map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(_: NotFoundException)  => NotFound
      case Left(error: Throwable) =>
        BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"findWithFilters error integration-catalogue ${error.getMessage}")))))
    }
}

 def findAllIntegrations: Action[AnyContent] =
   Action.async { implicit request =>
    integrationService.findWithFilters(List.empty, List.empty)
     .map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(_: NotFoundException)  => NotFound
      case Left(error: Throwable) =>
        BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"findAllIntegrations error integration-catalogue ${error.getMessage}")))))
    }
 }

 def findByIntegrationId(id: IntegrationId)  =
   Action.async { implicit request =>
  integrationService.findByIntegrationId(id)map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(_: NotFoundException)  => NotFound
      case Left(error: Throwable) =>
        BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"findByIntegrationId error integration-catalogue ${error.getMessage}")))))
    }
 }

  def deleteByIntegrationId(integrationId: IntegrationId) : Action[AnyContent] =
    Action.async { implicit request =>
    integrationService.deleteByIntegrationId(integrationId).map {
      case true => NoContent
      case false => NotFound(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"deleteByIntegrationId: The requested resource could not be found.")))))
    }
  }
}
