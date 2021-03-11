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


import play.api.http.HeaderNames
import play.api.mvc.Results._
import play.api.mvc.{ActionFilter, Request, Result}
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage}
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import scala.util.Try
import java.util.Base64
import java.nio.charset.StandardCharsets

@Singleton
class ValidateAuthorizationHeaderAction @Inject()(appConfig: AppConfig)(implicit ec: ExecutionContext)
  extends ActionFilter[Request] with HttpErrorFunctions {

  override def executionContext: ExecutionContext = ec

  override protected def filter[A](request: Request[A]): Future[Option[Result]] = {
     def base64Decode(stringToDecode: String): Try[String] = Try(new String(Base64.getDecoder.decode(stringToDecode), StandardCharsets.UTF_8))

    val authHeader = request.headers.get(HeaderNames.AUTHORIZATION).getOrElse("")

    if (!authHeader.isEmpty && base64Decode(authHeader).map(_ == appConfig.authorizationKey).getOrElse(false)) Future.successful(None)
    else Future.successful(Some(Forbidden(Json.toJson(ErrorResponse(List(ErrorResponseMessage("Authorisation failed")))))))

  }
}
