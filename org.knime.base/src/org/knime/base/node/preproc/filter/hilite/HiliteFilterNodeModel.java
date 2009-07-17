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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.filter.hilite;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * 
 * @author thiel, University of Konstanz
 */
public class HiliteFilterNodeModel extends NodeModel implements HiLiteListener {

    /**
     * Creates an instance of HiliteFilterNodeModel.
     */
    public HiliteFilterNodeModel() {
        super(1, 2);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 1;
        return new DataTableSpec[]{inSpecs[0], inSpecs[0]};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        BufferedDataContainer bufIn = exec.createDataContainer(inSpec);
        BufferedDataContainer bufOut = exec.createDataContainer(inSpec);
        synchronized (m_inHdl) {
            double rowCnt = inData[0].getRowCount();
            CloseableRowIterator it = inData[0].iterator();
            for (int i = 0; i < rowCnt; i++) {
                DataRow row = it.next();
                if (m_inHdl.isHiLit(row.getKey())) {
                    bufIn.addRowToTable(row);
                } else {
                    bufOut.addRowToTable(row);
                }
                exec.checkCanceled();
                exec.setProgress((i + 1) / rowCnt);
            }
        }
        bufIn.close();
        bufOut.close();
        m_inHdl.addHiLiteListener(this);
        return new BufferedDataTable[]{bufIn.getTable(), bufOut.getTable()};
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
        if (m_inHdl != null) {
            m_inHdl.removeHiLiteListener(this);
        }
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

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
        hiliteWarning();
        m_inHdl.addHiLiteListener(this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }
    
    /** Holds the current input HiLiteHandler. */
    private HiLiteHandler m_inHdl;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, 
            final HiLiteHandler hiLiteHdl) {
        m_inHdl = hiLiteHdl;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_inHdl;
    }
    
    /** Hilite warning, when hilite state as changed. */
    private void hiliteWarning() {
        if (m_inHdl != null) {
            m_inHdl.removeHiLiteListener(this);
        }
        super.setWarningMessage(
                "HiLite status has changed, re-execute node to apply changes.");
    }   
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {
        hiliteWarning();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        hiliteWarning();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        hiliteWarning();
    }

}
