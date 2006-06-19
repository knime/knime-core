/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.io.filereader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.container.DataContainer;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeModel;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.util.StringHistory;

/**
 * @author ohl University of Konstanz
 */
public class FileReaderNodeModel extends NodeModel {

    /**
     * the id this objects uses to store its file history in the
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
        super(0, 1); // tell the super we need no inputs and one output, please.
        m_frSettings = null;
    }

    /**
     * creates a new model and either read its settings from the specified file,
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
                // doesn't have the xml extension - consider it a data file
                m_frSettings = new FileReaderNodeSettings();
                try {
                    m_frSettings.setDataFileLocationAndUpdateTableName(
                            FileReaderNodeDialog.textToURL(filename));
                } catch (MalformedURLException mue) {
                    LOGGER.error("FileReader: " + mue.getMessage());
                    LOGGER.error("FileReader: Data file location not set.");
                    m_frSettings = null;
                }
            }
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeModel#reset()
     */
    protected void reset() {
        //m_frSettings = null;
    }

    /**
     * @see NodeModel#execute(DataTable[],ExecutionMonitor)
     */
    protected DataTable[] execute(final DataTable[] data,
            final ExecutionMonitor exec) throws 
            CanceledExecutionException, InvalidSettingsException {

        LOGGER.info("Preparing to read from '"
                + m_frSettings.getDataFileLocation().toString() + "'.");

        // check again the settings - especially file existence (under Linux
        // files could be deleted/renamed since last config-call...
        SettingsStatus status = m_frSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            throw new InvalidSettingsException(status.getAllErrorMessages(10));
        }
        
        // we always want to traverse through the entire table here to make
        // sure that warnings/errors/exceptions occure here, rather than 
        // somewhere later in the flow where nobody would relate them to the
        // filereader settings.
        DataTableSpec tSpec = m_frSettings.createDataTableSpec();
        FileTable fTable = new FileTable(tSpec, m_frSettings);
        
        // create a DataContainer and fill it with the rows read. It is faster
        // then reading the file everytime (for each row iterator), and it
        // collects the domain for each column for us.
        DataTable cacheTable = DataContainer.cache(fTable, exec);
        return new DataTable[] {cacheTable};
    }

    /**
     * @return the current settings for the file reader. Could be null.
     */
    FileReaderSettings getFileReaderSettings() {
        return m_frSettings;
    }

    /**
     * @see NodeModel#configure(DataTableSpec[])
     */
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        assert inSpecs.length == 0;

        if (m_frSettings == null) {
            throw new InvalidSettingsException("No Settings available.");
        }

        // see if settings are good enough for execution
        SettingsStatus status = m_frSettings.getStatusOfSettings();
        if (status.getNumOfErrors() == 0) {
            return new DataTableSpec[] {m_frSettings
                    .createDataTableSpec()};
        }

        throw new InvalidSettingsException(status.getAllErrorMessages(0));
    }

    /*
     * validates the settings object, or reads its settings from it. Depending
     * on the specified value of the 'validateOnly' parameter.
     */
    private void readSettingsFromConfiguration(final NodeSettings settings,
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
        SettingsStatus status = newSettings.getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            throw new InvalidSettingsException(status.getAllErrorMessages(0));
        }

        if (!validateOnly) {
            // everything looks good - take over the new settings.
            // If we are supposed to.
            m_frSettings = newSettings;
            // save the filename to our filehistory
            StringHistory h = StringHistory.getInstance(FILEREADER_HISTORY_ID);
            h.add(m_frSettings.getDataFileLocation().toString());
        }
    }

    /**
     * reads in all user settings of the model. If they are incomplete,
     * inconsistent, or in any way invalid it will throw an exception.
     * 
     * @param settings the object to read the user settings from. Must not be
     *            null. And must be validated with the validate method below.
     * @throws InvalidSettingsException if the settings are incorrect - which
     *             should not happen as they are supposed to be validated
     *             before.
     * @see NodeModel#loadValidatedSettingsFrom(NodeSettings)
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void loadValidatedSettingsFrom(final NodeSettings settings)
            throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */false);
    }

    /**
     * Writes the current user settings into a configuration object.
     * 
     * @see NodeModel#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {

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
     * checks all user settings in the specified spec object. If they are
     * incomplete, inconsistent, or in any way invalid it will throw an
     * exception.
     * 
     * @param settings the object to read the user settings from. Must not be
     *            null.
     * @throws InvalidSettingsException if the settings in the specified object
     *             are incomplete, inconsistent, or in any way invalid.
     * @see NodeModel#validateSettings(NodeSettings)
     */
    protected void validateSettings(final NodeSettings settings)
            throws InvalidSettingsException {
        readSettingsFromConfiguration(settings, /* validateOnly = */true);
    }

    /**
     * @return the current file history associated with the file reader.
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
                    File f = new File(url.getPath());
                    if ((f != null) && (f.exists())) {
                        validLoc.add(loc);
                    } // else  ignore old, not existing entries
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
