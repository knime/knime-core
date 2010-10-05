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
 */
package org.knime.base.data.filter.row;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

/**
 * This class wraps the {@link org.knime.core.data.RowIterator} which
 * includes only {@link org.knime.core.data.DataRow}s which satify the
 * {@link FilterRowGenerator} criteria.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
class FilterRowIterator extends RowIterator {
    /*
     * The underlying data's row iterator.
     */
    private final RowIterator m_it;

    /*
     * Row filter to check data rows with.
     */
    private final FilterRowGenerator m_gen;

    /*
     * The current data row to return or null if the iterator is at end.
     */
    private DataRow m_row;

    /**
     * Creates a new filter row iterator wrapping a row iterator and using the
     * filter row generator for checking each row.
     * 
     * @param it the row iterator to retrieve each row
     * @param gen the filter row generator
     */
    FilterRowIterator(final RowIterator it, final FilterRowGenerator gen) {
        m_it = it;
        m_gen = gen;
        m_row = null;
        // to retrieve the first row
        next();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return (m_row != null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        // keep current one to return it
        DataRow row = m_row;
        m_row = null;
        // try to find next
        while (m_it.hasNext()) {
            m_row = m_it.next();
            // for which the row filter returns true
            if (m_gen.isIn(m_row)) {
                break;
            }
            m_row = null;
        }
        // currently saved one
        return row;
    }
}
