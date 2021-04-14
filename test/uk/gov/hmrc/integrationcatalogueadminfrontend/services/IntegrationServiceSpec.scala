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
import java.util.UUID
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType

class IntegrationServiceSpec extends WordSpec with Matchers with GuiceOneAppPerSuite with MockitoSugar with ApiDetailTestData with AwaitTestSupport with BeforeAndAfterEach {

  val mockIntegrationCatalogueConnector: IntegrationCatalogueConnector = mock[IntegrationCatalogueConnector]
  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIntegrationCatalogueConnector)
  }
  trait SetUp {
    val objInTest = new IntegrationService(mockIntegrationCatalogueConnector)
    val exampleIntegrationId = IntegrationId(UUID.fromString("2840ce2d-03fa-46bb-84d9-0299402b7b32"))

  }

  "deleteByIntegrationId" should {
    "return true from connector when deletion successful" in new SetUp {
      when(mockIntegrationCatalogueConnector.deleteByIntegrationId(eqTo(exampleIntegrationId))(*)).thenReturn(Future.successful(true))
      val result: Boolean = await(objInTest.deleteByIntegrationId(exampleIntegrationId))
      result shouldBe true
    }
  }

  "deleteByPlatform" should {
    "return DeleteIntegrationsSuccess from connector when deletion successful" in new SetUp {
      when(mockIntegrationCatalogueConnector.deleteByPlatform(eqTo(PlatformType.CORE_IF))(*)).thenReturn(Future.successful(DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))))
      val result = await(objInTest.deleteByPlatform(PlatformType.CORE_IF))
      result shouldBe DeleteIntegrationsSuccess(DeleteIntegrationsResponse(1))
    }
  }


  "findWithFilter" should {
    "return a Right when successful" in new SetUp {
      val expectedResult = List(exampleApiDetail, exampleApiDetail2, exampleFileTransfer)
      when(mockIntegrationCatalogueConnector.findWithFilters(*, *)(*)).thenReturn(Future.successful(Right(IntegrationResponse(expectedResult.size, expectedResult))))

      val result: Either[Throwable, IntegrationResponse] = await(objInTest.findWithFilters(List("search"), List.empty))

      result match {
        case Left(_) => fail()
        case Right(integrationResponse: IntegrationResponse) => integrationResponse.results shouldBe expectedResult
      }
    }

    "return Left when error from connector" in new SetUp {
      when(mockIntegrationCatalogueConnector.findWithFilters(*,*)(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))

      val result: Either[Throwable, IntegrationResponse] =
        await(objInTest.findWithFilters(List("search"), List.empty))

      result match {
        case Left(_) => succeed
        case Right(_) => fail()
      }

      verify(mockIntegrationCatalogueConnector).findWithFilters(eqTo(List("search")), eqTo(List.empty))(eqTo(hc))
    }
  }

  "findById" should {
    "return error from connector" in new SetUp {
      val id = IntegrationId(UUID.randomUUID())
      when(mockIntegrationCatalogueConnector.findByIntegrationId(eqTo(id))(*)).thenReturn(Future.successful(Left(new RuntimeException("some error"))))

      val result: Either[Throwable, IntegrationDetail] =
        await(objInTest.findByIntegrationId(id))

      result match {
        case Left(_) => succeed
        case Right(_) => fail()
      }

      verify(mockIntegrationCatalogueConnector).findByIntegrationId(eqTo(id))(eqTo(hc))
    }

     "return apidetail from connector when returned from backend" in new SetUp {
      val id = IntegrationId(UUID.randomUUID())
      when(mockIntegrationCatalogueConnector.findByIntegrationId(eqTo(id))(*)).thenReturn(Future.successful(Right(exampleApiDetail)))

      val result: Either[Throwable, IntegrationDetail] =
        await(objInTest.findByIntegrationId(id))

      result match {
        case Right(apiDetail) => apiDetail shouldBe exampleApiDetail
        case Left(_) => fail()
      }

      verify(mockIntegrationCatalogueConnector).findByIntegrationId(eqTo(id))(eqTo(hc))
    }


  }

}