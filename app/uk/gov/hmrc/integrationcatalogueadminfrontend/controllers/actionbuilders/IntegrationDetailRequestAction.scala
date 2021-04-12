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

import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{ActionRefiner, Request, Result, WrappedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models.{ErrorResponse, ErrorResponseMessage, IntegrationDetail}
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.IntegrationService
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

// TODO change to an Integration (possibly an option?)
case class IntegrationDetailRequest[A](integrationDetail: IntegrationDetail,
                                        request: Request[A]   ) extends WrappedRequest[A](request)

object IntegrationDetailRequestAction {
  def IntegrationDetailAction(integrationId: IntegrationId, integrationService: IntegrationService)
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
