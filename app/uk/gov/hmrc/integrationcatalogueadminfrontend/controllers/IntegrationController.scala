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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.{IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders.{ValidateAuthorizationHeaderAction, ValidateIntegrationIdAgainstParametersAction, ValidateQueryParamKeyAction}
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.IntegrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders.ValidateDeleteByPlatformAction._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.integrationcatalogue.models.DeleteIntegrationsFailure
import uk.gov.hmrc.integrationcatalogue.models.DeleteIntegrationsSuccess
import play.api.mvc.ActionRefiner
import play.api.mvc.Request
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.IntegrationDetailRequest
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.play.HeaderCarrierConverter


@Singleton
class IntegrationController @Inject()(appConfig: AppConfig,
                                      integrationService: IntegrationService,
                                      validateQueryParamKeyAction: ValidateQueryParamKeyAction,
                                      validateAuthorizationHeaderAction: ValidateAuthorizationHeaderAction,
                                      validateIntegrationIdAgainstPlatformTypeAction: ValidateIntegrationIdAgainstParametersAction,
                                      mcc: MessagesControllerComponents)
                                 (implicit ec: ExecutionContext) extends FrontendController(mcc) with Logging {

  implicit val config: AppConfig = appConfig

  def findWithFilters(searchTerm: List[String], platformFilter: List[PlatformType]) : Action[AnyContent] =
    (Action andThen validateQueryParamKeyAction).async { implicit request =>
    integrationService.findWithFilters(searchTerm, platformFilter)
     .map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(error: Throwable) =>
        logger.error(s"findWithFilters error integration-catalogue ${error.getMessage}")
        InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Unable to process your request")))))
    }
}

 def findByIntegrationId(id: IntegrationId): Action[AnyContent] =
   Action.async { implicit request =>
    integrationService.findByIntegrationId(id)map {
        case Right(response) => Ok(Json.toJson(response))
        case Left(_: NotFoundException)  =>  NotFound(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"findByIntegrationId: The requested resource could not be found.")))))
        case Left(error: Throwable) =>
          BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"findByIntegrationId error integration-catalogue ${error.getMessage}")))))
      }
 }

  def deleteByIntegrationId(integrationId: IntegrationId): Action[AnyContent] =
    (Action andThen validateAuthorizationHeaderAction
    andThen IntegrationDetailAction(integrationId)
    andThen validateIntegrationIdAgainstPlatformTypeAction).async { implicit request =>
      integrationService.deleteByIntegrationId(integrationId).map {
        case true => NoContent
        case false => InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage("InternalServerError from integration-catalogue")))))
      }
    }

  def deleteByPlatform(platforms: List[PlatformType]): Action[AnyContent] =
    (Action andThen validateAuthorizationHeaderAction
    andThen ValidatePlatformTypeParams(platforms)).async { implicit request =>
      integrationService.deleteByPlatform(platforms.head).map {
        case DeleteIntegrationsSuccess(result) => Ok(Json.toJson(result))
        case DeleteIntegrationsFailure(errorMessage) => InternalServerError(Json.toJson(ErrorResponse(List(ErrorResponseMessage(errorMessage)))))
      }
    }

    private def IntegrationDetailAction(integrationId: IntegrationId)
                             (implicit ec: ExecutionContext) : ActionRefiner[Request, IntegrationDetailRequest] = {
      new ActionRefiner[Request, IntegrationDetailRequest] {

        override def executionContext: ExecutionContext = ec

        implicit def hc(implicit request: Request[_]): HeaderCarrier = {
          HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
  }
        override def refine[A](request: Request[A]): Future[Either[Result, IntegrationDetailRequest[A]]] =
         integrationService.findByIntegrationId(integrationId)(hc(request)).map {
           case Left(_) => Left(NotFound(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Integration with ID: ${integrationId.value.toString} not found"))))))
           case Right(integrationDetail: IntegrationDetail) => Right(IntegrationDetailRequest(integrationDetail, request))
         }
          
        }
      
  }
 
}
