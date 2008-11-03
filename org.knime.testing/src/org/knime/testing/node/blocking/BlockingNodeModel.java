/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 *   01.11.2008 (wiswedel): created
 */
package org.knime.testing.node.blocking;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.Lock;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
class BlockingNodeModel extends NodeModel {
    
    private final SettingsModelString m_lockIDModel;
    
    /** One data input, one data output.
     */
    BlockingNodeModel() {
        super(1, 1);
        m_lockIDModel = createLockIDModel();
    }
    
    /** {@inheritDoc} */
    @Override
    protected BufferedDataTable[] execute(BufferedDataTable[] inData,
            ExecutionContext exec) throws Exception {
        Lock lock = getLock();
        lock.lock();
        try {
            return inData;
        } finally {
            lock.unlock();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String id = m_lockIDModel.getStringValue();
        if (id == null || id.length() == 0) {
            throw new InvalidSettingsException("No lock id provided.");
        }
        return inSpecs;
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lockIDModel.loadSettingsFrom(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_lockIDModel.validateSettings(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
        m_lockIDModel.saveSettingsTo(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    
    private Lock getLock() throws InvalidSettingsException {
        String id = m_lockIDModel.getStringValue();
        if (id == null) {
            throw new InvalidSettingsException("No lock id set");
        }
        Lock lock = BlockingRepository.get(id);
        if (lock == null) {
            throw new InvalidSettingsException(
                    "No lock associated with id: " + id);
        }
        return lock;
    }
    
    /** Factory method to create the lock id model. 
     * @return a new model used in dialog and model. */
    static final SettingsModelString createLockIDModel() {
        return new SettingsModelString("lock_id", null);
    }

}
