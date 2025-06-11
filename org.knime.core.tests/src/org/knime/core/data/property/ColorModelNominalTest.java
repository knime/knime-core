/*
 * ------------------------------------------------------------------------
 *
*  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 15, 2023 (wiswedel): created
 */
package org.knime.core.data.property;

import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.PINK;
import static java.awt.Color.RED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;

/**
 * Basic functionality tests for {@link ColorModelNominal}.
 *
 * @author Bernd Wiswedel, KNIME
 */
class ColorModelNominalTest {

    private static ColorModelNominal newColorModel() {
        var map = new LinkedHashMap<DataCell, ColorAttr>();
        map.put(new StringCell("value 1"), ColorAttr.getInstance(GREEN));
        map.put(new StringCell("value 2"), ColorAttr.getInstance(RED));
        map.put(new StringCell("value 3"), ColorAttr.getInstance(GREEN));
        var palette = new ColorAttr[] {ColorAttr.getInstance(BLUE), ColorAttr.getInstance(PINK)};

        ColorModelNominal clrMdl = new ColorModelNominal(map, palette);
        return clrMdl;
    }

    /** Standard behavior. Color assignment for known and unknown values. */
    @SuppressWarnings("static-method")
    @Test
    final void testColorAssignment() {
        ColorModelNominal clrMdl = newColorModel();
        assertThat(clrMdl.getValues()).as("known values").containsAll(
            Arrays.asList(new StringCell("value 1"), new StringCell("value 2"), new StringCell("value 3")));
        assertThat(clrMdl.getColorAttr(new StringCell("value 2")).getColor()).as("Color is red").isEqualTo(RED);

        // why not pink or blue (as per model defintion) -- pink or blue or part of the palette that the color
        // applier node can use to apply the palette to new data.
        assertThat(clrMdl.getColorAttr(new StringCell("unknown")).getColor()) //
            .as("default color for unknowns").isEqualTo(ColorAttr.DEFAULT.getColor());
    }

    /** Write and read of model objects to a config object, equals/hash functionality. */
    @SuppressWarnings("static-method")
    @Test
    final void testPersistance() throws InvalidSettingsException {
        var clrMdl = newColorModel();
        var config = new ModelContent("persistance");
        clrMdl.save(config);
        var clrMdlLoaded = ColorModelNominal.load(config);

        assertThat(clrMdl) //
            .as("Model after load is the same").isEqualTo(clrMdlLoaded) //
            .as("Model hash after load is the same").hasSameHashCodeAs(clrMdlLoaded) //
            .as("Is equal to self").isEqualTo(clrMdl) //
            .as("Not equal to arbitrary object").isNotEqualTo(new Object());


        assertThat(clrMdlLoaded.getColorAttr(new StringCell("value 2")).getColor()).as("Color is red").isEqualTo(RED);

        assertThat(clrMdl.getColorAttr(new StringCell("unknown")).getColor()) //
            .as("default color for unknowns").isEqualTo(ColorAttr.DEFAULT.getColor());
    }

    /** Derivation of color model for new values, applying palette */
    @SuppressWarnings("static-method")
    @Test
    final void testApplyNewValues() throws InvalidSettingsException {
        var clrMdl = newColorModel();
        Iterable<DataCell> newValues = Arrays.asList(
            new StringCell("value 1"),
            new StringCell("value 3"),
            new StringCell("value 4"),
            new StringCell("value 5"),
            new StringCell("value 6"),
            new StringCell("value 7"));
        final var appliedClrMdl = clrMdl.applyToNewValues(newValues);
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 1"))).as("Color for value 1")
            .isEqualTo(ColorAttr.getInstance(GREEN));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 2"))).as("Color for value 2")
            .isEqualTo(ColorAttr.getInstance(RED));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 3"))).as("Color for value 3")
            .isEqualTo(ColorAttr.getInstance(GREEN));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 4"))).as("Color for value 4")
            .isEqualTo(ColorAttr.getInstance(BLUE));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 5"))).as("Color for value 5")
            .isEqualTo(ColorAttr.getInstance(PINK));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 6"))).as("Color for value 6")
            .isEqualTo(ColorAttr.getInstance(BLUE));
        assertThat(appliedClrMdl.getColorAttr(new StringCell("value 7"))).as("Color for value 7")
            .isEqualTo(ColorAttr.getInstance(PINK));

        assertThat(clrMdl.getColorAttr(new StringCell("unknown")).getColor()) //
        .as("default color for unknowns").isEqualTo(ColorAttr.DEFAULT.getColor());
    }

    /** Getters, added as part of AP-20239. */
    @SuppressWarnings("static-method")
    @Test
    final void testGetters() throws InvalidSettingsException {
        final var clrMdl = newColorModel();
        final Map<String, List<String>> colorToValueMap = clrMdl.getColorToValueMap();

        assertThat(colorToValueMap) //
            .as("Green maps to two values").containsEntry("#00FF00", Arrays.asList("value 1", "value 3")) //
            .as("Red to one value").containsEntry("#FF0000", Arrays.asList("value 2")) //
            .as("List length").hasSize(2);
    }

}
