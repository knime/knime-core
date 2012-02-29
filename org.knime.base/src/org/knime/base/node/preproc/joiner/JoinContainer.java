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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   25.11.2009 (Heiko Hofer): created
 */
package org.knime.base.node.preproc.joiner;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;

/**
 * A Container used to collect DataRows. The container has three
 * categories for inner, left outer and right outer matches.
 *
 * @author Heiko Hofer
 */
final class JoinContainer {
    private BufferedDataContainer m_matches;

    private BufferedDataContainer m_rightOuter;

    private BufferedDataContainer m_leftOuter;

    private DataTableSpec m_spec;

    private int m_rowCount;

    /**
     * Create a new instance.
     * @param settings The settings object of the OutputRow.
     */
    JoinContainer(
            final OutputRow.Settings settings) {
        m_spec = settings.getSpec();
        m_rowCount = 0;
    }


    /**
     * @return the rowCount
     */
    int getRowCount() {
        return m_rowCount;
    }

    /**
     * Add an inner match to the {@link JoinContainer}.
     *
     * @param row The matching row.
     * @param exec The Execution context.
     */
    void addMatch(final DataRow row,
            final ExecutionContext exec) {
        if (null == m_matches) {
            m_matches = exec.createDataContainer(m_spec);
        }
        m_matches.addRowToTable(row);
        m_rowCount++;
    }

    /**
     * Add an outer match of the right table to this {@link JoinContainer}.
     * This is a row of the right table appended by missing data cells which
     * does not match a row of the left table.
     *
     * @param row The matching row.
     * @param exec The execution context.
     */
    void addRightOuter(final DataRow row,
            final ExecutionContext exec) {
        if (null == m_rightOuter) {
            m_rightOuter = exec.createDataContainer(m_spec);
        }
        m_rightOuter.addRowToTable(row);

        m_rowCount++;
    }

    /**
     * Add an outer match of the left table to this {@link JoinContainer}.
     * This is a row of the left table appended by missing data cells which
     * does not match a row of the right table.
     *
     * @param row The matching row.
     * @param exec The execution context.
     */
    void addLeftOuter(final DataRow row,
            final ExecutionContext exec) {
        if (null == m_leftOuter) {
            m_leftOuter = exec.createDataContainer(m_spec);
        }
        m_leftOuter.addRowToTable(row);

        m_rowCount++;
    }

    /**
     * @return The {@link DataTable} which holds the inner joins.
     */
    BufferedDataTable getMatches() {
        return null != m_matches ? m_matches.getTable() : null;
    }

    /**
     * @return The {@link DataTable} which holds the right outer joins.
     */
    BufferedDataTable getRightOuter() {
        return null != m_rightOuter ? m_rightOuter.getTable() : null;
    }

    /**
     * @return The {@link DataTable} which holds the left outer joins.
     */
    BufferedDataTable getLeftOuter() {
        return null != m_leftOuter ? m_leftOuter.getTable() : null;
    }

    /**
     * Close this container.
     */
    public void close() {
        if (null != m_matches) {
            m_matches.close();
        }
        if (null != m_rightOuter) {
            m_rightOuter.close();
        }
        if (null != m_leftOuter) {
            m_leftOuter.close();
        }
    }
}
