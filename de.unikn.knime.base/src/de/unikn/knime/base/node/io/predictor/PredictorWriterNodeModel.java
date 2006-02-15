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
 *   29.10.2005 (mb): created
 */
package de.unikn.knime.base.node.io.predictor;

import java.io.File;
import java.io.FileOutputStream;
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
 * Write PredictorParams object into file.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PredictorWriterNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    private String m_fileName = null; // "<no file>";

    private PredictorParams m_predParams;

    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     */
    public PredictorWriterNodeModel() {
        super(0, 0, /* #model ports in= */1, 0);
        super.setPredictorParamsInputDescription(0, "Predictor Parameters");
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        // if (m_fileName != null) {
        settings.addString(FILENAME, m_fileName);
        // }
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        checkFileAccess(settings.getString(FILENAME));
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        m_fileName = checkFileAccess(settings.getString(FILENAME));
    }

    /**
     * Load PredictorParams from input port. TODO copy config, not just
     * reference? (mb)
     * 
     * @see de.unikn.knime.core.node.NodeModel#loadPredictorParams(int,
     *      PredictorParams)
     */
    protected void loadPredictorParams(final int index,
            final PredictorParams pConf) {
        assert index == 0 : index;
        m_predParams = pConf;
    }

    /**
     * Writes model as PredictorParams to file.
     * 
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] data,
            final ExecutionMonitor exec) throws CanceledExecutionException,
            IOException {
        try {
            // delete original file
            File realFile = new File(m_fileName);
            if (realFile.exists()) {
                realFile.delete();
            }
            // create temp file
            File tempFile = new File(m_fileName + "~");
            if (tempFile.exists()) {
                tempFile.delete();
            }
            // open stream
            FileOutputStream os = new FileOutputStream(tempFile);
            // and write PredictorParams object as XML
            m_predParams.saveToXML(os);
            os.close();
            // and finally rename temp file to real file name
            if (!tempFile.renameTo(realFile)) {
                throw new IOException("write: rename of temp file failed");
            }
        } catch (Exception e) {
            throw new IOException("write to file failed: " + e);
        }
        // execution succeful return empty array
        return new DataTable[0];
    }

    /**
     * Ignored.
     * 
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
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
        if (file.exists() && !file.canWrite()) {
            throw new InvalidSettingsException("Cannot write to file \""
                    + file.getAbsolutePath() + "\".");
        }
        String tempFileName = newFileName + "~";
        File tempFile = new File(tempFileName);
        if (tempFile.exists() && (!tempFile.canWrite())) {
            throw new InvalidSettingsException("Cannot write to (=delete) temp"
                    + " file \"" + tempFile.getAbsolutePath() + "\".");
        }

        return newFileName;
    }
}
