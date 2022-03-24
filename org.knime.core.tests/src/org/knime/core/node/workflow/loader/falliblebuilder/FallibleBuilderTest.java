package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;
import org.knime.core.workflow.def.impl.BaseNodeDefBuilder;
import org.knime.core.workflow.def.impl.ConfigValueByteDefBuilder;
import org.knime.core.workflow.loader.LoadException;

/**
 * Test basic functionality of builders that collect exceptions in their setters. This includes accessing exceptions of
 * nested objects.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FallibleBuilderTest {

    /**
     * Test that building fails when passing null to a required field.
     */
    @Test
    public void testRequiredField() {
        // explicitly set null
        final BaseNodeDefBuilder builder = new BaseNodeDefBuilder().setId(null);
        assertThrows(IllegalArgumentException.class, () -> builder.build());

        // supplier yield null
        final BaseNodeDefBuilder builder2 = new BaseNodeDefBuilder().setId(() -> null, 3);
        assertThrows(IllegalArgumentException.class, () -> builder2.build());

        // supplier default is null
        final BaseNodeDefBuilder builder3 = new BaseNodeDefBuilder().setId(() -> {
            throw new Exception();
        }, null);
        assertThrows(IllegalArgumentException.class, () -> builder3.build());

        // never called
        final BaseNodeDefBuilder builder4 = new BaseNodeDefBuilder();
        assertThrows(IllegalArgumentException.class, () -> builder4.build());
    }

    /**
     * Test that constructing a Def attribute with a value that it is out of its range constraint doesn't stop the load
     * process but is noted as a {@link LoadException}.
     */
    @Test
    public void testRangeValidation() {
        // overflow during build
        final var byteValueDef = new ConfigValueByteDefBuilder().setValue(Integer.MAX_VALUE).build();
        // causes load exception
        assertThat(byteValueDef.hasExceptions()).isTrue();
        // stored here
        assertThat(byteValueDef.getValueException()).containsInstanceOf(LoadException.class);
        // default value is used instead: minimum range value
        assertThat(byteValueDef.getValue()).isEqualByComparingTo(-128);
    }

}
