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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.data.append.column;

import java.util.Map;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;


/**
 * A default factory that generates cells based on an underlying Map. This
 * default implementation only allows extension by one column.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class DefaultAppendedCellFactory implements AppendedCellFactory {

    private final Map<RowKey, DataCell> m_map;

    /**
     * Creates new factory. The mapping is based on the argument. It has to map
     * the row key to the new (to be appended) cell.
     * 
     * <p>
     * If the map does not contain requested keys the factory method will throw
     * an {@link java.util.NoSuchElementException}. Thus, make sure you provide
     * a complete list.
     * 
     * @param map mapping {@link DataCell} --&gt; {@link DataCell}
     * @throws NullPointerException if the map is <code>null</code>
     */
    public DefaultAppendedCellFactory(final Map<RowKey, DataCell> map) {
        if (map == null) {
            throw new NullPointerException();
        }
        m_map = map;
    }

    /**
     * Get the value to row's key.
     * 
     * @param row where to get the key from
     * @return the cell from the underlying map
     * @throws NullPointerException if the argument is <code>null</code>
     * @throws IllegalArgumentException if the key is not contained in the map
     *             or the value to the key is not instance of {@link DataCell}
     * @see AppendedCellFactory#getAppendedCell(DataRow)
     */
    public DataCell[] getAppendedCell(final DataRow row) {
        RowKey key = row.getKey();
        DataCell val = m_map.get(key);
        return new DataCell[]{val};
    }
}
