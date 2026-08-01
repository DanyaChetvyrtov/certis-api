package ru.digitalhustle.certis.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import ru.digitalhustle.certis.constants.PathConstants
import ru.digitalhustle.certis.filter.JwtTokenFilter
import ru.digitalhustle.certis.service.security.JwtCookieManager
import ru.digitalhustle.certis.service.security.JwtTokenProvider
import ru.digitalhustle.certis.util.security.RestSecurityErrorHandler

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val cookieManager: JwtCookieManager,
    private val jwtTokenProvider: JwtTokenProvider,
    private val restSecurityErrorHandler: RestSecurityErrorHandler,
) {
    companion object {
        private const val SINGLE_STAR = "*"
        private const val DOUBLE_STAR = "**"
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(restSecurityErrorHandler)
                it.accessDeniedHandler(restSecurityErrorHandler)
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/$DOUBLE_STAR").permitAll()
                it.requestMatchers(HttpMethod.POST, PathConstants.AUTH).permitAll()
                it.requestMatchers(HttpMethod.POST, PathConstants.AUTH_REGISTRATION).permitAll()
                it.requestMatchers(HttpMethod.POST, PathConstants.AUTH_TOKEN).permitAll()
                it.requestMatchers(HttpMethod.POST, PathConstants.AUTH_LOGOUT).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                JwtTokenFilter(
                    cookieManager = cookieManager,
                    jwtTokenProvider = jwtTokenProvider,
                ),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .build()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:3000",
                "http://digital-hustle.ru",
                "https://digital-hustle.ru",
            )
            addAllowedMethod(SINGLE_STAR)
            addAllowedHeader(SINGLE_STAR)
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(DOUBLE_STAR, configuration)
        }
    }
}
