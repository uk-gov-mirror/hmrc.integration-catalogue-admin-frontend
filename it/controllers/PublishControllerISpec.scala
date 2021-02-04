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

import org.apache.commons.io.IOUtils
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{BAD_REQUEST, _}
import support.{IntegrationCatalogueService, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.{HeaderKeys, IntegrationId, PlatformType}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.JsonFormatters._
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishDetails, PublishError, PublishResult}
import utils.MultipartFormDataWritable

import java.io.{FileOutputStream, InputStream}
import java.util.UUID

class PublishControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with IntegrationCatalogueService {

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
    val validHeaders = List(CONTENT_TYPE -> "application/json")
    val allValidHeaders = List(HeaderKeys.platformKey -> "CORE_IF", HeaderKeys.publisherRefKey -> "1234", HeaderKeys.specificationTypeKey -> "OAS_V3")
    val headers = Headers(HeaderKeys.platformKey -> "CORE_IF", HeaderKeys.publisherRefKey -> "1234", HeaderKeys.specificationTypeKey -> "OAS_V3")
    implicit val writer = MultipartFormDataWritable.writeable


    val filePart =
    new MultipartFormData.FilePart[TemporaryFile](
      key = "selectedFile",
      filename = "text-to-upload.txt",
      None,
      ref = CreateTempFileFromResource("/text-to-upload.txt"))

    val multipartBody = MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(filePart), badParts = Nil)
    
    val validPublishRequest = FakeRequest(Helpers.POST, "/integration-catalogue-admin-frontend/services/apis/publish", headers, multipartBody)

    val invalidFilePart =
    new MultipartFormData.FilePart[TemporaryFile](
      key = "selectedFile",
      filename = "empty.txt",
      None,
      ref = CreateTempFileFromResource("/empty.txt"))

    val invalidMultipartBody = MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(invalidFilePart), badParts = Nil)
    
    val invalidPublishRequest = FakeRequest(Helpers.POST, "/integration-catalogue-admin-frontend/services/apis/publish", headers, invalidMultipartBody)

    def callPostEndpoint(url: String, body: String, headers: List[(String, String)]): WSResponse =
      wsClient
        .url(url)
        .withHttpHeaders(headers: _*)
        .withFollowRedirects(false)
        .post(body)
        .futureValue

    def createBackendResponse(isSuccess: Boolean,  isUpdate: Boolean) = {
        val publishDetails = if(isSuccess) Some(PublishDetails(isUpdate, IntegrationId(UUID.randomUUID()), "", PlatformType.CORE_IF)) else None
        val publishErrors = if(isSuccess) List.empty else List(PublishError(10000, "Some Error Message"))
        PublishResult(isSuccess, publishDetails, publishErrors)
    }

    def CreateTempFileFromResource(path: String) ={
      val testResource: InputStream = getClass.getResourceAsStream(path) 
      val tempFile = SingletonTemporaryFileCreator.create("file", "tmp")
      IOUtils.copy(testResource, new FileOutputStream(tempFile))
      tempFile
    }  
  }


  "PublishController" when {

    "POST /api/publish" should {

      "respond with 201 when valid request and a create" in new Setup{
      
        val backendResponse = createBackendResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServiceWithBody(200, Json.toJson(backendResponse).toString)
        
        val response  = route(app, validPublishRequest).get
        status(response) mustBe 201
  
      }

      "respond with 200 when valid request and a create but returns isSuccess = false" in new Setup{
      
        val backendResponse = createBackendResponse(isSuccess = false, isUpdate = false)
        primeIntegrationCatalogueServiceWithBody(200, Json.toJson(backendResponse).toString)
        
        val response  = route(app, validPublishRequest).get
        status(response) mustBe 200
        contentAsString(response) mustBe """{"isSuccess":false,"errors":[{"code":10000,"message":"Some Error Message"}]}"""
  
      }

      "respond with 200 when valid request and an update" in new Setup{
      
        val backendResponse = createBackendResponse(isSuccess = true, isUpdate = true)
        primeIntegrationCatalogueServiceWithBody(200, Json.toJson(backendResponse).toString)
     
        val response  = route(app, validPublishRequest).get
        status(response) mustBe 200
  
      }

      "respond with 400 from BodyParser when invalid body is sent" in new Setup {

        val backendResponse = createBackendResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServiceWithBody(200, Json.toJson(backendResponse).toString)
     
        
        val response  = route(app, invalidPublishRequest).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"code":"BAD_REQUEST","message":"selectedFile is missing from requestBody"}"""
      }

       "respond with 400 when invalid platform header" in new Setup {
         val invalidHeaders = Headers(HeaderKeys.platformKey -> "SOME_RUBBISH",
           HeaderKeys.specificationTypeKey -> "OAS_V3",
           HeaderKeys.publisherRefKey -> "123456")
          val request = validPublishRequest.withHeaders(invalidHeaders)
         
          val response  = route(app, request).get
          status(response) mustBe BAD_REQUEST
          contentAsString(response) mustBe """{"code":"BAD_REQUEST","message":"platform header is missing or invalid"}"""
         
       }

      "respond with 400 when invalid specification type header" in new Setup {

        val invalidHeaders = Headers(HeaderKeys.platformKey -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
          HeaderKeys.publisherRefKey -> "123456")
        val request = validPublishRequest.withHeaders(invalidHeaders)
         
        val response  = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"code":"BAD_REQUEST","message":"specification type header is missing or invalid"}"""
         

      }

      "respond with 400 when invalid publisher ref header" in new Setup {

        val invalidHeaders = Headers(HeaderKeys.platformKey -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey -> "")
         val request = validPublishRequest.withHeaders(invalidHeaders)
         
        val response  = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"code":"BAD_REQUEST","message":"publisher reference header is missing or invalid"}"""

      }

    }
  }
}
