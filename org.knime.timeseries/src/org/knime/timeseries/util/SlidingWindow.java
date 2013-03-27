/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ------------------------------------------------------------------------
 * <w
 */
package org.knime.timeseries.util;

import java.util.LinkedList;

/**
 * This class provides a sliding window. It controls the size of the window.
 * It is only possible to insert new values, replace previous ones or get the
 * whole list.
 *
 * @author Adae, University of Konstanz
 * @param <T> the object stored in the window.
 */
public class SlidingWindow<T extends Object> {

    private final int m_size;
    private LinkedList<T> m_list;

    /**
     * @param size the size of the sliding window
     */
    public SlidingWindow(final int size) {
        m_size = size;
        m_list = new LinkedList<T>();
    }


    /**
     * @param e the new last row of the sliding window
     * @return the removed row (the previous first one) or null,
     * if the window was not yet full.
     */
    public T addandget(final T e) {
        T ret = null;
        if (m_list.size() >= m_size) {
            // remove the first
            ret = m_list.removeFirst();
        }
        m_list.add(e);
        return ret;
    }

    /**
     * @param i the position of the wished row  in the window
     * @return the data row on position i, or null, if there  is no such row.
     */
    public T get(final int i) {
        if (i >= m_size) {
            return null;
        }
        try {
            return m_list.get(i);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /**Replaces the Data Row at position i with the given.
     * @param i the position to be replaced
     * @param e the new value (T) for position i
     * @return true, if the replacement was successful, false otherwise.
     */
    public boolean replace(final int i, final T e) {
        if (i >= m_size || e == null) {
            return false;
        }
        try {
            m_list.set(i, e);
        } catch (final IndexOutOfBoundsException exp) {
            return false;
        }
        return true;
    }

    /**
     * @return the complete stored window.
     */
    public LinkedList<T> getList() {
        return m_list;
    }

}
