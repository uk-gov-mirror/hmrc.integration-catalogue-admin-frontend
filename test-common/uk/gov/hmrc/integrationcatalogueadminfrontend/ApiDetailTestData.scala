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

package uk.gov.hmrc.integrationcatalogueadminfrontend.data

import java.util.UUID
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.integrationcatalogue.models.common._
import uk.gov.hmrc.integrationcatalogue.models._

trait ApiDetailTestData {

  val filename = "API1689_Get_Known_Facts_1.1.0.yaml"
  val fileContents = "{}"
  val uuid = UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")
  val dateValue: DateTime = DateTime.parse("04/11/2020 20:27:05", DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"));

  val apiPlatformMaintainer: Maintainer = Maintainer("API Platform Team", "#team-api-platform-sup")
  val coreIfMaintainer: Maintainer = Maintainer("IF Team", "N/A", List.empty)

  val jsonMediaType = "application/json"

  val exampleRequest1name = "example request 1"
  val exampleRequest1Body = "{\"someValue\": \"abcdefg\"}"
  val exampleRequest1: Example = Example(exampleRequest1name, exampleRequest1Body, Some(jsonMediaType))

  val exampleResponse1 = new Example("example response name", "example response body", Some(jsonMediaType))

  val endpoint1: Endpoint = Endpoint("/some/url", "GET", "some summary", "some description", List(exampleRequest1), List(exampleResponse1))
  val endpoints = List(endpoint1, Endpoint("/some/url", "PUT", "some summary", "some description"))

  val exampleApiDetail: ApiDetail = ApiDetail(
    IntegrationId(UUID.fromString("e2e4ce48-29b0-11eb-adc1-0242ac120002")),
    publisherReference = "API1689",
    title = "getKnownFactsName",
    description = "getKnownFactsDesc",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    searchText = s"Some Search Text",
    messageType = MessageType.JSON,
    version = "1.1.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints
  )

  val exampleApiDetail2 = ApiDetail(
    IntegrationId(UUID.fromString("28c0bd67-4176-42c7-be13-53be98a4db58")),
    publisherReference = "API1676",
    title = "getOtherFactsName",
    description = "getOtherFactsDesc",
    lastUpdated = dateValue,
    platform = PlatformType.CORE_IF,
    maintainer = coreIfMaintainer,
    searchText = s"Some Search Text2",
    messageType = MessageType.JSON,
    version = "1.2.0",
    specificationType = SpecificationType.OAS_V3,
    hods = List("ETMP"),
    endpoints = endpoints
  )

}
