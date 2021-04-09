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

import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.Json
import play.api.mvc.{ActionRefiner, Request, Result}
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.http.HttpErrorFunctions
import uk.gov.hmrc.integrationcatalogue.models.ErrorResponse
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.{ExtractedHeaders, ValidatedApiPublishRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import play.api.mvc.WrappedRequest
import java.util.UUID

// TODO change to an Integration (possibly an option?)
case class IntegrationDetailRequest[A]( integrationId: IntegrationId,
                                        request: Request[A]   ) extends WrappedRequest[A](request)

object stuff {
  def IntegrationDetailAction(integrationId: IntegrationId)(implicit ec: ExecutionContext) : ActionRefiner[Request, IntegrationDetailRequest] = {
      new ActionRefiner[Request, IntegrationDetailRequest] {

        override def executionContext: ExecutionContext = ec
        override def refine[A](request: Request[A]): Future[Either[Result, IntegrationDetailRequest[A]]] = Future.successful {
          // TODO: Do stuff here

          Right(IntegrationDetailRequest(integrationId, request))
        }
      }
  }
}
