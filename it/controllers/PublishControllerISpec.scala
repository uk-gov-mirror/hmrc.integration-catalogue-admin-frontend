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
import play.api.http.Writeable
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.{BAD_REQUEST, _}
import support.{IntegrationCatalogueConnectorStub, ServerBaseISpec}
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.HeaderKeys
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import utils.MultipartFormDataWritable

import java.io.{FileOutputStream, InputStream}
import java.util.UUID
import scala.concurrent.Future
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.JsValue
import play.api.http.HeaderNames
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig

class PublishControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with IntegrationCatalogueConnectorStub {

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

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig = new AppConfig(configuration, serviceConfig)
  private val encodedAuthHeader = "dGVzdC1hdXRoLWtleQ=="


  val wsClient: WSClient = app.injector.instanceOf[WSClient]


  trait Setup {
    val validHeaders = List(CONTENT_TYPE -> "application/json")
    val allValidHeaders = List(
      HeaderKeys.platformKey -> "CORE_IF", 
      HeaderKeys.publisherRefKey -> "1234", 
      HeaderKeys.specificationTypeKey -> "OAS_V3",
      HeaderNames.AUTHORIZATION -> encodedAuthHeader
      )
    val headers: Headers = Headers(
      HeaderKeys.platformKey -> "CORE_IF", 
      HeaderKeys.publisherRefKey -> "1234", 
      HeaderKeys.specificationTypeKey -> "OAS_V3", 
      HeaderNames.AUTHORIZATION -> encodedAuthHeader
      )
    implicit val writer: Writeable[MultipartFormData[TemporaryFile]] = MultipartFormDataWritable.writeable

    val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

    val fileTransferPublishRequestObj = FileTransferPublishRequest(
      fileTransferSpecificationVersion = "1.0",
      publisherReference = "BVD-DPS-PCPMonthly-pull",
      title = "BVD-DPS-PCPMonthly-pull",
      description = "A file transfer from Birth Verification Data (BVD) to Data Provisioning Systems (DPS)",
      platformType = PlatformType.CORE_IF,
      lastUpdated =  dateValue,
      contact = ContactInformation("Core IF Team", "example@gmail.com"),
      sourceSystem = List("BVD"),
      targetSystem = List("DPS"),
      fileTransferPattern = "Corporate to corporate"
    )
    val filePart =
    new MultipartFormData.FilePart[TemporaryFile](
      key = "selectedFile",
      filename = "text-to-upload.txt",
      None,
      ref = createTempFileFromResource("/text-to-upload.txt"))

    val multipartBody: MultipartFormData[TemporaryFile] = MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(filePart), badParts = Nil)

    val validApiPublishRequest: FakeRequest[MultipartFormData[TemporaryFile]] = FakeRequest(Helpers.PUT, "/integration-catalogue-admin-frontend/services/apis/publish", headers, multipartBody)
    
    val validFileTransferPublishRequest: FakeRequest[JsValue] = FakeRequest(Helpers.PUT, "/integration-catalogue-admin-frontend/services/filetransfers/publish", headers, Json.toJson(fileTransferPublishRequestObj))

     
    val invalidFileTransferPublishRequest: FakeRequest[JsValue] = FakeRequest(Helpers.PUT, "/integration-catalogue-admin-frontend/services/filetransfers/publish", headers, Json.toJson("{}"))


    val invalidFilePart =
    new MultipartFormData.FilePart[TemporaryFile](
      key = "selectedFile",
      filename = "empty.txt",
      None,
      ref = createTempFileFromResource("/empty.txt"))

    val invalidMultipartBody: MultipartFormData[TemporaryFile] =
      MultipartFormData[TemporaryFile](dataParts = Map.empty, files = Seq(invalidFilePart), badParts = Nil)

    val invalidPublishRequest: FakeRequest[MultipartFormData[TemporaryFile]] =
      FakeRequest(Helpers.PUT, "/integration-catalogue-admin-frontend/services/apis/publish", headers, invalidMultipartBody)


    def createBackendPublishResponse(isSuccess: Boolean, isUpdate: Boolean): PublishResult = {
        val publishDetails = if(isSuccess) Some(PublishDetails(isUpdate, IntegrationId(UUID.randomUUID()), "", PlatformType.CORE_IF)) else None
        val publishErrors = if(isSuccess) List.empty else List(PublishError(10000, "Some Error Message"))
        PublishResult(isSuccess, publishDetails, publishErrors)
    }

