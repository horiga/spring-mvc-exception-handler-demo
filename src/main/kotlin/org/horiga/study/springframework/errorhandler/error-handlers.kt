package org.horiga.study.springframework.errorhandler

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import javax.servlet.http.HttpServletRequest
import javax.validation.ConstraintViolationException

data class ErrorResponse(
    val status: Int,
    @JsonProperty("error_message")
    val message: String,
    @JsonProperty("error_type")
    val exception: String
)

@RestControllerAdvice(basePackages = ["org.horiga.study.springframework.errorhandler.api"])
class DemoRestControllerAdvice {

    companion object {
        val log = LoggerFactory.getLogger(DemoRestControllerAdvice::class.java)!!
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ErrorResponse = when (val cause = e.cause) {
        is MissingKotlinParameterException -> {
            // handle RequestBody and Valid non nullable request parameter, with out @NotNull
            errorResponse(e, request, HttpStatus.BAD_REQUEST, "'${cause.parameter.name}' must not be null")
        }
        is JsonParseException -> {
            // handle illegal json formatted request body.
            errorResponse(e, request, HttpStatus.BAD_REQUEST, "JSON parse error: ${cause.message}")
        }
        else -> errorResponse(e, request, HttpStatus.BAD_REQUEST, e.message)
    }

    @ExceptionHandler(
        ConstraintViolationException::class, // for @PathVariable, Request-mapping methods arguments,
        MethodArgumentNotValidException::class, // for @RequestBody with @Valid
        BindException::class,
        ServletRequestBindingException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        MissingServletRequestPartException::class,
        HttpMessageConversionException::class,
        HttpMediaTypeNotSupportedException::class, // unsupported content-type
        IllegalArgumentException::class
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequestExceptions(
        e: Exception,
        request: HttpServletRequest
    ): ErrorResponse {
        val message = when (e) {
            // RequestBody + @Valid の validation fail. ex: @NotEmpty, @Min, @Size...
            is MethodArgumentNotValidException -> {
                e.bindingResult.fieldErrors.joinToString(", ") {
                    "'${it.field}' value as '${it.rejectedValue}' rejected. ${it.defaultMessage}"
                }
            }
            is BindException -> {
                e.fieldErrors.joinToString(",") {
                    "'${it.field}' value as '${it.rejectedValue}' rejected. ${it.defaultMessage}"
                }
            }
            // PathVariable + Valid などで validation fail になる場合に発生
            is ConstraintViolationException -> {
                e.constraintViolations.joinToString(", ") { it.message }
            }
            is MethodArgumentTypeMismatchException -> {
                val typeName = e.requiredType?.let {
                    if (it.isPrimitive) it.name else it.simpleName
                } ?: "<unknown>"
                "'${e.name}' value of type must be '${typeName.toLowerCase()}'. ${e.cause?.message}"
            }
            else -> e.message
        }
        return errorResponse(e, request, HttpStatus.BAD_REQUEST, message ?: "Bad Request")
    }

    @ExceptionHandler(NoSuchElementException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNoSuchElementException(
        e: NoSuchElementException,
        request: HttpServletRequest
    ): ErrorResponse {
        return errorResponse(e, request, HttpStatus.NOT_FOUND, e.message)
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleAnonymousException(
        e: Exception,
        request: HttpServletRequest
    ): ErrorResponse {
        return errorResponse(e, request, HttpStatus.INTERNAL_SERVER_ERROR, e.message)
    }

    private fun errorResponse(
        e: Exception,
        request: HttpServletRequest,
        status: HttpStatus,
        message: String?
    ): ErrorResponse {
        log.warn("[handle errors] ${request.stringify()} : status:$status, message:$message", e)
        return ErrorResponse(status.value(), message ?: "", e.javaClass.simpleName)
    }
}