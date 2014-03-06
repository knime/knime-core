/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 */
package org.knime.core.data.model;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Renderer for PortObject content that are compatible with
 * <code>PortObjectValue</code> classes.
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 */
@SuppressWarnings("serial")
public final class PortObjectValueRenderer extends DefaultDataValueRenderer {
    /**
     * Factory for {@link PortObjectValueRenderer}.
     *
     * @since 2.8
     */
    public static final class Factory extends AbstractDataValueRendererFactory {
        private static final String DESCRIPTION = "Default";

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
            return new PortObjectValueRenderer(DESCRIPTION);
        }
    }

    /**
     * Singleton to be used.
     * @deprecated Do not use this singleton instance, renderers are not thread-safe!
     */
    @Deprecated
    public static final PortObjectValueRenderer INSTANCE = new PortObjectValueRenderer(Factory.DESCRIPTION);

    /**
     * Creates a new renderer for port objects.
     *
     * @param description a description for the renderer
     */
    PortObjectValueRenderer(final String description) {
        super(description);
    }

    /**
     * Formats the object. If <code>value</code> is instance of
     * <code>PortObjectValue</code>, the object's <code>getSummary</code>
     * is used. Otherwise the fallback: <code>value.toString()</code>
     * @param value The value to be rendered.
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(Object)
     */
    @Override
    protected void setValue(final Object value) {
        Object newValue;
        if (value instanceof PortObjectValue) {
            PortObjectValue cell = (PortObjectValue)value;
            newValue = cell.getPortObject().getSummary();
        } else {
            // missing data cells will also end up here
            newValue = value;
        }
        super.setValue(newValue);
    }
}
