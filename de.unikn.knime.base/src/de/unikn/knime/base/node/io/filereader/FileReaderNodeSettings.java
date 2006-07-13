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
 *   03.12.2004 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.IntCell;
import de.unikn.knime.core.data.def.StringCell;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeLogger;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * 
 * @author ohl, University of Konstanz
 */
public class FileReaderNodeSettings extends FileReaderSettings {

    /** The node logger fot this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileReaderNodeSettings.class);

    // a vector storing properties for each column. The size might not be
    // related to the actual number of columns.
    private Vector<ColProperty> m_columnProperties;

    // the number of columns
    private int m_numOfColumns;

    private static final String CFGKEY_NUMOFCOLS = "numOfColumns";

    private static final String CFGKEY_COLPROPS = "ColumnProperties";

    // flags indicating if the values were actually set or are still at
    // constructor's default. Won't be stored into config.
    private boolean m_hasColHeadersIsSet;

    private boolean m_hasRowHeadersIsSet;

    private boolean m_ignoreEmptyLinesIsSet;

    private boolean m_commentIsSet;

    private boolean m_quoteIsSet;

    private boolean m_delimIsSet;

    private boolean m_whiteIsSet;

    private boolean m_analyzedAllRows;

    /**
     * Creates a new settings object for the file reader note and initializes it
     * from the config object passed. If null is passed default settings will be
     * applied where applicable. The default setting are not valid in the sense
     * that they can't be used without modification.
     * 
     * @param cfg a config object containing all settings or null to create
     *            default settings.
     * @throws InvalidSettingsException if the settings in the config object are
     *             incomplete, inconsistent or in any other was invalid.
     */
    FileReaderNodeSettings(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        super(cfg);
        m_columnProperties = new Vector<ColProperty>();

        m_numOfColumns = -1;

        if (cfg != null) {
            m_numOfColumns = cfg.getInt(CFGKEY_NUMOFCOLS);
            readColumnPropsFromConfig(cfg.getNodeSettings(CFGKEY_COLPROPS));

            // check settings
            SettingsStatus status = getStatusOfSettings();
            if (status.getNumOfErrors() != 0) {
                throw new InvalidSettingsException(status
                        .getAllErrorMessages(0));
            }
        } else {
            setDefaultSettings();
        }
        // all these values were set through the config
        m_hasColHeadersIsSet = true;
        m_hasRowHeadersIsSet = true;
        m_commentIsSet = true;
        m_quoteIsSet = true;
        m_delimIsSet = true;
        m_whiteIsSet = true;
        m_ignoreEmptyLinesIsSet = true;
        m_analyzedAllRows = false;
    }

    /**
     * Creates an empty settings object. It contains no default values.
     */
    FileReaderNodeSettings() {
        m_columnProperties = new Vector<ColProperty>();

        m_numOfColumns = -1;

        m_hasColHeadersIsSet = false;
        m_hasRowHeadersIsSet = false;
        m_ignoreEmptyLinesIsSet = false;
        m_commentIsSet = false;
        m_quoteIsSet = false;
        m_delimIsSet = false;
        m_whiteIsSet = false;
        m_analyzedAllRows = false;

    }

