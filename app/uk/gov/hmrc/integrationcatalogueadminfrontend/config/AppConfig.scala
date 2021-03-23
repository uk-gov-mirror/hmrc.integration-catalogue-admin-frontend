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

package uk.gov.hmrc.integrationcatalogueadminfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.i18n.Lang
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType._
import uk.gov.hmrc.integrationcatalogue.models.common.PlatformType

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  val welshLanguageSupportEnabled: Boolean = config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  val en: String            = "en"
  val cy: String            = "cy"
  val defaultLanguage: Lang = Lang(en)

  val integrationCatalogueUrl = servicesConfig.baseUrl("integration-catalogue")
  val authorizationKey = servicesConfig.getString("authorizationKey")
  val cmaAauthorizationKey = servicesConfig.getString("auth.authKey.cma")
  val apiPlatformAauthorizationKey = servicesConfig.getString("auth.authKey.apiPlatform")
  val coreIfAauthorizationKey = servicesConfig.getString("auth.authKey.coreIF")
  val desAauthorizationKey = servicesConfig.getString("auth.authKey.DES")

  val authPlatformMap: Map[PlatformType, String] = Map (CMA -> cmaAauthorizationKey,
                                                        API_PLATFORM -> apiPlatformAauthorizationKey,
                                                        CORE_IF -> coreIfAauthorizationKey,
                                                        DES -> desAauthorizationKey)

}
