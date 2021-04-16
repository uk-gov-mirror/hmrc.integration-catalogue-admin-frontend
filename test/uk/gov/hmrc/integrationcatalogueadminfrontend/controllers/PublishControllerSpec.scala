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
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.JsObject
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubBodyParserFactory}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.controllers.actionbuilders._

import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._

import uk.gov.hmrc.integrationcatalogueadminfrontend.services.PublishService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.libs.json.Json
import uk.gov.hmrc.integrationcatalogueadminfrontend.models.HeaderKeys
import play.api.http.HeaderNames

class PublishControllerSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with StubBodyParserFactory {

  implicit lazy val mat: Materializer = app.materializer

  private val env = Environment.simple()
  private val configuration = Configuration.load(env)

  private val serviceConfig = new ServicesConfig(configuration)
  private val appConfig = new AppConfig(configuration, serviceConfig)

  private val headerValidator = app.injector.instanceOf[ValidateApiPublishRequestAction]
  private val authAction = app.injector.instanceOf[ValidateAuthorizationHeaderAction]
  private val encodedAuthHeader = "dGVzdC1hdXRoLWtleQ=="

  private val publisherReference = "123456"
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val validHeaders = Seq(
    HeaderKeys.platformKey -> PlatformType.CORE_IF.toString,
    HeaderKeys.specificationTypeKey -> "OAS_V3",
    HeaderKeys.publisherRefKey -> publisherReference,
    HeaderNames.AUTHORIZATION -> encodedAuthHeader
  )

  implicit class MyWrappedResult(result: Future[Result]) extends Matchers {

    def shouldBeResult(expectedStatus: Int): Unit = {
      status(result) should be(expectedStatus)
    }
  }

  trait Setup {
    val mockPublishService: PublishService = mock[PublishService]

    val controller = new PublishController(
      appConfig,
      stubMessagesControllerComponents(),
      mockPublishService,
      headerValidator,
      authAction,
      stubPlayBodyParsers(mat)
    )

    def callPublish(expectedConnectorResponse: Option[PublishResult], headers: Seq[(String, String)], filePartKey: String, fileName: String) = {
      expectedConnectorResponse.map(response => when(mockPublishService.publishApi(*, *, *, *)(*)).thenReturn(Future.successful(Right(response))))
      val tempFile = SingletonTemporaryFileCreator.create("text", "txt")
      tempFile.deleteOnExit()

      val data = new MultipartFormData[TemporaryFile](Map(), List(FilePart(filePartKey, fileName, Some("text/plain"), tempFile)), List())
      val publishRequest = FakeRequest.apply("PUT", "integration-catalogue-admin-frontend/publish/api")
        .withHeaders(headers: _*)
        .withBody(data)

      controller.publishApi()(publishRequest)
    }

    def callPublishReturnError(headers: Seq[(String, String)], filePartKey: String, fileName: String) = {
      when(mockPublishService.publishApi(*, *, *, *)(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))

      val tempFile = SingletonTemporaryFileCreator.create("text", "txt")
      tempFile.deleteOnExit()

      val data = new MultipartFormData[TemporaryFile](Map(), List(FilePart(filePartKey, fileName, Some("text/plain"), tempFile)), List())
      val publishRequest = FakeRequest.apply("PUT", "integration-catalogue-admin-frontend/publish/api")
        .withHeaders(headers: _*)
        .withBody(data)

      controller.publishApi()(publishRequest)

    }

  }

