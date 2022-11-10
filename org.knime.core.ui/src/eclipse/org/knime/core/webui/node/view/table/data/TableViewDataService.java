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
 *   Nov 26, 2021 (hornm): created
 */
package org.knime.core.webui.node.view.table.data;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public interface TableViewDataService {

    /**
     * @param columns the names of the columns that are to be displayed
     * @param fromIndex index of the first row
     * @param numRows number of rows to get
     * @param rendererIds indicates the renderer to be used per column; can contain {@code null}-values (or be
     *            {@code null} altogether) in which case the default renderer is being used
     * @param updateDisplayedColumns if true, the given columns will be partitioned by being columns in the table. The
     *            missing ones are filtered out and yield a warning. An exception is only thrown if all columns are
     *            missing.
     * @return the table
     */
    Table getTable(String[] columns, long fromIndex, int numRows, String[] rendererIds, boolean updateDisplayedColumns);

    /**
     * @param columns the names of the columns that are to be displayed
     * @param fromIndex index of the first row
     * @param numRows number of rows to get
     * @param sortColumn the name of the column on which to sort
     * @param sortAscending the direction of the sorting
     * @param globalSearchTerm search term that applies to all columns
     * @param columnFilterValue specific filters for each column
     * @param filterRowKeys if row keys should be considered when filtering
     * @param rendererIds indicates the renderer to be used per column; can contain {@code null}-values (or be
     *            {@code null} altogether) in which case the default renderer is being used
     * @param updateDisplayedColumns if true, the given columns will be partitioned by being columns in the table. The
     *            missing ones are filtered out and yield a warning. An exception is only thrown if all columns are
     *            missing.
     * @param updateTotalSelected if true, the current selected rows are loaded and the number of rows in the returned
     *            total table which are selected is returned
     * @return the table
     */
    @SuppressWarnings("java:S107") // accept the large number of parameters
    Table getFilteredAndSortedTable(String[] columns, long fromIndex, int numRows, String sortColumn,
        boolean sortAscending, String globalSearchTerm, String[][] columnFilterValue, boolean filterRowKeys,
        String[] rendererIds, boolean updateDisplayedColumns, boolean updateTotalSelected);

    /**
     * @return the row keys of the currently cached sorted and filtered table or the input table
     */
    String[] getCurrentRowKeys();

    /**
     * @return the current number of selected rows in the last cached filtered table
     */
    Long getTotalSelected();

}
