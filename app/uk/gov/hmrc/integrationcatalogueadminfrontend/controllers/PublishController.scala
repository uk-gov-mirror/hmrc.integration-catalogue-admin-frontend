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
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders._
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.HeaderKeys
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.PublishService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

@Singleton
class PublishController @Inject()(
                                   appConfig: AppConfig,
                                   mcc: MessagesControllerComponents,
                                   publishService: PublishService,
                                   validatePlatformHeaderAction: ValidatePlatformHeaderAction,
                                   validateSpecificationTypeHeaderAction: ValidateSpecificationTypeHeaderAction,
                                   validatePublisherRefHeaderAction: ValidatePublisherRefHeaderAction,
                                   playBodyParsers: PlayBodyParsers)
                                 (implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging {

  implicit val config: AppConfig = appConfig


  def publishApi(): Action[MultipartFormData[Files.TemporaryFile]] =

    (Action andThen
      validatePlatformHeaderAction andThen
      validatePublisherRefHeaderAction andThen
      validateSpecificationTypeHeaderAction).async(playBodyParsers.multipartFormData) { implicit request =>
      (request.headers.get(HeaderKeys.platformKey),
        request.headers.get(HeaderKeys.specificationTypeKey),
        request.headers.get(HeaderKeys.publisherRefKey)) match {
        case (Some(platformType), Some(specificationType), Some(publisherRef)) =>
          request.body.file("selectedFile") match {
            case None =>
              logger.info("selectedFile is missing from requestBody")
              Future.successful(BadRequest(Json.toJson(ErrorResponse( List(ErrorResponseMessage( "selectedFile is missing from requestBody"))))))
            case Some(selectedFile) =>
              val convertedPlatformType = PlatformType.withNameInsensitive(platformType)
              val convertedSpecType = SpecificationType.withNameInsensitive(specificationType)
              val bufferedSource = Source.fromFile(selectedFile.ref.path.toFile)
              val fileContents = bufferedSource.getLines.mkString("\r\n")
              bufferedSource.close()
              publishService.publishApi(publisherRef, convertedPlatformType, convertedSpecType, fileContents)
              .map(handlePublishResult)
          }
        case _ =>
          logger.info("invalid header(s) provided")
          Future.successful(BadRequest(Json.toJson(ErrorResponse( List(ErrorResponseMessage( "Please provide valid headers"))))))
      }

    }


 private def handlePublishResult(result : Either[Throwable, PublishResult] ) ={
   result match {
     case Right(publishResult) => {
       publishResult.publishDetails match {
         case Some(details) =>
           val resultAsJson = Json.toJson(PublishDetails.toPublishResponse(details))
           if(details.isUpdate) Ok(resultAsJson) else Created(resultAsJson)
         case None =>  if(publishResult.errors.nonEmpty){
           BadRequest(Json.toJson(ErrorResponse(publishResult.errors.map(x => ErrorResponseMessage(x.message)))))
         } else {
           BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( "selectedFile is missing from requestBody")))))
         }
       }
     }
     case Left(errorResult) =>
       BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage(s"Unexpected response from /integration-catalogue: ${errorResult.getMessage}")))))
   }
 }
}