    def createTempFileFromResource(path: String): TemporaryFile ={
      val testResource: InputStream = getClass.getResourceAsStream(path)
      val tempFile = SingletonTemporaryFileCreator.create("file", "tmp")
      IOUtils.copy(testResource, new FileOutputStream(tempFile))
      tempFile
    }
  }


  "PublishController" when {

    "PUT /services/api/publish" should {

      "respond with 201 when valid request and a create" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/apis/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validApiPublishRequest).get
        status(response) mustBe 201
        // check body
      }

      "respond with 400 and list of errors when backend returns isSuccess is false" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = false, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/apis/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validApiPublishRequest).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"Some Error Message"}]}"""

      }

      "respond with 200 when valid request and an update" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = true)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/apis/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validApiPublishRequest).get
        status(response) mustBe 200

      }

      "respond with 400 from BodyParser when invalid body is sent" in new Setup {

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/apis/publish", 200, Json.toJson(backendResponse).toString)


        val response: Future[Result] = route(app, invalidPublishRequest).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"selectedFile is missing from requestBody"}]}"""
      }

       "respond with 400 when invalid platform header" in new Setup {
         val invalidHeaders: Headers = Headers(
           HeaderKeys.platformKey -> "SOME_RUBBISH",
           HeaderKeys.specificationTypeKey -> "OAS_V3",
           HeaderKeys.publisherRefKey -> "123456",
           HeaderNames.AUTHORIZATION -> encodedAuthHeader)
          val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

          val response: Future[Result] = route(app, request).get
          status(response) mustBe BAD_REQUEST
          contentAsString(response) mustBe """{"errors":[{"message":"platform header is missing or invalid"}]}"""

       }

      "respond with 400 when invalid specification type header" in new Setup {

        val invalidHeaders: Headers = Headers(
          HeaderKeys.platformKey -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
          HeaderKeys.publisherRefKey -> "123456",
          HeaderNames.AUTHORIZATION -> encodedAuthHeader)
        val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"specification type header is missing or invalid"}]}"""


      }

      "respond with 400 when invalid publisher ref header" in new Setup {

        val invalidHeaders: Headers = Headers(
          HeaderKeys.platformKey -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey -> "",
          HeaderNames.AUTHORIZATION -> encodedAuthHeader)
         val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe BAD_REQUEST
        contentAsString(response) mustBe """{"errors":[{"message":"publisher reference header is missing or invalid"}]}"""

      }

      "respond with 403 when invalid Authorization header" in new Setup {

        val invalidHeaders: Headers = Headers(
          HeaderKeys.platformKey -> "CORE_IF",
          HeaderKeys.specificationTypeKey -> "OAS_V3",
          HeaderKeys.publisherRefKey -> "123456",
          HeaderNames.AUTHORIZATION -> "SOME_RUBBISH")
         val request: FakeRequest[MultipartFormData[TemporaryFile]] = validApiPublishRequest.withHeaders(invalidHeaders)

        val response: Future[Result] = route(app, request).get
        status(response) mustBe FORBIDDEN
        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""

      }

    }

    "PUT /services/filetransfer/publish" should {

      "respond with 201 when valid request and a create" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/filetransfer/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validFileTransferPublishRequest).get
        status(response) mustBe 201
        // check body
      }

      "respond with 400 and list of errors when backend returns isSuccess is false" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = false, isUpdate = false)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/filetransfer/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validFileTransferPublishRequest).get
        status(response) mustBe 400
        contentAsString(response) mustBe """{"errors":[{"message":"Some Error Message"}]}"""

      }

      "respond with 200 when valid request and an update" in new Setup{

        val backendResponse: PublishResult = createBackendPublishResponse(isSuccess = true, isUpdate = true)
        primeIntegrationCatalogueServicePutWithBody("/integration-catalogue/filetransfer/publish", 200, Json.toJson(backendResponse).toString)

        val response: Future[Result] = route(app, validFileTransferPublishRequest).get
        status(response) mustBe 200

      }

       "respond with 400 when invalid json is sent" in new Setup{

        val response: Future[Result] = route(app, invalidFileTransferPublishRequest).get
        status(response) mustBe 400

      }

      "respond with 403 and error message Authorization header is missing" in new Setup{
        val response: Future[Result] = route(app, validFileTransferPublishRequest.withHeaders(Headers())).get
        status(response) mustBe 403
        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""

      }

      "respond with 403 and error message Authorization header is invalid" in new Setup{
        val invalidHeaders: Headers = Headers(HeaderNames.AUTHORIZATION -> "SOME_RUBBISH")
        val response: Future[Result] = route(app, validFileTransferPublishRequest.withHeaders(invalidHeaders)).get
        status(response) mustBe 403
        contentAsString(response) mustBe """{"errors":[{"message":"Authorisation failed"}]}"""

      }
    }

  }
}
