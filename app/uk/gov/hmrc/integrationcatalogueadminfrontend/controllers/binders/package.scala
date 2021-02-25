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

package uk.gov.hmrc.integrationcatalogueadminfrontend.controllers

import play.api.mvc.PathBindable
import uk.gov.hmrc.integrationcatalogue.models.common.IntegrationId
import java.util.UUID
import scala.util.Try

package object binders {

  private def integrationIdFromString(text: String): Either[String, IntegrationId] = {
    Try(UUID.fromString(text))
    .toOption
    .toRight(s"Cannot accept $text as IntegrationsId")
    .map(IntegrationId(_))
  }

  implicit def integrationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[IntegrationId] = new PathBindable[IntegrationId] {
    override def bind(key: String, value: String): Either[String, IntegrationId] = {
      textBinder.bind(key, value).flatMap(integrationIdFromString)
    }

    override def unbind(key: String, integrationId: IntegrationId): String = {
      integrationId.value.toString
    }
  }

}