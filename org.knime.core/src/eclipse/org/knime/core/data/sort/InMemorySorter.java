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
 *   Apr 4, 2024 (leonard.woerteler): created
 */
package org.knime.core.data.sort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.knime.core.data.DataRow;
import org.knime.core.data.sort.ExternalSorter.Progress;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 *
 * @author leonard.woerteler
 * @since 5.4
 */
public final class InMemorySorter {

    private InMemorySorter() {
    }

    public static BufferedDataTable sortedTable(final ExecutionContext exec, final BufferedDataTable inputTable,
            final Comparator<DataRow> comparator) throws CanceledExecutionException {
        final var rowsInInputTable = inputTable.size();
        if (rowsInInputTable < 2) {
            // trivially sorted
            return inputTable;
        }

        try (final var progress = new Progress(exec)) {
            final var readCounter = new AtomicLong();
            final var readFraction = progress.newFractionBuilder(readCounter::get, rowsInInputTable);
            final Supplier<String> readMsg = () -> readFraction.apply(
                new StringBuilder("Reading data (row ")).append(")").toString();
            final var rowList = new ArrayList<DataRow>();
            try (final var readProgress = progress.createSubProgress(0.4)) {
                for (final DataRow row : inputTable) {
                    readProgress.checkCanceled();
                    readProgress.update(readCounter.incrementAndGet() / (double)rowsInInputTable, readMsg);
                    rowList.add(row);
                }
            }

            progress.update(() -> "Sorting...");
            try (final var sortProgress = progress.createSubProgress(0.2)) {
                Collections.sort(rowList, comparator);
            }

            var dc = exec.createDataContainer(inputTable.getDataTableSpec(), false);
            final var writeCounter = new AtomicLong();
            final var writeFraction = progress.newFractionBuilder(writeCounter::get, rowsInInputTable);
            final Supplier<String> writeMsg = () -> writeFraction.apply(
                new StringBuilder("Writing output table (row ")).append(")").toString();
            try (final var writeProgress = progress.createSubProgress(0.4)) {
                for (final DataRow r : rowList) {
                    writeProgress.checkCanceled();
                    writeProgress.update(writeCounter.incrementAndGet() / (double)rowsInInputTable, writeMsg);
                    dc.addRowToTable(r);
                }
                dc.close();
                final var output = dc.getTable();
                dc = null;
                return output;
            } finally {
                if (dc != null) {
                    dc.close();
                    exec.clearTable(dc.getTable());
                }
            }
        }
    }
}
