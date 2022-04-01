package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.knime.core.workflow.def.ConfigValueByteDef;
import org.knime.core.workflow.def.impl.ConfigValueByteDefBuilder;
import org.knime.core.workflow.def.impl.FallibleConfigValueByteDef;
import org.knime.core.workflow.loader.LoadException;

/**
 * Test basic functionality of builders that collect exceptions in their setters.
 *
 * Note that when a required value is not provided, this also does not abort the load process. It will add an exception
 * to the constructed element. Similarly, a value that is out of an allowed value range will have an exception
 * associated to it.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FallibleBuilderTest {

    /**
     * Test that constructing a Def attribute with a value that it is out of its range constraint doesn't stop the load
     * process but is noted as a {@link LoadException}.
     */
    @Test
    public void testRangeValidationExceptional() {
        // overflow during build
        final var byteValueOverflow = new ConfigValueByteDefBuilder().setValue(Integer.MAX_VALUE).build();
        // causes load exception
        assertThat(byteValueOverflow.hasExceptions()).isTrue();
        // stored here
        assertThat(byteValueOverflow.getValueException()).containsInstanceOf(LoadException.class);
        // but the originally provided value is kept
        assertThat(byteValueOverflow.getValue()).isEqualTo(Integer.MAX_VALUE);

        // overflow during build
        final var byteValueOverflow2 = new ConfigValueByteDefBuilder().setValue(() -> Integer.MAX_VALUE, 13).build();
        // causes load exception
        assertThat(byteValueOverflow2.hasExceptions()).isTrue();
        // stored here
        assertThat(byteValueOverflow2.getValueException()).containsInstanceOf(LoadException.class);
        // but the originally provided value is kept
        assertThat(byteValueOverflow2.getValue()).isEqualTo(13);

        // setting null to a numerical value
        final var byteValueNull = new ConfigValueByteDefBuilder().setConfigType("ConfigValue").setValue(null).build();
        // assuming a value range only applies when the value is present, no load exception is recorded
        assertThat(byteValueNull.hasExceptions()).isFalse();
        assertThat(byteValueNull.getValue()).isNull();
    }

    /**
     * The non-exceptional case for range validation.
     *
     * @see #testRangeValidationExceptional()
     */
    @Test
    public void testRangeValidationDefault() {
        // overflow during build
        final var byteValueDef = new ConfigValueByteDefBuilder().setConfigType("ConfigValue").setValue(12).build();
        assertThat(byteValueDef.hasExceptions()).isFalse();
        assertThat(byteValueDef.getValue()).isEqualTo(12);
    }

    /**
     * Test that constructing a Def attribute with a value that it is out of its range constraint doesn't stop the load
     * process but is noted as a {@link LoadException}.
     */
    @Test
    public void testRequiredFieldExceptional() {
        // three ways to construct a def with a null value for a required field
        final var faultyDefs = new FallibleConfigValueByteDef[]{
            // fall back to null value
            new ConfigValueByteDefBuilder().setConfigType(() -> {
                throw new IllegalStateException();
            }, null).build(),
            // set null for required value
            new ConfigValueByteDefBuilder().setConfigType(null).build(),
            // setter never called
            new ConfigValueByteDefBuilder().build()//
        };

        for (int i = 0; i < faultyDefs.length; i++) {
            // causes load exception
            assertThat(faultyDefs[i].hasExceptions()).as("Def at index " + i).isTrue();
            assertThat(faultyDefs[i].getConfigTypeException()).containsInstanceOf(LoadException.class);
        }
    }

    /**
     * The non-exceptional case for required field validation.
     *
     * @see #testRequiredFieldExceptional()
     */
    @Test
    public void testRequiredFieldDefault() {
        final var goodDef = new ConfigValueByteDefBuilder().setConfigType("ConfigValue").build();
        assertThat(goodDef.hasExceptions()).isFalse();

        final var semiGoodDef = new ConfigValueByteDefBuilder()//
            // set the required field
            .setConfigType("ConfigValue")
            // add an exception to a non-required field
            .setValue(() -> {
                throw new Exception();
            }, null).build();
        // no problem with the required field
        assertThat(semiGoodDef.hasExceptions(ConfigValueByteDef.Attribute.CONFIG_TYPE)).isFalse();
        // also not on the convenience getter
        assertThat(semiGoodDef.getConfigTypeException()).isEmpty();
    }
}
