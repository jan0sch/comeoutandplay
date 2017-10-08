/*
 * Copyright (C) 2017  Jens Grassel & André Schütz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package modules

import com.google.inject.name.Named
import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.crypto.{
  JcaCrypter,
  JcaCrypterSettings,
  JcaSigner,
  JcaSignerSettings
}
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.providers.oauth1._
import com.mohiva.play.silhouette.impl.providers.oauth1.secrets.{
  CookieSecretProvider,
  CookieSecretSettings
}
import com.mohiva.play.silhouette.impl.providers.oauth1.services.PlayOAuth1Service
import com.mohiva.play.silhouette.impl.providers.oauth2._
import com.mohiva.play.silhouette.impl.providers.openid.YahooProvider
import com.mohiva.play.silhouette.impl.providers.openid.services.PlayOpenIDService
import com.mohiva.play.silhouette.impl.providers.state.{ CsrfStateItemHandler, CsrfStateSettings }
import com.mohiva.play.silhouette.impl.services._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.{ BCryptPasswordHasher, BCryptSha256PasswordHasher }
import com.mohiva.play.silhouette.persistence.daos.{ DelegableAuthInfoDAO, InMemoryAuthInfoDAO }
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import models.daos._
import models.services.{ UserService, UserServiceImpl }
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.api.libs.openid.OpenIdClient
import play.api.libs.ws.WSClient
import play.api.mvc.CookieHeaderEncoding
import utils.auth.{ CustomSecuredErrorHandler, CustomUnsecuredErrorHandler, DefaultEnv }

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * The Guice module which wires all Silhouette dependencies.
  */
@SuppressWarnings(Array("org.wartremover.warts.NonUnitStatements"))
class SilhouetteModule extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  def configure(): Unit = {
    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]]
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler]
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler]
    bind[UserService].to[UserServiceImpl]
    bind[UserDAO].to[UserDAOImpl]
    bind[CacheLayer].to[PlayCacheLayer]
    bind[IDGenerator].toInstance(new SecureRandomIDGenerator())
