
package org.horiga.study.springframework.errorhandler

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
    AnnotationTarget.ANNOTATION_CLASS
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Constraint(validatedBy = [OrderPriorityValidator::class])
annotation class OrderPriority(
    val message: String = "{validation.OrderPriority.message}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
    val min: Int = 1,
    val max: Int = 5
)

class OrderPriorityValidator : ConstraintValidator<OrderPriority, Int> {

    lateinit var message: String

    data class OrderPriorityRange(
        val min: Int = 0,
        val max: Int = Int.MAX_VALUE
    )

    private lateinit var orderPriorityRange: OrderPriorityRange

    override fun initialize(constraintAnnotation: OrderPriority) {
        message = constraintAnnotation.message
        orderPriorityRange = OrderPriorityRange(constraintAnnotation.min, constraintAnnotation.max)
    }

    override fun isValid(value: Int?, context: ConstraintValidatorContext?) =
            value?.let { it in orderPriorityRange.min..orderPriorityRange.max } ?: true
}