  "POST /publish" should {

    "return 201 when valid payload is sent" in new Setup {

      val id = UUID.randomUUID()
      val result: Future[Result] = callPublish(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(false, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        "selectedFile",
        "text.txt"
      )

      result shouldBeResult CREATED
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 200 when valid payload is sent" in new Setup {

      val id = UUID.randomUUID()
      val result: Future[Result] = callPublish(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders,
        "selectedFile",
        "text.txt"
      )

      result shouldBeResult OK
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 400 when connector response has no details or error" in new Setup {

      val result: Future[Result] = callPublish(Some(PublishResult(isSuccess = true, None, List.empty)), validHeaders, "selectedFile", "text.txt")

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"Unexpected response from /integration-catalogue"}]}"""
    }

    "return 400 when connector returns a Left" in new Setup {

      val result: Future[Result] = callPublishReturnError(validHeaders, "selectedFile", "text.txt")

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"Unexpected response from /integration-catalogue: some error"}]}"""
    }

    "return 400 when valid payload is sent but publish fails" in new Setup {
      val result: Future[Result] = callPublish(Some(PublishResult(isSuccess = false, None, List(PublishError(123, "some message")))), validHeaders, "selectedFile", "text.txt")

      result shouldBeResult BAD_REQUEST
      contentAsString(result) shouldBe """{"errors":[{"message":"some message"}]}"""
    }

    "return 400 and not call connector when invalid file" in new Setup {

      val result: Future[Result] = callPublish(None, validHeaders, "CANT FIND ME", "text3.txt")

      contentAsString(result) shouldBe """{"errors":[{"message":"selectedFile is missing from requestBody"}]}"""
      result shouldBeResult BAD_REQUEST

      verifyZeroInteractions(mockPublishService)
    }

    "return 400 when plaform not set in header" in new Setup {
      val result: Future[Result] = callPublish(None, validHeaders.filterNot(_._1.equals(HeaderKeys.platformKey)), "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("platform type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when platform is invalid in header" in new Setup {
      val headers = Seq(
        HeaderKeys.platformKey -> "SOME_RUBBISH",
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey -> "123456",
        HeaderNames.AUTHORIZATION -> encodedAuthHeader
      )
      val result: Future[Result] = callPublish(None, headers, "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("platform type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when specType not set in header" in new Setup {
      val result: Future[Result] = callPublish(None, validHeaders.filterNot(_._1.equals(HeaderKeys.specificationTypeKey)), "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("specification type header is missing or invalid"))))
      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 400 when specType is invalid in header" in new Setup {
      val headers = Seq(
        HeaderKeys.platformKey -> "CORE_IF",
        HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
        HeaderKeys.publisherRefKey -> "123456",
        HeaderNames.AUTHORIZATION -> encodedAuthHeader
      )
      val result: Future[Result] = callPublish(None, headers, "selectedFile", "text.txt")

      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("specification type header is missing or invalid"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 200 when publisherRef not set in header" in new Setup {

      val id = UUID.randomUUID()
      val result: Future[Result] = callPublish(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        validHeaders.filterNot(_._1.equals(HeaderKeys.publisherRefKey)),
        "selectedFile",
        "text.txt"
      )

      status(result) shouldBe OK

      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 200 when publisherRef is invalid in header" in new Setup {
      val invalidHeaders = Seq(
        HeaderKeys.platformKey -> "CORE_IF",
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey -> "",
        HeaderNames.AUTHORIZATION -> encodedAuthHeader
      )

     val id = UUID.randomUUID()
      val result: Future[Result] = callPublish(
        Some(PublishResult(isSuccess = true, Some(PublishDetails(true, IntegrationId(id), publisherReference, PlatformType.CORE_IF)), List.empty)),
        invalidHeaders,
        "selectedFile",
        "text.txt"
      )

      status(result) shouldBe OK
      contentAsString(result) shouldBe raw"""{"id":"$id","publisherReference":"123456","platformType":"CORE_IF"}"""
    }

    "return 401 when Authorization not set in header" in new Setup {
      val result: Future[Result] = callPublish(None, validHeaders.filterNot(_._1.equals(HeaderNames.AUTHORIZATION)), "selectedFile", "text.txt")

      status(result) shouldBe UNAUTHORIZED

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("Authorisation failed"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }

    "return 401 when Authorization is invalid in header" in new Setup {
      val invalidHeaders = Seq(
        HeaderKeys.platformKey -> PlatformType.CORE_IF.toString,
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey -> publisherReference,
        HeaderNames.AUTHORIZATION -> "SOME_RUBBISH"
      )

      val result: Future[Result] = callPublish(None, invalidHeaders, "selectedFile", "text.txt")

      status(result) shouldBe UNAUTHORIZED

      val jsErrorResponse: JsObject = Json.toJsObject(ErrorResponse(List(ErrorResponseMessage("Authorisation failed"))))

      contentAsJson(result) shouldBe jsErrorResponse
    }
  }
}
