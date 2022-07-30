package ru.buhinder.alcoholicservice.service

import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcoholicservice.config.LoggerDelegate
import ru.buhinder.alcoholicservice.controller.advice.exception.EntityNotFoundException
import ru.buhinder.alcoholicservice.dto.AccessTokenDto
import ru.buhinder.alcoholicservice.dto.AlcoholicCredentials
import ru.buhinder.alcoholicservice.dto.AlcoholicDto
import ru.buhinder.alcoholicservice.dto.response.AlcoholicResponse
import ru.buhinder.alcoholicservice.dto.response.AuthResponse
import ru.buhinder.alcoholicservice.entity.AlcoholicEntity
import ru.buhinder.alcoholicservice.entity.SessionEntity
import ru.buhinder.alcoholicservice.entity.SessionToRefreshEntity
import ru.buhinder.alcoholicservice.repository.AlcoholicDaoFacade
import ru.buhinder.alcoholicservice.repository.SessionDaoFacade
import ru.buhinder.alcoholicservice.repository.SessionToRefreshDaoFacade
import ru.buhinder.alcoholicservice.service.validation.RegistrationValidationService
import ru.buhinder.alcoholicservice.service.validation.SessionValidationService
import java.util.UUID

@Service
class AuthService(
    private val alcoholicDaoFacade: AlcoholicDaoFacade,
    private val conversionService: ConversionService,
    private val passwordService: PasswordService,
    private val registrationValidationService: RegistrationValidationService,
    private val tokenService: TokenService,
    private val sessionDaoFacade: SessionDaoFacade,
    private val sessionToRefreshDaoFacade: SessionToRefreshDaoFacade,
    private val sessionValidationService: SessionValidationService,
) {
    private val logger by LoggerDelegate()

    fun register(dto: AlcoholicDto): Mono<AlcoholicResponse> {
        return dto.toMono()
            .doOnNext { logger.info("Registering alcoholic") }
            .flatMap {
                registrationValidationService.validateLoginDoesNotExist(it)
                    .zipWith(passwordService.encodePassword(it.password))
            }
            .map {
                AlcoholicEntity(
                    id = UUID.randomUUID(),
                    firstname = it.t1.firstname,
                    lastName = it.t1.lastName,
                    age = it.t1.age,
                    login = it.t1.login,
                    password = it.t2
                )
            }
            .flatMap { entity -> alcoholicDaoFacade.insert(entity) }
            .map { conversionService.convert(it, AlcoholicResponse::class.java)!! }
            .doOnNext { logger.info("Alcoholic successfully registered") }
    }

    // TODO: 26/07/2022 refactor this chain
    fun login(alcoholicCredentials: AlcoholicCredentials): Mono<AuthResponse> {
        return alcoholicCredentials.toMono()
            .flatMap { it ->
                val login = it.login
                val password = it.password
                alcoholicDaoFacade.findByLogin(login)
                    .switchIfEmpty {
                        EntityNotFoundException(
                            message = "No alcoholic found with login $login",
                            payload = mapOf("login" to login)
                        ).toMono()
                    }
                    .flatMap { alcoholicEntity ->
                        passwordService.comparePasswords(password, alcoholicEntity.password)
                            .flatMap {
                                if (!it) {
                                    return@flatMap Mono.error(RuntimeException("Password does not match"))
                                }
                                val sessionId = UUID.randomUUID()
                                val alcoholicId = alcoholicEntity.id
                                val accessToken = tokenService.createAccessToken(alcoholicId, sessionId)
                                val refreshToken = tokenService.createRefreshToken(alcoholicId, sessionId)
                                val refreshTokenCookie = tokenService.createRefreshTokenCookie(refreshToken)
                                val session = SessionEntity(id = sessionId, alcoholicId = alcoholicId)
                                val sessionToRefresh = SessionToRefreshEntity(UUID.randomUUID(), sessionId)
                                sessionDaoFacade.insert(session)
                                    .flatMap { sessionToRefreshDaoFacade.insert(sessionToRefresh) }
                                    .map { AuthResponse(AccessTokenDto(accessToken), refreshTokenCookie) }
                            }
                    }
            }
    }

    fun logout(jwt: String): Mono<Int> {
        return tokenService.validateToken(jwt)
            .flatMap { tokenService.getSession(it) }
            .flatMap { sessionValidationService.validateSessionIsActive(it) }
            .flatMap { sessionToRefreshDaoFacade.invalidateRefreshToken(it) }
            .flatMap { sessionDaoFacade.invalidateSession(it) }
    }

}