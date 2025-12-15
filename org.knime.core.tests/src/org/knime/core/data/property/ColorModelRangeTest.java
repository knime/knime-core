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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.property.ColorModelRange.SpecialColorType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;

/**
 * Basic functionality tests for {@link ColorModelRange}.
 *
 * @author Bernd Wiswedel, KNIME
 */
@SuppressWarnings("static-method")
class ColorModelRangeTest {

    private static ColorModelRange newColorModel() {
        return new ColorModelRange(-1d, GREEN, 1d, RED);
    }

    /** Standard behavior. Color assignment for known and unknown values. */
    @Test
    final void testColorAssignment() {
        final var defColor = ColorAttr.DEFAULT.getColor();
        final var clrRange = newColorModel();
        assertThat(clrRange.getColorAttr(new DoubleCell(-1d)).getColor()).as("green").isEqualTo(GREEN);
        assertThat(clrRange.getColorAttr(new DoubleCell(+1d)).getColor()).as("red").isEqualTo(RED);
        assertThat(clrRange.getColorAttr(new DoubleCell(-2d)).getColor()).as("green").isEqualTo(GREEN);
        assertThat(clrRange.getColorAttr(new DoubleCell(+2d)).getColor()).as("red").isEqualTo(RED);
        assertThat(clrRange.getColorAttr(new DoubleCell(0d)).getColor()).as("red/green")
            .isEqualTo(new Color(127, 128, 0));
        assertThat(clrRange.getColorAttr(DataType.getMissingCell()).getColor()).as("default color").isEqualTo(defColor);
    }

    /** Write and read of model objects to a config object, equals/hash functionality. */
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

    private static final Map<SpecialColorType, Color> SPECIAL_COLORS_MAP = Map.of( //
        SpecialColorType.MISSING, Color.BLUE, //
        SpecialColorType.NAN, Color.CYAN, //
        SpecialColorType.NEGATIVE_INFINITY, Color.MAGENTA, //
        SpecialColorType.POSITIVE_INFINITY, Color.ORANGE, //
        SpecialColorType.BELOW_MIN, Color.PINK, //
        SpecialColorType.ABOVE_MAX, Color.LIGHT_GRAY);

