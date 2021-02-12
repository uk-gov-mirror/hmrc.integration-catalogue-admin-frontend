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
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishDetails, PublishRequest, PublishResult}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common.{IntegrationId, PlatformType, SpecificationType}

import java.util.UUID
import org.scalatest.WordSpec
import org.scalatest.Matchers
import uk.gov.hmrc.integrationcatalogueadminfrontend.AwaitTestSupport
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.ApiDetail
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import play.api.http.Status._
import play.api.test.Helpers._
import _root_.uk.gov.hmrc.integrationcatalogueadminfrontend.domain.JsonFormatters._

class IntegrationCatalogueConnectorSpec extends WordSpec with Matchers with OptionValues with MockitoSugar with BeforeAndAfterEach with AwaitTestSupport with ApiDetailTestData {
  private val mockHttpClient = mock[HttpClient]
  private val mockAppConfig = mock[AppConfig]
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  val outboundUrl = "/integration-catalogue/apis/publish"
  val getAllUrl = "/integration-catalogue/apis"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
  }

  trait SetUp {
    val headerCarrierCaptor: Captor[HeaderCarrier] = ArgCaptor[HeaderCarrier]

    val connector = new IntegrationCatalogueConnector(
      mockHttpClient,
      mockAppConfig)

    def httpCallToPublishWillSucceedWithResponse(response: PublishResult) =
      when(mockHttpClient.PUT[PublishRequest, PublishResult]
        (eqTo(outboundUrl), any[PublishRequest], any[Seq[(String, String)]])
        (any[Writes[PublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

    def httpCallToPublishWillFailWithException(exception: Throwable) =
      when(mockHttpClient.PUT[PublishRequest, PublishResult]
        (eqTo(outboundUrl), any[PublishRequest], any[Seq[(String, String)]])
        (any[Writes[PublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

    def httpCallToGetAllWillSucceedWithResponse(response: List[ApiDetail]) =
      when(mockHttpClient.GET[List[ApiDetail]]
        (eqTo(getAllUrl))
        (any[HttpReads[List[ApiDetail]]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))
    
    def httpCallToGetAllWillFailWithException(exception: Throwable) =
      when(mockHttpClient.GET[List[ApiDetail]]
        (eqTo(getAllUrl))
        (any[HttpReads[List[ApiDetail]]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))

  }

  "IntegrationCatalogueConnector send" should {

      val request: PublishRequest = PublishRequest("publisherRef", PlatformType.CORE_IF, "fileName", SpecificationType.OAS_V3, "{}")

    "return successful result" in new SetUp {
      httpCallToPublishWillSucceedWithResponse(PublishResult(isSuccess = true, Some(PublishDetails(true, IntegrationId(UUID.randomUUID()),  request.publisherReference, request.platformType)), List.empty))


      val result = await(connector.publish(request))

      result match {
        case Left(_) => fail()
        case Right(publishResult: PublishResult) => publishResult.isSuccess shouldBe true
      }

      verify(mockHttpClient).PUT(eqTo(outboundUrl), eqTo(request),
        any[Seq[(String, String)]])(any[Writes[PublishRequest]], any[HttpReads[PublishResult]], headerCarrierCaptor.capture, any[ExecutionContext])

    }

    "handle exceptions" in new SetUp {
      httpCallToPublishWillFailWithException(new BadGatewayException("some error"))

      val result = await(connector.publish(request))

      result match {
        case Right(_) => fail()
        case Left(_:BadGatewayException) => succeed
      }

    }

  }

  "getAll" should {
    "return all apis when successful" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2)
      httpCallToGetAllWillSucceedWithResponse(expectedResult) 

      val result = await(connector.getAll())

      result match {
        case Left(_) => fail()
        case Right(allApisResult: List[ApiDetail]) => allApisResult shouldBe expectedResult
      }
    }

    "handle exceptions" in new SetUp {
      httpCallToGetAllWillFailWithException(new BadGatewayException("some error"))

      val result = await(connector.getAll())

      result match {
        case Right(_) => fail()
        case Left(_:BadGatewayException) => succeed
      }

    }
  }
}
