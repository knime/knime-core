/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.06.2006 (gabriel): created
 */
package org.knime.core.data.property;

import org.knime.core.data.DataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.util.valueformat.NumberFormatter;

/**
 * Defines a transformation from numbers to html.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public final class ValueFormatModelNumber implements ValueFormatModel {

    /**
     * Produces the formatted number value (without markup).
     */
    final NumberFormatter m_formatter;

    final boolean m_negativeInRed;

    /**
     * Produces the html surrounding the formatted value string.
     */
    private static String DEFAULT_MARKUP_TEMPLATE = "%s";

    /**
     * Produces the html surrounding the formatted value string.
     */
    private static String NEGATIVE_IN_RED_TEMPLATE = """
            <span style="color: red;">%s</span>
            """;

    /**
     * @param formatter to handle value to markup conversion
     */
    public ValueFormatModelNumber(final NumberFormatter formatter, final boolean negativeInRed) {
        m_formatter = formatter;
        m_negativeInRed = negativeInRed;
    }

    /**
     * @param dv value to transform
     * @return markup of the formatted value.
     */
    @Override
    public String getHTML(final DataValue dv) {
        var value = unpack(dv);
        if(value == null) {
            return "";
        }
        var formattedValue = m_formatter.format(value);
        var markupTemplate = m_negativeInRed && value.doubleValue() < 0 ? NEGATIVE_IN_RED_TEMPLATE : DEFAULT_MARKUP_TEMPLATE;
        return markupTemplate.formatted(formattedValue);
    }

    /**
     * @param dc data cell that holds the value to format
     * @return a Double, a Long, or null
     */
    private static Number unpack(final DataValue dc) {
        if (dc == null) {
            return null;
        }

        // if we get a long, we'd rather assign it to long than double (and potentially changing value)
        if (dc instanceof LongCell) {
            return ((LongCell)dc).getLongValue();
        }
        if (dc instanceof DoubleCell) {
            return ((DoubleCell)dc).getDoubleValue();
        }

        return null;
    }

    private static String CONFIG_KEY_NEGATIVE_IN_RED = "negative_in_red";

    @Override
    public void save(final ConfigWO config) {
        NumberFormatter.Persistor.save(config, m_formatter);
        config.addBoolean(CONFIG_KEY_NEGATIVE_IN_RED, m_negativeInRed);
    }

    /**
     * @param config to read from.
     * @return a new instance with loaded parameters.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static ValueFormatModelNumber load(final ConfigRO config) throws InvalidSettingsException {
        var numberFormatter = NumberFormatter.Persistor.load(config);
        var negativeInRed = config.getBoolean(CONFIG_KEY_NEGATIVE_IN_RED);
        return new ValueFormatModelNumber(numberFormatter, negativeInRed);
    }

    @Override
    public String toString() {
        return "Number FormatModel (pattern=<" + m_formatter.toString() + ">)";
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !(obj instanceof ValueFormatModelNumber)) {
            return false;
        }
        ValueFormatModelNumber cmodel = (ValueFormatModelNumber)obj;
        return m_formatter.equals(cmodel.m_formatter);
    }

    @Override
    public int hashCode() {
        return m_formatter.hashCode();
    }
}
