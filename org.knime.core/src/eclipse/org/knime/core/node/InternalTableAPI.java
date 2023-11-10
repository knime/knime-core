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
 *   Dec 20, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node;

import org.knime.core.data.TableBackend;
import org.knime.core.table.row.Selection;

/**
 * Provides internal API for manipulating tables.
 * May change at any point in time.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @since 5.1
 */
public final class InternalTableAPI {

    private InternalTableAPI() {

    }

    /**
     * Creates a new table consisting of the specified slice of the input table.
     *
     * @param exec for creating the table
     * @param table that is sliced
     * @param slice to extract from the table
     * @return the sliced table
     */
    public static BufferedDataTable slice(final ExecutionContext exec, final BufferedDataTable table,
        final Selection slice) {
        return exec.createSlicedTable(table, slice);
    }

    /**
     * Appends the two tables column-wise according to the provided config.
     *
     * @param exec for creating the table and providing progress
     * @param config for the appending
     * @param left table
     * @param right table
     * @return the appended table
     * @throws CanceledExecutionException if execution is cancelled by the user
     * @since 5.1
     */
    public static BufferedDataTable append(final ExecutionContext exec, final TableBackend.AppendConfig config,
        final BufferedDataTable left, final BufferedDataTable right) throws CanceledExecutionException {
        config.validateInputTables(left, right);
        return exec.appendTables(config, left, right);
    }

    /**
     * Concatenates the given tables and creates new RowIDs to ensure uniqueness.
     *
     * @param exec for concatenating the tables
     * @param tables to concatenate
     * @return the concatenated table
     * @since 5.1
     */
    public static BufferedDataTable concatenateWithNewRowID(final ExecutionContext exec,
        final BufferedDataTable... tables) {
        return exec.createConcatenateTableWithNewRowIDs(tables);
    }

    public static BufferedDataTable prependIndexColumn(final ExecutionContext exec, final BufferedDataTable table,
        final String indexColumnName) {
        return exec.prependRowIndex(table, indexColumnName);
    }

}