//    bind[PasswordHasher].toInstance(new BCryptPasswordHasher)
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())

    // Replace this with the bindings to your concrete DAOs
    bind[DelegableAuthInfoDAO[OpenIDInfo]].toInstance(new InMemoryAuthInfoDAO[OpenIDInfo])
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordInfoDAO] // Bind to our PasswordInfo DAO
    bind[DelegableAuthInfoDAO[OAuth1Info]].to[OAuth1InfoDao]     // Bind to our OAuth1 DAO
    bind[DelegableAuthInfoDAO[OAuth2Info]].to[OAuth2InfoDao]     // Bind to our OAuth2 DAO
  }

  /**
    * Provides the HTTP layer implementation.
    *
    * @param client Play's WS client.
    * @return The HTTP layer implementation.
    */
  @Provides
  def provideHTTPLayer(client: WSClient): HTTPLayer = new PlayHTTPLayer(client)

  /**
    * Provides the Silhouette environment.
    *
    * @param userService The user service implementation.
    * @param authenticatorService The authentication service implementation.
    * @param eventBus The event bus instance.
    * @return The Silhouette environment.
    */
  @Provides
  def provideEnvironment(userService: UserService,
                         authenticatorService: AuthenticatorService[CookieAuthenticator],
                         eventBus: EventBus): Environment[DefaultEnv] =
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )

  /**
    * Provides the social provider registry.
    *
    * @param facebookProvider The Facebook provider implementation.
    * @return The Silhouette environment.
    */
  @Provides
  def provideSocialProviderRegistry(
      facebookProvider: FacebookProvider
  ): SocialProviderRegistry =
    SocialProviderRegistry(
      Seq(
        facebookProvider
      )
    )

  /**
    * Provides the cookie signer for the OAuth1 token secret provider.
    *
    * @param configuration The Play configuration.
    * @return The cookie signer for the OAuth1 token secret provider.
    */
  @Provides
  @Named("oauth1-token-secret-signer")
  def provideOAuth1TokenSecretSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.oauth1TokenSecretProvider.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the crypter for the OAuth1 token secret provider.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the OAuth1 token secret provider.
    */
  @Provides
  @Named("oauth1-token-secret-crypter")
  def provideOAuth1TokenSecretCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying
      .as[JcaCrypterSettings]("silhouette.oauth1TokenSecretProvider.crypter")

    new JcaCrypter(config)
  }

  /**
    * Provides the cookie signer for the OAuth2 state provider.
    *
    * @param configuration The Play configuration.
    * @return The cookie signer for the OAuth2 state provider.
    */
  @Provides
  @Named("oauth2-state-signer")
  def provideOAuth2StageSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying
      .as[JcaSignerSettings]("silhouette.oauth2StateProvider.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the signer for the social state handler.
    *
    * @param configuration The Play configuration.
    * @return The signer for the social state handler.
    */
  @Provides
  @Named("social-state-signer")
  def provideSocialStateSigner(configuration: Configuration): Signer = {
    val config =
      configuration.underlying.as[JcaSignerSettings]("silhouette.socialStateHandler.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the signer for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The signer for the authenticator.
    */
  @Provides
  @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.authenticator.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the signer for the CSRF state item handler.
    *
    * @param configuration The Play configuration.
    * @return The signer for the CSRF state item handler.
    */
  @Provides
  @Named("csrf-state-item-signer")
  def provideCSRFStateItemSigner(configuration: Configuration): Signer = {
    val config =
      configuration.underlying.as[JcaSignerSettings]("silhouette.csrfStateItemHandler.signer")

    new JcaSigner(config)
  }

  /**
    * Provides the crypter for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the authenticator.
    */
  @Provides
  @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config =
      configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }

  /**
    * Provides the auth info repository.
    *
    * @param passwordInfoDAO The implementation of the delegable password auth info DAO.
    * @param oauth1InfoDAO The implementation of the delegable OAuth1 auth info DAO.
    * @param oauth2InfoDAO The implementation of the delegable OAuth2 auth info DAO.
    * @param openIDInfoDAO The implementation of the delegable OpenID auth info DAO.
    * @return The auth info repository instance.
    */
  @Provides
  def provideAuthInfoRepository(
      passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo],
      oauth1InfoDAO: DelegableAuthInfoDAO[OAuth1Info],
      oauth2InfoDAO: DelegableAuthInfoDAO[OAuth2Info],
      openIDInfoDAO: DelegableAuthInfoDAO[OpenIDInfo]
  ): AuthInfoRepository =
    new DelegableAuthInfoRepository(passwordInfoDAO, oauth1InfoDAO, oauth2InfoDAO, openIDInfoDAO)

  /**
    * Provides the authenticator service.
    *
    * @param signer The cookie signer implementation.
    * @param crypter The crypter implementation.
    * @param cookieHeaderEncoding Logic for encoding and decoding `Cookie` and `Set-Cookie` headers.
    * @param fingerprintGenerator The fingerprint generator implementation.
    * @param idGenerator The ID generator implementation.
    * @param configuration The Play configuration.
    * @param clock The clock instance.
    * @return The authenticator service.
    */
  @Provides
  def provideAuthenticatorService(@Named("authenticator-signer") signer: Signer,
                                  @Named("authenticator-crypter") crypter: Crypter,
                                  cookieHeaderEncoding: CookieHeaderEncoding,
                                  fingerprintGenerator: FingerprintGenerator,
                                  idGenerator: IDGenerator,
                                  configuration: Configuration,
                                  clock: Clock): AuthenticatorService[CookieAuthenticator] = {

    val config =
      configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val encoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config,
                                   None,
                                   signer,
                                   cookieHeaderEncoding,
                                   encoder,
                                   fingerprintGenerator,
                                   idGenerator,
                                   clock)
  }

  /**
    * Provides the avatar service.
    *
    * @param httpLayer The HTTP layer implementation.
    * @return The avatar service implementation.
    */
  @Provides
  def provideAvatarService(httpLayer: HTTPLayer): AvatarService = new GravatarService(httpLayer)

  /**
    * Provides the OAuth1 token secret provider.
    *
    * @param cookieSigner The cookie signer implementation.
    * @param crypter The crypter implementation.
    * @param configuration The Play configuration.
    * @param clock The clock instance.
    * @return The OAuth1 token secret provider implementation.
    */
  @Provides
  def provideOAuth1TokenSecretProvider(
      @Named("oauth1-token-secret-signer") cookieSigner: Signer,
      @Named("oauth1-token-secret-crypter") crypter: Crypter,
      configuration: Configuration,
      clock: Clock
  ): OAuth1TokenSecretProvider = {

    val settings =
      configuration.underlying.as[CookieSecretSettings]("silhouette.oauth1TokenSecretProvider")
    new CookieSecretProvider(settings, cookieSigner, crypter, clock)
  }

