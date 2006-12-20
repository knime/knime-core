/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   29.10.2005 (mb): created
 */
package org.knime.base.node.io.predictor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * Write ModelContent object into file.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class PredictorWriterNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    private final SettingsModelString m_fileName = 
        new SettingsModelString(FILENAME, null);

    private ModelContentRO m_predParams;

    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     */
    public PredictorWriterNodeModel() {
        super(0, 0, 1, 0);
    }

    /**
     * @see NodeModel#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileName.saveSettingsTo(settings);
    }

    /**
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
    }

    /**
     * Load ModelContent from input port.
     * 
     * @see org.knime.core.node.NodeModel#loadModelContent(int,
     *      ModelContentRO)
     */
    @Override
    protected void loadModelContent(final int index,
            final ModelContentRO pConf) {
        assert index == 0 : index;
        m_predParams = pConf;
    }

    /**
     * Writes model as ModelContent to file.
     * 
     * @see NodeModel#execute(BufferedDataTable[],ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            IOException {
        OutputStream os = null;
        try {
            // delete original file
            File realFile = new File(m_fileName.getStringValue());
            if (realFile.exists()) {
                realFile.delete();
            }
            // create temp file
            File tempFile = new File(m_fileName.getStringValue() + "~");
            if (tempFile.exists()) {
                tempFile.delete();
            }
            // open stream
            os = new BufferedOutputStream(new FileOutputStream(tempFile));
            if (m_fileName.getStringValue().endsWith(".gz")) {
                os = new GZIPOutputStream(os);
            }
            exec.setProgress(-1, "Writing to file: " 
                    + m_fileName.getStringValue());
            // and write ModelContent object as XML
            m_predParams.saveToXML(os);
            // and finally rename temp file to real file name
            if (!tempFile.renameTo(realFile)) {
                throw new IOException("write: rename of temp file failed");
            }
        } catch (Exception e) {
            throw new IOException("write to file failed: " + e);
        }
        // execution successful return empty array
        return new BufferedDataTable[0];
    }

    /**
     * Ignored.
     * 
     * @see org.knime.core.node.NodeModel#reset()
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
        checkFileAccess(m_fileName.getStringValue());
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
            throw new InvalidSettingsException("No output file specified.");
        }
        String newFileName = fileName;
        if (!fileName.endsWith(".pmml") && !fileName.endsWith(".pmml.gz")) {
            newFileName += ".pmml.gz";
        }
        File file = new File(newFileName);
        if (file.isDirectory()) {
            throw new InvalidSettingsException("\"" + file.getAbsolutePath()
                    + "\" is a directory.");
        }
        if (file.exists()) {
            // here it exists and we can write it: warn user!
            setWarningMessage("Selected output file exists and will be "
                    + "overwritten!");
            if (!file.canWrite()) {
                throw new InvalidSettingsException("Cannot write to file \""
                    + file.getAbsolutePath() + "\".");
            }
        }
        String tempFileName = newFileName + "~";
        File tempFile = new File(tempFileName);
        if (tempFile.exists() && (!tempFile.canWrite())) {
            throw new InvalidSettingsException("Cannot write to (=delete) temp"
                    + " file \"" + tempFile.getAbsolutePath() + "\".");
        }
        return newFileName;
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // nothing to do here
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File, 
     * org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) throws IOException, 
            CanceledExecutionException {
        // nothing to do here
    }
    
    
}
