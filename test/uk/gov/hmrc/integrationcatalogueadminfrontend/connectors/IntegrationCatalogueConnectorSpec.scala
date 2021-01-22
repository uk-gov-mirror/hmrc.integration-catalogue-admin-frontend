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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers
import uk.gov.hmrc.http.{BadGatewayException, HttpClient, _}
import uk.gov.hmrc.integrationcatalogueadminfrontend.config.AppConfig
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishRequest, PublishResult}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.JsonFormatters._

class IntegrationCatalogueConnectorSpec extends AnyWordSpec with Matchers with OptionValues with MockitoSugar with BeforeAndAfterEach {
  private val mockHttpClient = mock[HttpClient]
  private val mockAppConfig = mock[AppConfig]
  private implicit val ec: ExecutionContext = Helpers.stubControllerComponents().executionContext
  private implicit val hc: HeaderCarrier = HeaderCarrier()
  val outboundUrl = "/integration-catalogue/publish"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClient)
  }

  trait SetUp {
    val headerCarrierCaptor: Captor[HeaderCarrier] = ArgCaptor[HeaderCarrier]

    val connector = new IntegrationCatalogueConnector(
      mockHttpClient,
      mockAppConfig)
  }

  "IntegrationCatalogueConnector send" should {
    def httpCallWillSucceedWithResponse(response: PublishResult) =
      when(mockHttpClient.POST[PublishRequest, PublishResult]
        (eqTo(outboundUrl), any[PublishRequest], any[Seq[(String, String)]])
        (any[Writes[PublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.successful(response))

    def httpCallWillFailWithException(exception: Throwable) =
      when(mockHttpClient.POST[PublishRequest, PublishResult]
        (eqTo(outboundUrl), any[PublishRequest], any[Seq[(String, String)]])
        (any[Writes[PublishRequest]], any[HttpReads[PublishResult]], any[HeaderCarrier], any[ExecutionContext]))
        .thenReturn(Future.failed(exception))


    "return successful result" in new SetUp {
      httpCallWillSucceedWithResponse(PublishResult(isSuccess = true, List.empty))

      val request: PublishRequest = PublishRequest("fileName", "{}")

      val result: PublishResult = Await.result(connector.publish(request), 500 millis)
      result.isSuccess shouldBe true

      verify(mockHttpClient).POST(eqTo(outboundUrl), eqTo(request),
        any[Seq[(String, String)]])(any[Writes[PublishRequest]], any[HttpReads[PublishResult]], headerCarrierCaptor.capture, any[ExecutionContext])

    }

    "handle exceptions" in new SetUp {
      httpCallWillFailWithException(new BadGatewayException("some error"))

      val request: PublishRequest = PublishRequest("fileName", "{}")

     val exception=  intercept[RuntimeException]{
        Await.result(connector.publish(request), 500 millis)
      }
      exception.getMessage should be("Unexpected response from /integration-catalogue: some error")

      verify(mockHttpClient).POST(eqTo(outboundUrl), eqTo(request),
        any[Seq[(String, String)]])(any[Writes[PublishRequest]], any[HttpReads[PublishResult]], headerCarrierCaptor.capture, any[ExecutionContext])

    }

  }
}
