package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.workflow.def.AnnotationDataDef;
import org.knime.core.workflow.def.ConfigValueStringArrayDef;
import org.knime.core.workflow.def.StyleRangeDef;
import org.knime.core.workflow.def.impl.AnnotationDataDefBuilder;
import org.knime.core.workflow.def.impl.ConfigValueStringArrayDefBuilder;
import org.knime.core.workflow.def.impl.FallibleAnnotationDataDef;
import org.knime.core.workflow.def.impl.FallibleConfigValueStringArrayDef;
import org.knime.core.workflow.def.impl.StyleRangeDefBuilder;
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
        // load exception for string list
        final IOException containerException = new IOException("Cannot load base list");
        // load exception for a string element
        final InvalidSettingsException elementException =
            new InvalidSettingsException("Cannot load string array entry.");

        final FallibleConfigValueStringArrayDef stringArray = new ConfigValueStringArrayDefBuilder()//
            .setConfigType("ConfigValueStringArray")//
            // container supply exception
            .setArray(() -> {
                throw containerException;
            }, List.of())//
            .addToArray("some working string")//
            // supply exception for element at offset 1
            .addToArray(() -> {
                throw elementException;
            }, "ABC")//
            .build();

        // def has exceptions because container and one list element could not be loaded
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
        assertThat(stringArray.getArrayExceptionTree().get().getExceptionTree(1).get().getSupplyException().get().getCause())//
            .isSameAs(elementException);
        // and the convenience method for that
        assertThat(stringArray.getArrayElementException(1).get().getCause()).isSameAs(elementException);
    }

    /**
     * Construct a list of strings. Test the case with no exceptions.
     */
    @Test
    public void testStringListDefault() {
        final FallibleConfigValueStringArrayDef stringArray = new ConfigValueStringArrayDefBuilder()//
            .setConfigType("ConfigValueStringArray")//
            .addToArray("A").setArray(List.of("C", "D"))//
            .addToArray("B").build();

        assertThat(stringArray.hasExceptions()).isFalse();
        assertThat(stringArray.hasExceptions(ConfigValueStringArrayDef.Attribute.ARRAY)).isFalse();
        assertThat(stringArray.getSupplyException()).isEmpty();

        // single load exception supplier
        assertThat(stringArray.getArrayExceptionTree()).isEmpty();

        assertThat(stringArray.getArray()).containsExactly("A", "B", "C", "D");
    }

    /**
     * Construct a list of {@link AnnotationDataDef}s. Test that load exceptions during providing the container and the
     * string elements are recorded properly and the convenience getters for load exceptions behave consistently.
     * Assumes that all default values that are used to recover from supply exceptions are clean, i.e., have no load
     * exceptions themselves.
     */
    @Test
    public void testDefListExceptional() {
        // load exception for styles list
        final IOException containerException = new IOException("Cannot load style list");
        // load exceptions for two style elements
        final InvalidSettingsException element1Exception = new InvalidSettingsException("Cannot load style 1.");
        final IllegalArgumentException element2Exception = new IllegalArgumentException("Cannot load style 2.");

        StyleRangeDef style1 = new StyleRangeDefBuilder().setFontName("Font 1").build();
        StyleRangeDef style2 = new StyleRangeDefBuilder().setFontName("Font 2").build();
        StyleRangeDef style3 = new StyleRangeDefBuilder().setFontName("Font 3").build();

        final FallibleAnnotationDataDef styles = new AnnotationDataDefBuilder()//
            // container supply exception
            .setStyles(() -> {
                throw containerException;
            }, List.of(style3)) //
            .addToStyles(null) //
            .addToStyles(null) //
            // supply exception for element at offset 2
            .addToStyles(() -> {
                throw element1Exception;
            }, style1)//
            .addToStyles(null) //
            // supply exception for element at offset 4
            .addToStyles(() -> {
                throw element2Exception;
            }, style2)//
            .build();

        // def has exceptions because container and two list elements could not be loaded
        assertThat(styles.hasExceptions()).isTrue();
        assertThat(styles.hasExceptions(AnnotationDataDef.Attribute.STYLES)).isTrue();

        // but no exception in supplying this to (non-existent) parent object
        assertThat(styles.getSupplyException()).isEmpty();

        // ------------------------ container exception ------------------------
        assertThat(styles.getStylesExceptionTree()).isPresent();
        // supply exception
        assertThat(styles.getStylesExceptionTree().get().getSupplyException().get().getCause())
            .isSameAs(containerException);

        // ------------------------ element exceptions ------------------------
        // element at offset 2 has an exception
        assertThat(styles.getStylesExceptionTree().get().getExceptionTree(2).get().getSupplyException().get().getCause())//
            .isSameAs(element1Exception);
        // convenience getter: first child to have an exception is at offset 1
        assertThat(styles.getStylesExceptionTrees().get(0).getSupplyException()).get().isSameAs(element1Exception);

        // element at offset 4 has an exception
        assertThat(styles.getStylesExceptionTree().get().getExceptionTree(4).get().getSupplyException().get().getCause())//
            .isSameAs(element2Exception);
        // convenience getter: second child to have an exception is the one at offset 4
        assertThat(styles.getStylesExceptionTrees().get(1).getSupplyException()).get().isSameAs(element2Exception);

        // ------------------------ aggregate exceptions ------------------------
        // has a supply and two child exceptions
        assertThat(styles.getStylesExceptionTree().get().getFlattenedLoadExceptions())
            .extracting(LoadException::getCause).containsOnly(element1Exception, element2Exception, containerException);
    }

    /**
     * Construct a list of {@link AnnotationDataDef}s. Test the case where no exceptions occur.
     */
    @Test
    public void testDefListDefault() {

        StyleRangeDef style1 = new StyleRangeDefBuilder().setFontName("Font 1").build();
        StyleRangeDef style2 = new StyleRangeDefBuilder().setFontName("Font 2").build();
        StyleRangeDef style3 = new StyleRangeDefBuilder().setFontName("Font 3").build();

        final FallibleAnnotationDataDef styles = new AnnotationDataDefBuilder()//
            .setStyles(List.of(style3)) //
            .addToStyles(null) // offset 0
            .addToStyles(null) // offset 1
            .addToStyles(() -> style1, null) // offset 2
            .addToStyles(null) // offset 3
            .addToStyles(style2) // offset 4
            .build();

        // def has exceptions because container and two list elements could not be loaded
        assertThat(styles.hasExceptions()).isFalse();
        assertThat(styles.hasExceptions(AnnotationDataDef.Attribute.STYLES)).isFalse();
        // but no exception in supplying this to (non-existent) parent object
        assertThat(styles.getSupplyException()).isEmpty();

        // container clean
        assertThat(styles.getStylesExceptionTree()).isEmpty();
        // elements as expected
        assertThat(styles.getStyles().get(0)).isNull();
        assertThat(styles.getStyles().get(1)).isNull();
        assertThat(styles.getStyles().get(2)).isEqualTo(style1);
        assertThat(styles.getStyles().get(3)).isNull();
        assertThat(styles.getStyles().get(4)).isEqualTo(style2);
    }
}
