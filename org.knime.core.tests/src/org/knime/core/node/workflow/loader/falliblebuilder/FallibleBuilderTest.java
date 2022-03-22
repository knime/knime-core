package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;
import org.knime.core.workflow.def.impl.BaseNodeDefBuilder;

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

}
