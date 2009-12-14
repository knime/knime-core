/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   21.06.06 (bw & po): reviewed
 */
package org.knime.core.data.def;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;

/**
 * Specific implementation for a {@link RowIterator} that iterates over a
 * generic {@link org.knime.core.data.DataTable DataTable}. It delegates to a
 * given <code>Iterator&lt;DataRow&gt</code>; but disallows the invocation of
 * the <code>remove</code> method.
 * 
 * @author Bernd Wiswedel, University Konstanz
 */
public class DefaultRowIterator extends RowIterator {

    /** The wrapped iterator to get next rows from. */
    private final Iterator<DataRow> m_iterator;

    /**
     * Constructs a new iterator based on an {@link Iterable}.
     * 
     * @param iterable the underlying iterable row container.
     * @throws NullPointerException if the argument is null.
     */
    public DefaultRowIterator(final Iterable<DataRow> iterable) {
        m_iterator = iterable.iterator();
    }

    /**
     * Constructs a new iterator that traverses an array of {@link DataRow}.
     * 
     * @param rows the array to iterate over.
     * @throws NullPointerException if the argument is null.
     */
    public DefaultRowIterator(final DataRow... rows) {
        // prevents the caller from changing the array underneath.
        this(new ArrayList<DataRow>(Arrays.asList(rows)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        return m_iterator.next();
    }

}
