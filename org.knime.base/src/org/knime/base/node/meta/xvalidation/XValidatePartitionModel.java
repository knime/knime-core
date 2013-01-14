/* Created on Jul 10, 2006 4:19:52 PM by thor
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.meta.xvalidation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This is the cross validation partitioning node model that divides the input
 * table into partitions. It will only work together with a successing
 * {@link AggregateOutputNodeModel}.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidatePartitionModel extends NodeModel implements
        LoopStartNodeTerminator {
    private final XValidateSettings m_settings = new XValidateSettings();

    private short[] m_partNumbers;

    private int m_nrIterations;

    private int m_currIteration;

    /**
     * Creates a new model for the internal partitioner node.
     */
    public XValidatePartitionModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        new XValidateSettings().loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        boolean inLoop = (m_partNumbers != null);
        if (!inLoop) {
            if (m_settings.leaveOneOut()) {
                m_nrIterations = inData[0].getRowCount();
                m_currIteration = 0;
                m_partNumbers = new short[0];
            } else {
                m_partNumbers = new short[inData[0].getRowCount()];

                final double partSize =
                        m_partNumbers.length / (double)m_settings.validations();

                if (m_settings.stratifiedSampling()) {
                    ExecutionMonitor subExec = exec.createSubProgress(0.0);
                    subExec.setMessage("Preparing stratified sampling");
                    Map<DataCell, List<Integer>> valueCounts =
                            countValues(inData[0], subExec,
                                    m_settings.classColumn());

                    int part = 0;
                    for (Map.Entry<DataCell, List<Integer>> e : valueCounts
                            .entrySet()) {
                        List<Integer> l = e.getValue();

                        for (Integer i : l) {
                            m_partNumbers[i] = (short)part++;
                            part %= m_settings.validations();
                        }
                    }
                } else {
                    for (int i = 0; i < m_partNumbers.length; i++) {
                        m_partNumbers[i] =
                                (short)Math.min(i / partSize,
                                        m_partNumbers.length);
                    }

                    if (m_settings.randomSampling()) {
                        long seed =
                                m_settings.useRandomSeed() ? m_settings
                                        .randomSeed() : System
                                        .currentTimeMillis();
                        Random rand = new Random(seed);

                        for (int i = 0; i < m_partNumbers.length; i++) {
                            int pos = rand.nextInt(m_partNumbers.length);
                            short x = m_partNumbers[pos];
                            m_partNumbers[pos] = m_partNumbers[i];
                            m_partNumbers[i] = x;
                        }
                    }
                }
                m_nrIterations = m_settings.validations();
                m_currIteration = 0;
            }
        }

        BufferedDataContainer test =
                exec.createDataContainer(inData[0].getDataTableSpec());

        BufferedDataContainer train =
                exec.createDataContainer(inData[0].getDataTableSpec());

        int count = 0;
        final double max = inData[0].getRowCount();
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(count / max);

            if (m_settings.leaveOneOut() && (count == m_currIteration)) {
                test.addRowToTable(row);
            } else if (!m_settings.leaveOneOut()
                    && (m_partNumbers[count] == m_currIteration)) {
                test.addRowToTable(row);
            } else {
                train.addRowToTable(row);
            }
            count++;
        }
        test.close();
        train.close();

        // we need to put the counts on the stack for the loop's tail to see:
        pushFlowVariableInt("currentIteration", m_currIteration);
        pushFlowVariableInt("maxIterations", m_nrIterations);
        m_currIteration++;

        return new BufferedDataTable[]{train.getTable(), test.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_currIteration = 0;
        m_nrIterations = -1;
        m_partNumbers = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return m_currIteration >= m_nrIterations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert m_currIteration == 0;

        if (m_settings.stratifiedSampling()) {
            if (m_settings.classColumn() == null) {
                throw new InvalidSettingsException("No class column for "
                        + "stratified sampling selected");
            }
            if (!inSpecs[0].containsName(m_settings.classColumn())) {
                throw new InvalidSettingsException("Class column '"
                        + m_settings.classColumn()
                        + "' does not exist in input table");
            }
        }

        // we need to put the counts on the stack for the loop's tail to see:
        pushFlowVariableInt("currentIteration", m_currIteration);
        pushFlowVariableInt("maxIterations", m_nrIterations);
        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do here
    }

    private Map<DataCell, List<Integer>> countValues(
            final BufferedDataTable table, final ExecutionMonitor exec,
            final String classColumn) throws CanceledExecutionException {
        HashMap<DataCell, List<Integer>> valueCounts =
                new LinkedHashMap<DataCell, List<Integer>>();

        int classColIndex =
                table.getDataTableSpec().findColumnIndex(classColumn);

        final double rowTotalCount = table.getRowCount();
        int rowCount = 0;
        for (DataRow row : table) {
            exec.setProgress(rowCount / rowTotalCount, "Row " + rowCount
                    + " (\"" + row.getKey() + "\")");
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

        long seed =
                m_settings.useRandomSeed() ? m_settings.randomSeed() : System
                        .currentTimeMillis();
        for (Map.Entry<DataCell, List<Integer>> e : valueCounts.entrySet()) {
            List<Integer> l = e.getValue();
            Collections.shuffle(l, new Random(seed));
        }

        return valueCounts;
    }
}
