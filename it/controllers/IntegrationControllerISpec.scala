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
import support.{IntegrationCatalogueConnectorStub, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogue.models.IntegrationResponse
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData

import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models.IntegrationDetail
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import play.api.http.HeaderNames
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.HeaderKeys

class IntegrationControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with IntegrationCatalogueConnectorStub with ApiDetailTestData {

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

    private val encodedMasterAuthKey = "dGVzdC1hdXRoLWtleQ=="
    private val encodedCoreIfAuthKey = "c29tZUtleTM="
    val coreIfAuthHeader = List(HeaderNames.AUTHORIZATION -> encodedCoreIfAuthKey)
    val coreIfPlatformTypeHeader =  List(HeaderKeys.platformKey -> "CORE_IF")
    val masterKeyHeader = List(HeaderNames.AUTHORIZATION -> encodedMasterAuthKey)

    val exampleIntegrationId = "2840ce2d-03fa-46bb-84d9-0299402b7b32"
    val validGetApisRequest: FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, "/integration-catalogue-admin-frontend/services/integrations")

    def validFindByIntegrationIdRequest(id: IntegrationId): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, s"/integration-catalogue-admin-frontend/services/integrations/${id.value}")

    def validFindwithFilterRequest(searchTerm: String): FakeRequest[AnyContentAsEmpty.type] =
      FakeRequest(Helpers.GET, s"/integration-catalogue-admin-frontend/services/integrations$searchTerm")

    
    def validDeleteIntegrationRequest(integrationId: String): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-frontend/services/integrations/$integrationId")
        .withHeaders(masterKeyHeader : _*)
    }

    def invalidPathRequest(): FakeRequest[AnyContentAsEmpty.type] = {
      FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-frontend/services/iamanunknownpath")
    }
  }

  "IntegrationController" when {

    "DELETE [some unknown path]" should {
      "return blah" in new Setup {
         val response: Future[Result] = route(app, invalidPathRequest).get
         status(response) mustBe NOT_FOUND
         contentAsString(response) mustBe """{"errors":[{"message":"Path or Http method may be wrong. "}]}"""
      }
    }

    "GET /services/integrations/{id}" should  {

      "return 200 and integration detail from backend" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(OK, Json.toJson(exampleApiDetail.asInstanceOf[IntegrationDetail]).toString, exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe OK
        contentAsString(response) mustBe """{"_type":"uk.gov.hmrc.integrationcatalogue.models.ApiDetail","id":"e2e4ce48-29b0-11eb-adc1-0242ac120002","publisherReference":"API1689","title":"getKnownFactsName","description":"getKnownFactsDesc","platform":"CORE_IF","searchText":"Some Search Text","hods":["ETMP"],"lastUpdated":"2020-11-04T20:27:05.000+0000","maintainer":{"name":"IF Team","slackChannel":"N/A","contactInfo":[]},"version":"1.1.0","specificationType":"OAS_V3","endpoints":[{"path":"/some/url","methods":[{"httpMethod":"GET","operationId":"operationId","summary":"some summary","description":"some description","responses":[{"statusCode":200,"description":"response","schema":{"_type":"uk.gov.hmrc.integrationcatalogue.models.DefaultSchema","name":"agentReferenceNumber","type":"object","properties":[{"_type":"uk.gov.hmrc.integrationcatalogue.models.DefaultSchema","name":"agentReferenceNumber","type":"string","pattern":"^[A-Z](ARN)[0-9]{7}$","properties":[],"enum":[],"required":[]}],"enum":[],"required":[]},"mediaType":"application/json","examples":[{"name":"example response name","jsonBody":"example response body"}]}]}]},{"path":"/some/url","methods":[{"httpMethod":"PUT","operationId":"operationId2","summary":"some summary","description":"some description","request":{"description":"request","schema":{"_type":"uk.gov.hmrc.integrationcatalogue.models.DefaultSchema","name":"agentReferenceNumber","type":"string","pattern":"^[A-Z](ARN)[0-9]{7}$","properties":[],"enum":[],"required":[]},"mediaType":"application/json","examples":[{"name":"example request 1","jsonBody":"{\"someValue\": \"abcdefg\"}"}]},"responses":[]}]},{"path":"/some/url","methods":[]}],"schemas":[{"_type":"uk.gov.hmrc.integrationcatalogue.models.DefaultSchema","name":"agentReferenceNumber","type":"string","pattern":"^[A-Z](ARN)[0-9]{7}$","properties":[],"enum":[],"required":[]}]}"""
      }


      "return 404 when backend returns 404" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(NOT_FOUND, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe NOT_FOUND
        contentAsString(response) mustBe """{"errors":[{"message":"findByIntegrationId: The requested resource could not be found."}]}"""
      }

      "return 400 when backend returns 400" in new Setup {
       primeIntegrationCatalogueServiceGetByIdWithBody(BAD_REQUEST, "", exampleApiDetail.id)

        val response: Future[Result] = route(app, validFindByIntegrationIdRequest(exampleApiDetail.id)).get
        status(response) mustBe BAD_REQUEST
      }

    }

     "GET /integrations" should {
        "return 200 and integration response from backend when using searchTerm" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(0, List.empty)).toString, searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe OK
          contentAsString(response) mustBe """{"count":0,"results":[]}"""
        }

        "return 200 and integration response from backend when using platformFilter" in new Setup {
        val platformFilter = "?platformFilter=CORE_IF"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(0, List.empty)).toString, platformFilter)

          val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
          status(response) mustBe OK
          contentAsString(response) mustBe """{"count":0,"results":[]}"""
        }

        "return 400 when using invalid filter key" in new Setup {
          val invalidFilterKey = "?invalidFilterKey=UNKNOWN"

          val response: Future[Result] = route(app, validFindwithFilterRequest(invalidFilterKey)).get
          status(response) mustBe 400
          contentAsString(response) mustBe """{"errors":[{"message":"Invalid query parameter key provided. It is case sensitive"}]}"""
        }


      "return 400 when using invalid platformFilter" in new Setup {
        val platformFilter = "?platformFilter=UNKNOWN"

          val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
          status(response) mustBe 400
          contentAsString(response) mustBe """{"errors":[{"message":"Cannot accept UNKNOWN as PlatformType"}]}"""
        }

      "return 400 when using empty platformFilter value" in new Setup {
        val platformFilter = "?platformFilter="

          val response: Future[Result] = route(app, validFindwithFilterRequest(platformFilter)).get
          status(response) mustBe 400
          contentAsString(response) mustBe """{"errors":[{"message":"platformType cannot be empty"}]}"""
        }

      "return 500 and when 404 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(NOT_FOUND, "", searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe INTERNAL_SERVER_ERROR
          contentAsString(response) mustBe """{"errors":[{"message":"Unable to process your request"}]}"""

        }

      "return 500 and when 400 returned from backend" in new Setup {
        val searchTerm = "?searchTerm=API1689"
        primeIntegrationCatalogueServiceFindWithFilterWithBody(BAD_REQUEST, "", searchTerm)

          val response: Future[Result] = route(app, validFindwithFilterRequest(searchTerm)).get
          status(response) mustBe INTERNAL_SERVER_ERROR

        }

     }

    "GET /services/integrations" should {


      "respond with 200 when api results returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(OK, Json.toJson(IntegrationResponse(1, List(exampleApiDetail))).toString, "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe OK
        contentAsString(response) mustBe Json.toJson(IntegrationResponse(1, List(exampleApiDetail))).toString

      }

      "respond with 500 when 404 returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(NOT_FOUND, "", "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe INTERNAL_SERVER_ERROR
        contentAsString(response) mustBe """{"errors":[{"message":"Unable to process your request"}]}"""
      }

      "respond with 500 when bad request returned from backend" in new Setup {
        primeIntegrationCatalogueServiceFindWithFilterWithBody(BAD_REQUEST, "", "")

        val response: Future[Result] = route(app, validGetApisRequest).get
        status(response) mustBe INTERNAL_SERVER_ERROR
        contentAsString(response) mustBe """{"errors":[{"message":"Unable to process your request"}]}"""

      }
    }

    "DELETE /services/integrations/:integrationId" should {

      "respond with 204 when deletion successful" in new Setup {
        primeIntegrationCatalogueServiceDelete(exampleIntegrationId, NO_CONTENT)

        val response: Future[Result] = route(app, validDeleteIntegrationRequest(exampleIntegrationId)).get
        status(response) mustBe NO_CONTENT
      }

     "respond with 400 when non uuid id provided" in new Setup {

        val request = FakeRequest(Helpers.DELETE, s"/integration-catalogue-admin-frontend/services/integrations/invalidId")

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"Cannot accept invalidId as IntegrationId"}]}"""
      }

      "respond with 404 when deletion unsuccessful" in new Setup {
        primeIntegrationCatalogueServiceDelete(exampleIntegrationId, NOT_FOUND)

        val response: Future[Result] = route(app, validDeleteIntegrationRequest(exampleIntegrationId)).get
        status(response) mustBe NOT_FOUND
        contentAsString(response) mustBe """{"errors":[{"message":"deleteByIntegrationId: The requested resource could not be found."}]}"""
      }

      "respond with 401 when no auth header but platform type header is present" in new Setup {
        primeIntegrationCatalogueServiceDelete(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader =
          FakeRequest(Helpers.DELETE,s"/integration-catalogue-admin-frontend/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfPlatformTypeHeader : _*)).get
        status(response) mustBe UNAUTHORIZED

        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""
      }

      "respond with 400 when auth header present but platform type header is missing" in new Setup {
        primeIntegrationCatalogueServiceDelete(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader =
          FakeRequest(Helpers.DELETE,s"/integration-catalogue-admin-frontend/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader : _*)).get
        status(response) mustBe BAD_REQUEST

        contentAsString(response) mustBe """{"errors":[{"message":"Platform header is missing or invalid"}]}"""
      }

      "respond with 400 when auth header present but platform type header is invalid" in new Setup {
        primeIntegrationCatalogueServiceDelete(exampleIntegrationId, NOT_FOUND)

        val requestWithNoAuthHeader =
          FakeRequest(Helpers.DELETE,s"/integration-catalogue-admin-frontend/services/integrations/$exampleIntegrationId")

        val response: Future[Result] = route(app, requestWithNoAuthHeader.withHeaders(coreIfAuthHeader ++ List(HeaderKeys.platformKey -> "INVALID_PLATFORM"): _*)).get
        status(response) mustBe BAD_REQUEST

        contentAsString(response) mustBe """{"errors":[{"message":"Platform header is missing or invalid"}]}"""
      }
    }
  }
}
