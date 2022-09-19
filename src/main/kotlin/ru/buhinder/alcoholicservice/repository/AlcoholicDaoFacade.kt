package ru.buhinder.alcoholicservice.repository

import java.util.UUID
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.CriteriaDefinition
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.buhinder.alcoholicservice.config.LoggerDelegate
import ru.buhinder.alcoholicservice.controller.advice.exception.EntityNotFoundException
import ru.buhinder.alcoholicservice.entity.AlcoholicEntity

@Repository
class AlcoholicDaoFacade(
    private val r2dbcEntityOperations: R2dbcEntityOperations,
) {
    private val logger by LoggerDelegate()

    fun insert(entity: AlcoholicEntity): Mono<AlcoholicEntity> {
        return Mono.just(logger.info("Trying to save AlcoholicEntity with id ${entity.id}"))
            .flatMap { r2dbcEntityOperations.insert(entity) }
            .doOnNext { logger.info("Saved AlcoholicEntity with id ${entity.id}") }
            .doOnError { logger.info("Error saving AlcoholicEntity with id ${entity.id}") }

    }

    fun findByLogin(login: String): Mono<AlcoholicEntity> {
        return Mono.just(logger.info("Trying to find AlcoholicEntity by login"))
            .flatMap {
                r2dbcEntityOperations.selectOne(
                    Query.query(CriteriaDefinition.from(Criteria.where("login").`is`(login))),
                    AlcoholicEntity::class.java
                )
            }
            .doOnNext { logger.info("Found AlcoholicEntity by login") }
            .doOnError { logger.info("Error retrieving AlcoholicEntity by login") }
    }

    fun existsByLogin(login: String): Mono<Boolean> {
        return Mono.just(logger.info("Trying to check existence AlcoholicEntity by email"))
            .flatMap {
                r2dbcEntityOperations.exists(
                    Query.query(CriteriaDefinition.from(Criteria.where("login").`is`(login))),
                    AlcoholicEntity::class.java
                )
            }
            .doOnNext { logger.info("Found AlcoholicEntity by login") }
            .doOnError { logger.info("Error retrieving AlcoholicEntity by login") }
    }

    fun existsByEmail(email: String): Mono<Boolean> {
        return Mono.just(logger.info("Trying to check existence AlcoholicEntity by email"))
            .flatMap {
                r2dbcEntityOperations.exists(
                    Query.query(CriteriaDefinition.from(Criteria.where("email").`is`(email))),
                    AlcoholicEntity::class.java
                )
            }
            .doOnNext { logger.info("Found AlcoholicEntity by login") }
            .doOnError { logger.info("Error retrieving AlcoholicEntity by login") }
    }

    fun getByEmail(email: String): Mono<AlcoholicEntity> {
        return Mono.just(logger.info("Trying to find AlcoholicEntity by email"))
            .flatMap {
                r2dbcEntityOperations.selectOne(
                    Query.query(CriteriaDefinition.from(Criteria.where("email").`is`(email))),
                    AlcoholicEntity::class.java
                )
            }
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = "Alcoholic not found",
                        payload = mapOf("email" to email)
                    )
                )
            }
            .doOnNext { logger.info("Found AlcoholicEntity by email") }
            .doOnError { logger.info("Error retrieving AlcoholicEntity by email") }
    }

    fun getById(id: UUID): Mono<AlcoholicEntity> {
        return Mono.just(logger.info("Trying to find AlcoholicEntity by id $id"))
            .flatMap {
                r2dbcEntityOperations.selectOne(
                    Query.query(CriteriaDefinition.from(Criteria.where("id").`is`(id))),
                    AlcoholicEntity::class.java
                )
            }
            .switchIfEmpty {
                Mono.error(
                    EntityNotFoundException(
                        message = "Alcoholic not found",
                        payload = mapOf("id" to id)
                    )
                )
            }
            .doOnNext { logger.info("Found AlcoholicEntity by id $id") }
            .doOnError { logger.info("Error retrieving AlcoholicEntity by id $id") }
    }

    fun findById(id: UUID): Mono<AlcoholicEntity> {
        return r2dbcEntityOperations.selectOne(
            Query.query(CriteriaDefinition.from(Criteria.where("id").`is`(id))),
            AlcoholicEntity::class.java
        )
    }

    fun existsImageByAlcoholicId(id: UUID): Mono<Boolean> {
        return r2dbcEntityOperations.exists(
            Query.query(
                CriteriaDefinition.from(
                    Criteria.where("id").`is`(id)
                        .and("photo_id").isNotNull
                )
            ),
            AlcoholicEntity::class.java
        )
    }

    fun update(alcoholicEntity: AlcoholicEntity) = r2dbcEntityOperations.update(alcoholicEntity)

}
