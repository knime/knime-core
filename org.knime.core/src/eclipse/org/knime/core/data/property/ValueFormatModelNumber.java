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

import java.text.DecimalFormat;

import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;

/**
 * Computes colors based on a range of minimum and maximum values assigned to certain colors which are interpolated
 * between a min and maximum color.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.1
 */
public final class ValueFormatModelNumber implements ValueFormatModel {

    DecimalFormat m_format;

    public ValueFormatModelNumber(final DecimalFormat format) {
        m_format = format;
    }

    /**
     * Returns a ColorAttr for the given DataCell value, or <code>ColorAttr.DEFAULT</code> if not set. The colors red,
     * green, and blue are merged in the same ratio from the original spread of the lower and upper bounds.
     *
     * @param dc A DataCell value to get color for.
     * @return A ColorAttr for a DataCell value or the DEFAULT ColorAttr.
     */
    @Override
    public String getHTML(final DataCell dc) {
        var value = unpack(dc);
        return value == null ? "" : m_format.format(value);
    }


    /**
     * @param dc data cell that holds the value to format
     * @return a Double, a Long, or null
     */
    private static Object unpack(final DataCell dc) {
        if (dc == null || dc.isMissing()) {
            return null;
        }

        // if we get a long, we'd rather assign it to long than double (and potentially changing value)
        if (dc.getType().isCompatible(LongValue.class)) {
            return ((LongValue)dc).getLongValue();
        }
        if (dc.getType().isCompatible(DoubleValue.class)) {
            return ((DoubleValue)dc).getDoubleValue();
        }

        return null;

    }

    private static final String CFG_DECIMAL_FORMAT_PATTERN = "pattern";

    @Override
    public void save(final ConfigWO config) {
        config.addString(CFG_DECIMAL_FORMAT_PATTERN, m_format.toPattern());
    }

    /**
     * @param config to read from.
     * @return a new instance with loaded parameters.
     * @throws InvalidSettingsException If the settings could not be read.
     */
    public static ValueFormatModelNumber load(final ConfigRO config) throws InvalidSettingsException {
        String pattern = config.getString(CFG_DECIMAL_FORMAT_PATTERN);
        // TODO not sure if to pattern/from pattern is lossless
        return new ValueFormatModelNumber(new DecimalFormat(pattern));
    }

    @Override
    public String toString() {
        return "Number FormatModel (pattern=<" + m_format.toPattern() + ">)";
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
        return m_format.toPattern().equals(cmodel.m_format.toPattern());
    }

    @Override
    public int hashCode() {
        return m_format.toPattern().hashCode();
    }
}
