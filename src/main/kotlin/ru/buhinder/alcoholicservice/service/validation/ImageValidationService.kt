package ru.buhinder.alcoholicservice.service.validation

import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcoholicservice.controller.advice.exception.EntityAlreadyExistsException
import ru.buhinder.alcoholicservice.controller.advice.exception.EntityCannotBeCreatedException
import ru.buhinder.alcoholicservice.controller.advice.exception.EntityNotFoundException
import ru.buhinder.alcoholicservice.repository.AlcoholicDaoFacade

@Service
class ImageValidationService(
    private val alcoholicDaoFacade: AlcoholicDaoFacade,
) {

    fun validateImageFormat(image: FilePart): Mono<FilePart> {

        return image.toMono()
            .filter {
                val contentType = it.headers().contentType
                contentType != null && contentType == MediaType.IMAGE_JPEG
            }
            .switchIfEmpty(
                Mono.error(
                    EntityCannotBeCreatedException(
                        message = "Invalid media type. Only .jpeg files are allowed",
                        payload = emptyMap()
                    )
                )
            )
    }

    fun validateUserImageNotExists(alcoholicId: UUID): Mono<UUID> {
        return alcoholicDaoFacade.existsImageByAlcoholicId(alcoholicId)
            .filter { it.not() }
            .map { alcoholicId }
            .switchIfEmpty(
                Mono.error(
                    EntityAlreadyExistsException(
                        message = "Photo already exists",
                        payload = emptyMap()
                    )
                )
            )
    }

    fun validateUserImageExists(alcoholicId: UUID): Mono<UUID> {
        return alcoholicDaoFacade.existsImageByAlcoholicId(alcoholicId)
            .filter { it }
            .map { alcoholicId }
            .switchIfEmpty(
                Mono.error(
                    EntityNotFoundException(
                        message = "Photo does not exist",
                        payload = emptyMap()
                    )
                )
            )
    }
}