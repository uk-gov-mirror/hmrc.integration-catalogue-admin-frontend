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

package uk.gov.hmrc.integrationcatalogue.models

import org.joda.time.DateTime
import uk.gov.hmrc.integrationcatalogue.models.common._

sealed trait IntegrationDetail {
    def id: IntegrationId
    def publisherReference: String
    def title: String
    def description: String
    def platform: PlatformType
    def searchText: String
    def lastUpdated: DateTime
    def maintainer : Maintainer
    def integrationType: IntegrationType
    def score: Option[Double]
}

case class Example(name: String, jsonBody: String)

case class StringAttributes(minLength: Option[Int], maxLength: Option[Int])
case class NumberAttributes(
  minimum: Option[BigDecimal],
 maximum: Option[BigDecimal],
 multipleOf: Option[BigDecimal],
 exclusiveMinimum: Option[Boolean],
 exclusiveMaximum: Option[Boolean]
 )

 // types that could be T are: OffsetDateTime, byte[], UUID, Number, Date, Boolean, BigDecimal, String

sealed trait Schema {
  def name: Option[String]
  def not: Option[Schema]
  def `type`: Option[String]
  def pattern: Option[String]
  def description: Option[String]
  def ref: Option[String]
  def properties: List[Schema]
  def `enum`: List[String]
  def required: List[String]
  def minProperties: Option[Int]
  def maxProperties: Option[Int]
}


case class DefaultSchema(
    name: Option[String],
    not: Option[Schema],
    `type`: Option[String],
    pattern: Option[String],
    description: Option[String],
    ref: Option[String],
    properties: List[Schema],
    `enum`: List[String],
    required: List[String],
    stringAttributes: Option[StringAttributes],
    numberAttributes: Option[NumberAttributes],
    minProperties: Option[Int],
    maxProperties: Option[Int],
    format: Option[String],
    default: Option[String],
    example: Option[String]
    ) extends Schema

case class ComposedSchema(
    name: Option[String],
    not: Option[Schema],
    `type`: Option[String],
    pattern: Option[String],
    description: Option[String],
    ref: Option[String],
    properties: List[Schema],
    `enum`: List[String],
    required: List[String],
    minProperties: Option[Int],
    maxProperties: Option[Int],
    allOf: List[Schema],
    anyOf: List[Schema],
    oneOf: List[Schema])
    extends Schema

case class ArraySchema(
    name: Option[String],
    not: Option[Schema],
    `type`: Option[String],
    pattern: Option[String],
    description: Option[String],
    ref: Option[String],
    properties: List[Schema],
    `enum`: List[String],
    required: List[String],
    minProperties: Option[Int],
    maxProperties: Option[Int],
    minItems: Option[Int],
    maxItems: Option[Int],
    uniqueItems: Option[Boolean],
    items: Option[Schema])
    extends Schema

case class Request(description: Option[String], schema: Option[Schema], mediaType: Option[String], examples: List[Example] = List.empty)

//TODO response object needs fleshing out with headers, example errors, schema etc
case class Response(statusCode: Int, description: Option[String], schema: Option[Schema], mediaType: Option[String], examples: List[Example] = List.empty)
case class Endpoint(path: String, methods: List[EndpointMethod])

case class EndpointMethod(httpMethod: String, operationId: Option[String], summary: Option[String], description: Option[String], request: Option[Request], responses: List[Response])


case class ApiDetail(id: IntegrationId,
                     publisherReference: String,
                     title: String,
                     description: String,
                     platform: PlatformType,
                     searchText: String,
                     hods: List[String] = List.empty,
                     lastUpdated: DateTime,
                     maintainer: Maintainer,
                     score: Option[Double] = None,
                     version: String,
                     specificationType: SpecificationType,
                     endpoints: List[Endpoint],
                     schemas: List[Schema]) extends IntegrationDetail {
 override val integrationType: IntegrationType = IntegrationType.API
}

case class FileTransferDetail(id: IntegrationId, // Ignore
                              fileTransferSpecificationVersion: String, // Set to 0.1?        
                              publisherReference: String,
                              title: String,
                              description: String,
                              platform: PlatformType, // Split this to Platform and type
                              searchText: String, // Ignore
                              lastUpdated: DateTime,
                              maintainer: Maintainer,
                              score: Option[Double] = None,
                              sourceSystem: List[String],
                              targetSystem: List[String],
                              fileTransferPattern: String) extends IntegrationDetail {
    override val integrationType: IntegrationType = IntegrationType.FILE_TRANSFER
}
