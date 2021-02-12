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

package uk.gov.hmrc.integrationcatalogueadminfrontend.domain


import org.joda.time.DateTime
import uk.gov.hmrc.integrationcatalogueadminfrontend.domain.common._

case class Example(name: String, jsonBody: String, mediaType: Option[String])

case class Endpoint(path: String,
                    httpMethod: String,
                    summary: String,
                    description: String,
                    exampleRequests: List[Example] = List.empty,
                    exampleResponses: List[Example] = List.empty)

case class ApiDetail(id: IntegrationId,
                           publisherReference: String,
                           name: String,
                           description: String,
                           platform: PlatformType,
                           searchText: String,
                           hods: List[String] = List.empty,
                           lastUpdated: DateTime,
                           maintainer: Maintainer,
                           messageType: MessageType,
                           version: String,
                           specificationType: SpecificationType,
                           endpoints: List[Endpoint])
                          