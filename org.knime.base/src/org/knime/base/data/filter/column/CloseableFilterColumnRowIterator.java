/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.08.2008 (thor): created
 */
package org.knime.base.data.filter.column;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;

/**
 * Filter column row iterator needed to wrap the filter column's original row
 * iterator. In contrast to the FilterColumnRowIterator this iterator
 * is closeable.
 *
 * @see FilterColumnTable
 *
 * @author Thomas Gabriel, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
final class CloseableFilterColumnRowIterator extends CloseableRowIterator {
    /*
     * Original row iterator.
     */
    private final CloseableRowIterator m_it;

    /*
     * Array of column indices.
     */
    private final int[] m_columns;

    /**
     * Creates a new filter iterator using the original row iterator and an
     * array of column indices.
     *
     * @param it the original row iterator
     * @param columns an array of column indices
     */
    CloseableFilterColumnRowIterator(final CloseableRowIterator it,
            final int[] columns) {
        m_it = it;
        m_columns = columns;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_it.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        return new FilterColumnRow(m_it.next(), m_columns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        m_it.close();
    }
}
