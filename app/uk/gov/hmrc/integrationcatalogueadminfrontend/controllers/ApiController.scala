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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.ApiService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common.ErrorResponseMessage
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common.ErrorResponse


@Singleton
class ApiController @Inject()(appConfig: AppConfig,
                              apiService: ApiService,
                               mcc: MessagesControllerComponents,
                               playBodyParsers: PlayBodyParsers)
                                 (implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging {

  implicit val config: AppConfig = appConfig


 def getAllApis: Action[AnyContent] = Action.async { implicit request =>
    apiService.getAllApis() map {
      case Right(response) => Ok(Json.toJson(response))
      case Left(error: Throwable)  =>  BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( s"error integration-catalogue ${error.getMessage}")))))
    }

 }


  def deleteByPublisherReference(publisherReference: String) : Action[AnyContent] = Action.async { implicit request =>
    apiService.deleteByPublisherReference(publisherReference).map {
      case true => NoContent
      case false => NotFound
    }
  }
}
