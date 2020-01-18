package org.horiga.study.springframework.errorhandler

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import javax.servlet.http.HttpServletRequest

data class From(
    val userAgent: String?,
    val ip: String?,
    val requestId: String?
)

class FromArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        From::class.java.isAssignableFrom(parameter.parameterType)

    @Throws(Exception::class)
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? = try {
        (webRequest.nativeRequest as? HttpServletRequest)?.let {
            // for the test code
            it.getHeader("X-Request-Id")?.takeIf { requestId -> requestId == "error" }?.run {
                throw IllegalArgumentException("Request ID must be specified")
            }
            From(
                it.getHeader("User-Agent") ?: "<undefined>",
                it.remoteAddr ?: "",
                it.getHeader("X-Request-Id") ?: "<undefined>"
            )
        }
    } catch (e: Exception) {
        when (e) {
            is IllegalArgumentException -> throw e
            else -> null
        }
    }
}