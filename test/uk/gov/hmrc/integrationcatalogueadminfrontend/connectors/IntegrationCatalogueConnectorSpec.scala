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

package uk.gov.hmrc.integrationcatalogueadminfrontend.connectors

import org.mockito.captor.{ArgCaptor, Captor}
import org.mockito.scalatest.MockitoSugar

import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.libs.json.Writes
import play.api.test.Helpers
import uk.gov.hmrc.http.{BadGatewayException, HttpClient, _}
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogue.models.common._

import java.util.UUID
import org.scalatest.WordSpec
import org.scalatest.Matchers
import uk.gov.hmrc.integrationcatalogueadminfrontend.AwaitTestSupport
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData
import play.api.test.Helpers._
import uk.gov.hmrc.integrationcatalogue.models.JsonFormatters._

class IntegrationCatalogueConnectorSpec extends WordSpec with Matchers with OptionValues
  with MockitoSugar with BeforeAndAfterEach with AwaitTestSupport with ApiDetailTestData {

  private val mockHttpClient = mock[HttpClient]
  private val mockAppConfig = mock[AppConfig]
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
  }

  trait SetUp {
    val headerCarrierCaptor: Captor[HeaderCarrier] = ArgCaptor[HeaderCarrier]

    val connector = new IntegrationCatalogueConnector(
      mockHttpClient,
      mockAppConfig)

    val examplePublisherReference = "example-publisher-reference"
    val searchTerm  = "API1689" 
    val outboundUrl = "/integration-catalogue/apis/publish"
    val findWithFilterlUrl = s"/integration-catalogue/integrations/find-with-filter"
    val deleteApiUrl = s"/integration-catalogue/integrations/$examplePublisherReference"

    def httpCallToPublishWillSucceedWithResponse(response: PublishResult) =
      when(mockHttpClient.PUT[ApiPublishRequest, PublishResult]
        (eqTo(outboundUrl), any[ApiPublishRequest], any[Seq[(String, String)]])
        (any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

    def httpCallToPublishWillFailWithException(exception: Throwable) =
      when(mockHttpClient.PUT[ApiPublishRequest, PublishResult]
        (eqTo(outboundUrl), any[ApiPublishRequest], any[Seq[(String, String)]])
        (any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

    def httpCallToFindWithFilterWillSucceedWithResponse(response: IntegrationResponse) =
      when(mockHttpClient.GET[IntegrationResponse]
        (eqTo(findWithFilterlUrl), eqTo(Seq(("searchTerm",searchTerm))))
        (any[HttpReads[IntegrationResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))
    
    def httpCallToFindWithFilterWillFailWithException(exception: Throwable) =
           when(mockHttpClient.GET[IntegrationResponse]
        (eqTo(findWithFilterlUrl), eqTo(Seq(("searchTerm",searchTerm))))
        (any[HttpReads[IntegrationResponse]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

    def httpCallToDeleteApiWillSucceed(response: HttpResponse) =
      when(mockHttpClient.DELETE[HttpResponse](eqTo(deleteApiUrl), *)(*, *, *)).thenReturn(Future.successful(response))


    def httpCallToDeleteApiWillFailWithNotFound(exception: Throwable) =
      when(mockHttpClient.DELETE[HttpResponse](eqTo(deleteApiUrl), *)(*, *, *)).thenReturn(Future.failed(exception))
  }

  "IntegrationCatalogueConnector send" should {

      val request: ApiPublishRequest = ApiPublishRequest("publisherRef", PlatformType.CORE_IF, SpecificationType.OAS_V3, "{}")

    "return successful result" in new SetUp {
      httpCallToPublishWillSucceedWithResponse(PublishResult(isSuccess = true, Some(PublishDetails(true, IntegrationId(UUID.randomUUID()),  request.publisherReference, request.platformType)), List.empty))


      val result = await(connector.publishApis(request))

      result match {
        case Left(_) => fail()
        case Right(publishResult: PublishResult) => publishResult.isSuccess shouldBe true
      }

      verify(mockHttpClient).PUT(eqTo(outboundUrl), eqTo(request),
        any[Seq[(String, String)]])(any[Writes[ApiPublishRequest]], any[HttpReads[PublishResult]], headerCarrierCaptor.capture, any[ExecutionContext])

    }

    "handle exceptions" in new SetUp {
      httpCallToPublishWillFailWithException(new BadGatewayException("some error"))

      val result = await(connector.publishApis(request))

      result match {
        case Right(_) => fail()
        case Left(_:BadGatewayException) => succeed
      }

    }

  }

  "findWithFilter" should {
    "return Right when successful" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2)
      httpCallToFindWithFilterWillSucceedWithResponse(IntegrationResponse(2, expectedResult))

      val result = await(connector.findWithFilters(List(searchTerm), List.empty))

      result match {
        case Left(_) => fail()
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
      }
    }

    "handle exceptions" in new SetUp {
      httpCallToFindWithFilterWillFailWithException(new BadGatewayException("some error"))

      val result = await(connector.findWithFilters(List(searchTerm), List.empty))

      result match {
        case Right(_) => fail()
        case Left(_:BadGatewayException) => succeed
      }

    }
  }

  "deleteByPublisherReference" should {

    "return true when successful and NO_CONTENT status returned" in new SetUp {
      val noContentResponse = HttpResponse(NO_CONTENT, "")
      httpCallToDeleteApiWillSucceed(noContentResponse)
      await(connector.deleteByPublisherReference(examplePublisherReference)) shouldBe true
    }

    "return false when successful but NOT_FOUND status returned" in new SetUp {
      val noContentResponse = HttpResponse(NOT_FOUND, "")
      httpCallToDeleteApiWillSucceed(noContentResponse)
      await(connector.deleteByPublisherReference(examplePublisherReference)) shouldBe false
    }

    "return false when NotFoundException is thrown" in new SetUp {
      httpCallToDeleteApiWillFailWithNotFound(new NotFoundException(s"api with publisherReference: $examplePublisherReference not found"))
      await(connector.deleteByPublisherReference(examplePublisherReference)) shouldBe false
    }

  }
}
