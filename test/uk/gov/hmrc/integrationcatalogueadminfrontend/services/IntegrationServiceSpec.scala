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
import uk.gov.hmrc.integrationcatalogueadminfrontend.AwaitTestSupport
import uk.gov.hmrc.integrationcatalogueadminfrontend.connectors.IntegrationCatalogueConnector
import uk.gov.hmrc.integrationcatalogueadminfrontend.data.ApiDetailTestData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IntegrationServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ApiDetailTestData with AwaitTestSupport {

  val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  trait SetUp {
    val objInTest = new IntegrationService(mockIntegrationCatalogueConnector)
    val examplePublisherReference = "example-publisher-reference"

  }

  "deleteByPublisherReference" should {
    "return true from connector when deletion successful" in new SetUp {
      when(mockIntegrationCatalogueConnector.deleteByPublisherReference(*)(*)).thenReturn(Future.successful(true))
      val result: Boolean = await(objInTest.deleteByPublisherReference(examplePublisherReference))
      result shouldBe true
    }
  }

  "getAllApis" should {
    "return error from connector" in new SetUp {
      when(mockIntegrationCatalogueConnector.getAll()(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))

      val result: Either[Throwable, IntegrationResponse] =
        await(objInTest.getAllIntegrations())

      result match {
        case Left(_) => succeed
        case Right(_) => fail()
      }

      verify(mockIntegrationCatalogueConnector).getAll()(eqTo(hc))
    }


    "return apis from connector" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2)
      when(mockIntegrationCatalogueConnector.getAll()(*)).thenReturn(Future.successful(Right(IntegrationResponse(expectedResult.size, expectedResult))))

      val result: Either[Throwable, IntegrationResponse] = await(objInTest.getAllIntegrations())

      result match {
        case Left(_) => fail()
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
      }
    }
  }

}