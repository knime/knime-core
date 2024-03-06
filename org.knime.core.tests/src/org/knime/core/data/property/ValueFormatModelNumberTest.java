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
 *   31 May 2023 (carlwitt): created
 */
package org.knime.core.data.property;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.util.valueformat.NumberFormatter;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class ValueFormatModelNumberTest {

    // before each test, create a new model
    // after each test, check that the model is unchanged

    private static ValueFormatModelNumber createModel() throws InvalidSettingsException {
        return new ValueFormatModelNumber(//
            NumberFormatter.builder()//
                .setAlwaysShowDecimalSeparator(false)//
                .setMaximumDecimals(0)//
                .setGroupSeparator(",")//
                .build());
    }

    private static ValueFormatModelNumber createModelWithStyles() throws InvalidSettingsException {
        return new ValueFormatModelNumber(//
            NumberFormatter.builder()//
                .setAlwaysShowDecimalSeparator(false)//
                .setMaximumDecimals(0)//
                .setGroupSeparator(",")//
                .build(),
            "custom-style: yes;");
    }

    private static Stream<ValueFormatModelNumber> getModels() throws InvalidSettingsException {
        return Stream.of(createModel(), createModelWithStyles());
    }

    /**
     * Test formatting an int value.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testInt(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given
        var intCell = new IntCell(123456789);

        // when
        var formatted = model.getPlaintext(intCell);

        // then
        assertEquals("123,456,789", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a long data value.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testLong(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given
        var longCell = new LongCell(499999999000000001L);

        // when
        var formatted = model.getPlaintext(longCell);

        // then
        assertEquals("499,999,999,000,000,001", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a double data value.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testDouble(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given
        var doubleCell = new DoubleCell(123456789.987654321);

        // when
        var formatted = model.getPlaintext(doubleCell);

        // then
        assertEquals("123,456,790", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a non-numeric data value.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testNonNumeric(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given
        var stringCell = new StringCell("abc");

        // when
        var formatted = model.getPlaintext(stringCell);

        // then
        assertEquals("", formatted, "Empty string expected for non-numeric value.");
    }

    /**
     * Test formatting a missing data value.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testMissing(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given the model ^

        // when
        var formatted = model.getPlaintext(null);

        // then
        assertEquals("", formatted, "Empty string expected for missing value.");
    }

    /**
     * Test the wrapper for the model.
     *
     * @throws InvalidSettingsException
     */
    @ParameterizedTest
    @MethodSource("getModels")
    void testHandler(final ValueFormatModelNumber model) throws InvalidSettingsException {
        // given
        var handler = new ValueFormatHandler(model);

        // when saving and loading the model
        var config = new ModelContent("persistence");
        handler.save(config);
        var loadedHandler = ValueFormatHandler.load(config);

        // then
        assertEquals(handler.getFormatModel(), loadedHandler.getFormatModel(),
            "Loaded model is not equal to saved model.");
    }

    /**
     * Test that additional styles are indeed added to the HTML string, but not the plaintext output
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testAdditionalStyles() throws InvalidSettingsException {
        // given handlers and number cell
        var modelNoStyles = createModel();
        var modelWithStyles = createModelWithStyles();
        var cell = new DoubleCell(123456.123);

        // when formatting
        var plaintextNoStyles = modelNoStyles.getPlaintext(cell);
        var htmlNoStyles = modelNoStyles.getHTML(cell);
        var plaintextWithStyles = modelWithStyles.getPlaintext(cell);
        var htmlWithStyles = modelWithStyles.getHTML(cell);

        // then
        assertEquals(plaintextNoStyles, plaintextWithStyles,
            "Additional styles should not change anything about the plaintext output");
        assertEquals(plaintextNoStyles, "123,456", "The plaintext should be formatted correctly.");
        checkDiamonds(htmlWithStyles, htmlNoStyles);
        assertTrue(htmlWithStyles.contains("style=\"custom-style: yes;\""));
    }

    private static void checkDiamonds(final String ...htmls) {
        for (var html : htmls) {
            assertEquals(html.chars().filter(c -> c == '<').count(),
                html.chars().filter(c -> c == '>').count(), "The number of < and > should equal.");
        }
    }

}
