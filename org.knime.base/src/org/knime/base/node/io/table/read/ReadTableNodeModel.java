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
 *   May 19, 2006 (wiswedel): created
 */
package org.knime.base.node.io.table.read;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * NodeMode for table that reads the file as written from the
 * Write table node.
 * @author wiswedel, University of Konstanz
 */
public class ReadTableNodeModel extends NodeModel {
    
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ReadTableNodeModel.class);
    
    /** Identifier for the node settings object. */
    static final String CFG_FILENAME = "filename";
    
    /** The extension of the files to store, \".knime\". */
    public static final String PREFERRED_FILE_EXTENSION = ".table";
    
    private final SettingsModelString m_fileName = 
        new SettingsModelString(CFG_FILENAME, null);

    /**
     * Creates new model with no inputs, one output.
     */
    public ReadTableNodeModel() {
        super(0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_fileName.getStringValue() != null) {
            m_fileName.saveSettingsTo(settings);
        }

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
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        File f = new File(m_fileName.getStringValue());
        DataTable table = DataContainer.readFromZip(f);
        BufferedDataTable out = exec.createBufferedDataTable(table, exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
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
        if (m_fileName.getStringValue() == null) {
            throw new InvalidSettingsException("No file set.");
        }
        try {
            File f = new File(m_fileName.getStringValue());
            if (!f.isFile()) {
                throw new InvalidSettingsException(
                        "No such file: " + m_fileName.getStringValue());
            }
            DataTableSpec spec = peekDataTableSpec(f);
            if (spec == null) { // if written with 1.3.x and before
                LOGGER.debug("Table spec is not first entry in input file, " 
                        + "need to deflate entire file");
                DataTable outTable = DataContainer.readFromZip(f);
                spec = outTable.getDataTableSpec();
            }
            return new DataTableSpec[]{spec};
        } catch (IOException ioe) {
            String message = ioe.getMessage();
            if (message == null) {
                message = "Unable to read spec from file, "
                    + "no detailed message available.";
            }
            throw new InvalidSettingsException(message);
            
        }
    }
    
    /** Opens the zip file and checks whether the first entry is the spec. If
     * so, the spec is parsed and returned. Otherwise null is returned.
     * 
     * <p> This method is used to fix bug #1141: Dialog closes very slowly.
     * @param file To read from.
     * @return The spec or null (null will be returned when the file was
     * written with a version prior 2.0)
     * @throws IOException If that fails for any reason.
     */
    private DataTableSpec peekDataTableSpec(final File file) 
        throws IOException {
        // must not use ZipFile here as it is known to have memory problems
        // on large files, see e.g. 
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5077277
        ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        ZipEntry entry = zipIn.getNextEntry();
        try {
            // hardcoded constants here as we do not want additional 
            // functionality to DataContainer ... at least not yet.
            if ("spec.xml".equals(entry != null ? entry.getName() : "")) {
                NodeSettingsRO settings = NodeSettings.loadFromXML(
                        new NonClosableInputStream.Zip(zipIn));
                try {
                    NodeSettingsRO specSettings = 
                        settings.getNodeSettings("table.spec");
                    return DataTableSpec.load(specSettings);
                } catch (InvalidSettingsException ise) {
                    IOException ioe = new IOException(
                    "Unable to read spec from file");
                    ioe.initCause(ise);
                    throw ioe;
                }
            } else {
                return null;
            }
        } finally {
            zipIn.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to load
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // not internals to save
    }
}
