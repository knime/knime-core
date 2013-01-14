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
 * ---------------------------------------------------------------------
 */
package org.knime.base.data.filter.row;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;

/**
 * This class filters a given number of rows from a
 * {@link org.knime.core.data.DataTable} using the
 * {@link FilterRowGenerator} interface to
 * check the criteria.
 * 
 * @see FilterRowGenerator
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class FilterRowTable implements DataTable {
    /*
     * Underlying table to filter.
     */
    private final DataTable m_data;

    /*
     * Used to check for included/excluded rows.
     */
    private final FilterRowGenerator m_gen;

    /**
     * Creates a new row filter table by wraping the given data table. The
     * filter row generator is used to check if each row from the iteration
     * belongs is included in this table.
     * 
     * @param data the underlying data table
     * @param gen the filter row generator
     * @throws NullPointerException if one of the arguments is <code>null</code>
     */
    public FilterRowTable(final DataTable data, final FilterRowGenerator gen) {
        if (data == null) {
            throw new NullPointerException();
        }
        if (gen == null) {
            throw new NullPointerException();
        }
        m_data = data;
        m_gen = gen;
    }

    /**
     * {@inheritDoc}
     */
    public DataTableSpec getDataTableSpec() {
        return m_data.getDataTableSpec();
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator iterator() {
        return new FilterRowIterator(m_data.iterator(), m_gen);
    }
}