//  /**
//    * Provides the OAuth2 state provider.
//    *
//    * @param idGenerator The ID generator implementation.
//    * @param cookieSigner The cookie signer implementation.
//    * @param configuration The Play configuration.
//    * @param clock The clock instance.
//    * @return The OAuth2 state provider implementation.
//    */
//  @Provides
//  def provideOAuth2StateProvider(idGenerator: IDGenerator,
//                                 @Named("oauth2-state-cookie-signer") cookieSigner: Signer,
//                                 configuration: Configuration,
//                                 clock: Clock): SocialStateProvider = {
//
//    val settings =
//      configuration.underlying.as[CookieStateSettings]("silhouette.oauth2StateProvider")
//    new CookieStateProvider(settings, idGenerator, cookieSigner, clock)
//  }

  /**
    * Provides the CSRF state item handler.
    *
    * @param idGenerator The ID generator implementation.
    * @param signer The signer implementation.
    * @param configuration The Play configuration.
    * @return The CSRF state item implementation.
    */
  @Provides
  def provideCsrfStateItemHandler(idGenerator: IDGenerator,
                                  @Named("csrf-state-item-signer") signer: Signer,
                                  configuration: Configuration): CsrfStateItemHandler = {
    val settings =
      configuration.underlying.as[CsrfStateSettings]("silhouette.csrfStateItemHandler")
    new CsrfStateItemHandler(settings, idGenerator, signer)
  }

  @Provides
  def provideSocialStateHandler(@Named("social-state-signer") signer: Signer,
                                csrfStateItemHandler: CsrfStateItemHandler): SocialStateHandler =
    new DefaultSocialStateHandler(Set(csrfStateItemHandler), signer)

  /**
    * Provides the password hasher registry.
    *
    * @return The password hasher registry.
    */
  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry =
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))

  /**
    * Provides the credentials provider.
    *
    * @param authInfoRepository The auth info repository implementation.
    * @param passwordHasherRegistry The password hasher registry.
    * @return The credentials provider.
    */
  @Provides
  def provideCredentialsProvider(
      authInfoRepository: AuthInfoRepository,
      passwordHasherRegistry: PasswordHasherRegistry
  ): CredentialsProvider =
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)

  /**
    * Provides the Facebook provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param stateHandler The OAuth2 state provider implementation.
    * @param configuration The Play configuration.
    * @return The Facebook provider.
    */
  @Provides
  def provideFacebookProvider(httpLayer: HTTPLayer,
                              stateHandler: SocialStateHandler,
                              configuration: Configuration): FacebookProvider =
    new FacebookProvider(httpLayer,
                         stateHandler,
                         configuration.underlying.as[OAuth2Settings]("silhouette.facebook"))

  /**
    * Provides the Google provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param stateHandler The OAuth2 state provider implementation.
    * @param configuration The Play configuration.
    * @return The Google provider.
    */
  @Provides
  def provideGoogleProvider(httpLayer: HTTPLayer,
                            stateHandler: SocialStateHandler,
                            configuration: Configuration): GoogleProvider =
    new GoogleProvider(httpLayer,
                       stateHandler,
                       configuration.underlying.as[OAuth2Settings]("silhouette.google"))

  /**
    * Provides the VK provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param stateHandler The OAuth2 state provider implementation.
    * @param configuration The Play configuration.
    * @return The VK provider.
    */
  @Provides
  def provideVKProvider(httpLayer: HTTPLayer,
                        stateHandler: SocialStateHandler,
                        configuration: Configuration): VKProvider =
    new VKProvider(httpLayer,
                   stateHandler,
                   configuration.underlying.as[OAuth2Settings]("silhouette.vk"))

  /**
    * Provides the Twitter provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param tokenSecretProvider The token secret provider implementation.
    * @param configuration The Play configuration.
    * @return The Twitter provider.
    */
  @Provides
  def provideTwitterProvider(httpLayer: HTTPLayer,
                             tokenSecretProvider: OAuth1TokenSecretProvider,
                             configuration: Configuration): TwitterProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.twitter")
    new TwitterProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  /**
    * Provides the Xing provider.
    *
    * @param httpLayer The HTTP layer implementation.
    * @param tokenSecretProvider The token secret provider implementation.
    * @param configuration The Play configuration.
    * @return The Xing provider.
    */
  @Provides
  def provideXingProvider(httpLayer: HTTPLayer,
                          tokenSecretProvider: OAuth1TokenSecretProvider,
                          configuration: Configuration): XingProvider = {

    val settings = configuration.underlying.as[OAuth1Settings]("silhouette.xing")
    new XingProvider(httpLayer, new PlayOAuth1Service(settings), tokenSecretProvider, settings)
  }

  /**
    * Provides the Yahoo provider.
    *
    * @param cacheLayer The cache layer implementation.
    * @param httpLayer The HTTP layer implementation.
    * @param client The OpenID client implementation.
    * @param configuration The Play configuration.
    * @return The Yahoo provider.
    */
  @Provides
  def provideYahooProvider(cacheLayer: CacheLayer,
                           httpLayer: HTTPLayer,
                           client: OpenIdClient,
                           configuration: Configuration): YahooProvider = {

    val settings = configuration.underlying.as[OpenIDSettings]("silhouette.yahoo")
    new YahooProvider(httpLayer, new PlayOpenIDService(client, settings), settings)
  }
}
