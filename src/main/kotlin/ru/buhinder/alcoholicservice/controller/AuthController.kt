package ru.buhinder.alcoholicservice.controller

import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.buhinder.alcoholicservice.dto.AccessTokenDto
import ru.buhinder.alcoholicservice.dto.AlcoholicCredentials
import ru.buhinder.alcoholicservice.dto.AlcoholicDto
import ru.buhinder.alcoholicservice.service.AuthService
import ru.buhinder.alcoholicservice.service.TokenService
import javax.validation.Valid

@RestController
@RequestMapping("/api/alcoholic")
class AuthController(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {

    @PostMapping("/login")
    fun login(
        @Valid
        @RequestBody
        alcoholicCredentials: AlcoholicCredentials,
    ): Mono<ResponseEntity<AccessTokenDto>> {
        return authService.login(alcoholicCredentials)
            .map {
                ResponseEntity.ok()
                    .header(SET_COOKIE, "${it.responseCookie}")
                    .body(it.accessToken)
            }
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(value = "refreshToken") refreshToken: String,
    ): Mono<ResponseEntity<AccessTokenDto>> {
        return tokenService.refreshToken(refreshToken)
            .map {
                ResponseEntity.ok()
                    .header(SET_COOKIE, "${it.responseCookie}")
                    .body(it.accessToken)
            }
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader(AUTHORIZATION) authorizationHeader: String): Mono<ResponseEntity<Void>> {
        return authService.logout(authorizationHeader)
            .map { ResponseEntity.ok().build() }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun registerAlcoholic(
        @Valid
        @RequestPart("user") dto: AlcoholicDto,
        @RequestPart(value = "image", required = false)
        image: Mono<FilePart>,
    ): Mono<Void> {
        return image
            .flatMap { authService.register(dto, it) }
            .switchIfEmpty(authService.register(dto, null))
            .then()
    }

}
