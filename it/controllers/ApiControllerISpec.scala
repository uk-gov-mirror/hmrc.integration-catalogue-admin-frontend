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

package controllers

import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.JsonFormatters._
import support.{IntegrationCatalogueService, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.ApiDetail
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData

import scala.concurrent.Future

class ApiControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with IntegrationCatalogueService with ApiDetailTestData {

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.port" -> wireMockPort,
        "metrics.enabled" -> true,
        "auditing.enabled" -> false,
        "auditing.consumer.baseUri.host" -> wireMockHost,
        "auditing.consumer.baseUri.port" -> wireMockPort,
        "microservice.services.integration-catalogue.host" -> wireMockHost,
        "microservice.services.integration-catalogue.port" -> wireMockPort
      )

  val url = s"http://localhost:$port/integration-catalogue-admin-frontend"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]

  trait Setup {

    val examplePublisherReference = "example-publisher-reference"
    val validGetApisRequest = FakeRequest(Helpers.GET, "/integration-catalogue-admin-frontend/services/apis")
    def validDeleteApiRequest(publisherReference: String) = FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-frontend/services/apis/$publisherReference")
  }

  "ApiController" when {

    "POST /services/api" should {

      "respond with 200 when no results returned from backend" in new Setup {
        primeIntegrationCatalogueServiceGetWithBody(200, Json.toJson(List.empty[ApiDetail]).toString)

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe OK
      }

      "respond with 200 when api results returned from backend" in new Setup {
        primeIntegrationCatalogueServiceGetWithBody(200, Json.toJson(List(exampleApiDetail)).toString)

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe OK
        contentAsString(response) mustBe Json.toJson(List(exampleApiDetail)).toString

      }

      "respond with 400 when bad request returned from backend" in new Setup {
        primeIntegrationCatalogueServiceGetWithBody(400, "error")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe s"""{"errors":[{"message":"error integration-catalogue GET of 'http://localhost:$wireMockPort/integration-catalogue/apis' returned 400 (Bad Request). Response body 'error'"}]}"""

      }
    }

    "DELETE /services/api/:publisherReference" should {

      "respond with 204 when deletion successful" in new Setup {
        primeIntegrationCatalogueServiceDelete(examplePublisherReference, 204)

        val response: Future[Result] = route(app, validDeleteApiRequest(examplePublisherReference)).get
        status(response) mustBe NO_CONTENT
      }

      "respond with 404 when deletion unsuccessful" in new Setup {
        primeIntegrationCatalogueServiceDelete(examplePublisherReference, 404)

        val response: Future[Result] = route(app, validDeleteApiRequest(examplePublisherReference)).get
        status(response) mustBe NOT_FOUND
      }
    }
  }
}
