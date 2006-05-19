/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   May 19, 2006 (wiswedel): created
 */
package de.unikn.knime.base.node.io.table.write;

import java.io.File;

import de.unikn.knime.base.data.container.DataContainer;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class WriteTableNodeModel extends NodeModel {
    
    static final String CFG_FILENAME = "filename";
    
    private String m_fileName;
    
    public WriteTableNodeModel() {
        super(1, 0);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#saveSettingsTo(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void saveSettingsTo(NodeSettings settings) {
        if (m_fileName != null) {
            settings.addString(CFG_FILENAME, m_fileName);
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#validateSettings(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void validateSettings(NodeSettings settings)
            throws InvalidSettingsException {
        settings.getString(CFG_FILENAME);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#loadValidatedSettingsFrom(de.unikn.knime.core.node.NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(NodeSettings settings)
            throws InvalidSettingsException {
        m_fileName = settings.getString(CFG_FILENAME);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#execute(de.unikn.knime.core.data.DataTable[], de.unikn.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected DataTable[] execute(DataTable[] inData, ExecutionMonitor exec)
            throws Exception {
        DataTable in = inData[0];
        DataContainer.writeToZip(in, new File(m_fileName), exec);
        return new DataTable[0];
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {

    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#configure(de.unikn.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[0];
    }

}
