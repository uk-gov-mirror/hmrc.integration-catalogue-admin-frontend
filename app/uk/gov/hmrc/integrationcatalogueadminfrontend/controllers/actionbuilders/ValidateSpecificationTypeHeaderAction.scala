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

package uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders


import javax.inject.{Inject, Singleton}
import play.api.mvc.Results._
import play.api.mvc.{ActionFilter, Request, Result}
import scala.concurrent.{ExecutionContext, Future}

import play.api.Logging
import _root_.uk.gov.hmrc.http.HttpErrorFunctions
import _root_.uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import play.api.libs.json.Json
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.HeaderKeys


@Singleton
class ValidateSpecificationTypeHeaderAction @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext)
  extends ActionFilter[Request] with HttpErrorFunctions with Logging {

  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {

    val specType = request.headers.get(HeaderKeys.specificationTypeKey).getOrElse("")

     if (specType.nonEmpty && validateSpecificationType(specType)) Future.successful(None)
     else {
       logger.info("Invalid specification type header provided")
       Future.successful(Some(BadRequest(Json.toJson(ErrorResponse(List(ErrorResponseMessage( "specification type header is missing or invalid")))))))
     }
  
  }

  private def validateSpecificationType(specType: String) ={
    SpecificationType.values.map(_.toString()).contains(specType.toUpperCase)
  }
}
