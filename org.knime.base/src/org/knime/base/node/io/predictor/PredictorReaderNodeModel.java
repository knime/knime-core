/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   30.10.2005 (mb): created
 */
package org.knime.base.node.io.predictor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Read ModelContent object from file.
 *
 * @author M. Berthold, University of Konstanz
 */
public class PredictorReaderNodeModel extends NodeModel {

    /** key for filename entry in config object. */
    static final String FILENAME = "filename";

    private final SettingsModelString m_fileName =
            new SettingsModelString(FILENAME, null);

    private ModelContentRO m_predParams;

    /**
     * Constructor: Create new NodeModel with only one Model Input Port.
     */
    public PredictorReaderNodeModel() {
        super(0, 0, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_fileName.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_fileName.loadSettingsFrom(settings);
    }

    /**
     * Save model into ModelContent for a specific output port.
     *
     * @param index of the ModelContent's output port.
     * @param predParam The object to write the model into.
     * @throws InvalidSettingsException If the model could not be written to
     *             file.
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParam) throws InvalidSettingsException {
        assert index == 0 : index;
        if (predParam != null && m_predParams != null) {
            m_predParams.copyTo(predParam);
        }
    }

    /**
     * Execute does nothing - the reading of the file and writing to the
     * NodeSettings object has already happened during savePredictorParams.
     *
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            IOException {
        m_predParams = null;
        InputStream is = new BufferedInputStream(new FileInputStream(
                        new File(m_fileName.getStringValue())));
        // if file ending is ".gz"
        if (m_fileName.getStringValue().toLowerCase().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        } else if (
            !m_fileName.getStringValue().toLowerCase().endsWith(".pmml")) {
            // file does not end with ".gz" and ".pmml"
            try {
                // try to open temp zip stream
                GZIPInputStream zip = new GZIPInputStream(
                    new FileInputStream(new File(m_fileName.getStringValue())));
                zip.close();
                is = new GZIPInputStream(is);
            } catch (IOException ioe) {
                // ignored, seems to be zip archive
            }
        }
        exec.setMessage("Reading model from file: "
                + m_fileName.getStringValue());
        m_predParams = ModelContent.loadFromXML(is);
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
     * {@inheritDoc}
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
     * @param fileName The file to check @throws InvalidSettingsException If
     * that fails.
     */
    private void checkFileAccess(final String fileName)
            throws InvalidSettingsException {
        if (fileName == null) {
            throw new InvalidSettingsException("No file set.");
        }
        File file = new File(fileName);
        if (file.isDirectory()) {
            throw new InvalidSettingsException("\"" + file.getAbsolutePath()
                    + "\" is a directory, but must be a file.");
        }
        if (!file.exists()) {
            throw new InvalidSettingsException(
                    "File \"" + file.getAbsolutePath() + "\""
                        + " does not exist.");
        }
        // check read access to file
        if (!file.canRead()) {
            throw new InvalidSettingsException("Cannot read from file \""
                    + file.getAbsolutePath() + "\".");
        }
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

}
