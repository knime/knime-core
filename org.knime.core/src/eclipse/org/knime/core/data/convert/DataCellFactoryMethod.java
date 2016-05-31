package org.knime.core.data.convert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method as able to create a DataCell from a Java object.
 *
 * @author Jonathan Hale, KNIME, Konstanz, Germany
 * @since 3.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({java.lang.annotation.ElementType.METHOD})
public @interface DataCellFactoryMethod {

    /**
     * @return descriptive name for the factory method, usually used to differentiate factory methods with the same
     *         parameter type.
     */
    String name() default "";
}
