package org.horiga.study.springframework.errorhandler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@SpringBootApplication
class SpringMvcExceptionHandlerDemoApplication {
}

// org.jetbrains.kotlin.plugin.noarg
annotation class NoArgs

fun main(args: Array<String>) {
    runApplication<SpringMvcExceptionHandlerDemoApplication>(*args)
}

@Configuration
class DemoWebMvcConfigurer : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(FromArgumentResolver())
    }
}