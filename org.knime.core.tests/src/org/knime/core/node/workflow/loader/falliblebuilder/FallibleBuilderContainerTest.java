package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.workflow.def.ConfigValueStringArrayDef;
import org.knime.core.workflow.def.impl.ConfigValueStringArrayDefBuilder;
import org.knime.core.workflow.def.impl.FallibleConfigValueStringArrayDef;
import org.knime.core.workflow.loader.LoadException;

/**
 * Test basic list and map construction functionality of builders that collect exceptions in their setters. This
 * includes accessing exceptions of nested objects.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FallibleBuilderContainerTest {

    /**
     * Construct a list of strings. Test that load exceptions during providing the container and the string elements are
     * recorded properly.
     */
    @Test
    public void testStringListExceptional() {
        final IOException containerException = new IOException("Cannot load base list");
        final InvalidSettingsException elementException =
            new InvalidSettingsException("Cannot load string array entry.");
        final FallibleConfigValueStringArrayDef stringArray = new ConfigValueStringArrayDefBuilder()//
            .setConfigType("ConfigValueStringArray")//
            // container supply exception
            .setArray(() -> {
                throw containerException;
            }, List.of())//
            .addToArray("id")//
            // element supply exception
            .addToArray(() -> {
                throw elementException;
            }, "ABC")//
            .build();

        // has exceptions because container and one list element could not be loaded
        assertThat(stringArray.hasExceptions()).isTrue();
        assertThat(stringArray.hasExceptions(ConfigValueStringArrayDef.Attribute.ARRAY)).isTrue();

        // but no exception in supplying this to (non-existent) parent object
        assertThat(stringArray.getSupplyException()).isEmpty();

        // single load exception supplier
        assertThat(stringArray.getArrayExceptionTree()).isPresent();
        // has a supply and one child exception
        assertThat(stringArray.getArrayExceptionTree().get().getFlattenedLoadExceptions())
            .extracting(LoadException::getCause).containsOnly(elementException, containerException);

        // supply exception
        assertThat(stringArray.getArrayExceptionTree().get().getSupplyException().get().getCause())
            .isSameAs(containerException);

        // element at offset 1 has an exception
        assertThat(stringArray.getArrayExceptionTree().get().getExceptions(1).get().getSupplyException().get().getCause())//
            .isSameAs(elementException);
        // and the convenience method for that
        assertThat(stringArray.getArrayElementException(1).get().getCause()).isSameAs(elementException);
    }

}
