/* 
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
 *   30.10.2005 (mb): created
 */
package de.unikn.knime.base.node.io.predictor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.PredictorParams;

/**
 * Read PredictorParams object from file.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PredictorReaderNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    private String m_fileName = null; // "<no file>";

    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     */
    public PredictorReaderNodeModel() {
        super(0, 0, 0, /* #model ports out= */1);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {
        // if (m_fileName != null) {
        settings.addString(FILENAME, m_fileName);
        // }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        checkFileAccess(settings.getString(FILENAME));
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        m_fileName = checkFileAccess(settings.getString(FILENAME));
    }

    /**
     * Save model into PredictorParams for a specific output port.
     * 
     * @param index of the PredictorParams's ouput port.
     * @param predParam The object to write the model into.
     * @throws InvalidSettingsException If the model could not be written to
     *             file.
     */
    @Override
    protected void savePredictorParams(final int index,
            final PredictorParams predParam) throws InvalidSettingsException {
        assert index == 0 : index;
        try {
            FileInputStream is = new FileInputStream(new File(m_fileName));
            NodeSettings.loadFromXML(predParam, is);
        } catch (IOException ioe) {
            throw new InvalidSettingsException(ioe);
        }
    }

    /**
     * Execute does nothing - the reading of the file and writing to the
     * NodeSettings object has already happened during savePredictorParams.
     * 
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    @Override
    protected DataTable[] execute(final DataTable[] data,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            IOException {
        // execution always succeful: return empty array
        return new DataTable[0];
    }

    /**
     * Ignored.
     * 
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        checkFileAccess(m_fileName);
        return new DataTableSpec[0];
    }

    /**
     * Helper that checks some properties for the file argument.
     * 
     * @param fileName The file to check
     * @throws InvalidSettingsException If that fails.
     */
    private String checkFileAccess(final String fileName)
            throws InvalidSettingsException {
        if (fileName == null) {
            throw new InvalidSettingsException("No file set.");
        }
        String newFileName = fileName;
        if (!fileName.endsWith(".pmml")) {
            newFileName += ".pmml";
        }
        File file = new File(newFileName);
        if (file.isDirectory()) {
            throw new InvalidSettingsException("\"" + file.getAbsolutePath()
                    + "\" is a directory.");
        }
        if (!file.exists()) {
            // dunno how to check the write access to the directory. If we can't
            // create the file the execute of the node will fail. Well, too bad.
            throw new InvalidSettingsException("File does not exist: "
                    + newFileName);
        }
        if (!file.canRead()) {
            throw new InvalidSettingsException("Cannot write to file \""
                    + file.getAbsolutePath() + "\".");
        }
        return newFileName;
    }
}
