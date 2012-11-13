/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.core.data.collection;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.container.BlobWrapperDataCell;

/**
 * Default implementation to {@link BlobSupportDataCellIterator}.
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultBlobSupportDataCellIterator implements
        BlobSupportDataCellIterator {

    private final InternalIterator m_it;

    /** Create new instance by wrapping an existing iterator.
     * @param it To wrap. */
    public DefaultBlobSupportDataCellIterator(final ListIterator<DataCell> it) {
        m_it = new ListInternalIterator(it);
    }

    /** Create new instance by wrapping an existing iterator.
     * @param it To wrap. */
    public DefaultBlobSupportDataCellIterator(final Iterator<Map.Entry<?, DataCell>> it) {
        m_it = new MapEntryInternalIterator(it);
    }

    /** {@inheritDoc} */
    @Override
    public DataCell nextWithBlobSupport() {
        return m_it.next();
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasNext() {
        return m_it.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public DataCell next() {
        DataCell next = nextWithBlobSupport();
        if (next instanceof BlobWrapperDataCell) {
            return ((BlobWrapperDataCell)next).getCell();
        }
        return next;
    }

    /** {@inheritDoc}
     * @since 2.7
     */
    @Override
    public void replaceLastReturnedWithWrapperCell(final DataCell cell) {
        m_it.replaceLast(cell);
    }

    /** {@inheritDoc} */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("No write DataCell collections");
    }

    /** An iterator that allows replacing the last returned element. Used by Buffer to replace a data cell
     * with a blob wrapper cell. */
    abstract static class InternalIterator implements Iterator<DataCell> {

        /** Called by DataContainer Buffer to replace the contained data cell by a address-corrected blob wrapper.
         * @param cell The replacement
         */
        abstract void replaceLast(final DataCell cell);

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("No write DataCell collections");
        }
    }

    /** List based internal iterator. Used {@link ListIterator#set(Object)} to modify the collection. */
    private static final class ListInternalIterator extends InternalIterator {

        private final ListIterator<DataCell> m_iterator;


        /** @param iterator to wrap. */
        ListInternalIterator(final ListIterator<DataCell> iterator) {
            m_iterator = iterator;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_iterator.hasNext();
        }

        /** {@inheritDoc} */
        @Override
        public DataCell next() {
            return m_iterator.next();
        }


        /** {@inheritDoc} */
        @Override
        public void replaceLast(final DataCell cell) {
            m_iterator.set(cell);
        }
    }

    /** Map based internal iterator. Used {@link java.util.Map.Entry#setValue(Object)} to modify map. */
    private static final class MapEntryInternalIterator extends InternalIterator {

        private final Iterator<Map.Entry<?, DataCell>> m_mapIterator;
        private Map.Entry<?, DataCell> m_lastReturnedEntry;

        /** @param mapIterator iterator to wrap (only the values)
         */
        MapEntryInternalIterator(final Iterator<Entry<?, DataCell>> mapIterator) {
            m_mapIterator = mapIterator;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasNext() {
            return m_mapIterator.hasNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell next() {
            m_lastReturnedEntry = m_mapIterator.next();
            return m_lastReturnedEntry.getValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void replaceLast(final DataCell cell) {
            if (m_lastReturnedEntry == null) {
                throw new IllegalStateException("next not called");
            }
            m_lastReturnedEntry.setValue(cell);
            m_lastReturnedEntry = null;
        }

    }

}
