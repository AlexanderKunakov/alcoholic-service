package ru.buhinder.alcoholicservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import ru.buhinder.alcoholicservice.security.converter.JwtServerAuthenticationConverter
import ru.buhinder.alcoholicservice.security.manager.JwtAuthenticationManager


@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    companion object {
        const val jwtMatcher = "/api/alcoholic/**"
        const val image = "/api/alcoholic/image/**"
        val auth = arrayOf(
            "/api/alcoholic/refresh",
            "/api/alcoholic/register",
            "/api/alcoholic/logout",
            "/api/alcoholic/login",
        )
    }

    @Bean
    fun http(
        http: ServerHttpSecurity,
        jwtAuthManager: JwtAuthenticationManager,
        jwtServerAuthenticationConverter: JwtServerAuthenticationConverter,
    ): SecurityWebFilterChain? {
        return http
            // TODO: 27/07/2022 handle exceptions
            .csrf().disable()
            .httpBasic().disable()
            .formLogin().disable()
            .logout().disable()
            .cors()

            .and()

            .authorizeExchange()
            .pathMatchers(HttpMethod.OPTIONS).permitAll()
            .pathMatchers(*auth).permitAll()
            .pathMatchers(image).permitAll()

            .and()

            .authorizeExchange()
            .pathMatchers(jwtMatcher)
            .authenticated()
            .and()
            .addFilterAt(getJwtAuthenticationFilter(jwtAuthManager, jwtServerAuthenticationConverter), AUTHENTICATION)

            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // TODO: 27/07/2022 make it a bean
    private fun getJwtAuthenticationFilter(
        jwtAuthenticationManager: JwtAuthenticationManager,
        jwtServerAuthenticationConverter: JwtServerAuthenticationConverter,
    ): AuthenticationWebFilter {
        val authenticationWebFilter = AuthenticationWebFilter(jwtAuthenticationManager)
        authenticationWebFilter.setServerAuthenticationConverter(jwtServerAuthenticationConverter)
        authenticationWebFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers(jwtMatcher))
        return authenticationWebFilter
    }

}
