package ru.buhinder.alcoholicservice.controller

import java.security.Principal
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.buhinder.alcoholicservice.dto.response.AlcoholicResponse
import ru.buhinder.alcoholicservice.dto.response.IdResponse
import ru.buhinder.alcoholicservice.service.AlcoholicService
import ru.buhinder.alcoholicservice.util.toUUID

@RestController
@RequestMapping("/api/alcoholic/alcoholic")
class AlcoholicController(
    private val alcoholicService: AlcoholicService,
) {

    @GetMapping("/email/{email}")
    fun get(@PathVariable email: String): Mono<AlcoholicResponse> {
        return alcoholicService.get(email)
    }

    @GetMapping("/own")
    fun getOwnInfo(principal: Principal): Mono<AlcoholicResponse> {
        return alcoholicService.get(principal.name.toUUID())
    }

    @GetMapping("/{alcoholicId}")
    fun getById(@PathVariable alcoholicId: UUID): Mono<AlcoholicResponse> {
        return alcoholicService.get(alcoholicId)
    }

    @PostMapping("/image")
    fun addImage(
        principal: Principal,
        @RequestPart(value = "image") image: Mono<FilePart>,
    ): Mono<IdResponse> {

        return image
            .flatMap { alcoholicService.addNewAlcoholicImage(principal.name.toUUID(), it) }
            .map { IdResponse(it) }
    }

    @PutMapping("/image")
    fun updateImage(
        principal: Principal,
        @RequestPart(value = "image") image: Mono<FilePart>
    ): Mono<IdResponse> {

        return image
            .flatMap { alcoholicService.updateAlcoholicImage(principal.name.toUUID(), it) }
            .map { IdResponse(it) }
    }

    @DeleteMapping("/image")
    @ResponseStatus(HttpStatus.OK)
    fun deleteImage(
        principal: Principal
    ): Mono<Void> {

        return alcoholicService.deleteAlcoholicImage(principal.name.toUUID())
            .then()
    }


}
