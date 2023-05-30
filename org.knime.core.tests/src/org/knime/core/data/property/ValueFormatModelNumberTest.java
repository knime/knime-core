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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
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
        return new ValueFormatModelNumber(
            NumberFormatter.builder().setAlwaysShowDecimalSeparator(false).setMaximumDecimals(0).build());
    }

    /**
     * Test formatting an int value.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testInt() throws InvalidSettingsException {
        // given
        ValueFormatModelNumber model = createModel();
        IntCell intCell = new IntCell(123456789);

        // when
        String formatted = model.getHTML(intCell);

        // then
        assertEquals("123,456,789", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a long data value.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testLong() throws InvalidSettingsException {
        // given
        ValueFormatModelNumber model = createModel();
        LongCell longCell = new LongCell(499999999000000001L);

        // when
        String formatted = model.getHTML(longCell);

        // then
        assertEquals("499,999,999,000,000,001", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a double data value.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testDouble() throws InvalidSettingsException {
        // given
        DoubleCell doubleCell = new DoubleCell(123456789.987654321);
        ValueFormatModelNumber model = createModel();

        // when
        String formatted = model.getHTML(doubleCell);

        // then
        assertEquals("123,456,790", formatted, "Value is formatted incorrectly.");
    }

    /**
     * Test formatting a non-numeric data value.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testNonNumeric() throws InvalidSettingsException {
        // given
        ValueFormatModelNumber model = createModel();
        StringCell stringCell = new StringCell("abc");

        // when
        String formatted = model.getHTML(stringCell);

        // then
        assertEquals("", formatted, "Empty string expected for non-numeric value.");
    }

    /**
     * Test formatting a missing data value.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testMissing() throws InvalidSettingsException {
        // given
        ValueFormatModelNumber model = createModel();

        // when
        String formatted = model.getHTML(null);

        // then
        assertEquals("", formatted, "Empty string expected for missing value.");
    }

    /**
     * Test the wrapper for the model.
     *
     * @throws InvalidSettingsException
     */
    @Test
    void testHandler() throws InvalidSettingsException {
        // given
        ValueFormatModelNumber model = createModel();
        ValueFormatHandler handler = new ValueFormatHandler(model);

        // when saving and loading the model
        var config = new ModelContent("persistence");
        handler.save(config);
        var loadedHandler = ValueFormatHandler.load(config);

        // then
        assertEquals(handler.getFormatModel(), loadedHandler.getFormatModel(), "Loaded model is not equal to saved model.");
    }

}
