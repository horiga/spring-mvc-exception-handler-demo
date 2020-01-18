package org.horiga.study.springframework.errorhandler.api

import org.apache.commons.lang3.RandomStringUtils
import org.hibernate.validator.constraints.ISBN
import org.horiga.study.springframework.errorhandler.From
import org.horiga.study.springframework.errorhandler.OrderPriority
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Min
import javax.validation.constraints.Size
import kotlin.random.Random

data class Book(
    @field:Size(max = 13)
    // @field:ISBN
    val isbn: String,
    @field:Size(min = 1, max = 50)
    val title: String,
    @field:Min(0)
    val price: Int,
    @field:OrderPriority
    val priority: Int
) {
    companion object {
        const val MIN_PRIORITY = 1
        const val MAX_PRIORITY = 5
    }
}

@RestController
@RequestMapping("api/books")
@Validated
class BooksRestController {

    companion object {
        // Will use opendb api: https://api.openbd.jp/v1/get?isbn=9784297109059
        val openDb = "https://api.openbd.jp"
        val log = LoggerFactory.getLogger(BooksRestController::class.java)!!
        fun genIsbn() = "${RandomStringUtils.randomNumeric(
            3,
            3
        )}-${RandomStringUtils.randomNumeric(1)}-${RandomStringUtils.randomNumeric(
            6,
            6
        )}-${RandomStringUtils.randomNumeric(1)}"
    }

    @GetMapping
    fun searchBooks(
        @RequestParam(value = "isbn", required = false) isbn: String?,
        @RequestParam(value = "title", required = false, defaultValue = "") title: String?,
        @RequestParam(value = "price", required = false, defaultValue = "0") price: Int?
    ): List<Book> {
        return listOf(
            Book(genIsbn(), "Foo books", 580, Book.MAX_PRIORITY),
            Book(genIsbn(), "Foo books", 580, Random.nextInt(5))
        )
    }

    @GetMapping("{isbn}")
    fun getBook(
        @PathVariable("isbn") @Valid @ISBN isbn: String
    ) = Book(isbn, "Foo books", 580, Book.MAX_PRIORITY)

    @PostMapping
    fun addBook(
        @Valid @RequestBody book: Book
    ): Book {
        log.info("Received: books: $book")
        return book
    }
}

@RestController
@RequestMapping("api/greetings")
@Validated
class GreetingsRestController {

    companion object {
        val log = LoggerFactory.getLogger(GreetingsRestController::class.java)!!
    }

    @GetMapping
    fun hello(
        @RequestParam(value = "m", required = false, defaultValue = "hello")
        @Valid @Size(max = 10, message = "length of query 'm' must be less than or equal to {max}")
        message: String,
        from: From
    ): String {
        return message
    }

    @GetMapping("{id}")
    fun mustPathVariableTypeMatches(
        // handle `MethodArgumentTypeMismatchException` if path variable is not numeric values.
        @PathVariable("id") identifier: Int
    ): Int {
        return identifier
    }
}