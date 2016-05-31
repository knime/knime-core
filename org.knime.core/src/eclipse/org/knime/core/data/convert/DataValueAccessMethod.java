package org.knime.core.data.convert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.knime.core.data.DataValue;

/**
 * Annotation to mark a {@link DataValue} method as able to get the value of a DataValue as a certain Java type.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD})
public @interface DataValueAccessMethod {

    /**
     * @return descriptive name for the factory method, usually used to differentiate factory methods with the same
     *         parameter type.
     */
    String name() default "";
}
