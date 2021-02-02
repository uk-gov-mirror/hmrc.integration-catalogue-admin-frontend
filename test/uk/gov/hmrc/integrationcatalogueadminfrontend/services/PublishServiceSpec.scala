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

package uk.gov.hmrc.integrationcatalogueadminfrontend.services

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.mockito.scalatest.MockitoSugar
import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain._
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.connectors.{PublishDetails, PublishRequest, PublishResult}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class PublishServiceSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with MockitoSugar  {

val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]
private implicit val hc: HeaderCarrier = HeaderCarrier()

trait SetUp {
    val objInTest = new PublishService(mockIntegrationCatalogueConnector)
    val publishRequest: PublishRequest = PublishRequest("publisherRef", PlatformType.CORE_IF, "fileName", SpecificationType.OAS_V3, "contents")
    val publishResult: PublishResult =
      PublishResult(isSuccess = true, Some(PublishDetails(IntegrationId(UUID.randomUUID()),  "publisherReference", PlatformType.CORE_IF)))
}

"publishApi" should {
  "return value from connector" in new SetUp {
    when(mockIntegrationCatalogueConnector.publish(eqTo(publishRequest))(*)).thenReturn(Future.successful(publishResult))

    val result: PublishResult =
      Await.result(objInTest.publishApi("publisherRef", PlatformType.CORE_IF, "fileName", SpecificationType.OAS_V3, "contents"), 500 millis)
    result shouldBe publishResult

    verify(mockIntegrationCatalogueConnector).publish(eqTo(publishRequest))(eqTo(hc))
  }
}

}