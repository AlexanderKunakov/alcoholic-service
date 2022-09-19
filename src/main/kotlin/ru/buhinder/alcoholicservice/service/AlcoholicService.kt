package ru.buhinder.alcoholicservice.service

import java.util.UUID
import org.springframework.core.convert.ConversionService
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import ru.buhinder.alcoholicservice.dto.response.AlcoholicResponse
import ru.buhinder.alcoholicservice.repository.AlcoholicDaoFacade
import ru.buhinder.alcoholicservice.service.validation.ImageValidationService

@Service
class AlcoholicService(
    private val alcoholicDaoFacade: AlcoholicDaoFacade,
    private val conversionService: ConversionService,
    private val imageService: ImageService,
    private val imageValidationService: ImageValidationService,
) {

    fun get(email: String): Mono<AlcoholicResponse> {
        return alcoholicDaoFacade.getByEmail(email)
            .map { conversionService.convert(it, AlcoholicResponse::class.java)!! }
    }

    fun get(alcoholicId: UUID): Mono<AlcoholicResponse> {
        return alcoholicDaoFacade.getById(alcoholicId)
            .map { conversionService.convert(it, AlcoholicResponse::class.java)!! }
    }

    fun addNewAlcoholicImage(alcoholicId: UUID, image: FilePart): Mono<UUID> {

        return imageValidationService.validateUserImageNotExists(alcoholicId)
            .zipWith(imageValidationService.validateImageFormat(image))
            .flatMap { imageService.saveImage(it.t2) }
            .zipWith(alcoholicDaoFacade.findById(alcoholicId))
            .flatMap { alcoholicDaoFacade.update(it.t2.copy(photoId = it.t1)) }
            .mapNotNull { it.photoId }
    }

    fun deleteAlcoholicImage(alcoholicId: UUID): Mono<Void> {
        return alcoholicId.toMono()
            .flatMap(imageValidationService::validateUserImageExists)
            .flatMap { alcoholicDaoFacade.findById(it) }
            .doOnNext { imageService.deleteImage(it.photoId!!).subscribe() }
            .flatMap { alcoholicDaoFacade.update(it.copy(photoId = null)) }
            .then()
    }

    fun updateAlcoholicImage(alcoholicId: UUID, image: FilePart): Mono<UUID> {

        return alcoholicId.toMono()
            .flatMap(imageValidationService::validateUserImageExists)
            .zipWith(imageValidationService.validateImageFormat(image))
            .flatMap { alcoholicDaoFacade.findById(it.t1) }
            .doOnNext { imageService.updateImage(it.photoId!!, image).subscribe() }
            .map { it.photoId!! }
    }

}
