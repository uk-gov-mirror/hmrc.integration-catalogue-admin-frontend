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
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{MultipartFormData, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, StubBodyParserFactory}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishError, PublishResult}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublishControllerSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with MockitoSugar with StubBodyParserFactory {
   private val fakeRequest = FakeRequest("POST", "/")
     .withHeaders("Content-Type" -> "multipart/form-data; boundary=------------------------26f1ee0a6a23ce9b")

   implicit lazy val mat: Materializer = app.materializer

   private val env           = Environment.simple()
   private val configuration = Configuration.load(env)

   private val serviceConfig = new ServicesConfig(configuration)
   private val appConfig     = new AppConfig(configuration, serviceConfig)
   private implicit val hc: HeaderCarrier = HeaderCarrier()


  implicit class MyWrappedResult(result:Future[Result]) extends Matchers {
    def shouldBeResult(expectedStatus:Int): Unit = {
      status(result) should be(expectedStatus)
    }
  }

  trait Setup {
    val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]

    val controller = new PublishController(appConfig, stubMessagesControllerComponents(), mockIntegrationCatalogueConnector, stubPlayBodyParsers(mat))

  }


  "POST /publish" should {
     "return 200 when valid payload is sent" in new Setup{

       val successResponse = PublishResult(true, List.empty)
       when(mockIntegrationCatalogueConnector.publish(*)(*)).thenReturn(Future.successful(successResponse))

       val tempFile = SingletonTemporaryFileCreator.create("text","txt")
       tempFile.deleteOnExit()

       val data = new MultipartFormData[TemporaryFile](Map(),
         List(FilePart("selectedFile", "text.txt", Some("text/plain"), tempFile)), List())
       val request =  FakeRequest.apply("POST", "integration-catalogue-admin-frontend/publish/api")
         .withBody(data)
       val result = controller.publishApi()(request)

       result shouldBeResult OK
       contentAsString(result) shouldBe "Publish Successful"
     }

    "return 200 when valid payload is sent but publish fails" in new Setup{

      val successResponse = PublishResult(false, List(PublishError(123, "some message")))
      when(mockIntegrationCatalogueConnector.publish(*)(*)).thenReturn(Future.successful(successResponse))

      val tempFile = SingletonTemporaryFileCreator.create("text","txt")
      tempFile.deleteOnExit()

      val data = new MultipartFormData[TemporaryFile](Map(),
        List(FilePart("selectedFile", "text.txt", Some("text/plain"), tempFile)), List())
      val request =  FakeRequest.apply("POST", "integration-catalogue-admin-frontend/publish/api")
        .withBody(data)
      val result = controller.publishApi()(request)

      result shouldBeResult OK
      contentAsString(result) shouldBe "Publish Failed"
    }

     "return 400 and not call connector when invalid file" in new Setup{

       val successResponse = PublishResult(true, List.empty)

       val tempFile = SingletonTemporaryFileCreator.create("text","txt")
       tempFile.deleteOnExit()

       val data = new MultipartFormData[TemporaryFile](Map(),
         List(FilePart("CANT FIND ME", "text3.txt", Some("text/plain"), tempFile)), List())
       val request =  FakeRequest.apply("POST", "integration-catalogue-admin-frontend/publish/api")
         .withBody(data)
       val result = controller.publishApi()(request)

       contentAsString(result) shouldBe "SOME ERROR"
       result shouldBeResult BAD_REQUEST

       verifyZeroInteractions(mockIntegrationCatalogueConnector)
     }
   }
}
