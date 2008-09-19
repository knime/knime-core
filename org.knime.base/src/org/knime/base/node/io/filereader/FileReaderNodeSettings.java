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
 *   03.12.2004 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.xml.sax.SAXException;

/**
 *
 * @author Peter Ohl, University of Konstanz
 */
public class FileReaderNodeSettings extends FileReaderSettings {

    // a vector storing properties for each column. The size might not be
    // related to the actual number of columns.
    private Vector<ColProperty> m_columnProperties;

    // the number of columns
    private int m_numOfColumns;

    private static final String CFGKEY_NUMOFCOLS = "numOfColumns";

    private static final String CFGKEY_COLPROPS = "ColumnProperties";

    private static final String CFGKEY_EOLDELIMUSERVAL = "delimsAtEOLuserVal";

    private static final String CFG_KEY_PRESERVE = "PreserveSettings";

    // flags indicating if the values were actually set or are still at
    // constructor's default. Won't be stored into config.
    private boolean m_hasColHeadersIsSet;

    private boolean m_hasRowHeadersIsSet;

    private boolean m_ignoreEmptyLinesIsSet;

    private boolean m_ignoreDelimsAtEndOfRowIsSet;

    private boolean m_decimalSeparatorIsSet;

    private boolean m_delimsAtEOLUserValue;

    private boolean m_commentIsSet;

    private boolean m_quoteIsSet;

    private boolean m_delimIsSet;

    private boolean m_whiteIsSet;

    private boolean m_analyzedAllRows;

    private boolean m_preserveSettings;

