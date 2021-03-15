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

import org.mockito.scalatest.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.integrationcatalogue.models._
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat


class PublishServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar  {

val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]
private implicit val hc: HeaderCarrier = HeaderCarrier()

trait SetUp {
    val objInTest = new PublishService(mockIntegrationCatalogueConnector)
    val apiPublishRequest: ApiPublishRequest = ApiPublishRequest("publisherRef", PlatformType.CORE_IF, SpecificationType.OAS_V3, "contents")
    val expectedApiPublishResult: PublishResult =
      PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(UUID.randomUUID()),  "publisherReference", PlatformType.CORE_IF)))

    val expectedFileTransferPublishResult: PublishResult =
      PublishResult(isSuccess = true, Some(PublishDetails(isUpdate = true, IntegrationId(UUID.randomUUID()),  "BVD-DPS-PCPMonthly-pull", PlatformType.CORE_IF)))
    
    val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

    val fileTransferPublishRequest = FileTransferPublishRequest(
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
}

"publishApi" should {
  "return value from connector" in new SetUp {
    when(mockIntegrationCatalogueConnector.publishApis(eqTo(apiPublishRequest))(*)).thenReturn(Future.successful(Right(expectedApiPublishResult)))

    val result: Either[Throwable, PublishResult] =
      Await.result(objInTest.publishApi("publisherRef", PlatformType.CORE_IF, SpecificationType.OAS_V3, "contents"), 500 millis)

    result match {
      case Left(_) => fail()
      case Right(publishResult: PublishResult) => publishResult shouldBe expectedApiPublishResult
    }

    verify(mockIntegrationCatalogueConnector).publishApis(eqTo(apiPublishRequest))(eqTo(hc))
  }
}

"publishFileTransfer" should {
  "return value from connector" in new SetUp {
    when(mockIntegrationCatalogueConnector.publishFileTransfer(eqTo(fileTransferPublishRequest))(*)).thenReturn(Future.successful(Right(expectedFileTransferPublishResult)))

    val result: Either[Throwable, PublishResult] =
      Await.result(objInTest.publishFileTransfer(fileTransferPublishRequest), 500 millis)

    result match {
      case Left(_) => fail()
      case Right(publishResult: PublishResult) => publishResult shouldBe expectedFileTransferPublishResult
    }

    verify(mockIntegrationCatalogueConnector).publishFileTransfer(eqTo(fileTransferPublishRequest))(eqTo(hc))
  }
}

}