    @Test
    final void testGetSpecialColors() {
        final var colorModel =
            new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10}, new Color[]{Color.WHITE, Color.BLACK}, false);
        assertThat(colorModel) //
            .as("missing").returns(ColorAttr.getInstance(Color.BLUE), //
                cm -> cm.getColorAttr(new MissingCell(null)))
            .as("NaN").returns(ColorAttr.getInstance(Color.CYAN), //
                cm -> cm.getColorAttr(new DoubleCell(Double.NaN)))
            .as("negative infinity").returns(ColorAttr.getInstance(Color.MAGENTA), //
                cm -> cm.getColorAttr(new DoubleCell(Double.NEGATIVE_INFINITY)))
            .as("positive infinity").returns(ColorAttr.getInstance(Color.ORANGE), //
                cm -> cm.getColorAttr(new DoubleCell(Double.POSITIVE_INFINITY)))
            .as("below min").returns(ColorAttr.getInstance(Color.PINK), //
                cm -> cm.getColorAttr(new DoubleCell(-1)))
            .as("above max").returns(ColorAttr.getInstance(Color.LIGHT_GRAY), //
                cm -> cm.getColorAttr(new DoubleCell(11)))
            .as("min").returns(ColorAttr.getInstance(Color.WHITE), //
                cm -> cm.getColorAttr(new DoubleCell(0)))
            .as("max").returns(ColorAttr.getInstance(Color.BLACK), //
                cm -> cm.getColorAttr(new DoubleCell(10)))
            .as("mid").returns(ColorAttr.getInstance(new Color(119, 119, 119)), //
                cm -> cm.getColorAttr(new DoubleCell(5)));
    }

    @Test
    final void testReturnsDefaultForNonAppliedModels() {
        final var customPercentageGradientModel =
            new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10}, new Color[]{Color.WHITE, Color.BLACK}, true);
        assertThat(customPercentageGradientModel.getColorAttr(new DoubleCell(5))) //
            .as("custom gradient with percentage stops").isEqualTo(ColorAttr.DEFAULT);

        final var predefinedGradientModel = new ColorModelRange(SPECIAL_COLORS_MAP, ColorGradient.MAGMA);
        assertThat(predefinedGradientModel.getColorAttr(new DoubleCell(5))) //
            .as("predefined gradient model").isEqualTo(ColorAttr.DEFAULT);
    }

    @Test
    final void testThrowsWhenConstructorWithSpecialColorsIsUsedWithoutSpecifyingAllSpecialColors() {
        final var incompleteSpecialColorsMap = Map.of( //
            SpecialColorType.MISSING, Color.BLUE, //
            SpecialColorType.NAN, Color.CYAN);

        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(incompleteSpecialColorsMap, new double[]{0, 10},
                new Color[]{Color.WHITE, Color.BLACK}, false),
            "All special colors must be defined when interpolating in CIELab color space.");

        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(incompleteSpecialColorsMap, ColorGradient.CIVIDIS),
            "All special colors must be defined when interpolating in CIELab color space.");
    }

    @Test
    final void testThrowsWhenStopValuesAndStopColorsDifferInLengthForAbsoluteModels() {
        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10, 50},
                new Color[]{Color.WHITE, Color.BLACK}, false),
            "The length of stopValues and stopColors must be equal.");
    }

    @Test
    final void testThrowsWhenStopValuesAreNotInBetween0And100ForPercentageModels() {
        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10, 20, 200},
                new Color[]{Color.WHITE, Color.RED, Color.CYAN, Color.DARK_GRAY}, true),
            "All stop values must be between 0 and 100 when using percentage values.");
    }

    @Test
    final void testThrowsWhenStopColorsHaveLengthLessThan2() {
        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10, 20, 200}, new Color[]{Color.WHITE}, true),
            "At least two stop colors are required.");
    }

    @Test
    final void testThrowsWhenStopValuesAreNotSortedNonDescreasing() {
        assertThrows(IllegalArgumentException.class,
            () -> new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 10, 5, 100},
                new Color[]{Color.WHITE, Color.BLACK, Color.RED, Color.BLUE}, true),
            "Stop values must be sorted in non-decreasing order.");
    }

    @Test
    final void testOldConstructorDoesNotThrowWhenStopValuesAreNotANumber() {
        assertDoesNotThrow(() -> new ColorModelRange(Double.NaN, Color.WHITE, Double.NaN, Color.BLACK));
    }

    @Test
    final void testTransformsPredefinedGradientPercentageModels() {
        final var colorModel = new ColorModelRange(SPECIAL_COLORS_MAP, ColorGradient.RED_BLUE_5);
        final var appliedColorModel = colorModel.applyToDomain(0, 100);
        assertThat(appliedColorModel.getColorAttr(new DoubleCell(0)))
            .isEqualTo(ColorAttr.getInstance(Color.decode("#ca0020")));
        assertThat(appliedColorModel.getColorAttr(new DoubleCell(25)))
            .isEqualTo(ColorAttr.getInstance(Color.decode("#f4a582")));
        assertThat(appliedColorModel.getColorAttr(new DoubleCell(50)))
            .isEqualTo(ColorAttr.getInstance(Color.decode("#f7f7f7")));
        assertThat(appliedColorModel.getColorAttr(new DoubleCell(75)))
            .isEqualTo(ColorAttr.getInstance(Color.decode("#92c5de")));
        assertThat(appliedColorModel.getColorAttr(new DoubleCell(100)))
            .isEqualTo(ColorAttr.getInstance(Color.decode("#0571b0")));
    }

    @Test
    final void testTransformsCustomGradientPercentageModels() {
        final var colorModel = new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 25, 100},
            new Color[]{Color.YELLOW, Color.GREEN, Color.BLUE}, true);
        final var appliedColorModel = colorModel.applyToDomain(-50, 50);
        final var expectedColorModel = new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{-50, -25, 50},
            new Color[]{Color.YELLOW, Color.GREEN, Color.BLUE}, false);
        assertThat(appliedColorModel).isEqualTo(expectedColorModel);
    }

    @Test
    final void testApplyingThrowsForInfiniteOrNaNDomain() {
        final var colorModel = new ColorModelRange(SPECIAL_COLORS_MAP, ColorGradient.MAGMA);
        assertThrows(IllegalArgumentException.class, () -> colorModel.applyToDomain(Double.NaN, 100));
        assertThrows(IllegalArgumentException.class, () -> colorModel.applyToDomain(0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> colorModel.applyToDomain(Double.NEGATIVE_INFINITY, 100));
        assertThrows(IllegalArgumentException.class, () -> colorModel.applyToDomain(0, Double.POSITIVE_INFINITY));
    }

    @Test
    final void testSaveLoadMinMaxModelWithNonDefaultAlphas() throws InvalidSettingsException {
        final var colorMin = new Color(255, 128, 32, 128);
        final var colorMax = new Color(0, 0, 255, 8);
        final var colorModelOriginal = new ColorModelRange(0, colorMin, 100, colorMax);
        final var config = new ModelContent("test");
        colorModelOriginal.save(config);
        final var colorModelLoaded = ColorModelRange.load(config);
        assertThat(colorModelLoaded).isEqualTo(colorModelOriginal);
    }

    @Test
    final void testSaveLoadGradientModel() throws InvalidSettingsException {
        final var colorModelOriginal = new ColorModelRange(SPECIAL_COLORS_MAP, ColorGradient.PURPLE_ORANGE_5);
        final var config = new ModelContent("test");
        colorModelOriginal.save(config);
        final var colorModelLoaded = ColorModelRange.load(config);
        assertThat(colorModelLoaded).isEqualTo(colorModelOriginal);
    }

    @Test
    final void testSaveLoadCustomPercentageModel() throws InvalidSettingsException {
        final var colorModelOriginal = new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{0, 20, 50, 80, 100},
            new Color[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE}, true);
        final var config = new ModelContent("test");
        colorModelOriginal.save(config);
        final var colorModelLoaded = ColorModelRange.load(config);
        assertThat(colorModelLoaded).isEqualTo(colorModelOriginal);
    }

    @Test
    final void testSaveLoadCustomAbsoluteModel() throws InvalidSettingsException {
        final var colorModelOriginal = new ColorModelRange(SPECIAL_COLORS_MAP, new double[]{-10, 4, 8, 20, 50},
            new Color[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE}, false);
        final var config = new ModelContent("test");
        colorModelOriginal.save(config);
        final var colorModelLoaded = ColorModelRange.load(config);
        assertThat(colorModelLoaded).isEqualTo(colorModelOriginal);
    }

}
