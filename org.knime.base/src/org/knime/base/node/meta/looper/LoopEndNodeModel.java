/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * This model is the tail node of a for loop.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class LoopEndNodeModel extends NodeModel implements LoopEndNode {

    private BufferedDataContainer m_resultContainer;
    private int m_count;

    /**
     * Creates a new model.
     */
    public LoopEndNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0])};
    }

    private static DataTableSpec createSpec(final DataTableSpec inSpec) {
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(
                        inSpec, "Iteration"), IntCell.TYPE);
        DataTableSpec newSpec = new DataTableSpec(crea.createSpec());

        return new DataTableSpec(inSpec, newSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        if (!(this.getLoopStartNode() instanceof LoopStartNodeTerminator)) {
            throw new IllegalStateException("Loop End is not connected"
                   + " to matching/corresponding Loop Start node. You"
                   + " are trying to create an infinite loop!");
        }
        BufferedDataTable in = inData[0];
        DataTableSpec amendedSpec = createSpec(in.getDataTableSpec());
        if (m_resultContainer == null) {
            // first time we are getting to this: open container
            m_resultContainer = exec.createDataContainer(amendedSpec);
        } else if (!amendedSpec.equalStructure(m_resultContainer.getTableSpec())) {
            DataTableSpec predSpec = m_resultContainer.getTableSpec();
            StringBuilder error = new StringBuilder(
                    "Input table's structure differs from reference " 
                    + "(first iteration) table: ");
            if (amendedSpec.getNumColumns() != predSpec.getNumColumns()) {
                error.append("different column counts ");
                error.append(amendedSpec.getNumColumns());
                error.append(" vs. ").append(predSpec.getNumColumns());
            } else {
                for (int i = 0; i < amendedSpec.getNumColumns(); i++) {
                    DataColumnSpec inCol = amendedSpec.getColumnSpec(i);
                    DataColumnSpec predCol = predSpec.getColumnSpec(i);
                    if (!inCol.equalStructure(predCol)) {
                      error.append("Column ").append(i).append(" [");
                      error.append(inCol).append("] vs. [");
                      error.append(predCol).append("]");
                    }
                }
            }
            throw new IllegalArgumentException(error.toString());
        }

        IntCell currIterCell = new IntCell(m_count);
        for (DataRow row : in) {
            AppendedColumnRow newRow =
                    new AppendedColumnRow(new DefaultRow(new RowKey(row.getKey()
                    + "#" + m_count), row), currIterCell);
            m_resultContainer.addRowToTable(newRow);
        }

        boolean terminateLoop = 
            ((LoopStartNodeTerminator)this.getLoopStartNode()).terminateLoop();
        if (terminateLoop) {
            // this was the last iteration - close container and continue
            m_resultContainer.close();
            BufferedDataTable outTable = m_resultContainer.getTable();
            m_resultContainer.close();
            m_resultContainer = null;
            m_count = 0;
            return new BufferedDataTable[]{outTable};
        } else {
            continueLoop();
            m_count++;
            return new BufferedDataTable[1];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultContainer = null;
        m_count = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }
}
