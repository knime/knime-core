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

    public void setMissingValueStrategy(final MissingValueSortStrategy missingValueStrategy) {
        m_natural = false;
        m_missingValueStrategy = missingValueStrategy;
    }

    public boolean isNaturalSorting() {
        return m_natural;
    }

    public MissingValueSortStrategy getMissingValueSortStrategy() {
        return m_missingValueStrategy;
    }

    public static class ColumnSortInformation {

        private final boolean m_rowkey;
        private final String m_name;
        private final boolean m_ascending;

        public ColumnSortInformation(final String name, final boolean isRowkey, final boolean isAscending) {
            m_name = name;
            m_rowkey = isRowkey;
            m_ascending = isAscending;
        }

        public ColumnSortInformation(final String name) {
            this(name, false, true);
        }

        public ColumnSortInformation(final String name, final boolean isAscending) {
            this(name, false, isAscending);
        }

        public ColumnSortInformation(final boolean isAscending) {
            this(null, true, isAscending);
        }
    }

    public static enum MissingValueSortStrategy {
        FIRST, LAST
    }

}
