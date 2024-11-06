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
 *   12.08.2005 (bernd): created
 */
package org.knime.core.data.renderer;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;

/**
 * Renderer for DataCells that are compatible with
 * <code>StringValue</code> classes.
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class StringValueRenderer extends DefaultDataValueRenderer {
    /**
     * Factory for the {@link StringValueRenderer}.
     *
     * @since 2.8
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataValueRenderer createRenderer(final DataColumnSpec colSpec) {
            return new StringValueRenderer();
        }
    }

    private static final String DESCRIPTION = "String";

    /**
     * Instance to be used.
     * @deprecated Do not use this singleton instance, renderers are not thread-safe!
     */
    @Deprecated
    public static final StringValueRenderer INSTANCE = new StringValueRenderer();

    /**
     * Formats the object. If <code>value</code> is instance of
     * <code>StringValue</code>, the object's <code>getStringValue</code>
     * is used. Otherwise the fallback: <code>value.toString()</code>
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof StringValue strValue) {
            newValue = truncateOverlyLongStrings(strValue.getStringValue());
        } else {
            // missing data cells will also end up here (non-null MissingCell object)
            newValue = truncateOverlyLongStrings(Objects.toString(value, ""));
        }
        super.setValue(newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    private static final int TRUNCATED_LENGTH = 1_000;

    static String truncateOverlyLongStrings(final String s) {
        return truncateOverlyLongStrings(s, TRUNCATED_LENGTH);
    }

    /**
     * Truncates long strings to a size that they won't cause endless loops when rendering.
     *
     * @param s the to-be-truncated string
     * @param truncatedLength the length (in characters) of the truncated string
     * @return the truncated string
     * @since 5.4
     */
    public static String truncateOverlyLongStrings(final String s, final int truncatedLength) {
        final int length = StringUtils.length(s);
        if (length > truncatedLength) {
            return String.format("%s ... (truncated %d characters)", StringUtils.truncate(s, truncatedLength),
                length - truncatedLength);
        }
        return s;
    }
}
