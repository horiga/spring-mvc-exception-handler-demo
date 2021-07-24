package org.horiga.study.springframework.errorhandler

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import javax.validation.ConstraintViolationException
import kotlin.reflect.KClass

@SpringBootTest
@AutoConfigureMockMvc
class ErrorHandlersTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `PathVariable validation failed`() {
        mockMvc.perform(get("/api/books/hoge"))
            .andExpect(status().is4xxClientError)
            .andExpect(resultMatcher(ConstraintViolationException::class))
    }



    @Test
    fun `RequestParam type mismatched failed`() {
        mockMvc.perform(get("/api/books").param("price", "evil"))
            .andExpect(status().is4xxClientError)
            .andExpect(resultMatcher(MethodArgumentTypeMismatchException::class))
    }

    @Test
    fun `RequestBody with Valid failed`() {
        mockMvc.perform(
            post("/api/books")
                .content(
                    ObjectMapper().writeValueAsString(
                        mapOf(
                            "isbn" to "9784777519699",
                            "title" to "",
                            "price" to 2750,
                            "priority" to 6
                        )
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError)
            .andExpect(resultMatcher(MethodArgumentNotValidException::class))
    }

    @Test
    fun `RequestBody validate non-nullable failed - Kotlin`() {

        mockMvc.perform(
            post("/api/books")
                .content(
                    ObjectMapper().writeValueAsString(
                        mapOf(
                            "isbn" to "9784777519699",
                            "price" to 2750,
                            "priority" to 6
                        )
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.error_message").value("'title' must not be null"))
            .andExpect(resultMatcher(HttpMessageNotReadableException::class))
    }

    @Test
    fun `Request parse failed`() {
        mockMvc.perform(
            post("/api/books")
                .content("{{")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().is4xxClientError)
            .andExpect(
                jsonPath("$.error_message").value(
                    "JSON parse error: Unexpected character ('{' (code 123)): was expecting double-quote to start field name\n at [Source: (PushbackInputStream); line: 1, column: 3]"
                )
            )
            .andExpect(resultMatcher(HttpMessageNotReadableException::class))
    }

    @Test
    fun `RequestParam with bean validation failed`() {
        mockMvc.perform(get("/api/greetings").param("m", "this-is-a-evil"))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.error_message").value("length of query 'm' must be less than or equal to 10"))
            .andExpect(resultMatcher(ConstraintViolationException::class))
    }

    @Test
    fun `ArgumentResolvers validation failed`() {
        mockMvc.perform(
            get("/api/greetings")
                .param("m", "hello")
                .header("X-Request-Id", "error")
        )
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.error_message").value("Request ID must be specified"))
            .andExpect(resultMatcher(IllegalArgumentException::class))
    }

    @Test
    fun `PathVariable type mismatched failed`() {
        mockMvc.perform(get("/api/greetings/foo"))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.error_message").value("'id' value of type must be 'int'. For input string: \"foo\""))
            .andExpect(resultMatcher(MethodArgumentTypeMismatchException::class))
    }

    private fun resultMatcher(klass: KClass<*>): ResultMatcher =
        jsonPath("$.error_type").value(klass.java.simpleName)
}