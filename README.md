spring-mvc-exception-handler-demo
---



```Kotlin
    @NoArgs
    data class RequestMessage(
        @field:NotBlank
        @field:NotNull
        @field:Size(max = 100)
        val name: String,

        // Nullable with @NotNull, @NotBlank
        @field:NotBlank
        @field:NotNull
        @field:Size(max = 200)
        val nickname: String?,

        @field:NotBlank
        @field:Size(max = 300)
        val message: String?,

        @field:NotNull
        @field:Max(1000)
        val num1: Int?,

        // NonNull with default value
        val num2: Int = 10,

        @field:Valid
        @field:NotEmpty
        @field:NotNull
        @field:Size(max = 10)
        val tags: Collection<@Pattern(regexp = "^[0-9a-zA-Z]{2,10}") String>?
)
```

**Case1:**

 `val name: String` NonNull な property に対して`"name"` を指定しないでメッセージを送る場合、`String?`  と`@NotNull` どちらが優先されるのか？どういう振る舞い？

```bash
// without `name`
$ curl -iks -X POST http://localhost:8080/api/validations -H 'Content-Type:application/json' -d '{"nickname":"horiga", "message":"Hello", "num1":10, "num2":5, "tags":["developer"]}'
->
{"status":400,"error_message":"'name' must not be null","error_type":"HttpMessageNotReadableException"}%

// without `nickname`
$ curl -iks -X POST http://localhost:8080/api/validations -H 'Content-Type:application/json' -d '{"name":"Hiroyuki Horigami", "message":"Hello", "num1":10, "num2":5, "tags":["developer"]}'
->
{"status":400,"error_message":"'nickname' value as 'null' rejected. must not be null, 'nickname' value as 'null' rejected. must not be blank","error_type":"MethodArgumentNotValidException"}%
```

結果をみると発生する Exception が異なる

**結果**

`NonNull(val name :String)` の validation は `HttpMessageNotReadableException` が発生するが、`Nullable(val nickname: String?)かつ@javax.validation.constraints.NotNull` の検証では、`MethodArgumentNotValidException` が発生する。

エラーハンドリングするときに、上記の区別は特に必要ないとは思うが発生するExceptionがことなるので注意が必要。

補足すると、JSON Parse に失敗するメッセージの場合も `HttpMessageNotReadableException` が発生する。今回の例では以下のようにハンドリングしている

```kotlin
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

```



**Case2:**

`@NotBlank`のpropertyの場合

```bash
$ curl -iks -X POST http://localhost:8080/api/validations -H 'Content-Type:application/json' -d '{"name":"Hiroyuki Horigami", "nickname":"horiga", "message":null, "num1":10, "num2":5, "tags":["developer"]}'
->
{"status":400,"error_message":"'message' value as 'null' rejected. must not be blank","error_type":"MethodArgumentNotValidException"}%
```

これは、想定どおり、`val message: String?`だけど、`@NotBlank` が着いているのでvalidtionエラーとなる。そのため、空白文字かつnullもだめなら `@NotBlank` だけで良いと思う。



**その他検証**

```Kotlin
        @field:Valid
        @field:NotEmpty
        @field:NotNull
        @field:Size(max = 10)
        val tags: Collection<@Pattern(regexp = "^[0-9a-zA-Z]{2,10}") String>?
```

`List of Strings` の場合このようにできるか検証したが、`@Pattern(regexp = "^[0-9a-zA-Z]{2,10}")`  はどうやら検証されないようだった



そして、list of containers `List<@Pattern(regexp="...") String>` このようなことが[できると思った](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#container-element-constraints)けど、なぜか上手く行かないので追加で調査する。現在は個別にdata classを用意してvalidationするが `@RequestParam` の場合と`@RequestBody`によるJSONの場合で振る舞い変わるんでちょっとキモチワルイ

```bash
?tags\=1,22は上手くいく

    @GetMapping("{id}")
    fun get(
        @Valid
        @Pattern(regexp = "u[0-9]{5}", message = "This ID is invalid.")
        @PathVariable("id") id: String?,
        @Valid
        @Min(0)
        @RequestParam(value = "page", required = false, defaultValue = "0") page: Int,
        @Valid
        @Max(50)
        @Min(1)
        @RequestParam(value = "count", required = false, defaultValue = "10") count: Int,
        @Valid
        @RequestParam(value = "tags", required = false, defaultValue = "")
        // HACK(?): Want to use `tags: List<@Valid @Pattern(regexp="...") String>`, refs: https://stackoverflow.com/questions/22233512/adding-notnull-or-pattern-constraints-on-liststring
        tags: List<Tag>
    ) = Callable {...}

---

curl http://localhost:8080/api/validations/u12345\?tags\=1,22
-> {"status":400,"error_message":"must match \"^[0-9a-zA-Z]{2,10}\"","error_type":"ConstraintViolationException"}
```


**結局Kotlinの場合は、以下のオプションが必要になることがわかった
Bean validation 2.0 から利用できる container element validation(`List<@NotBlank String>`)をKotlin で有効にするには build option が必要なようだった。[stackoverflow](https://stackoverflow.com/questions/51085138/kotlin-data-class-and-bean-validation-with-container-element-constraints) に記載があった

```kotlin
// Kotlin DSL
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xemit-jvm-type-annotations")
        jvmTarget = "11"
    }
}
```





