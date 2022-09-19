package ru.buhinder.alcoholicservice.controller

import java.util.UUID
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import ru.buhinder.alcoholicservice.service.AlcoholicService
import ru.buhinder.alcoholicservice.service.ImageService

@RestController
@RequestMapping("/api/alcoholic/image")
class ImageController(
    private val imageService: ImageService,
    private val alcoholicService: AlcoholicService,
) {

    @GetMapping("/{imageId}", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun getImage(@PathVariable imageId: UUID): Flux<DataBuffer> {
        return imageService.getImage(imageId)
    }
}