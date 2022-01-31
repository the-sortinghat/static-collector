package com.erickrodrigues.staticcollector.application.controllers

import com.erickrodrigues.staticcollector.application.http.requests.RegisterSystemRequest
import com.erickrodrigues.staticcollector.application.http.responses.RegisterSystemResponse
import com.erickrodrigues.staticcollector.domain.converters.ConverterToDomain
import com.erickrodrigues.staticcollector.domain.entities.ServiceBasedSystem
import com.erickrodrigues.staticcollector.domain.entities.SpecificTechnology
import com.erickrodrigues.staticcollector.domain.exceptions.UnableToFetchDataException
import com.erickrodrigues.staticcollector.domain.exceptions.UnableToParseDataException
import com.erickrodrigues.staticcollector.domain.factories.ExtractionComponentsAbstractFactory
import com.erickrodrigues.staticcollector.domain.fetchers.DataFetcher
import com.erickrodrigues.staticcollector.domain.fetchers.FetchResponse
import com.erickrodrigues.staticcollector.domain.parsers.DataParser
import com.erickrodrigues.staticcollector.domain.ports.MessageBroker
import com.erickrodrigues.staticcollector.domain.ports.ServiceBasedSystemRepository
import com.erickrodrigues.staticcollector.domain.usecases.ExtractDataUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.http.ResponseEntity

@DisplayName("SystemController")
class SystemControllerTest {

    @Nested
    @DisplayName("POST /systems")
    inner class PostSystems {

        @Nested
        @DisplayName("when request is successful")
        inner class SuccessfulRequest {
            private lateinit var response: ResponseEntity<RegisterSystemResponse>

            @BeforeEach
            fun init() {
                val system = ServiceBasedSystem("my-system")
                val res = FetchResponse("my-system", "")
                val extractDataUseCase = mock(ExtractDataUseCase::class.java)
                val specificTechnology = mock(SpecificTechnology::class.java)
                val factory = mock(ExtractionComponentsAbstractFactory::class.java)
                val fetcher = mock(DataFetcher::class.java)
                val parser = mock(DataParser::class.java)
                val converter = mock(ConverterToDomain::class.java)
                val repo = mock(ServiceBasedSystemRepository::class.java)
                val broker = mock(MessageBroker::class.java)
                val controller = SystemController(factory)
                val request = RegisterSystemRequest("https://foo.com/bar")

                `when`(fetcher.run(anyString())).thenReturn(res)
                `when`(parser.run(res)).thenReturn(specificTechnology)
                `when`(converter.run(specificTechnology)).thenReturn(system)
                `when`(factory.createDataFetcher()).thenReturn(fetcher)
                `when`(factory.createDataParser()).thenReturn(parser)
                `when`(factory.createConverterToDomain()).thenReturn(converter)
                `when`(factory.createServiceBasedSystemRepository()).thenReturn(repo)
                `when`(factory.createMessageBroker()).thenReturn(broker)
                `when`(extractDataUseCase.run(anyString())).thenReturn(system)

                response = controller.registerSystem(request)
            }

            @Test
            fun `it returns a created status code`() {
                val status = response.statusCodeValue
                assertEquals(201, status)
            }

            @Test
            fun `it returns a non-null response`() {
                val body = response.body
                assertNotNull(body)
            }

            @Test
            fun `it returns a saved system`() {
                val body = response.body
                assertEquals("my-system", body!!.name)
            }
        }

        @Nested
        @DisplayName("when request fails")
        inner class FailedRequest {
            private lateinit var response: ResponseEntity<RegisterSystemResponse>
            private lateinit var factory: ExtractionComponentsAbstractFactory
            private lateinit var fetcher: DataFetcher
            private lateinit var parser: DataParser
            private lateinit var converter: ConverterToDomain
            private lateinit var repo: ServiceBasedSystemRepository

            @BeforeEach
            fun init() {
                converter = mock(ConverterToDomain::class.java)
                repo = mock(ServiceBasedSystemRepository::class.java)
                fetcher = mock(DataFetcher::class.java)
                parser = mock(DataParser::class.java)
                factory = mock(ExtractionComponentsAbstractFactory::class.java)
                `when`(factory.createDataFetcher()).thenReturn(fetcher)
                `when`(factory.createDataParser()).thenReturn(parser)
                `when`(factory.createConverterToDomain()).thenReturn(converter)
                `when`(factory.createServiceBasedSystemRepository()).thenReturn(repo)
            }

            @Nested
            @DisplayName("when collector does not find the system data")
            inner class NotFound {

                @BeforeEach
                fun init() {
                    val controller = SystemController(factory)
                    val request = RegisterSystemRequest("https://foo.com/bar")

                    `when`(fetcher.run(anyString())).thenAnswer { throw UnableToFetchDataException("") }

                    response = controller.registerSystem(request)
                }

                @Test
                fun `it returns a not found status code`() {
                    val status = response.statusCodeValue
                    assertEquals(404, status)
                }
            }

            @Nested
            @DisplayName("when collector cannot parse the system data")
            inner class BadRequestFail {

                @BeforeEach
                fun init() {
                    val controller = SystemController(factory)
                    val request = RegisterSystemRequest("https://foo.com/bar")
                    val fetchResponse = FetchResponse("foo", "bar")

                    `when`(fetcher.run(anyString())).thenReturn(fetchResponse)
                    `when`(parser.run(fetchResponse)).thenAnswer { throw UnableToParseDataException("") }

                    response = controller.registerSystem(request)
                }

                @Test
                fun `it returns a bad request status code`() {
                    val status = response.statusCodeValue
                    assertEquals(400, status)
                }
            }

            @Nested
            @DisplayName("when system with that name already exists")
            inner class ConflictFail {

                @BeforeEach
                fun init() {
                    val controller = SystemController(factory)
                    val request = RegisterSystemRequest("https://foo.com/bar")
                    val fetchResponse = FetchResponse("foo", "bar")
                    val specificTechnology = mock(SpecificTechnology::class.java)

                    `when`(fetcher.run(anyString())).thenReturn(fetchResponse)
                    `when`(parser.run(fetchResponse)).thenReturn(specificTechnology)
                    `when`(converter.run(specificTechnology)).thenReturn(ServiceBasedSystem("bar"))
                    `when`(repo.findByName(anyString())).thenReturn(ServiceBasedSystem("bar"))

                    response = controller.registerSystem(request)
                }

                @Test
                fun `it returns a conflict status code`() {
                    val status = response.statusCodeValue
                    assertEquals(409, status)
                }
            }
        }
    }
}