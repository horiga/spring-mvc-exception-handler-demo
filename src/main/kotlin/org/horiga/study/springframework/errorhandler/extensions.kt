package org.horiga.study.springframework.errorhandler

import javax.servlet.http.HttpServletRequest

fun HttpServletRequest.stringify(multiline: Boolean = false): String {
    val lines = mutableListOf<String>()
    val q: String? = this.queryString
    if (!q.isNullOrBlank()) {
        lines.add("${this.method} ${this.servletPath}?$q  ")
    } else {
        lines.add("${this.method} ${this.servletPath} ")
    }
    this.headerNames.toList().map {
        lines.add("$it:${this.getHeaders(it).toList().joinToString(", ")}")
    }
    return lines.joinToString(if (multiline) "\n" else " ")
}