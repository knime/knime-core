/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   11.02.2005 (ohl): created
 */
package org.knime.base.node.io.arffreader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.swing.filechooser.FileFilter;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.StringHistory;


/**
 * The model of the ARFF reader node. The interesting work is done in the
 * {@link ARFFTable} and
 * {@link ARFFRowIterator}.
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ARFFReaderNodeModel extends NodeModel {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ARFFReaderNodeModel.class);

    /** Key used to store the ARFF file location in the settings object. */
    static final String CFGKEY_FILEURL = "FileURL";

    /** Key used to store the row prefix in the settings object. */
    static final String CFGKEY_ROWPREFIX = "RowPrefix";

    private static final String ARFF_HISTORY_ID = "ARFFFiles";

    private String m_rowPrefix;

    private URL m_file;

    /**
     * Creates a new ARFF reader model.
     */
    public ARFFReaderNodeModel() {
        super(0, 1);
        m_rowPrefix = null;
        m_file = null;
        reset();
    }

    /**
     * Creates a new ARFF reader with a default file.
     * 
     * @param arffFileLocation URL to the ARFF file to read
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
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (m_file == null) {
            throw new InvalidSettingsException("File is not specified.");
        }
        try {
            return new DataTableSpec[]{ARFFTable
                    .createDataTableSpecFromARFFfile(m_file, null)};
        } catch (IOException ioe) {
            throw new InvalidSettingsException("ARFFReader: I/O Error", ioe);
        } catch (InvalidSettingsException ise) {
            throw new InvalidSettingsException("ARFFReader: ARFF Header Error",
                    ise);
        } catch (CanceledExecutionException cee) {
            // never flies
            throw new InvalidSettingsException(
                    "ARFFReader: User canceled action.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {

        assert m_file != null;
        if (m_file == null) {
            throw new NullPointerException("Configure the ARFF reader before"
                    + " you execute it, please.");
        }
        // now that we actually read it, add it to the history.
        ARFFReaderNodeModel.addToFileHistory(m_file.toString());

        BufferedDataTable out = exec.createBufferedDataTable(new ARFFTable(
                m_file,
                ARFFTable.createDataTableSpecFromARFFfile(m_file, exec),
                m_rowPrefix), exec);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_file = stringToURL(settings.getString(CFGKEY_FILEURL));
        } catch (MalformedURLException mue) {
            throw new InvalidSettingsException(mue);
        }
        m_rowPrefix = settings.getString(CFGKEY_ROWPREFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (m_file != null) {
            settings.addString(CFGKEY_FILEURL, m_file.toString());
        } else {
            settings.addString(CFGKEY_FILEURL, null);
        }
        settings.addString(CFGKEY_ROWPREFIX, m_rowPrefix);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            stringToURL(settings.getString(CFGKEY_FILEURL));
        } catch (MalformedURLException mue) {
            throw new InvalidSettingsException(mue);
        }
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
            newURL = tmp.getAbsoluteFile().toURI().toURL();
        }
        return newURL;
    }

    /**
     * @param removeNotExistingFiles if <code>true</code> the returned list
     *            will not contain files that doesn't exist (they will not be
     *            removed from the global history though
     * @return the current file history associated with the ARFF reader/writer
     */
    public static String[] getFileHistory(
            final boolean removeNotExistingFiles) {

        StringHistory h = StringHistory.getInstance(ARFF_HISTORY_ID);
        Vector<String> allLocs = new Vector<String>();

        for (int l = 0; l < h.getHistory().length; l++) {
            String loc = h.getHistory()[l];

            if (removeNotExistingFiles) {
                URL url;
                try {
                    url = new URL(loc);
                    if (url.getProtocol().equalsIgnoreCase("FILE")) {
                        // if we have a file location check its existence
                        File f = new File(url.getPath());
                        if (f.exists()) {
                            allLocs.add(loc);
                        } // else ignore old, not existing entries
                    } else {
                        // non-file URL we just take over
                        allLocs.add(loc);
                    }
                } catch (MalformedURLException mue) {
                    // ignore this (invalid) entry in the history
                }

            } else {
                allLocs.add(loc);
            }

        }
        return allLocs.toArray(new String[0]);

    }

    /**
     * Adds the specified string to the ARFF reader/writer history.
     * 
     * @param filename the filename to add
     */
    public static void addToFileHistory(final String filename) {
        StringHistory h = StringHistory.getInstance(ARFF_HISTORY_ID);
        h.add(filename);
    }

    /**
     * FileFilter for the ARFFReader/writer file chooser dialog.
     * 
     * @author Peter Ohl, University of Konstanz
     */
    public static class ARFFFileFilter extends FileFilter {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File f) {
            if (f != null) {
                if (f.isDirectory()) {
                    return true;
                }
                String name = f.getName();
                return name.toLowerCase().endsWith(".arff");
            }
            return true;

        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return "ARFF data files (*.arff)";
        }
    }
}
