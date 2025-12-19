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

import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;

/**
 * Basic functionality tests for {@link ColorModelRange}.
 *
 * @author Bernd Wiswedel, KNIME
 */
class ColorModelRangeTest {

    private static ColorModelRange newColorModel() {
        return new ColorModelRange(-1d, GREEN, 1d, RED);
    }

    /** Standard behavior. Color assignment for known and unknown values. */
    @SuppressWarnings("static-method")
    @Test
    final void testColorAssignment() {
        final var defColor = ColorAttr.DEFAULT.getColor();
        final var clrRange = newColorModel();
        assertThat(clrRange.getColorAttr(new DoubleCell(-1d)).getColor()).as("green").isEqualTo(GREEN);
        assertThat(clrRange.getColorAttr(new DoubleCell(+1d)).getColor()).as("red").isEqualTo(RED);
        assertThat(clrRange.getColorAttr(new DoubleCell(-2d)).getColor()).as("green").isEqualTo(GREEN);
        assertThat(clrRange.getColorAttr(new DoubleCell(+2d)).getColor()).as("red").isEqualTo(RED);
        assertThat(clrRange.getColorAttr(new DoubleCell(0d)).getColor()).as("red/green").isEqualTo(new Color(127, 128, 0));
        assertThat(clrRange.getColorAttr(DataType.getMissingCell()).getColor()).as("default color").isEqualTo(defColor);
    }

    /** Write and read of model objects to a config object, equals/hash functionality. */
    @SuppressWarnings("static-method")
    @Test
    final void testPersistance() throws InvalidSettingsException {
        var clrMdl = newColorModel();
        var config = new ModelContent("persistance");
        clrMdl.save(config);
        var clrMdlLoaded = ColorModelRange.load(config);

        assertThat(clrMdl) //
            .as("Model after load is the same").isEqualTo(clrMdlLoaded) //
            .as("Model hash after load is the same").hasSameHashCodeAs(clrMdlLoaded) //
            .as("Is equal to self").isEqualTo(clrMdl) //
            .as("Not equal to arbitrary object").isNotEqualTo(new Object());


        assertThat(clrMdlLoaded.getColorAttr(new DoubleCell(-1d)).getColor()).as("Color is green").isEqualTo(GREEN);
    }

    /** Getters, added as part of AP-20239. */
    @SuppressWarnings("static-method")
    @Test
    final void testGetters() throws InvalidSettingsException {
        final var clrMdl = newColorModel();
        assertThat(clrMdl) //
            .as("min value").returns(-1d, ColorModelRange::getMinValue) //
            .as("min color").returns(GREEN, ColorModelRange::getMinColor) //
            .as("min color hex").returns("#00FF00", ColorModelRange::getMinColorHex) //
            .as("max value").returns(1d, ColorModelRange::getMaxValue) //
            .as("max color").returns(RED, ColorModelRange::getMaxColor) //
            .as("max color hex").returns("#FF0000", ColorModelRange::getMaxColorHex);
    }

}
