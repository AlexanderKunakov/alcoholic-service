package ru.buhinder.alcoholicservice.controller.advice

import io.minio.errors.ErrorResponseException
import java.util.StringJoiner
import java.util.stream.Collectors
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.server.ServerWebInputException
import ru.buhinder.alcoholicservice.config.LoggerDelegate
import ru.buhinder.alcoholicservice.controller.advice.dto.AlcoholicErrorCode.VALIDATION_ERROR
import ru.buhinder.alcoholicservice.controller.advice.dto.ErrorInfoDto
import ru.buhinder.alcoholicservice.controller.advice.dto.MinioErrorCode
import ru.buhinder.alcoholicservice.controller.advice.exception.AlcoholicApiException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger by LoggerDelegate()

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(exception: WebExchangeBindException): ResponseEntity<ErrorInfoDto> {
        val messageJoiner = StringJoiner("; ")
        val bindingResult = exception.bindingResult
        if (bindingResult.hasGlobalErrors()) {
            val globalErrorsMessage = bindingResult.globalErrors.stream()
                .map { String.format("%s: %s", it.objectName, it.defaultMessage) }
                .collect(Collectors.joining("; "))
            messageJoiner.add(globalErrorsMessage)
        }
        if (bindingResult.hasFieldErrors()) {
            val fieldErrorsMessage = bindingResult.fieldErrors.stream()
                .map { String.format("Field '%s': %s", it.field, it.defaultMessage) }
                .collect(Collectors.joining("; "))
            messageJoiner.add(fieldErrorsMessage)
        }
        val apiErrorDto = ErrorInfoDto(
            code = VALIDATION_ERROR,
            message = messageJoiner.toString(),
            payload = emptyMap()
        )
        return ResponseEntity.badRequest().body(apiErrorDto)
    }

    @ExceptionHandler(ServerWebInputException::class)
    fun handleWebInputException(exception: ServerWebInputException): ResponseEntity<ErrorInfoDto> {
        logger.error("Got error: ", exception)

        val apiErrorDto = ErrorInfoDto(
            code = VALIDATION_ERROR,
            message = exception.cause?.message,
            payload = emptyMap()
        )
        return ResponseEntity.status(exception.status)
            .body(apiErrorDto)
    }

    @ExceptionHandler(AlcoholicApiException::class)
    fun handleCommonException(exception: AlcoholicApiException): ResponseEntity<ErrorInfoDto> {
        val apiErrorDto = ErrorInfoDto(exception)
        val status = exception.responseStatus
        return ResponseEntity.status(status)
            .body(apiErrorDto)
    }

    @ExceptionHandler(WebClientException::class)
    fun handleWebClientException(exception: WebClientException): ResponseEntity<ErrorInfoDto> {
        val apiErrorDto = ErrorInfoDto(exception)
        return ResponseEntity.status(SERVICE_UNAVAILABLE)
            .body(apiErrorDto)
    }

    @ExceptionHandler(ErrorResponseException::class)
    fun handleMinioException(exception: ErrorResponseException): ResponseEntity<ErrorInfoDto> {
        logger.error("Thrown exception: ", exception)
        val apiErrorDto = ErrorInfoDto(
            code = MinioErrorCode.KEY_DOES_NOT_EXIST,
            message = exception.errorResponse().message(),
            payload = mapOf("key" to exception.errorResponse().objectName())
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(apiErrorDto)
    }

    @ExceptionHandler(Exception::class)
    fun handleCommonException(exception: Exception): ResponseEntity<ErrorInfoDto> {
        logger.error("Thrown exception: ", exception)

        val apiErrorDto = ErrorInfoDto(exception)
        return ResponseEntity.status(INTERNAL_SERVER_ERROR)
            .body(apiErrorDto)
    }
}
