/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 24.05.2013 by thor
 */
package org.knime.core.data.collection;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.renderer.AbstractDataValueRendererFactory;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DefaultDataValueRenderer;

/**
 * Generic renderer for {@link CollectionDataValue} which prints the string representation of each object in the
 * collection.
 *
 * @since 2.8
 */
@SuppressWarnings("serial")
public final class CollectionValueRenderer extends DefaultDataValueRenderer {
    /**
     * Factory for a {@link CollectionValueRenderer} that shows at most the first three elements of the collection.
     */
    public static final class ShortRendererFactory extends AbstractDataValueRendererFactory {
        private static final String DESCRIPTION = "Collection (short)";

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
            return new CollectionValueRenderer(3, DESCRIPTION);
        }
    }


    /**
     * Factory for a {@link CollectionValueRenderer} that shows all elements of the collection.
     */
    public static final class FullRendererFactory extends AbstractDataValueRendererFactory {
        private static final String DESCRIPTION = "Collection (full)";

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
            return new CollectionValueRenderer(Integer.MAX_VALUE, DESCRIPTION);
        }
    }


    private final int m_maxElements;

    /**
     * Instantiate renderer.
     *
     * @param maxElements maximum number of element to show
     * @param description a description for the renderer
     */
    CollectionValueRenderer(final int maxElements, final String description) {
        super(description);
        m_maxElements = maxElements;
    }

    /** {@inheritDoc} */
    @Override
    protected void setValue(final Object value) {
        if (value instanceof CollectionDataValue) {
            StringBuilder buf = new StringBuilder();
            buf.append('[');
            Iterator<DataCell> it = ((CollectionDataValue)value).iterator();
            if (it.hasNext() && (m_maxElements > 0)) {
                buf.append(it.next().toString());
            }

            for (int i = 1; it.hasNext() && (i < m_maxElements); i++) {
                buf.append(',').append(it.next().toString());
            }

            if (it.hasNext()) {
                if (m_maxElements > 0) {
                    buf.append(',');
                }
                buf.append("...");
            }
            buf.append(']');
            super.setValue(buf.toString());
        } else {
            super.setValue(value);
        }
    }
}
