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
 *   11.02.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.arffreader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.BufferedDataTable;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;

/**
 * The model of the ARFF reader node. The interesting work is done in the
 * <code>ARFFTable</code> and <code>ARFFRowIterator</code>.
 * 
 * @author ohl, University of Konstanz
 */
public class ARFFReaderNodeModel extends NodeModel {
    
    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(ARFFReaderNodeModel.class);

    /** key used to store the ARFF file location in the settings object. */
    static final String CFGKEY_FILEURL = "FileURL";

    /** key used to store the row prefix in the settings object. */
    static final String CFGKEY_ROWPREFIX = "RowPrefix";

    private String m_rowPrefix;

    private URL m_file;

    /**
     * creates a new ARFF reader model.
     */
    public ARFFReaderNodeModel() {
        super(0, 1);
        m_rowPrefix = null;
        m_file = null;
        reset();
    }

    /**
     * creates a new ARFF reader with a default file.
     * @param arffFileLocation URL to the ARFF file to read.
     */
    public ARFFReaderNodeModel(final String arffFileLocation) {
        this();
        try {
            m_file = stringToURL(arffFileLocation);
        } catch (MalformedURLException mue) {
            LOGGER.error(mue.getMessage()); 
        }
    }
    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) 
            throws InvalidSettingsException {
        if (m_file == null) {
            throw new InvalidSettingsException("File is not specified.");
        }
        try {
            return new DataTableSpec[]{
                ARFFTable.createDataTableSpecFromARFFfile(m_file, null)};
        } catch (IOException ioe) {
            throw new InvalidSettingsException(
                    "ARFFReader: I/O Error", ioe);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException(
                    "ARFFReader: ARFF Header Error", ise);
        } catch (CanceledExecutionException cee) {
            // never flies
            throw new InvalidSettingsException(
                    "ARFFReader: User canceled action.");
        }
    }

    /**
     * @see NodeModel#execute(BufferedDataTable[],ExecutionMonitor)
     */
    @Override
    protected BufferedDataTable[] execute(
            final BufferedDataTable[] inData, final ExecutionMonitor exec) 
            throws Exception {

        assert m_file != null;
        if (m_file == null) {
            throw new NullPointerException("Initialize ARFF reader before you"
                    + " execute it.");
        }

        return new DataTable[]{new ARFFTable(m_file, ARFFTable.
                createDataTableSpecFromARFFfile(m_file, exec), m_rowPrefix)};

    }

    /**
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        try {
            m_file = stringToURL(settings.getString(CFGKEY_FILEURL));
        } catch (MalformedURLException mue) {
            throw new InvalidSettingsException(mue);
        }
        m_rowPrefix = settings.getString(CFGKEY_ROWPREFIX);
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do.
    }

    /**
     * @see de.unikn.knime.core.node.
     *      NodeModel#saveInternals(java.io.File, ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
        throws IOException, CanceledExecutionException {
     
    }
    
    /**
     * @see de.unikn.knime.core.node.
     *      NodeModel#loadInternals(java.io.File, ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec) 
            throws IOException, CanceledExecutionException {
     
    }
    
    /**
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    @Override
    protected void saveSettingsTo(final NodeSettings settings) {

        if (m_file != null) {
            settings.addString(CFGKEY_FILEURL, m_file.toString());
        } else {
            settings.addString(CFGKEY_FILEURL, null);
        }
        settings.addString(CFGKEY_ROWPREFIX, m_rowPrefix);
 
    }

    /**
     * @see NodeModel#validateSettings(NodeSettings)
     */
    @Override
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        settings.getString(CFGKEY_FILEURL);
        settings.getString(CFGKEY_ROWPREFIX);
    }

    /**
     * Tries to create an URL from the passed string.
     * 
     * @param url the string to transform into an URL
     * @return URL if entered value could be properly tranformed, or
     * @throws MalformedURLException if the value entered in the text field was
     *             invalid
     */
    public static URL stringToURL(final String url)
            throws MalformedURLException {

        if ((url == null) || (url.equals(""))) {
            throw new MalformedURLException("Specify a not empty valid URL");
        }

        URL newURL;
        try {
            newURL = new URL(url);
        } catch (Exception e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(url);

            // if that blows off we let the exception go up the stack.
            newURL = tmp.getAbsoluteFile().toURL();
        }
        return newURL;
    }

}
