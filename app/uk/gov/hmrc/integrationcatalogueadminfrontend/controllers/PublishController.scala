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
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders._
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.ValidatedApiPublishRequest
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.PublishService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success, Try}
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogue.models.FileTransferPublishRequest
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponse
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponseMessage
import uk.gov.hmrc.integrationcatalogue.models.PublishResult
import uk.gov.hmrc.integrationcatalogue.models.PublishDetails

@Singleton
class PublishController @Inject() (
    appConfig: AppConfig,
    mcc: MessagesControllerComponents,
    publishService: PublishService,
    validateApiPublishRequest: ValidateApiPublishRequestAction,
    validateAuthorizationHeaderAction: ValidateAuthorizationHeaderAction,
    playBodyParsers: PlayBodyParsers
  )(implicit ec: ExecutionContext)
    extends FrontendController(mcc)
    with Logging {

  implicit val config: AppConfig = appConfig

  def publishFileTransfer(): Action[JsValue] =
    (Action andThen validateAuthorizationHeaderAction).async(playBodyParsers.tolerantJson) { implicit request =>
      validateAndExtractJsonString[FileTransferPublishRequest](request.body.toString()) match {
        case Some(validBody) => if(validatePlatformTypesMatch(validBody))publishService.publishFileTransfer(validBody).map(handlePublishResult)
          else Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body - platform type mismatch"))))))
        case None => logger.error("Invalid request body, must be a valid publish request")
          Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Invalid request body"))))))
      }
    }

  private def validatePlatformTypesMatch(fileTransferRequest: FileTransferPublishRequest)(implicit request: Request[JsValue])={
    val platformHeader = request.headers.get(HeaderKeys.platformKey).getOrElse("")
    fileTransferRequest.platformType.entryName.equalsIgnoreCase(platformHeader)
  } 

  def publishApi(): Action[MultipartFormData[Files.TemporaryFile]] = (Action andThen
    validateAuthorizationHeaderAction andThen
    validateApiPublishRequest).async(playBodyParsers.multipartFormData) {
    implicit request: ValidatedApiPublishRequest[MultipartFormData[Files.TemporaryFile]] =>
      request.body.file("selectedFile") match {
        case None               =>
          logger.info("selectedFile is missing from requestBody")
          Future.successful(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("selectedFile is missing from requestBody"))))))
        case Some(selectedFile) =>
          val bufferedSource = Source.fromFile(selectedFile.ref.path.toFile)
          val fileContents = bufferedSource.getLines.mkString("\r\n")
          bufferedSource.close()
          publishService.publishApi(request.publisherReference, request.platformType, request.specificationType, fileContents)
            .map(handlePublishResult)
      }

    }


  private def handlePublishResult(result: Either[Throwable, PublishResult]) = {
    result match {
      case Right(publishResult) =>
        publishResult.publishDetails match {
          case Some(details) =>
            val resultAsJson = Json.toJson(PublishDetails.toPublishResponse(details))
            if (details.isUpdate) Ok(resultAsJson) else Created(resultAsJson)
          case None => if (publishResult.errors.nonEmpty) {
            BadRequest(Json.toJson(ErrorResponse(publishResult.errors.map(x => ErrorResponseMessage(x.message)))))
          } else {
            BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Unexpected response from /integration-catalogue")))))
          }
        }
      case Left(errorResult) =>
        BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Unexpected response from /integration-catalogue: ${errorResult.getMessage}")))))
    }
  }

  private def validateAndExtractJsonString[T](body: String)(implicit write: Writes[T], reads: Reads[T]) = {
    validateJsonAndExtract[T](body, body => Json.parse(body))
  }

  private def validateJsonAndExtract[T](body: String, f: String => JsValue)(implicit reads: Reads[T]) = {
    Try[T] {
      f(body).as[T]
    } match {
      case Success(result) => Some(result)
      case Failure(_) => None
    }
  }
}
