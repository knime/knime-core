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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;


/**
 * Iterator over all
 * {@link BasisFunctionLearnerRow}s within the
 * model.
 * 
 * Supports to skip certain classes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class BasisFunctionIterator extends RowIterator {
    /*
     * The underlying table containing the basisfunctions.
     */
    private final BasisFunctionLearnerTable m_table;

    /*
     * A map containing the basisfunctions in an <code>ArrayList</code> of
     * each class addressed by the class label as <code>DataCell</code> key.
     */
    private final Map<DataCell, List<BasisFunctionLearnerRow>> m_map;

    /*
     * The iterator from the underlying table.
     */
    private final Iterator<DataCell> m_it;

    /*
     * Current basisfunctions for class index <code>m_bfIndex</code>.
     */
    private ArrayList<BasisFunctionLearnerRow> m_bfs;

    /*
     * Current class index for the basisfunction array <code>m_bfs</code>.
     */
    private int m_bfIndex;

    /**
     * Creates a new basisfunction iterator. Does not perform error checking.
     * 
     * @param table the underlying learner table
     * @throws NullPointerException if the table is <code>null</code>
     */
    public BasisFunctionIterator(final BasisFunctionLearnerTable table) {
        m_table = table;
        m_map = m_table.getBasisFunctions();
        m_it = m_map.keySet().iterator();
        skipClass();
    }

    /**
     * Checks if the iterator already reached the end of the iteration. Here, we
     * need to check to conditions. One, if the current iterator is at end, and,
     * second, if the all classes have been processed.
     * 
     * @return <code>true</code> if the end has been reached otherwise.
     */
    @Override
    public boolean hasNext() {
        if (m_bfs == null) {
            return false;
        }
        if (m_bfIndex == m_bfs.size()) {
            skipClass();
            if (m_bfs == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the next row in the iteration. If the current iteration is at
     * end, the iteration will be start over at the next class, otherwise the
     * basisfunction index is increased.
     * 
     * @return the next row in the iteration
     * @throws NoSuchElementException if there are no more rows
     */
    public BasisFunctionLearnerRow nextBasisFunction() {
        if (hasNext()) {
            return m_bfs.get(m_bfIndex++);
        }
        throw new NoSuchElementException("No more elements to return.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        return nextBasisFunction();
    }

    /**
     * Skips the current class, {@link #next()} will then return the first basis
     * function of the next class. If the current class is the last, the
     * basisfunction index is set to the last element <code>+1</code> and
     * {@link #hasNext()} will return <code>false</code>.
     */
    public void skipClass() {
        m_bfIndex = 0;
        // step forward until non-empty list is found
        while (m_it.hasNext()) {
            // jump to next class and init next list of basisfunctions
            m_bfs = (ArrayList<BasisFunctionLearnerRow>)m_map.get(m_it.next());
            if (m_bfs.size() > 0) {
                // break while loop, new bf found
                return;
            }
            assert false;
        }
        m_bfs = null;
        assert (!m_it.hasNext());
    }
}
