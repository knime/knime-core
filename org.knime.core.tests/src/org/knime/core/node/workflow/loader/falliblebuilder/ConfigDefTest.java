package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.ConfigValueStringArrayDef;
import org.knime.core.workflow.def.ConfigValueStringDef;
import org.knime.core.workflow.def.impl.ConfigMapDefBuilder;
import org.knime.core.workflow.def.impl.ConfigValueStringArrayDefBuilder;
import org.knime.core.workflow.def.impl.ConfigValueStringDefBuilder;
import org.knime.core.workflow.def.impl.FallibleConfigValueStringArrayDef;
import org.knime.core.workflow.loader.LoadException;
import org.knime.core.workflow.loader.LoadExceptionSupplier;

/**
 * Test basic functionality of builders that collect exceptions in their setters. This includes accessing exceptions of
 * nested objects.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class ConfigDefTest {

    static final ConfigValueStringDef stringValue = new ConfigValueStringDefBuilder().setValue("sales").build();

    static final FallibleConfigValueStringArrayDef stringArray = new ConfigValueStringArrayDefBuilder()//
        .setArray(() -> {
            throw new IOException("Cannot load base list");
        }, List.of())//
        .addToArray("id")//
        .addToArray(() -> {
            throw new InvalidSettingsException("Cannot load string array entry.");
        }, "ABC")//
        .build();

    static final ConfigMapDef config = new ConfigMapDefBuilder()//
        .setChildren(Map.of("table", stringValue, "columns", stringArray))//
        .build();

    /**
     * {@link ConfigValueStringArrayDef} is an example of a Def that contains a list of a non-Def type.
     */
    @Test
    public void testPrimitiveArray() {

        FallibleConfigValueStringArrayDef columns =
            (FallibleConfigValueStringArrayDef)config.getChildren().get("columns");

        // has exceptions because one list entry could not be loaded
        assertThat(columns.hasExceptions()).isTrue();
        // but no exception in supplying this to (non-existent) parent object
        assertThat(columns.getSupplyException()).isEmpty();

        // access via LES is same as convenience method
        LoadExceptionSupplier<?> listSupplier = columns.getExceptionalChildren().get(ConfigValueStringArrayDef.Attribute.ARRAY);
        assertThat(columns.getArrayExceptions()).isSameAs(listSupplier);

        // get exception
        assertThat(listSupplier.getSupplyException().get().getCause().getMessage()).isEqualTo("Cannot load base list");
        assertThat(listSupplier.getExceptionalChildren()).isEqualTo(Map.of(1, new LoadException(new InvalidSettingsException("Cannot load string array entry"))));

        System.out.println(config);

    }

}
