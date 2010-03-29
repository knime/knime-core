/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.io.filereader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
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
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.tokenizer.SettingsStatus;


/**
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderNodeModel extends NodeModel {
    /**
     * The id this objects uses to store its file history in the
     * <code>StringHistory</code> object. Don't reuse this id unless you want
     * to share the history list.
     */
    public static final String FILEREADER_HISTORY_ID = "ASCIIfile";

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileReaderNodeModel.class);

    /*
     * The settings structure used to create a DataTable from during execute.
     */
    private FileReaderNodeSettings m_frSettings;

    /**
     * Creates a new model that creates and holds a Filetable.
     */
    public FileReaderNodeModel() {
        super(0, 1); // tell the super we need no inputs and one output,
        // please.
        m_frSettings = null;
    }

    /**
     * Creates a new model and either read its settings from the specified file,
     * if the filename ends with ".xml", or set the specified file name as
     * default data file name for the configuration dialog. Settings in the
     * model resulting from this constructor must not necessarily be correct or
     * valid.
     *
     * @param filename valid URL to a data file or a XML configuration file. If
     *            the filename ends with ".xml" it's considered a xml file spec.
     */
    public FileReaderNodeModel(final String filename) {
        this();
        if (filename != null) {
            if ((filename.lastIndexOf('.') >= 0)
                    && (filename.substring(filename.lastIndexOf('.'))
                            .equals(".xml"))) {
                // Its a XML file - try reading it in.
                try {
                    m_frSettings = FileReaderNodeSettings
                            .readSettingsFromXMLFile(filename);
                } catch (IllegalStateException ise) {
                    LOGGER.error("FileReader: " + ise.getMessage());
                    LOGGER.error("FileReader: XML file not read.");
                }
            } else {
                // doesn't have the XML extension - consider it a data file
                m_frSettings = new FileReaderNodeSettings();
                try {
                    m_frSettings.setDataFileLocationAndUpdateTableName(
                                    FileReaderNodeDialog
                                    .textToURL(filename));
                } catch (MalformedURLException mue) {
                    LOGGER.error("FileReader: " + mue.getMessage());
                    LOGGER.error("FileReader: Data file location not set.");
                    m_frSettings = null;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // m_frSettings = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] data,
            final ExecutionContext exec) throws CanceledExecutionException,
            InvalidSettingsException {

        LOGGER.info("Preparing to read from '"
                + m_frSettings.getDataFileLocation().toString() + "'.");

        // check again the settings - especially file existence (under Linux
        // files could be deleted/renamed since last config-call...
        SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            throw new InvalidSettingsException(status.getAllErrorMessages(10));
        }

        DataTableSpec tSpec = m_frSettings.createDataTableSpec();

        FileTable fTable =
                new FileTable(tSpec, m_frSettings, m_frSettings
                        .getSkippedColumns(), exec);

        // create a DataContainer and fill it with the rows read. It is faster
        // then reading the file every time (for each row iterator), and it
        // collects the domain for each column for us. Also, if things fail,
        // the error message is printed during file reader execution (were it
        // belongs to) and not some time later when a node uses the row
        // iterator from the file table.

        BufferedDataContainer c = exec.createDataContainer(
                fTable.getDataTableSpec(), /*initDomain=*/true);
        int row = 0;
        FileRowIterator it = fTable.iterator();
        try {
            if (it.getZipEntryName() != null) {
                // seems we are reading a ZIP archive.
                LOGGER.info("Reading entry '" + it.getZipEntryName()
                        + "' from the specified ZIP archive.");
            }

            while (it.hasNext()) {
                row++;
                DataRow next = it.next();
                String message = "Caching row #" + row + " (\""
                        + next.getKey() + "\")";
                exec.setMessage(message);
                exec.checkCanceled();
                c.addRowToTable(next);
            }

            if (it.zippedSourceHasMoreEntries()) {
                // after reading til the end of the file this returns a valid
                // result
                setWarningMessage("Source is a ZIP archive with multiple "
                        + "entries. Only reading first entry!");
            }
            c.close();
        } catch (DuplicateKeyException dke) {
            String msg = dke.getMessage();
            if (msg == null) {
                msg = "Duplicate row IDs";
            }
            msg += ". Consider making IDs unique in the advanced settings.";
            DuplicateKeyException newDKE = new DuplicateKeyException(msg);
            newDKE.initCause(dke);
            throw newDKE;
        }

        // user settings allow for truncating the table
        if (it.iteratorEndedEarly()) {
            setWarningMessage("Data was truncated due to user settings.");
        }
        BufferedDataTable out = c.getTable();

        // closes all sources.
        fTable.dispose();

        return new BufferedDataTable[]{out};
    }

    /**
     * @return the current settings for the file reader. Could be
     *         <code>null</code>.
     */
    FileReaderSettings getFileReaderSettings() {
        return m_frSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 0;

        if (m_frSettings == null) {
            throw new InvalidSettingsException("No Settings available.");
        }

        // see if settings are good enough for execution
        SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() == 0) {
            return new DataTableSpec[]{m_frSettings.createDataTableSpec()};
        }

        throw new InvalidSettingsException(status.getAllErrorMessages(0));
    }

    /*
     * validates the settings object, or reads its settings from it. Depending
     * on the specified value of the 'validateOnly' parameter.
     */
    private void readSettingsFromConfiguration(final NodeSettingsRO settings,
            final boolean validateOnly) throws InvalidSettingsException {
        if (settings == null) {
            throw new NullPointerException(
                    "Can't read filereader node settings"
                            + " from null config object");
        }
        // will puke and die if config is not readable.
        FileReaderNodeSettings newSettings = new FileReaderNodeSettings(
                settings);

        // check consistency of settings.
        SettingsStatus status = newSettings.getStatusOfSettings();
        if (status.getNumOfErrors() > 0) {
            throw new InvalidSettingsException(status.getAllErrorMessages(0));
        }

        if (!validateOnly) {
            // everything looks good - take over the new settings.
            m_frSettings = newSettings;
            // save the filename to our filehistory
            StringHistory h = StringHistory.getInstance(FILEREADER_HISTORY_ID);
            h.add(m_frSettings.getDataFileLocation().toString());
        }
    }

    /**
     * Reads in all user settings of the model. If they are incomplete,
     * inconsistent, or in any way invalid it will throw an exception.
     *
     * @param settings the object to read the user settings from. Must not be
     *            <code>null</code> and must be validated with the validate
     *            method below.
     * @throws InvalidSettingsException if the settings are incorrect - which
     *             should not happen as they are supposed to be validated
     *             before.
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettingsRO)
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */false);
    }

    /**
     * Writes the current user settings into a configuration object.
     *
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        if (settings == null) {
            throw new NullPointerException("Can't write filereader node "
                    + "settings into null config object.");
        }
        FileReaderNodeSettings s = m_frSettings;

        if (s == null) {
            s = new FileReaderNodeSettings();
        }
        s.saveToConfiguration(settings);

    }

    /**
     * Checks all user settings in the specified spec object. If they are
     * incomplete, inconsistent, or in any way invalid it will throw an
     * exception.
     *
     * @param settings the object to read the user settings from. Must not be
     *            <code>null</code>.
     * @throws InvalidSettingsException if the settings in the specified object
     *             are incomplete, inconsistent, or in any way invalid.
     * @see NodeModel#validateSettings(NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        /*
         * This is a special "deal" for the file reader: The file reader, if
         * previously executed, has data at it's output - even if the file that
         * was read doesn't exist anymore. In order to warn the user that the
         * data cannot be recreated we check here if the file exists and set a
         * warning message if it doesn't.
         */
        if (m_frSettings == null) {
            // no settings - no checking.
            return;
        }

        URL location = m_frSettings.getDataFileLocation();
        try {
            if ((location == null)
                    || !location.toString().startsWith("file:")) {
                // We can only check files. Other protocols are ignored.
                return;
            }

            InputStream inStream = location.openStream();
            if (inStream == null) {
                setWarningMessage("The file '" + location.toString()
                        + "' can't be accessed anymore!");
            } else {
                inStream.close();
            }
        } catch (IOException ioe) {
            setWarningMessage("The file '" + location.toString()
                    + "' can't be accessed anymore!");
        } catch (NullPointerException npe) {
            // thats a bug in the windows open stream
            // a path like c:\blah\ \ (space as dir) causes a NPE.
            setWarningMessage("The file '" + location.toString()
                    + "' can't be accessed anymore!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // no internals to save.
        return;
    }

    /**
     * @return the current file history associated with the file reader
     */
    static String[] getFileHistory() {
        StringHistory h = StringHistory.getInstance(FILEREADER_HISTORY_ID);
        Vector<String> validLoc = new Vector<String>();
        // dismiss not existing files
        for (int l = 0; l < h.getHistory().length; l++) {
            String loc = h.getHistory()[l];
            URL url;
            try {
                url = new URL(loc);
                if (url.getProtocol().equalsIgnoreCase("FILE")) {
                    // if we have a file location check its existence
                    try {
                        File f = new File(url.toURI());
                        if (f.exists()) {
                            validLoc.add(loc);
                        } // else ignore old, not existing entries
                    } catch (URISyntaxException use) {
                        // ignore it
                    }
                } else {
                    // non-file URL we just take over
                    validLoc.add(loc);
                }
            } catch (MalformedURLException mue) {
                // ignore this (invalid) entry in the history
            }
        }
        return validLoc.toArray(new String[0]);
    }

}