    /**
     * Creates a new settings object for the file reader note and initializes it
     * from the config object passed. If <code>null</code> is passed default
     * settings will be applied where applicable. The default setting are not
     * valid in the sense that they can't be used without modification.
     *
     * @param cfg a config object containing all settings or <code>null</code>
     *            to create default settings
     * @throws InvalidSettingsException if the settings in the config object are
     *             incomplete, inconsistent or in any other was invalid
     */
    FileReaderNodeSettings(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        super(cfg);
        m_columnProperties = new Vector<ColProperty>();

        m_numOfColumns = -1;

        if (cfg != null) {
            m_numOfColumns = cfg.getInt(CFGKEY_NUMOFCOLS);
            m_preserveSettings = cfg.getBoolean(CFG_KEY_PRESERVE, false);
            readColumnPropsFromConfig(cfg.getNodeSettings(CFGKEY_COLPROPS));
            m_delimsAtEOLUserValue =
                    cfg.getBoolean(CFGKEY_EOLDELIMUSERVAL,
                            ignoreEmptyTokensAtEndOfRow());

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
        m_ignoreDelimsAtEndOfRowIsSet = true;
        m_decimalSeparatorIsSet = true;
        m_analyzedAllRows = false;
    }

    /**
     * Creates a new settings object with the exact same settings as the object
     * passed in.
     *
     * @param clonee the settings object to copy the settings values from.
     */
    FileReaderNodeSettings(final FileReaderNodeSettings clonee) {
        super(clonee);

        m_columnProperties =
                new Vector<ColProperty>(clonee.m_columnProperties.size());
        m_columnProperties.setSize(clonee.m_columnProperties.size());
        for (int i = 0; i < m_columnProperties.size(); i++) {
            if (clonee.m_columnProperties.get(i) != null) {
                m_columnProperties.set(i,
                        (ColProperty)clonee.m_columnProperties.get(i).clone());
            }
        }

        m_numOfColumns = clonee.m_numOfColumns;

        m_hasColHeadersIsSet = clonee.m_hasColHeadersIsSet;
        m_hasRowHeadersIsSet = clonee.m_hasRowHeadersIsSet;
        m_ignoreEmptyLinesIsSet = clonee.m_ignoreEmptyLinesIsSet;
        m_ignoreDelimsAtEndOfRowIsSet = clonee.m_ignoreDelimsAtEndOfRowIsSet;
        m_decimalSeparatorIsSet = clonee.m_decimalSeparatorIsSet;
        m_delimsAtEOLUserValue = clonee.m_delimsAtEOLUserValue;
        m_commentIsSet = clonee.m_commentIsSet;
        m_quoteIsSet = clonee.m_quoteIsSet;
        m_delimIsSet = clonee.m_delimIsSet;
        m_whiteIsSet = clonee.m_whiteIsSet;
        m_analyzedAllRows = clonee.m_analyzedAllRows;
        m_preserveSettings = clonee.m_preserveSettings;

    }

    /**
     * Creates an empty settings object. It contains no default values.
     */
    public FileReaderNodeSettings() {
        m_columnProperties = new Vector<ColProperty>();

        m_numOfColumns = -1;

        m_hasColHeadersIsSet = false;
        m_hasRowHeadersIsSet = false;
        m_ignoreEmptyLinesIsSet = false;
        m_ignoreDelimsAtEndOfRowIsSet = false;
        m_decimalSeparatorIsSet = false;
        m_delimsAtEOLUserValue = false;
        m_commentIsSet = false;
        m_quoteIsSet = false;
        m_delimIsSet = false;
        m_whiteIsSet = false;
        m_analyzedAllRows = false;
        m_preserveSettings = false;

    }

    /**
     * Writes all settings into the passed configuration object. Except for the
     * analyzedAllRows flag.
     *
     * {@inheritDoc}
     */
    @Override
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        super.saveToConfiguration(cfg);
        cfg.addBoolean(CFGKEY_EOLDELIMUSERVAL, m_delimsAtEOLUserValue);
        cfg.addInt(CFGKEY_NUMOFCOLS, m_numOfColumns);
        cfg.addBoolean(CFG_KEY_PRESERVE, m_preserveSettings);
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

            m_columnProperties.set(pos, new ColProperty(cfg
                    .getNodeSettings(key)));

        }

    }

    /**
     * Stores a copy of the vector of properties in the structure.
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
     * @return a copy of the Vector storing {@link ColProperty} objects or
     *         <code>null</code> if not set
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
     * Stores the number of columns set by the user. (Must not be the same as
     * the number of column properties stored in this object).
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
     * Derives a DataTableSpec from the current settings. The spec will not
     * contain any domain information. It will contain only the columns to
     * include in the table (excl. the columns to skip).
     *
     * @return a DataTableSpec corresponding to the current settings or
     *         <code>null</code> if the current settings are invalid
     *
     */
    public DataTableSpec createDataTableSpec() {

        // first check if the settings are in a state we can create a valid
        // table spec for.
        // SettingsStatus status = new SettingsStatus();
        // addThisStatus(status);
        SettingsStatus status = getStatusOfSettings(true, null);
        if (status.getNumOfErrors() > 0) {
            return null;
        }

        // collect the ColumnSpecs for each column
        Vector<DataColumnSpec> cSpec = new Vector<DataColumnSpec>();
        for (int c = 0; c < getNumberOfColumns(); c++) {
            ColProperty cProp = m_columnProperties.get(c);
            if (!cProp.getSkipThisColumn()) {
                cSpec.add(cProp.getColumnSpec());
            }
        }

        return new DataTableSpec(getTableName(), cSpec
                .toArray(new DataColumnSpec[cSpec.size()]));
    }

    /**
     * @return an array whose elements are set to true if the corresponding
     *         column should be skipped - not included in the table.
     */
    public boolean[] getSkippedColumns() {
        boolean[] result = new boolean[getNumberOfColumns()];
        for (int c = 0; c < result.length; c++) {
            ColProperty cProp = m_columnProperties.get(c);
            result[c] = cProp.getSkipThisColumn();
        }
        return result;
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
     * Reads the FileReaderNodeSettings from the specified XML file and returns
     * a new settings object. It throws an exception if something goes wrong.
     * The settings are not checked. They could be incomplete or invalid. It
     * also reads possible values from the data file - but only if the settings
     * are useable and the table contains a string column.
     *
     * @param xmlLocation location of the xml file to read. Must be a valid URL.
     * @return a new settings object containing the settings read fromt the
     *         specified XML file.
     *
     * @throws IllegalStateException if something goes wrong
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
                xmlURL = tmp.getAbsoluteFile().toURI().toURL();
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
     * Set true to indicate that the flag is actually set and is not still the
     * default value.
     *
     * @param s the new value of the flag
     */
    public void setIgnoreEmptyLinesUserSet(final boolean s) {
        m_ignoreEmptyLinesIsSet = s;
    }

    /**
     * @return <code>true</code> if the value was set by the user,
     *         <code>false</code> if it's still at constructor's default value
     */
    public boolean isIgnoreEmptyLinesUserSet() {
        return m_ignoreEmptyLinesIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setFileHasRowHeadersUserSet(final boolean s) {
        m_hasRowHeadersIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set, <code>false</code> if
     *         it's still at constructor's default value
     */
    public boolean isFileHasRowHeadersUserSet() {
        return m_hasRowHeadersIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setFileHasColumnHeadersUserSet(final boolean s) {
        m_hasColHeadersIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set, <code>false</code> if
     *         it's still at constructor's default value
     */
    public boolean isFileHasColumnHeadersUserSet() {
        return m_hasColHeadersIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setCommentUserSet(final boolean s) {
        m_commentIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set by the user,
     *         <code>false</code> if it's still at constructor's default value
     */
    public boolean isCommentUserSet() {
        return m_commentIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setDelimiterUserSet(final boolean s) {
        m_delimIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set by the user,
     *         <code>false</code> if it's still at constructor's default value
     */
    public boolean isDelimiterUserSet() {
        return m_delimIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setQuoteUserSet(final boolean s) {
        m_quoteIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set by the user,
     *         <code>false</code> if it's still at constructor's default value
     */
    public boolean isQuoteUserSet() {
        return m_quoteIsSet;
    }

    /**
     * Set <code>true</code> to indicate that the flag is actually set and is
     * not still the default value.
     *
     * @param s the new value of the flag
     */
    public void setWhiteSpaceUserSet(final boolean s) {
        m_whiteIsSet = s;
    }

    /**
     * @return <code>true</code> is the value was set by the user,
     *         <code>false</code> if it's still at constructor's default value
     */
    public boolean isWhiteSpaceUserSet() {
        return m_whiteIsSet;
    }

    /**
     * Sets the "is user set" flag and stores the user value.
     *
     * @param ignoreEm if <code>true</code> extra delims at the end of the row
     *            (in case of a tab or space delim) will be ignored.
     */
    public void setIgnoreDelimsAtEndOfRowUserValue(final boolean ignoreEm) {
        m_ignoreDelimsAtEndOfRowIsSet = true;
        m_delimsAtEOLUserValue = ignoreEm;
    }

    /**
     * @return <code>true</code>, if user set the value for "ignore delims at
     *         end of row"
     */
    public boolean ignoreDelimsAtEORUserSet() {
        return m_ignoreDelimsAtEndOfRowIsSet;
    }

    /**
     * Tells whether the decimal separator is set by the user or guessed by the
     * analyzer (or still at its default).
     *
     * @return true, if the user explicitly set the decimal separator, false, if
     *         the separators are still at their default, or the analyser
     *         guessed it.
     */
    public boolean decimalSeparatorUserSet() {
        return m_decimalSeparatorIsSet;
    }

    /**
     * Sets a new value to the flag that indicates that the decimal value is
     * explictly set by the user.
     *
     * @param value the new value of the flag.
     */
    public void setDecimalSeparatorUserSet(final boolean value) {
        m_decimalSeparatorIsSet = value;
    }

    /**
     * @return the value the user chose for this flag.
     */
    public boolean ignoreDelimsAtEORUserValue() {
        return m_delimsAtEOLUserValue;
    }

    /**
     * @return the value of the analyze flag previously set. Or
     *         <code>false</code> by default.
     */
    boolean analyzeUsedAllRows() {
        return m_analyzedAllRows;
    }

    /**
     * Sets the value of the flag which is used to indicate if the
     * {@link FileAnalyzer} looked at all rows when it extracts the default
     * settings. The value of the flag is not stored when the settings are saved
     * into a config, and is not recovered from a config object.
     *
     * @param val the new value of the flag
     */
    void setAnalyzeUsedAllRows(final boolean val) {
        m_analyzedAllRows = val;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingsStatus getStatusOfSettings() {
        return getStatusOfSettings(false, null);
    }

    /**
     * Method to check consistency and completeness of the current settings. It
     * will return a {@link SettingsStatus} object which contains info, warning
     * and error messages, if something is fishy with the settings.
     *
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
            status.addError("Missing column properties for columns "
                    + m_columnProperties.size() + " to " + m_numOfColumns);
            propsToCheck = m_columnProperties.size();
        }

        // map for faster uniqueness checking
        Map<String, Integer> colNames = new HashMap<String, Integer>();

        for (int c = 0; c < propsToCheck; c++) {
            // check if we got a column property object for each column
            ColProperty cProp = m_columnProperties.get(c);
            if (cProp == null) {
                status.addError("No column properties specified for column"
                        + " with index " + c);
                // that's all we can do with a null col prop...
                continue;
            }
            // check column spec, i.e. name, type and possible values
            if (cProp.getColumnSpec() == null) {
                status.addError("Column name and type not specified for column"
                        + c);
                continue;
            }
            // check the name
            String cName = cProp.getColumnSpec().getName();
            if (cName == null) {
                status.addError("No column name specified for column " + c);
            }
            // and type
            DataType cType = cProp.getColumnSpec().getType();
            if (cType == null) {
                status.addError("No column type specified for column " + c);
            }
            // check uniqueness of column name
            if ((cName != null) && !cProp.getSkipThisColumn()) {
                Integer prevCol = colNames.put(cName, c);
                if (prevCol != null) {
                    status
                            .addError("Columns with index " + c + " and "
                                    + prevCol + " have the same name ('"
                                    + cName + "')");
                }
            } // if (cName != null)

            // check a possible values - if any
            Set<DataCell> possVals =
                    cProp.getColumnSpec().getDomain().getValues();
            if (possVals != null) {
                // possible values must not be null
                for (DataCell val : possVals) {
                    if (val == null) {
                        status.addError("Invalid possible value for column "
                                + c + " (<null>).");
                    } else {
                        if (cType != null) {
                            if (!cType.isASuperTypeOf(val.getType())) {
                                status.addError("Incompatible possible "
                                        + "value specified for column " + c);
                            }
                        }
                    }
                }
            }

        }

        if (m_numOfColumns < m_columnProperties.size()) {
            status.addInfo("More column properties specified "
                    + "than number of columns - extra props are unchecked");

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer res = new StringBuffer(super.toString());
        for (int c = 0; c < getNumberOfColumns(); c++) {
            res.append("\nCol_No." + c + ":");
            res.append(m_columnProperties.get(c).toString());
        }
        return res.toString();
    }

    /**
     * Checks the flag that indicates if settings will be reset at location
     * change.
     *
     * @return true if settings are not reset on file location change.
     */
    boolean getPreserveSettings() {
        return m_preserveSettings;
    }

    /**
     * Sets the flag that determines if settings are reset if a new data
     * location is entered in the dialog.
     *
     * @param preserveSettings set true to reset all dialog settings if the data
     *            location changes, or false to preserve the current settings.
     */
    void setPreserveSettings(final boolean preserveSettings) {
        m_preserveSettings = preserveSettings;
    }
}
