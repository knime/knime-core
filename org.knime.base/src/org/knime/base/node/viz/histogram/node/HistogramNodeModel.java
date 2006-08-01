/*
 * ------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   08.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * The NodeModel class of the Histogram plotter.
 * 
 * @author Tobias Koetter, University of Konstanz
 */
public class HistogramNodeModel extends NodeModel {
    private static final String CFG_DATA = "histogramData";

    // private static final String CFG_SETTINGS = "histogramSettings";

    /**
     * Used to store the attribute column name in the settings.
     */
    static final String CFGKEY_ATTRCOLNAME = "HistogramAttrColname";

    /** The <code>BufferedDataTable</code> of the input port. */
    private DataTable m_data;

    /** The name of the attribute column. */
    // private String m_attrColName;
    /**
     * The constructor.
     */
    protected HistogramNodeModel() {
        super(1, 0); // one input, no outputs
        m_data = null;
        setAutoExecutable(true);
        // m_attrColName = null;
    }

    /**
     * @see org.knime.core.node.NodeModel #saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        // settings.addString(CFGKEY_ATTRCOLNAME, m_attrColName);
    }

    /**
     * @see org.knime.core.node.NodeModel #validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        /*
         * try { settings.getString(CFGKEY_ATTRCOLNAME); } catch
         * (InvalidSettingsException ise) { throw new
         * InvalidSettingsException("Attribute column " + "not specified"); }
         */
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // m_attrColName = settings.getString(CFGKEY_ATTRCOLNAME);
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, CFG_DATA);
        m_data = DataContainer.readFromZip(f);
        /*
         * File settingsFile = new File(nodeInternDir, CFG_SETTINGS);
         * FileInputStream in = new FileInputStream(settingsFile);
         * NodeSettingsRO settings = NodeSettings.loadFromXML(in); try {
         * m_attrColName = settings.getString(CFGKEY_ATTRCOLNAME); } catch
         * (InvalidSettingsException e) { throw new IOException(e.getMessage()); }
         */
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals( java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, CFG_DATA);
        DataContainer.writeToZip(m_data, f, exec);
        /*
         * NodeSettings settings = new NodeSettings(CFG_SETTINGS);
         * settings.addString(CFGKEY_ATTRCOLNAME, getSelectedColumnName()); File
         * settingsFile = new File(nodeInternDir, CFG_SETTINGS);
         * FileOutputStream out = new FileOutputStream(settingsFile);
         * settings.saveToXML(out);
         */
    }

    /**
     * @see org.knime.core.node.NodeModel #execute(BufferedDataTable[],
     *      ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        // create the data object
        m_data = inData[0];
        /*
         * int colIdx =
         * m_data.getDataTableSpec().findColumnIndex(m_attrColName); assert
         * colIdx >= 0;
         */
        return new BufferedDataTable[0];
    }

    /**
     * @return the data of the input port.
     */
    public DataTable getData() {
        return m_data;
    }

    /*
     * protected String getSelectedColumnName() { return m_attrColName; }
     */

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        m_data = null;
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        /*
         * if ((m_attrColName == null) || (m_attrColName.length() == 0)) { throw
         * new InvalidSettingsException("Attribute column must be" + "
         * specified."); } int colIdx =
         * inSpecs[0].findColumnIndex(m_attrColName); if (colIdx < 0) { throw
         * new InvalidSettingsException("Specified attribute column" + " not in
         * input table"); }
         */
        return new DataTableSpec[0];
    }
}
