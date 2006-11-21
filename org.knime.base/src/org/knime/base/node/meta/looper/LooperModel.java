/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.meta.MetaNodeModel;

/**
 * This is the model for the looper node which allows execution of an inner
 * workflow multiple times. The results are collected in the output table.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class LooperModel extends MetaNodeModel {
    private final LooperSettings m_settings = new LooperSettings();

    /**
     * Creates a new new looper node model.
     */
    public LooperModel() {
        super(1, 1, 0, 0);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #execute(org.knime.core.node.BufferedDataTable[],
     *      org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        BufferedDataContainer output = null;
        for (int i = 0; i < m_settings.loops(); i++) {
            exec.setProgress(i / (double)m_settings.loops(),
                    "Executing loop " + (i + 1));

            resetAndConfigureInternalWF();
            executeInternalWF();

            exec.checkCanceled();
            if (innerExecCanceled()) {
                throw new CanceledExecutionException("Inner workflow canceled");
            }
            if (dataOutModel(0).getBufferedDataTable() == null) {
                throw new Exception("Execution failed in inner node");
            }

            DataTable innerResult = dataOutModel(0).getBufferedDataTable();
            if (output == null) {
                output = exec.createDataContainer(innerResult
                        .getDataTableSpec());
            }

            for (DataRow row : innerResult) {
                RowKey newKey = new RowKey(row.getKey() + "_" + (i + 1));
                
                DataCell[] cells = new DataCell[row.getNumCells()];
                for (int m = 0; m < cells.length; m++) {
                    cells[m] = row.getCell(m);
                }
                output.addRowToTable(new DefaultRow(newKey, cells));
            }
        }
        if (output != null) {
            output.close();
            return new BufferedDataTable[]{output.getTable()};
        } else {
            return new BufferedDataTable[] {null};
        }
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #loadValidatedSettingsFrom(java.io.File,
     *      org.knime.core.node.NodeSettingsRO,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadValidatedSettingsFrom(final File nodeFile,
            final NodeSettingsRO settings, final ExecutionMonitor exec)
            throws InvalidSettingsException, IOException {
        super.loadValidatedSettingsFrom(nodeFile, settings, exec);
        
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *  #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_settings.saveSettingsTo(settings);
    }

    /**
     * @see org.knime.core.node.meta.MetaNodeModel
     *      #validateSettings(java.io.File, org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final File nodeFile,
            final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(nodeFile, settings);

        LooperSettings s = new LooperSettings();
        s.loadSettingsFrom(settings);
        if (s.loops() < 1) {
            throw new InvalidSettingsException("Loops count must be > 0");
        }
    }
}
