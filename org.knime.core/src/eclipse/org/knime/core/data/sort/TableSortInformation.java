/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   28 Feb 2019 (albrecht): created
 */
package org.knime.core.data.sort;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 */
public class TableSortInformation {

    private final Queue<ColumnSortInformation> m_columns;

    private boolean m_natural;

    private MissingValueSortStrategy m_missingValueStrategy;

    private TableSortInformation(final Queue<ColumnSortInformation> columns, final boolean natural,
        final MissingValueSortStrategy missingValueStrategy) {
        m_columns = columns;
        m_natural = natural;
        m_missingValueStrategy = missingValueStrategy;
    }

    public TableSortInformation() {
        this(new LinkedList<ColumnSortInformation>(), true, MissingValueSortStrategy.LAST);
    }

    public boolean isNaturalSorting() {
        return m_natural;
    }

    /**
     * @return the columns
     */
    public Queue<ColumnSortInformation> getColumns() {
        return m_columns;
    }

    public void addColumn(final ColumnSortInformation colInfo) {
        m_columns.add(colInfo);
    }

    public void setColumns(final Queue<ColumnSortInformation> columns) {
        m_columns.clear();
        m_columns.addAll(columns);
    }

    public MissingValueSortStrategy getMissingValueSortStrategy() {
        return m_missingValueStrategy;
    }

    public void setMissingValueStrategy(final MissingValueSortStrategy missingValueStrategy) {
        m_natural = false;
        m_missingValueStrategy = missingValueStrategy;
    }

    public static class ColumnSortInformation {

        private final String m_name;

        private final SortDirection m_direction;

        private final boolean m_rowkey;

        public ColumnSortInformation(final String name, final SortDirection direction, final boolean isRowkey) {
            m_name = name;
            m_direction = direction;
            m_rowkey = isRowkey;
        }

        public ColumnSortInformation(final String name) {
            this(name, SortDirection.ASCENDING, false);
        }

        public ColumnSortInformation(final String name, final SortDirection direction) {
            this(name, direction, false);
        }

        public ColumnSortInformation(final SortDirection direction) {
            this(null, direction, true);
        }

        /**
         * @return the name
         */
        public String getName() {
            return m_name;
        }

        /**
         * @return the rowkey
         */
        public boolean isRowkey() {
            return m_rowkey;
        }

        /**
         * @return the ascending
         */
        public SortDirection getDirection() {
            return m_direction;
        }
    }

    public static enum SortDirection {
            ASCENDING, DESCENDING;
    }

    public static enum MissingValueSortStrategy {
            FIRST, LAST
    }

}
