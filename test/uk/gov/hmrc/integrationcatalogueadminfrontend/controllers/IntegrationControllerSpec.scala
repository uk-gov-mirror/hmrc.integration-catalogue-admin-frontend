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

import akka.stream.Materializer
import org.mockito.scalatest.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.HeaderNames
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubBodyParserFactory}
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders._
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.IntegrationService
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationControllerSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with StubBodyParserFactory with ApiDetailTestData {

  implicit lazy val mat: Materializer = app.materializer

  val mockAppConfig = mock[AppConfig]
  val mockIntegrationService: IntegrationService = mock[IntegrationService]
  
  private val validateQueryParamKeyAction = app.injector.instanceOf[ValidateQueryParamKeyAction]
  private val authAction = new ValidateAuthorizationHeaderAction(mockAppConfig)
  private val validateIntegrationIdAgainstPlatformTypeAction = new ValidateIntegrationIdAgainstPlatformTypeAction(mockIntegrationService)

  trait Setup {

    val controller = new IntegrationController(
      mockAppConfig,
      mockIntegrationService,
      validateQueryParamKeyAction,
      authAction,
      validateIntegrationIdAgainstPlatformTypeAction,
      stubMessagesControllerComponents()
    )

    private val encodedCoreIfAuthKey = "c29tZUtleTM="
    val coreIfAuthHeader = List(HeaderNames.AUTHORIZATION -> encodedCoreIfAuthKey)
    val coreIfPlatformTypeHeader =  List(HeaderKeys.platformKey -> "CORE_IF")

  }

  "DELETE /services/integrations/{id}" should {

    "respond with 401 when platform header does not have any auth setup in app config" in new Setup {
      when(mockIntegrationService.findByIntegrationId(*[IntegrationId])(*)).thenReturn(Future.successful(Right(exampleApiDetail)))

      when(mockAppConfig.authPlatformMap).thenReturn(Map.empty)

      val deleteRequest = FakeRequest.apply("DELETE", s"integration-catalogue-admin-frontend/services/integrations/${exampleApiDetail.id.value.toString}")
        .withHeaders(coreIfAuthHeader ++ coreIfPlatformTypeHeader : _*)

      val result = controller.deleteByIntegrationId(exampleApiDetail.id)(deleteRequest)
      status(result) shouldBe UNAUTHORIZED
      contentAsString(result) shouldBe """{"errors":[{"message":"Authorisation failed"}]}"""
      
    }
}
}