/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.core.data.append;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsIterator.PairSupplier;
import org.knime.core.data.append.AppendedRowsIterator.RuntimeCanceledExecutionException;
import org.knime.core.data.append.AppendedRowsIterator.TableIndexAndRowKey;
import org.knime.core.data.append.AppendedRowsTable.DuplicatePolicy;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.Pair;

/**
 * Extended {@link RowInput} that pulls its data from an array of argument {@link RowInput}. For duplicate Row-ID
 * handling, column mis-matches etc. see {@link AppendedRowsTable} for details.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @since 3.1
 */
public class AppendedRowsRowInput extends RowInput {

    /** The iterator reading the data. */
    private final AppendedRowsIterator m_iterator;

    /**
     * The DataTableSpec for this table (defined mainly be the order of the top table (apart from column that only occur
     * in other tables).
     */
    private final DataTableSpec m_spec;

    private AppendedRowsRowInput(final DataTableSpec spec, final AppendedRowsIterator iterator) {
        m_spec = spec;
        m_iterator = iterator;
    }

    /** {@inheritDoc} */
    @Override
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /** {@inheritDoc} */
    @Override
    public DataRow poll() throws InterruptedException {
        if (m_iterator.hasNext()) {
            try {
                return m_iterator.next();
            } catch (RuntimeCanceledExecutionException cancelEx) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                throw cancelEx;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        m_iterator.close();
    }

    /**
     * @return number rows skipped as per duplicate handling.
     * @see AppendedRowsIterator#getNrRowsSkipped()
     */
    public int getNrRowsSkipped() {
        return m_iterator.getNrRowsSkipped();
    }



    /**
     * @return de-duplicate map, used for hilite translation
     * @see AppendedRowsIterator#getDuplicateNameMap()
     */
    public Map<RowKey, RowKey> getDuplicateNameMap() {
        return m_iterator.getDuplicateNameMap();
    }

    /**
     * @return de-duplicate map, used for hilite translation, including the table index of the input table
     * @see AppendedRowsIterator#getDuplicateNameMapWithIndices()
     * @since 5.4
     */
    public Map<RowKey, TableIndexAndRowKey> getDuplicateNameMapWithIndices() {
        return m_iterator.getDuplicateNameMapWithIndices();
    }

    /**
     * Concatenates a set of Inputs. Duplicates are handled according to the policy argument. Columns present in one
     * input but not the others or conflicting column tables are handled as described in {@link AppendedRowsTable}.
     * <br><br>
     * NOTE: This method calls {@link #create(RowInput[], DuplicatePolicy, String, ExecutionMonitor, long, boolean)}
     * with {@code fillDuplicateMap} set to {@code false} because the duplicate map may cause memory problems for large
     * tables.
     *
     * @param ins all inputs to be appended (non-null and no null values allowed)
     * @param duplPolicy How to deal with duplicate keys. Non-null. Note: {@link DuplicatePolicy#Skip} and
     *            {@link DuplicatePolicy#AppendSuffix} store all RowIDs in memory which can cause memory issues for
     *            large tables
     * @param suffix suffix to append to duplicate keys (must not be null if policy is
     *            {@link DuplicatePolicy#AppendSuffix})
     * @param exec (optional) execution monitor that is used to report progress and check for cancelation. Can be null.
     * @param totalRowCount The number of rows to expect (sum over all row counts in the inputs). Only be used for
     *            progress -- can be negative to have no progress.
     * @return a new row input whose iteration scans all argument inputs.
     */
    public static AppendedRowsRowInput create(final RowInput[] ins, final DuplicatePolicy duplPolicy,
        final String suffix, final ExecutionMonitor exec, final long totalRowCount) {
        return create(ins, duplPolicy, suffix, exec, totalRowCount, false);
    }

    /**
     * Concatenates a set of Inputs. Duplicates are handled according to the policy argument. Columns present in one
     * input but not the others or conflicting column tables are handled as described in {@link AppendedRowsTable}.
     *
     * @param ins all inputs to be appended (non-null and no null values allowed)
     * @param duplPolicy How to deal with duplicate keys. Non-null. Note: {@link DuplicatePolicy#Skip} and
     *            {@link DuplicatePolicy#AppendSuffix} store all RowIDs in memory which can cause memory issues for
     *            large tables
     * @param suffix suffix to append to duplicate keys (must not be null if policy is
     *            {@link DuplicatePolicy#AppendSuffix})
     * @param exec (optional) execution monitor that is used to report progress and check for cancelation. Can be null.
     * @param totalRowCount The number of rows to expect (sum over all row counts in the inputs). Only be used for
     *            progress -- can be negative to have no progress.
     * @param fillDuplicateMap if provided, the duplicate map is filled for all {@link DuplicatePolicy
     *            DuplicatePolicies} including {@link DuplicatePolicy#CreateNew} and {@link DuplicatePolicy#Fail} which
     *            don't fill it otherwise. Note that the duplicate map may cause memory issues for large tables
     * @return a new row input whose iteration scans all argument inputs.
     * @since 5.1
     */
    public static AppendedRowsRowInput create(final RowInput[] ins, final DuplicatePolicy duplPolicy,
        final String suffix, final ExecutionMonitor exec, final long totalRowCount, final boolean fillDuplicateMap) {
        DataTableSpec[] specs = new DataTableSpec[ins.length];
        for (int i = 0; i < specs.length; i++) {
            specs[i] = ins[i].getDataTableSpec();
        }
        DataTableSpec spec = AppendedRowsTable.generateDataTableSpec(specs);
        CheckUtils.checkArgumentNotNull(duplPolicy, "Arg must not be null");
        if (DuplicatePolicy.AppendSuffix.equals(duplPolicy)) {
            CheckUtils.checkArgument(StringUtils.isNotEmpty(suffix), "Suffix must not be an empty string.");
        }

        PairSupplier[] suppliers = new PairSupplier[ins.length];
        for (int i = 0; i < suppliers.length; i++) {
            suppliers[i] = new PairSupplier(new Pair<RowIterator, DataTableSpec>(
                    new RowInputIterator(ins[i]), ins[i].getDataTableSpec()));
        }
        AppendedRowsIterator it =
            new AppendedRowsIterator(suppliers, duplPolicy, suffix, spec, exec, totalRowCount, fillDuplicateMap);
        return new AppendedRowsRowInput(spec, it);
    }

    /** Iterator on {@link RowInput}. {@link InterruptedException} is wrapped in {@link RuntimeException}. */
    static final class RowInputIterator extends RowIterator {

        private final RowInput m_input;

        private DataRow m_internalNext;

        RowInputIterator(final RowInput input) {
            m_input = input;
            internalNext();
        }

        private void internalNext() {
            try {
                m_internalNext = m_input.poll();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return m_internalNext != null;
        }

        /** {@inheritDoc} */
        @Override
        public DataRow next() {
            final DataRow result = m_internalNext;
            CheckUtils.checkState(result != null, "No more rows");
            internalNext();
            return result;
        }
    }
}