    /**
     * writes all settings into the passed configuration object. Except for the
     * analyzedAllRows flag.
     * 
     * @see de.unikn.knime.base.node.io.filetokenizer.FileTokenizerSettings
     *      #saveToConfiguration(NodeSettings)
     */
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        super.saveToConfiguration(cfg);
        cfg.addInt(CFGKEY_NUMOFCOLS, m_numOfColumns);
        saveColumnPropsToConfig(cfg.addNodeSettings(CFGKEY_COLPROPS));
    }

    /*
     * writes all column property objects into the passed config object. To
     * preserve the order (as it represents the column index the properties are
     * for) the key the properties are associated with is the (string
     * representation) of the column index.
     */
    private void saveColumnPropsToConfig(final NodeSettingsWO cfg) {

        if (cfg == null) {
            throw new NullPointerException("Can't store column properties in"
                    + " null config.");
        }

        for (int c = 0; c < m_columnProperties.size(); c++) {
            ColProperty cProp = m_columnProperties.get(c);
            if (cProp != null) {
                cProp.saveToConfiguration(cfg.addNodeSettings("" + c));
            }
        }

    }

    private void readColumnPropsFromConfig(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        if (cfg == null) {
            throw new NullPointerException("Can't read column props from null"
                    + " config.");
        }

        int len = cfg.keySet().size();
        m_columnProperties = new Vector<ColProperty>();
        m_columnProperties.setSize(len);

        for (String key : cfg.keySet()) {
            int pos = -1;
            try {
                pos = Integer.parseInt(key);
            } catch (NumberFormatException nfe) {
                throw new InvalidSettingsException(
                        "Keyword for column property"
                                + " must be a integer number (got '" + key
                                + "').");
            }
            if ((pos < 0) || (pos >= len)) {
                throw new InvalidSettingsException("Expecting column property"
                        + " key between 0 and " + (len - 1) + " (got " + pos
                        + ").");
            }
            if (m_columnProperties.get(pos) != null) {
                throw new InvalidSettingsException(
                        "Properties for column number " + pos
                                + " are specified twice in the conf object.");
            }

            m_columnProperties.set(pos, new ColProperty(cfg.getNodeSettings(key)));

        }

    }

    /**
     * stores a copy of the vector of properties in the structure.
     * 
     * @param colProps the column properties to store
     */
    public void setColumnProperties(
            final Vector<? extends ColProperty> colProps) {
        if (colProps == null) {
            throw new NullPointerException("column properties can't be null.");
        }
        m_columnProperties.clear();
        m_columnProperties.addAll(colProps);

    }

    /**
     * @return a copy of the Vector storing <code>ColProperty</code> objects
     *         or null if not set.
     */
    public Vector<ColProperty> getColumnProperties() {

        if (m_columnProperties == null) {
            return null;
        }

        return new Vector<ColProperty>(m_columnProperties);
    }

    /**
     * Overriding super method because we store these missing values now in the
     * column properties.
     * 
     * @see FileReaderSettings#getMissingValueOfColumn(int)
     */
    public String getMissingValueOfColumn(final int colIdx) {
        if ((m_columnProperties == null)
                || (colIdx >= m_columnProperties.size())) {
            return null;
        }
        return m_columnProperties.get(colIdx).getMissingValuePattern();
    }

    /**
     * Overriding super method because we store these missing values now in the
     * column properties.
     * 
     * @see FileReaderSettings#setMissingValueForColumn(int, java.lang.String)
     */
    public void setMissingValueForColumn(final int colIdx, 
            final String pattern) {
        if ((m_columnProperties == null)
                || (colIdx >= m_columnProperties.size())) {
            throw new IllegalArgumentException("Can't set missing value"
                    + " patterns for index '" + colIdx + "' yet.");
        }
        m_columnProperties.get(colIdx).setMissingValuePattern(pattern);
    }

    /**
     * stores the number of columns set by the user. (Must not be the same as
     * the number of column properties stored in this object.
     * 
     * @param numOfCols the number of columns to store
     */
    public void setNumberOfColumns(final int numOfCols) {
        // we don't check the parameter here. "getStatusOfSettings" will.
        m_numOfColumns = numOfCols;
    }

    /**
     * @return the number set with the method above.
     */
    public int getNumberOfColumns() {
        return m_numOfColumns;
    }

    /**
     * derives a DataTableSpec from the current settings. The spec will not 
     * contain any domain information.
     * @return a DataTableSpec corresponding to the current settings or null if
     *         the current settings are invalid.
     * 
     */
    DataTableSpec createDataTableSpec() {

        // first check if the settings are in a state we can create a valid
        // table spec for.
        // SettingsStatus status = new SettingsStatus();
        // addThisStatus(status);
        SettingsStatus status = getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            return null;
        }

        // collect the ColumnSpecs for each column
        DataColumnSpec[] cSpec = new DataColumnSpec[getNumberOfColumns()];
        for (int c = 0; c < getNumberOfColumns(); c++) {
            ColProperty cProp = m_columnProperties.get(c);
            cSpec[c] = cProp.getColumnSpec();
        }

        return new DataTableSpec(getTableName(), cSpec);

    }

    /**
     * Sets default settings in this object. See the submethods for details, but
     * basically its: zero number of columns, no column names and types, file
     * has row headers and columns headers; One line per row, create empty rows
     * for empty lines, space or comma seperates columns, c-Style comments,
     * double and single quotes (with escape char). Call this only on an empty
     * settings object.
     */
    private void setDefaultSettings() {

        // set our own defaults.
        setColumnProperties(new Vector<ColProperty>());
        setNumberOfColumns(1);
        setTableName("");

        // now the filereader settings
        setDataFileLocationAndUpdateTableName(null);
        setFileHasColumnHeaders(true);
        setFileHasRowHeaders(true);
        setRowHeaderPrefix(null);
        addRowDelimiter("\n", false);

        // the tokenizer settings
        addDelimiterPattern(",", false, false, false);
        addDelimiterPattern(" ", false, false, false);
        addQuotePattern("\"", "\"", '\\');
        addQuotePattern("'", "'", '\\');
        addBlockCommentPattern("/*", "*/", false, false);
        addSingleLineCommentPattern("//", false, false);

    }

    /**
     * reads the FileReaderNodeSettings from the specified XML file and returns
     * a new settings object. It throws an exception if something goes wrong.
     * The settings are not checked. They could be incomplete or invalid. It
     * also reads possible values from the data file - but only if the settings
     * are useable and the table contains a string column.
     * 
     * @param xmlLocation location of the xml file to read. Must be a valid URL.
     * @return a new settings object containing the settings read fromt the
     *         specified XML file. throws IllegalStateEception if something goes
     *         wrong.
     */
    public static FileReaderNodeSettings readSettingsFromXMLFile(
            final String xmlLocation) {
        URL xmlURL = null;
        XMLPropsReader xmlReader = null;

        // convert location string to URL - at least try.
        if ((xmlLocation == null) || (xmlLocation.equals(""))) {
            throw new IllegalStateException("Specify a not empty "
                    + "valid URL for the XML file to read.");
        }
        try {
            xmlURL = new URL(xmlLocation);
        } catch (Exception e) {
            // see if they specified a file without giving the protocol
            File tmp = new File(xmlLocation);

            try {
                // if this blows up we give up
                xmlURL = tmp.getAbsoluteFile().toURL();
            } catch (MalformedURLException mue) {
                throw new IllegalStateException("Cannot convert '"
                        + xmlLocation + "' to a valid URL.");
            }
        }

        assert xmlURL != null;

        try {
            // reads in the XML file and stores it internally
            xmlReader = new XMLPropsReader(xmlURL);

        } catch (IOException ioe) {
            if (ioe.getMessage() != null) {
                throw new IllegalStateException("I/O Error: "
                        + ioe.getMessage() + "\n while reading '" + xmlURL
                        + "'.");
            } else {
                throw new IllegalStateException("I/O Error: While trying to "
                        + "read '" + xmlURL + "'.");
            }
        } catch (SAXException saxe) {
            if (saxe.getMessage() != null) {
                throw new IllegalStateException("Parser Error: "
                        + saxe.getMessage() + "\n while reading '" + xmlURL
                        + "'.");
            } else {
                throw new IllegalStateException("Parser Error: While trying "
                        + "to read '" + xmlURL + "'.");
            }
        } catch (ParserConfigurationException pce) {
            throw new IllegalStateException("Parser ConfError: While trying "
                    + "to read '" + xmlURL + "'.");
        }

        FileReaderNodeSettings frSettings = new FileReaderNodeSettings();

        try {
            int numOfCols = xmlReader.getNumColumns();
            frSettings.setDataFileLocationAndUpdateTableName(xmlReader
                    .getDataFileURL());
            frSettings.setNumberOfColumns(numOfCols);
            frSettings.setFileHasColumnHeaders(xmlReader
                    .isColumnHeaderSpecified());
            frSettings.setFileHasRowHeaders(xmlReader.isRowHeaderSpecified());
            if (xmlReader.getColumnDelimiter() != null) {
                frSettings.addDelimiterPattern(xmlReader.getColumnDelimiter(),
                        false, false, false);
            }
            if (!xmlReader.isRowHeaderSpecified()) {
                frSettings.setRowHeaderPrefix(xmlReader.getRowPrefix());
            } // otherwise we ignore the prefix - that's how we always did it.
            if (xmlReader.getRowDelimiter() != null) {
                frSettings.addRowDelimiter(xmlReader.getRowDelimiter(), false);
            }
            if (xmlReader.getLineComment() != null) {
                frSettings.addSingleLineCommentPattern(xmlReader
                        .getLineComment(), false, false);
            }
            if ((xmlReader.getBlockCommentLeft() != null)
                    && (xmlReader.getBlockCommentRight() != null)) {
                frSettings.addBlockCommentPattern(xmlReader
                        .getBlockCommentLeft(), xmlReader
                        .getBlockCommentRight(), false, false);
            }
            if ((xmlReader.getQuoteLeft() != null)
                    && (xmlReader.getQuoteRight() != null)) {
                if ((xmlReader.getQuoteEscape() != null)
                        && (xmlReader.getQuoteEscape().length() > 0)) {
                    frSettings.addQuotePattern(xmlReader.getQuoteLeft(),
                            xmlReader.getQuoteRight(), xmlReader
                                    .getQuoteEscape().charAt(0));
                }
            }
            Vector<ColProperty> cProps = new Vector<ColProperty>();
            for (int c = 0; c < numOfCols; c++) {
                String missVal = xmlReader.getColumnMissing(c);
                DataType type = xmlReader.getColumnType(c);
                String name = null;
                // create the ColProperty object
                ColProperty cProp = new ColProperty();

                cProp.setUserSettings(true);
                name = xmlReader.getColumnName(c);
                cProp.setMissingValuePattern(missVal);
                
                if (type.equals(IntCell.TYPE)) {
                    // we could read possible values for int column. 
                    // Default is false though.
                    cProp.setReadPossibleValuesFromFile(false);
                } 
                DataColumnSpecCreator dcsc = 
                    new DataColumnSpecCreator(name, type);
                cProp.setColumnSpec(dcsc.createSpec());
                cProps.add(cProp);
            }

            frSettings.setColumnProperties(cProps);

        } catch (Exception e) {
            throw new IllegalStateException("Error reading xml file. "
                    + e.getMessage());
        }

        return frSettings;
    }


    /**
     * reads the column headers from the file setting the column name in the
     * colProperty objects - if the useFileHeader flag is set in there. All
     * settings must be set properly to enable file reading. Column names will
     * be made unique by adding an increasing index to duplicate names.
     * 
     * @throws IOException if there was an error reading the data file.
     */
    public void readColumnHeadersFromFile() throws IOException {
        /*
         * this is how we do this: We create a filetable with numOfCols columns
         * and StringCells only. Then we read just one line, and set the strings
         * read as column names.
         */

        if (getDataFileLocation() == null) {
            throw new IllegalStateException("All settings must be properly set"
                    + " before calling 'readColumnHeadersFromFile!");
        }
        if (!getFileHasColumnHeaders()) {
            throw new IllegalStateException("Why would you want to read "
                    + "col headers from file if there aren't any?");
        }

        // temporarily turn off this switch so the file reader doesn't skip
        // the column headers.
        setFileHasColumnHeaders(false);

        int numOfCols = getNumberOfColumns();

        // create a fake table spec that we can pass to the reader.
        DataColumnSpec[] colSpec = new DataColumnSpec[numOfCols];
        for (int i = 0; i < numOfCols; i++) {
            DataColumnSpecCreator dcsc = new DataColumnSpecCreator("col" + i,
                    StringCell.TYPE);
            colSpec[i] = dcsc.createSpec();
        }
        DataTableSpec dts = new DataTableSpec(colSpec);

        DataTable dt = new FileTable(dts, this);
        RowIterator rowIter = dt.iterator();
        if (rowIter == null) {
            throw new IOException("Couldn't read from col headers from "
                    + "specified source ('" + getDataFileLocation() + "').");
        }
        // get the first row
        DataRow row = dt.iterator().next();
        if (row.getNumCells() != numOfCols) {
            // didn't get enough column headers - that's bad
            LOGGER.warn("Found " + row.getNumCells()
                    + " column headers in the file, expecting " + numOfCols
                    + ".");
        }
        int col = 0;
        if ((row.getNumCells() == numOfCols - 1) && getFileHasRowHeaders()) {
            // at least we get enough if we use the row header ID of this row
            LOGGER.warn("Using the \"corner value\" as" + " column name.");
            ColProperty cProp = (ColProperty)getColumnProperties().get(0);
            if (!cProp.getUserSettings()) {
                cProp.changeColumnName(row.getKey().getId().toString());
            }
            col = 1; // col header '0' is done.
        }

        for (int cell = 0; cell < row.getNumCells(); cell++) {
            if (col >= numOfCols) {
                break;
            }
            // replace the columnspec in the cols props
            ColProperty cProp = (ColProperty)getColumnProperties().get(col);
            if (!cProp.getUserSettings()) {
                String uniqueName = uniquifyColName(col, row.getCell(cell)
                        .toString());
                cProp.changeColumnName(uniqueName);
            }
            col++;
        }

        while (col < numOfCols) {
            // found less col names than columns. Create some.
            ColProperty cProp = (ColProperty)getColumnProperties().get(col);
            if (!cProp.getUserSettings()) {
                String uniqueName = uniquifyColName(col, "Col" + col);
                cProp.changeColumnName(uniqueName);
            }
            col++;
        }

        setFileHasColumnHeaders(true);

    }

    /**
     * Generates a unique column name based on the specified preliminary name
     * and unique to all columns with indecies less than the specified one.
     * 
     * @param colIdx the index of the column up to which we should look at and
     *            make the name unique. (That is we ignore all existing col
     *            names of cols wihth higher index.)
     * @param prelimName the preliminary name for the column with index colIdx.
     *            The method will add a number in parantheses to make it unique.
     * @return a column name prefixed by the specified prelimName, that is
     *         unique with respect to all colums with idx less than the
     *         specified colIdx.
     */
    String uniquifyColName(final int colIdx, final String prelimName) {

        String uniqueName = prelimName;
        int cnt = 2;

        boolean unique = false;
        while (!unique) {
            unique = true;
            // run through all colums (up to colIdx) and compare the name.
            // Do that with each newly generated name completely
            for (int c = 0; 
                    (c < colIdx) && (c < m_columnProperties.size()); 
                    c++) {
                ColProperty colProp = m_columnProperties.get(c);
                if ((colProp != null) && (colProp.getColumnSpec() != null)) {
                    String colName = colProp.getColumnSpec().getName();
                    if (colName.equals(uniqueName)) {
                        unique = false;
                        uniqueName = prelimName + "(" + cnt + ")";
                        cnt++;
                        break; // start all over again
                    }
                }
            }
        }

        return uniqueName;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setIgnoreEmptyLinesUserSet(final boolean s) {
        m_ignoreEmptyLinesIsSet = s;
    }

    /**
     * @return true is the value was set by the user, false if it's still at
     *         constructor's default value.
     */
    public boolean isIgnoreEmptyLinesUserSet() {
        return m_ignoreEmptyLinesIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setFileHasRowHeadersUserSet(final boolean s) {
        m_hasRowHeadersIsSet = s;
    }

    /**
     * @return true is the value was set, false if it's still at constructor's
     *         default value.
     */
    public boolean isFileHasRowHeadersUserSet() {
        return m_hasRowHeadersIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setFileHasColumnHeadersUserSet(final boolean s) {
        m_hasColHeadersIsSet = s;
    }

    /**
     * @return true is the value was set, false if it's still at constructor's
     *         default value.
     */
    public boolean isFileHasColumnHeadersUserSet() {
        return m_hasColHeadersIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setCommentUserSet(final boolean s) {
        m_commentIsSet = s;
    }

    /**
     * @return true is the value was set by the user, false if it's still at
     *         constructor's default value.
     */
    public boolean isCommentUserSet() {
        return m_commentIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setDelimiterUserSet(final boolean s) {
        m_delimIsSet = s;
    }

    /**
     * @return true is the value was set by the user, false if it's still at
     *         constructor's default value.
     */
    public boolean isDelimiterUserSet() {
        return m_delimIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setQuoteUserSet(final boolean s) {
        m_quoteIsSet = s;
    }

    /**
     * @return true is the value was set by the user, false if it's still at
     *         constructor's default value.
     */
    public boolean isQuoteUserSet() {
        return m_quoteIsSet;
    }

    /**
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     * 
     * @param s the new value of the flag
     */
    public void setWhiteSpaceUserSet(final boolean s) {
        m_whiteIsSet = s;
    }

    /**
     * @return true is the value was set by the user, false if it's still at
     *         constructor's default value.
     */
    public boolean isWhiteSpaceUserSet() {
        return m_whiteIsSet;
    }

    /**
     * @return the value of the analyze flag previously set. Or false by
     *         default.
     */
    boolean analyzeUsedAllRows() {
        return m_analyzedAllRows;
    }

    /**
     * sets the value of the flag which is used to indicate if the FileAnalyzer
     * looked at all rows when it extracts the default settings. The value of
     * the flag is not stored when the settings are saved into a config, and is
     * not recovered from a config object.
     * 
     * @param val the new value of the flag
     */
    void setAnalyzeUsedAllRows(final boolean val) {
        m_analyzedAllRows = val;
    }

    /**
     * @see de.unikn.knime.base.node.io.filereader.FileReaderSettings
     *      #getStatusOfSettings()
     */
    public SettingsStatus getStatusOfSettings() {
        return getStatusOfSettings(false, null);
    }

    /**
     * Method to check consistency and completeness of the current settings. It
     * will return a <code>SettingsStatus</code> object which contains info,
     * warning and error messages, if something is fishy with the settings.
     * 
     * @see FileReaderSettings#getStatusOfSettings(boolean, DataTableSpec)
     */
    public SettingsStatus getStatusOfSettings(final boolean openDataFile,
            final DataTableSpec tableSpec) {

        SettingsStatus status = new SettingsStatus();

        addStatusOfSettings(status, openDataFile, tableSpec);

        return status;
    }

    /**
     * Call this from derived classes to add the status of all super classes.
     * For parameters:
     * 
     * @see de.unikn.knime.base.node.io.filereader.FileReaderSettings
     *      #addStatusOfSettings(SettingsStatus, boolean, DataTableSpec)
     */
    protected void addStatusOfSettings(final SettingsStatus status,
            final boolean openDataFile, final DataTableSpec tableSpec) {

        super.addStatusOfSettings(status, openDataFile, tableSpec);
        addThisStatus(status);

    }

    private void addThisStatus(final SettingsStatus status) {

        if (m_numOfColumns < 1) {
            status.addError("Invalid number of columns specified ('"
                    + m_numOfColumns + "').");
            return;
        }
        if (m_columnProperties == null) {
            status.addError("Not one column property is set!");
            return;
        }

        int propsToCheck = m_numOfColumns;
        if (m_columnProperties.size() < m_numOfColumns) {
            status
                    .addError("Missing column properies for columns "
                            + (m_columnProperties.size() + 1) + " to "
                            + m_numOfColumns);
            propsToCheck = m_columnProperties.size();
        }
        for (int c = 0; c < propsToCheck; c++) {
            // check if we got a column property object for each column
            ColProperty cProp = m_columnProperties.get(c);
            if (cProp == null) {
                status.addError("No column properties specified for column"
                        + " no. " + (c + 1));
                // that's all we can do with a null col prop...
                continue;
            }
            // check column spec, i.e. name, type and possible values
            if (cProp.getColumnSpec() == null) {
                status.addError("Column name and type not specified for "
                        + " column no. " + (c + 1));
                continue;
            }
            // check the name
            String cName = cProp.getColumnSpec().getName();
            if (cName == null) {
                status.addError("No column name specified for column no. "
                        + (c + 1));
            }
            // and type
            DataType cType = cProp.getColumnSpec().getType();
            if (cType == null) {
                status.addError("No column type specified for column no. "
                        + (c + 1));
            }
            if (!DataType.class.isAssignableFrom(cType.getClass())) {
                status.addError("Column type of column no. " + (c + 1)
                        + " is not derived from type DataType");
                cType = null; // set it null here because its useless anyway
            }
            // check uniquity of column name
            if (cName != null) {
                for (int compC = c + 1; compC < propsToCheck; compC++) {
                    ColProperty compProp = m_columnProperties.get(compC);
                    if (compProp != null) {
                        DataColumnSpec compSpec = compProp.getColumnSpec();
                        if ((compSpec != null) 
                                && (compSpec.getName() != null)) {
                            if (cName.equals(compSpec.getName())) {
                                status.addError("Column no. " + (c + 1)
                                        + " and no. " + (compC + 1)
                                        + " have the same name ('"
                                        + cName + "')");
                            }
                        }
                    }
                } // for all colProps after colProp(c)
            } // if (cName != null)

            // check a possible values - if any
            Set<DataCell> possVals = cProp.getColumnSpec().getDomain()
                    .getValues();
            if (possVals != null) {
                // possible values must not be null
                for (DataCell val : possVals) {
                    if (val == null) {
                        status.addError("Invalid possible value for column"
                                + " no." + (c + 1) + "(<null>).");
                    } else {
                        if (cType != null) {
                            if (!cType.isASuperTypeOf(val.getType())) {
                                status.addError("Incompatible possible "
                                        + "value specified for column no. "
                                        + (c + 1));
                            }
                        }
                    }
                }
            }

        } // for all column properties

        if (m_numOfColumns < m_columnProperties.size()) {
            status.addInfo("More column properties specified "
                    + "than number of columns - extra props are unchecked");

        }
    } // addColumnPropertiesStatus(SettingsStatus)

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer res = new StringBuffer(super.toString());
        for (int c = 0; c < getNumberOfColumns(); c++) {
            res.append("\nCol_No." + c + ":");
            res.append(m_columnProperties.get(c).toString());
        }
        return res.toString();
    }
} // class FileReaderNodeSettings
