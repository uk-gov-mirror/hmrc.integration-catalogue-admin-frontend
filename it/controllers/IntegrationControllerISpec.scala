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
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import support.{IntegrationCatalogueService, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.models.IntegrationResponse
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData

import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId

class IntegrationControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with IntegrationCatalogueService with ApiDetailTestData {

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
    val validGetApisRequest = FakeRequest(Helpers.GET, "/integration-catalogue-admin-frontend/services/integrations")
    def validFindByIntegrationIdRequest(id: IntegrationId) = FakeRequest(Helpers.GET, s"/integration-catalogue-admin-frontend/services/integrations/${id.value}")
    def validFindwithFilterRequest(searchTerm: String) = FakeRequest(Helpers.GET, s"/integration-catalogue-admin-frontend/services/integrations/find-with-filter$searchTerm")
    def validDeleteApiRequest(publisherReference: String) = FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-frontend/services/integrations/$publisherReference")
  }

  "IntegrationController" when {

    "GET /services/integrations/{id}" should  {

      "return 200 and integration detail from backend" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(200, Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString, exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe OK
        contentAsString(response) mustBe """{"_type":"uk.gov.hmrc.integrationcatalogue.models.ApiDetail","id":"e2e4ce48-29b0-11eb-adc1-0242ac120002","publisherReference":"API1689","title":"getKnownFactsName","description":"getKnownFactsDesc","platform":"CORE_IF","searchText":"Some Search Text","hods":["ETMP"],"lastUpdated":"2020-11-04T20:27:05.000+0000","maintainer":{"name":"IF Team","slackChannel":"N/A","contactInfo":[]},"messageType":"JSON","version":"1.1.0","specificationType":"OAS_V3","endpoints":[{"path":"/some/url","httpMethod":"GET","summary":"some summary","description":"some description","exampleRequests":[{"name":"example request 1","jsonBody":"{\"someValue\": \"abcdefg\"}","mediaType":"application/json"}],"exampleResponses":[{"name":"example response name","jsonBody":"example response body","mediaType":"application/json"}]},{"path":"/some/url","httpMethod":"PUT","summary":"some summary","description":"some description","exampleRequests":[],"exampleResponses":[]}]}"""
      }


      "return 404 when backend returns 404" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(404, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe NOT_FOUND
      }

      "return 400 when backend returns 400" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(400, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe BAD_REQUEST
      }

    }

     "GET /integrations/find-with-filter" should {
        "return 200 and integration response from backend when using searchTerm" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(200, Json.toJson(IntegrationResponse(0, List.empty)).toString, searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe OK
          contentAsString(response) mustBe """{"count":0,"results":[]}"""
        }

        "return 200 and integration response from backend when using platformFilter" in new Setup {
        val platformFilter = "?platformFilter=CORE_IF"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(200, Json.toJson(IntegrationResponse(0, List.empty)).toString, platformFilter)

          val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
          status(response) mustBe OK
          contentAsString(response) mustBe """{"count":0,"results":[]}"""
        }

        "return 400 when using invalid platformFilter" in new Setup {
        val platformFilter = "?platformFilter=UNKNOWN"

          val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
          status(response) mustBe 400

        }

      "return 404 and when 404 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(404, "", searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe NOT_FOUND
          
        }

      "return 400 and when 400 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(400, "", searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe BAD_REQUEST
          
        }


     }

    "GET /services/integrations" should {


      "respond with 200 when api results returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(200, Json.toJson(IntegrationResponse(1, List(exampleApiDetail))).toString, "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe OK
        contentAsString(response) mustBe Json.toJson(IntegrationResponse(1, List(exampleApiDetail))).toString

      }

      "respond with 404 when no results returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(404, "", "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe NOT_FOUND
      }

      "respond with 400 when bad request returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(400, "", "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe BAD_REQUEST
       

      }
    }

    "DELETE /services/integrations/:publisherReference" should {

      "respond with 204 when deletion successful" in new Setup {
        primeIntegrationCatalogueServiceDelete(examplePublisherReference, 204)

        val response: Future[Result] = route(app, validDeleteApiRequest(examplePublisherReference)).get
        status(response) mustBe NO_CONTENT
      }

      "respond with 404 when deletion unsuccessful" in new Setup {
        primeIntegrationCatalogueServiceDelete(examplePublisherReference, 404)

        val response: Future[Result] = route(app, validDeleteApiRequest(examplePublisherReference)).get
        status(response) mustBe NOT_FOUND
        contentAsString(response) mustBe """{"errors":[{"message":"deleteByPublisherReference: The requested resource could not be found."}]}"""
      }
    }
  }
}
