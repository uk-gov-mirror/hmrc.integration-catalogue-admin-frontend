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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
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
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishDetails, PublishError, PublishResult}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain._
import uk.gov.hmrc.integrationcatalogueadminfrontend.services.PublishService
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import java.util.UUID
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublishControllerSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with MockitoSugar with StubBodyParserFactory {

   implicit lazy val mat: Materializer = app.materializer

   private val env           = Environment.simple()
   private val configuration = Configuration.load(env)

   private val serviceConfig = new ServicesConfig(configuration)
   private val appConfig     = new AppConfig(configuration, serviceConfig)
   private val platformHeaderValidator = app.injector.instanceOf[ValidatePlatformHeaderAction]
    private val specificationTypeHeaderValidator = app.injector.instanceOf[ValidateSpecificationTypeHeaderAction]
     private val publisherRefHeaderValidator = app.injector.instanceOf[ValidatePublisherRefHeaderAction]

  private val publisherReference = "123456"
   private implicit val hc: HeaderCarrier = HeaderCarrier()
    val validHeaders = Seq(HeaderKeys.platformKey -> PlatformType.CORE_IF.toString,
     HeaderKeys.specificationTypeKey -> "OAS_V3",
     HeaderKeys.publisherRefKey -> publisherReference)

  implicit class MyWrappedResult(result:Future[Result]) extends Matchers {
    def shouldBeResult(expectedStatus:Int): Unit = {
      status(result) should be(expectedStatus)
    }
  }

  trait Setup {
    val mockPublishService: PublishService = mock[PublishService]

    val controller = new PublishController(appConfig,
        stubMessagesControllerComponents(),
        mockPublishService,
        platformHeaderValidator,
        specificationTypeHeaderValidator,
        publisherRefHeaderValidator,
        stubPlayBodyParsers(mat),
      )

    def callPublish(expectedConnectorResponse: Option[PublishResult],
              headers: Seq[(String, String)],
              filePartKey: String,
              fileName: String): Future[Result] = {
     expectedConnectorResponse.map(response =>
        when(mockPublishService.publishApi(*, *, *, *, *)(*)).thenReturn(Future.successful(response))
     )

      val tempFile = SingletonTemporaryFileCreator.create("text","txt")
      tempFile.deleteOnExit()

      val data = new MultipartFormData[TemporaryFile](Map(),
        List(FilePart(filePartKey, fileName, Some("text/plain"), tempFile)), List())

          val publishRequest =  FakeRequest.apply("PUT", "integration-catalogue-admin-frontend/publish/api")
        .withHeaders(headers: _*)
        .withBody(data)

      controller.publishApi()(publishRequest)
  }

  }


  "POST /publish" should {

     "return 200 when valid payload is sent" in new Setup{

       val id = UUID.randomUUID()
       val result: Future[Result] =  callPublish(Some(PublishResult(isSuccess = true,
           Some(PublishDetails(IntegrationId(id), publisherReference, PlatformType.CORE_IF)),
         List.empty)), validHeaders, "selectedFile", "text.txt")

       result shouldBeResult OK
       contentAsString(result) shouldBe raw"""{"isSuccess":true,"publishDetails":{"integrationId":"$id","publisherReference":"123456","platformType":"CORE_IF"},"errors":[]}"""
     }

    "return 200 when valid payload is sent but publish fails" in new Setup{
     val result: Future[Result] =  callPublish(Some(PublishResult(isSuccess = false, None, List(PublishError(123, "some message")))),
      validHeaders, "selectedFile", "text.txt")


      result shouldBeResult OK
      contentAsString(result) shouldBe "{\"isSuccess\":false,\"errors\":[{\"code\":123,\"message\":\"some message\"}]}"
    }

     "return 400 and not call connector when invalid file" in new Setup{


      val result: Future[Result] =  callPublish(None, validHeaders, "CANT FIND ME", "text3.txt")


       contentAsString(result) shouldBe "{\"code\":\"BAD_REQUEST\",\"message\":\"Unable to retrieve published file contents\"}"
       result shouldBeResult BAD_REQUEST

       verifyZeroInteractions(mockPublishService)
     }

     "return 400 when plaform not set in header" in new Setup {
      val result: Future[Result] =  callPublish(None, validHeaders.filterNot(_._1.equals(HeaderKeys.platformKey)), "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "platform Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }


     "return 400 when platform is invalid in header" in new Setup {
           val headers = Seq(HeaderKeys.platformKey -> "SOME_RUBBISH",
                              HeaderKeys.specificationTypeKey -> "OAS_V3",
                              HeaderKeys.publisherRefKey -> "123456")
      val result: Future[Result] =  callPublish(None, headers, "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "platform Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }


     "return 400 when specType not set in header" in new Setup {
      val result: Future[Result] =  callPublish(None, validHeaders.filterNot(_._1.equals(HeaderKeys.specificationTypeKey)), "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "specification type Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }


     "return 400 when specType is invalid in header" in new Setup {
           val headers = Seq(HeaderKeys.platformKey -> "CORE_IF",
                              HeaderKeys.specificationTypeKey -> "SOME_RUBBISH",
                              HeaderKeys.publisherRefKey -> "123456")
      val result: Future[Result] =  callPublish(None, headers, "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST


      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "specification type Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }



     "return 400 when publisherRef not set in header" in new Setup {
      val result: Future[Result] =  callPublish(None, validHeaders.filterNot(_._1.equals(HeaderKeys.publisherRefKey)), "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST


      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "publisher reference Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }


     "return 400 when publisherRef is invalid in header" in new Setup {
      val invalidHeaders = Seq(HeaderKeys.platformKey -> "CORE_IF",
        HeaderKeys.specificationTypeKey -> "OAS_V3",
        HeaderKeys.publisherRefKey -> "")
      val result: Future[Result] =  callPublish(None, invalidHeaders, "selectedFile", "text.txt")
      
      status(result) shouldBe BAD_REQUEST

      val jsErrorResponse: JsObject =  JsErrorResponse(ErrorCode.BAD_REQUEST, "publisher reference Header is missing or invalid")
      contentAsJson(result) shouldBe jsErrorResponse
    }

   }
}
