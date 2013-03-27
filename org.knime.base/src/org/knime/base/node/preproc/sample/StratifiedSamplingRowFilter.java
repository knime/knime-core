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
 * 
 * History
 *   13.06.2007 (thor): created
 */
package org.knime.base.node.preproc.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This row filter retains the distribution of values in a certain column upon
 * filtering out rows.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class StratifiedSamplingRowFilter extends RowFilter {
    private final BitSet m_includedRows;

    private final int m_classes;
    
    private boolean m_includeAll;

    private static HashMap<DataCell, List<Integer>> countValues(
            final DataTable table, final ExecutionMonitor exec,
            final String classColumn) throws CanceledExecutionException {
        HashMap<DataCell, List<Integer>> valueCounts =
                new LinkedHashMap<DataCell, List<Integer>>();

        int classColIndex =
                table.getDataTableSpec().findColumnIndex(classColumn);

        int rowCount = 0;
        for (DataRow row : table) {
            exec.checkCanceled();
            DataCell cell = row.getCell(classColIndex);
            List<Integer> rowKeys = valueCounts.get(cell);
            if (rowKeys == null) {
                rowKeys = new ArrayList<Integer>();
                valueCounts.put(cell, rowKeys);
            }
            rowKeys.add(rowCount);
            rowCount++;
        }

        return valueCounts;
    }

    /**
     * Creates a new stratified sampling row filter.
     * 
     * @param table the table whose rows should be filtered afterwards
     * @param classColumn the column with the "class" labels
     * @param fraction the fraction of rows that should be passed on,
     *        i.e. <b>not</b> filtered out
     * @param exec an execution monitor for cancel checking
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public StratifiedSamplingRowFilter(final DataTable table,
            final String classColumn, final double fraction,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        this(table, classColumn, fraction, new Random(), exec);
    }

    /**
     * Creates a new stratified sampling row filter.
     * 
     * @param table the table whose rows should be filtered afterwards
     * @param classColumn the column with the "class" labels
     * @param fraction the fraction of rows that should be passed on,
     *        i.e. <b>not</b> filtered out
     * @param random a random number generator
     * @param exec an execution monitor for cancel checking
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public StratifiedSamplingRowFilter(final DataTable table,
            final String classColumn, final double fraction,
            final Random random, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        HashMap<DataCell, List<Integer>> valueCounts =
                countValues(table, exec, classColumn);
        m_classes = valueCounts.size();
        int rowCount = 0;
        for (List<Integer> l : valueCounts.values()) {
            rowCount += l.size();
        }
        final int includeCount = (int)Math.round(fraction * rowCount);
        m_includedRows = new BitSet(includeCount);
        computeSampling(valueCounts, random, includeCount);
    }

    /**
     * Creates a new stratified sampling row filter.
     * 
     * @param table the table whose rows should be filtered afterwards
     * @param classColumn the column with the "class" labels
     * @param includeCount the number of rows that should be passed on i.e.
     *            <b>not</b> filtered out
     * @param exec an execution monitor for cancel checking
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public StratifiedSamplingRowFilter(final DataTable table,
            final String classColumn, final int includeCount,
            final ExecutionMonitor exec) throws CanceledExecutionException {
        this(table, classColumn, includeCount, new Random(), exec);
    }

    /**
     * Creates a new stratified sampling row filter.
     * 
     * @param table the table whose rows should be filtered afterwards
     * @param classColumn the column with the "class" labels
     * @param includeCount the number of rows that should be passed on i.e.
     *            <b>not</b> filtered out
     * @param random a random number generator
     * @param exec an execution monitor for cancel checking
     * @throws CanceledExecutionException if the user canceled the execution
     */
    public StratifiedSamplingRowFilter(final DataTable table,
            final String classColumn, final int includeCount,
            final Random random, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        HashMap<DataCell, List<Integer>> valueCounts =
                countValues(table, exec, classColumn);
        m_classes = valueCounts.size();
        m_includedRows = new BitSet(includeCount);
        computeSampling(valueCounts, random, includeCount);
    }

    private void computeSampling(
            final HashMap<DataCell, List<Integer>> valueCounts, 
            final Random random, final int includeCount) {
        int rowCount = 0;
        for (List<Integer> l : valueCounts.values()) {
            rowCount += l.size();
        }

        if (rowCount <= includeCount) {
            m_includeAll = true;
            return;
        }

        int inc = 0;
        double fraction = includeCount / (double)rowCount;
        for (Map.Entry<DataCell, List<Integer>> e : valueCounts.entrySet()) {
            List<Integer> l = e.getValue();

            int max = (int)Math.round(l.size() * fraction);
            Collections.shuffle(l, random);
            for (int i = 0; (i < max) && (inc < includeCount); i++) {
                // remove from end, avoids expensive copy operations!
                m_includedRows.set(l.remove(l.size() - 1));
                inc++;
            }
        }

        Iterator<List<Integer>> it = null;
        while (m_includedRows.cardinality() < includeCount) {
            if ((it == null) || !it.hasNext()) {
                it = valueCounts.values().iterator();
            }
            List<Integer> l = it.next();
            if (l.size() > 0) {
                m_includedRows.set(l.remove(l.size() - 1));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        if (m_includeAll) {
            throw new IncludeFromNowOn();
        }
        return m_includedRows.get(rowIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        // nothing to do
    }
    
    /**
     * Returns the number of distinct values (classes) in the class column
     * that is used for the stratified sampling.
     * 
     * @return the number of classes
     */
    public int getClassCount() {
        return m_classes;
    }
}
