package com.erickrodrigues.staticcollector.application.controllers

import com.erickrodrigues.staticcollector.application.factories.GetSystemUseCaseFactory
import com.erickrodrigues.staticcollector.application.http.requests.RegisterSystemRequest
import com.erickrodrigues.staticcollector.application.http.responses.RegisterSystemResponse
import com.erickrodrigues.staticcollector.domain.exceptions.EntityAlreadyExistsException
import com.erickrodrigues.staticcollector.domain.exceptions.EntityNotFoundException
import com.erickrodrigues.staticcollector.domain.exceptions.UnableToFetchDataException
import com.erickrodrigues.staticcollector.domain.exceptions.UnableToParseDataException
import com.erickrodrigues.staticcollector.domain.factories.ExtractionComponentsAbstractFactory
import com.erickrodrigues.staticcollector.domain.usecases.ExtractDataUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/systems")
class SystemController(
    private val extractionFactory: ExtractionComponentsAbstractFactory,
    private val getSystemUseCaseFactory: GetSystemUseCaseFactory
) {

    @GetMapping("/{id}")
    fun getSystem(@PathVariable id: String) =
        try {
            val getSystemUseCase = getSystemUseCaseFactory.create()
            val system = getSystemUseCase.run(id)
            ResponseEntity(RegisterSystemResponse(system), HttpStatus.OK)
        } catch (e: Exception) {
            handleExceptions(e)
        }

    @PostMapping
    fun registerSystem(@RequestBody request: RegisterSystemRequest) =
        try {
            val extractDataUseCase = ExtractDataUseCase(extractionFactory)
            val system = extractDataUseCase.run(request.repoUrl)
            ResponseEntity(RegisterSystemResponse(system), HttpStatus.CREATED)
        } catch (e: Exception) {
            handleExceptions(e)
        }

    private fun <T> handleExceptions(e: Exception): ResponseEntity<T> {
        val mapError = MultiValueMapAdapter(mapOf("error" to listOf(e.message)))
        return when (e) {
            is UnableToFetchDataException -> ResponseEntity<T>(mapError, HttpStatus.NOT_FOUND)
            is UnableToParseDataException -> ResponseEntity<T>(mapError, HttpStatus.BAD_REQUEST)
            is EntityAlreadyExistsException -> ResponseEntity<T>(mapError, HttpStatus.CONFLICT)
            is EntityNotFoundException -> ResponseEntity<T>(mapError, HttpStatus.NOT_FOUND)
            else -> {
                val internalError = MultiValueMapAdapter(mapOf("error" to listOf("Internal Server Error")))
                ResponseEntity<T>(internalError, HttpStatus.INTERNAL_SERVER_ERROR)
            }
        }
    }
}
