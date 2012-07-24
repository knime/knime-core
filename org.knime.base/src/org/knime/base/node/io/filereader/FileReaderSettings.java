/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   25.11.2004 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.knime.base.node.util.BufferedFileReader;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.tokenizer.Delimiter;
import org.knime.core.util.tokenizer.SettingsStatus;
import org.knime.core.util.tokenizer.TokenizerSettings;

/**
 * Contains all settings needed to read in a ASCII data file. This includes the
 * location of the data file, the settings for the tokenizer (like column
 * delimiter, comment patterns etc.) as well as the row headers and more. This
 * object combined with a {@link org.knime.core.data.DataTableSpec} can be used
 * to create a {@link FileTable} from. A <code>FileTable</code> will represent
 * then the data of the file in a {@link org.knime.core.data.DataTable}.
 *
 * @author ohl, University of Konstanz
 */
public class FileReaderSettings extends TokenizerSettings {

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(FileReaderSettings.class);

    /* the list of settings that are stored in here. */

    private URL m_dataFileLocation;

    /* the table name (derived from the filename if not overridden) */
    private String m_tableName;

    /*
     * in tokens read for a double column, this char gets replaced with a "."
     */
    private char m_decimalSeparator;

    /*
     * in tokens read for a double column, this char (if different to \0)
     * gets removed
     */
    private char m_thousandsSeparator;

    /*
     * if set, the reader will eat all surplus empty tokens at the end of a row.
     */
    private boolean m_ignoreEmptyTokensAtEOR;

    /*
     * if set, lines with too few data item are filled with missing values
     */
    private boolean m_supportShortLines;

    /*
     * if not null, used as missing value pattern for all string columns that
     * don't have their own missing value pattern set.
     */
    private String m_globalMissPatternStrCols;

    /*
     * if set, the first row in the file will be considered column names - and
     * discarded (we read rows, not column headers!)
     */
    private boolean m_fileHasColumnHeaders;

    /* delimiters ending a row, will be also added as token delimiters */
    private HashSet<String> m_rowDelimiters;

    /* true if the first column in the file should be considered a row header */
    private boolean m_fileHasRowHeaders;

    /* true if it ignores empty lines */
    private boolean m_ignoreEmptyLines;

    /*
     * if set, this will be used to generate a row header - and the one in the
     * file - if any - will be ignored
     */
    private String m_rowHeaderPrefix;

    /*
     * true by default. The Reader makes IDs it read from file unique.
     */
    private boolean m_uniquifyRowIDs;

    /*
     * for each column a string (or null) that will be replaced - if read - with
     * a missing cell.
     */
    private Vector<String> m_missingPatterns;

    /*
     * if set to a not-negative number, it limits the number of lines read from
     * the input file. -1 reads all.
     */
    private long m_maxNumberOfRowsToRead;

    // for nicer error messages
    private int m_columnNumberDeterminingLine;

    // name of the character set used to decode the stream.
    // Null uses VM default.
    private String m_charsetName;

    /**
     * This will be used if the file has not row headers and no row prefix is
     * set.
     */
    public static final String DEF_ROWPREFIX = "Row";

    /** Key used to store data file location in a config object. */
    public static final String CFGKEY_DATAURL = "DataURL";

    private static final String CFGKEY_DECIMALSEP = "DecimalSeparator";

    private static final String CFGKEY_THOUSANDSEP = "ThrousandsSeparator";

    private static final String CFGKEY_HASCOL = "hasColHdr";

    private static final String CFGKEY_HASROW = "hasRowHdr";

    private static final String CFGKEY_ROWPREF = "rowPrefix";

    private static final String CFGKEY_UNIQUIFYID = "uniquifyRowID";

    private static final String CFGKEY_IGNOREEMPTY = "ignoreEmptyLines";

    private static final String CFGKEY_IGNOREATEOR = "ignEmtpyTokensAtEOR";

    private static final String CFGKEY_SHORTLINES = "acceptShortLines";

    private static final String CFGKEY_ROWDELIMS = "RowDelims";

    private static final String CFGKEY_ROWDELIM = "RDelim";

    private static final String CFGKEY_RDCOMB = "SkipEmptyLine";

    private static final String CFGKEY_MISSINGS = "MissingPatterns";

    private static final String CFGKEY_MISSING = "MissPattern";

    private static final String CFGKEY_GLOBALMISSPATTERN = "globalMissPattern";

    private static final String CFGKEY_TABLENAME = "TableName";

    private static final String CFGKEY_MAXROWS = "MaxNumOfRows";

    private static final String CFGKEY_COLDETERMLINENUM = "ColNumDetermLine";

    private static final String CFGKEY_CHARSETNAME = "CharsetName";


    /**
     * Creates a new object holding all settings needed to read the specified
     * file. The file must be an ASCII representation of the data to read. We
     * are not specifying any default behavior of that newly created object, you
     * really need to set all parameters before reading the file with these
     * settings.
     */
    public FileReaderSettings() {

        init();
    }

    /**
     * Creates a new object holding the same settings values as the one passed
     * in.
     *
     * @param clonee the object to read the settings values from
     */
    public FileReaderSettings(final FileReaderSettings clonee) {
        super(clonee);
        m_dataFileLocation = clonee.m_dataFileLocation;
        m_tableName = clonee.m_tableName;

        m_decimalSeparator = clonee.m_decimalSeparator;
        m_thousandsSeparator = clonee.m_thousandsSeparator;

        m_fileHasColumnHeaders = clonee.m_fileHasColumnHeaders;
        m_fileHasRowHeaders = clonee.m_fileHasRowHeaders;
        m_ignoreEmptyLines = clonee.m_ignoreEmptyLines;
        m_ignoreEmptyTokensAtEOR = clonee.m_ignoreEmptyTokensAtEOR;
        m_supportShortLines = clonee.m_supportShortLines;
        m_maxNumberOfRowsToRead = clonee.m_maxNumberOfRowsToRead;

        m_rowHeaderPrefix = clonee.m_rowHeaderPrefix;
        m_uniquifyRowIDs = clonee.m_uniquifyRowIDs;

        m_rowDelimiters = new HashSet<String>(clonee.m_rowDelimiters);
        m_missingPatterns = new Vector<String>(clonee.m_missingPatterns);
        m_globalMissPatternStrCols = clonee.m_globalMissPatternStrCols;
        m_columnNumberDeterminingLine = clonee.m_columnNumberDeterminingLine;

        m_charsetName = clonee.m_charsetName;

    }

    // initializes private members. Needs to be called from two constructors.
    private void init() {
        m_dataFileLocation = null;
        m_tableName = null;

        m_decimalSeparator = '.';

        m_thousandsSeparator = '\0';

        m_fileHasColumnHeaders = false;
        m_fileHasRowHeaders = false;
        m_ignoreEmptyLines = false;
        m_ignoreEmptyTokensAtEOR = false;
        m_supportShortLines = false;
        m_maxNumberOfRowsToRead = -1; // default is: read them all

        m_rowHeaderPrefix = null;
        m_uniquifyRowIDs = false;

        m_rowDelimiters = new HashSet<String>();
        m_missingPatterns = new Vector<String>();
        m_globalMissPatternStrCols = null; // no global missing value pattern
        m_columnNumberDeterminingLine = -1;

        m_charsetName = null; // uses the default char set name

    }

    /**
     * Creates a new FileReaderSettings object initializing its settings from
     * the passed config object.
     *
     * @param cfg the config object containing all settings this object will be
     *            initialized with
     * @throws InvalidSettingsException if the passed config object contains
     *             invalid or insufficient settings
     */
    public FileReaderSettings(final NodeSettingsRO cfg)
            throws InvalidSettingsException {

        // set the tokenizer settings first. The rowDelimiter reader depends
        // on the fact that the tokenizer reads its settings first.
        super(cfg);
        init();
        if (cfg != null) {
            try {
                URL dataFileLocation = new URL(cfg.getString(CFGKEY_DATAURL));
                setDataFileLocationAndUpdateTableName(dataFileLocation);
            } catch (MalformedURLException mfue) {
                throw new IllegalArgumentException(
                        "Cannot create URL of data file" + " from '"
                                + cfg.getString(CFGKEY_DATAURL)
                                + "' in filereader config");
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for "
                        + "file reader settings! Key '" + CFGKEY_DATAURL
                        + "' missing!");
            }
            // see if we got a tablename. For backwardcompatibility reasons
            // don't fail if its missing.
            try {
                setTableName(cfg.getString(CFGKEY_TABLENAME));
            } catch (InvalidSettingsException ise) {
                // when we set the data location (above) we already set a name
            }
            try {
                m_fileHasColumnHeaders = cfg.getBoolean(CFGKEY_HASCOL);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for "
                        + "file reader settings! Key '" + CFGKEY_HASCOL
                        + "' missing!");
            }

            try {
                m_fileHasRowHeaders = cfg.getBoolean(CFGKEY_HASROW);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for "
                        + "file reader settings! Key '" + CFGKEY_HASROW
                        + "' missing!");
            }

            try {
                m_ignoreEmptyLines = cfg.getBoolean(CFGKEY_IGNOREEMPTY);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException("Illegal config object for "
                        + "file reader settings! Key '" + CFGKEY_IGNOREEMPTY
                        + "' missing!");
            }

            // set the row header prefix - if specified. It's optional.
            if (cfg.containsKey(CFGKEY_ROWPREF)) {
                try {
                    m_rowHeaderPrefix = cfg.getString(CFGKEY_ROWPREF);
                } catch (InvalidSettingsException ice) {
                    throw new InvalidSettingsException(
                            "Illegal config object for file"
                                    + " reader settings! Wrong type of key '"
                                    + CFGKEY_HASROW + "'!");
                }
            }

            if (cfg.containsKey(CFGKEY_MISSINGS)) {
                NodeSettingsRO missPattConf;
                try {
                    missPattConf = cfg.getNodeSettings(CFGKEY_MISSINGS);
                } catch (InvalidSettingsException ice) {
                    throw new InvalidSettingsException(
                            "Illegal config object for file "
                                    + "reader settings! Wrong type of key '"
                                    + CFGKEY_MISSINGS + "'!");
                }
                readMissingPatternsFromConfig(missPattConf);
            }

            m_globalMissPatternStrCols =
                    cfg.getString(CFGKEY_GLOBALMISSPATTERN, null);
            NodeSettingsRO rowDelimConf = null;
            try {
                rowDelimConf = cfg.getNodeSettings(CFGKEY_ROWDELIMS);
            } catch (InvalidSettingsException ice) {
                throw new InvalidSettingsException(
                        "Illegal config object for file reader settings!"
                                + " Not existing or wrong type of key '"
                                + CFGKEY_ROWDELIMS + "'!");

            }
            // get the decimal and thousands separator.
            // It's optional for backward compatibility and defaults to '.'
            m_decimalSeparator = cfg.getChar(CFGKEY_DECIMALSEP, '.');

            m_thousandsSeparator = cfg.getChar(CFGKEY_THOUSANDSEP, '\0');

            if (m_decimalSeparator == m_thousandsSeparator) {
                throw new InvalidSettingsException("Decimal separator and "
                        + "thousands separator can't be the same character");
            }

            // ignore empty tokens at end of row?
            // It'S optional and default to false, for backward compatibility.
            m_ignoreEmptyTokensAtEOR =
                    cfg.getBoolean(CFGKEY_IGNOREATEOR, false);

            // default is false, for backward compatibility.
            m_supportShortLines = cfg.getBoolean(CFGKEY_SHORTLINES, false);

            // default to false, for backward compatibility
            m_uniquifyRowIDs = cfg.getBoolean(CFGKEY_UNIQUIFYID, false);

            // default to "read them all", for backward compatibility
            m_maxNumberOfRowsToRead = cfg.getLong(CFGKEY_MAXROWS, -1);

            m_columnNumberDeterminingLine =
                    cfg.getInt(CFGKEY_COLDETERMLINENUM, -1);

            readRowDelimitersFromConfig(rowDelimConf);

            // default to null - which uses the default char set name
            m_charsetName = cfg.getString(CFGKEY_CHARSETNAME, null);

        } // if (cfg != null)

    }

    /**
     * Saves all settings into a {@link org.knime.core.node.NodeSettingsWO}
     * object. Using the cfg object to construct a new FileReaderSettings object
     * should lead to an object identical to this.
     *
     * @param cfg the config object the settings are stored into
     */
    @Override
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'file "
                    + "reader settings' to null config!");
        }

        if (m_dataFileLocation != null) {
            cfg.addString(CFGKEY_DATAURL, m_dataFileLocation.toString());
        }

        super.saveToConfiguration(cfg);

        cfg.addString(CFGKEY_TABLENAME, m_tableName);
        cfg.addBoolean(CFGKEY_HASCOL, m_fileHasColumnHeaders);
        cfg.addBoolean(CFGKEY_HASROW, m_fileHasRowHeaders);
        cfg.addBoolean(CFGKEY_IGNOREEMPTY, m_ignoreEmptyLines);

        if (m_rowHeaderPrefix != null) {
            cfg.addString(CFGKEY_ROWPREF, m_rowHeaderPrefix);
        }

        saveRowDelimitersToConfig(cfg.addNodeSettings(CFGKEY_ROWDELIMS));
        saveMissingPatternsToConfig(cfg.addNodeSettings(CFGKEY_MISSINGS));
        cfg.addString(CFGKEY_GLOBALMISSPATTERN, m_globalMissPatternStrCols);
        cfg.addChar(CFGKEY_DECIMALSEP, m_decimalSeparator);
        cfg.addChar(CFGKEY_THOUSANDSEP, m_thousandsSeparator);
        cfg.addBoolean(CFGKEY_IGNOREATEOR, m_ignoreEmptyTokensAtEOR);
        cfg.addBoolean(CFGKEY_SHORTLINES, m_supportShortLines);
        cfg.addBoolean(CFGKEY_UNIQUIFYID, m_uniquifyRowIDs);
        cfg.addLong(CFGKEY_MAXROWS, m_maxNumberOfRowsToRead);
        cfg.addInt(CFGKEY_COLDETERMLINENUM, m_columnNumberDeterminingLine);
        cfg.addString(CFGKEY_CHARSETNAME, m_charsetName);

    }

    /*
     * read the patterns, one for each column, that will be replaced by missing
     * cells from the configuration object.
     */
    private void readMissingPatternsFromConfig(
            final NodeSettingsRO missPattConf) {
        if (missPattConf == null) {
            throw new NullPointerException(
                    "Can't read missing patterns from null config object");
        }
        int m = 0;
        for (String key : missPattConf.keySet()) {
            // they should all start with "MissPattern"...
            if (key.indexOf(CFGKEY_MISSING) != 0) {
                LOGGER.warn("Illegal missing pattern " + "configuration '"
                        + key + "'. Ignoring it!");
                continue;
            }
            try {
                String missi = missPattConf.getString(CFGKEY_MISSING + m);
                setMissingValueForColumn(m, missi);
            } catch (InvalidSettingsException ice) {
                LOGGER.warn("Illegal missing pattern " + "configuration '"
                        + key + "' (should be of type"
                        + " string). Ignoring it!");
                continue;
            }
            m++;
        }
    }

    /*
     * saves all currently set missing patterns for each column to a config
     * object
     */
    private void saveMissingPatternsToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException(
                    "Can't save missing patterns to null config object");
        }

        for (int m = 0; m < m_missingPatterns.size(); m++) {
            cfg.addString(CFGKEY_MISSING + m, m_missingPatterns.get(m));
        }

    }

    /*
     * reads the Row delimiters and settings from a config object or reads them
     * from it (next function). The crux with the row delimtiers is, that they
     * are ordinary delimiters for the file tokenizers (just returned as
     * separate token). Thus they will be read in already! - And they will be
     * saved before we save our row delimiters. So, we need to be a bit careful
     * here.
     */
    private void readRowDelimitersFromConfig(final NodeSettingsRO rowDelims)
            throws InvalidSettingsException {

        for (int rowDelIdx = 0; rowDelims.containsKey(CFGKEY_ROWDELIM
                + rowDelIdx); rowDelIdx++) {

            boolean combine;
            String rowDelim;

            try {
                rowDelim = rowDelims.getString(CFGKEY_ROWDELIM + rowDelIdx);
            } catch (InvalidSettingsException ice) {
                LOGGER.warn("Invalid configuration for" + " row delimiter '"
                        + CFGKEY_ROWDELIM + rowDelIdx
                        + "' (must be of type string). Ignoring it!");
                continue;
            }

            if (rowDelims.containsKey(CFGKEY_RDCOMB + rowDelIdx)) {
                try {
                    combine = rowDelims.getBoolean(CFGKEY_RDCOMB + rowDelIdx);
                } catch (InvalidSettingsException ice) {
                    // shouldn't happen anyway
                    combine = false;
                }
            } else {
                combine = false;
            }

            // the row delimiter should already be set as delimiter (as the
            // super reads its settings first and all row delims are also
            // token delims).
            Delimiter delim = getDelimiterPattern(rowDelim);
            if (delim == null) {
                throw new InvalidSettingsException("Row delimiter must be "
                        + "defined as delimiter.");
            }
            if (!delim.returnAsToken()) {
                throw new InvalidSettingsException("Row delimiter must be "
                        + "returned as token.");
            }
            if (!(delim.combineConsecutiveDelims() == combine)) {
                throw new InvalidSettingsException("Delimiter definition "
                        + "doesn't match row delim definition.");
            }

            // we just add the pattern to the list of row delim patterns
            m_rowDelimiters.add(rowDelim);

        }
    }

    /*
     * See comment above previous method
     */
    private void saveRowDelimitersToConfig(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save 'row delimiters' "
                    + "to null config!");
        }
        int rowDelIdx = 0;
        for (String rowDelim : m_rowDelimiters) {
            boolean combineMultiple;
            Delimiter delim = getDelimiterPattern(rowDelim);
            if (delim == null) {
                LOGGER.error("Row delimiter '" + rowDelim
                        + "' was not defined with the tokenizer.");
                LOGGER.error("Storing the property 'skip empty lines' "
                        + "with 'false'.");
                combineMultiple = false;
            } else {
                // we should really not include row delimiters in tokens but
                // always return them as separate token - otherwise we can't
                // recognize them when reading the file.
                assert !delim.includeInToken();
                assert delim.returnAsToken();
                combineMultiple = delim.combineConsecutiveDelims();
            }

            cfg.addString(CFGKEY_ROWDELIM + rowDelIdx, rowDelim);
            if (combineMultiple) {
                cfg.addBoolean(CFGKEY_RDCOMB + rowDelIdx, combineMultiple);
            }
            rowDelIdx++;
        }

    }

    /**
     * Sets the location of the file to read data from. Won't check correctness.
     *
     * @param dataFileLocation the URL of the data file these settings are for
     */
    public void setDataFileLocationAndUpdateTableName(
            final URL dataFileLocation) {
        if (dataFileLocation == null) {
            setTableName("");
        } else {
            /*
             * don't override a (possibly user set) name if it's not a new
             * location
             */
            if (!dataFileLocation.toExternalForm().equals(
                    m_dataFileLocation.toExternalForm())) {
                setTableName(getPureFileNameWithExtension(dataFileLocation));
            }
        }
        m_dataFileLocation = dataFileLocation;
    }

    /**
     * @return the location of the file these settings are meant for
     */
    public URL getDataFileLocation() {
        return m_dataFileLocation;
    }

    /**
     * Set the new character set name that will be used the next time a new
     * input reader is created (see {@link #createNewInputReader()}).
     *
     * @param name any character set supported by Java, or null to use the VM
     *            default char set.
     * @throws IllegalArgumentException if the specified name is not supported.
     * @throws java.nio.charset.IllegalCharsetNameException if the specified
     *             name is not supported.
     */
    public void setCharsetName(final String name) {
        if (name == null) {
            m_charsetName = null;
        } else {
            if (!Charset.isSupported(name)) {
                throw new IllegalArgumentException("Unsupported charset name '"
                        + name + "'.");
            }
            m_charsetName = name;
        }
    }

    /**
     * @return the charset name set, or null if the VM's default is used
     */
    public String getCharsetName() {
       return m_charsetName;
    }

    /**
     * @return a new reader to read from the data file location. It will create
     *         a buffered reader, and for zipped sources a GZIP one.
     *         If the data location is not set an exception will fly.
     * @throws NullPointerException if the data location is not set
     * @throws IOException if an IO Error occurred when opening the stream
     */
    public BufferedFileReader createNewInputReader() throws IOException {
        return BufferedFileReader.createNewReader(getDataFileLocation(),
                getCharsetName());
    }

    /**
     * Sets a new name for the table created by this node.
     *
     *
     * @param newName the new name to set.
     *          Valid names are not <code>null</code>.
     */
    public void setTableName(final String newName) {
        m_tableName = newName;
    }

    /**
     * @return the currently set name of the table created by this node. Valid
     *         names are not <code>null</code>, but the method could return
     *         null, if no name was set yet.
     */
    public String getTableName() {
        return m_tableName;
    }

    /**
     * @param loc the location to extract the filename from.
     * @return the filename part of the URL without path. Or <code>null</code>
     *         if the URL is <code>null</code>.
     */
    private String getPureFileNameWithExtension(final URL loc) {
        if (loc != null) {
            String name = loc.getPath();
            int firstIdx = name.lastIndexOf('/') + 1;
            if (firstIdx == name.length()) {
                // last character is '/' ?!?! Filename is empty. Weird anyway.
                return "";
            }
            return name.substring(firstIdx);
        }
        return null;
    }

    /**
     * Tells whether the first line in the file should be considered column
     * headers, or not.
     *
     * @param flag if <code>true</code> the first line in the file will not be
     *            considered data, but either ignored or used as column headers,
     *            depending on the column headers set (or not) in this object.
     */
    public void setFileHasColumnHeaders(final boolean flag) {
        m_fileHasColumnHeaders = flag;
    }

    /**
     * @return a flag telling if the first line in the file will not be
     *         considered data, but either ignored or used as column headers,
     *         depending on the column headers set (or not) in this object.
     */
    public boolean getFileHasColumnHeaders() {
        return m_fileHasColumnHeaders;
    }

    /**
     * Tells whether the first token in each line in the file should be
     * considered row header, or not.
     *
     * @param flag if <code>true</code> the first item in each line in the
     *            file will not be considered data, but either ignored or used
     *            as row header, depending on the row header prefix set (or not)
     *            in this object.
     */
    public void setFileHasRowHeaders(final boolean flag) {
        m_fileHasRowHeaders = flag;
    }

    /**
     * @return a flag telling if the first item in each line in the file will
     *         not be considered data, but either ignored or used as row header,
     *         depending on the row header prefix set (or not) in this object.
     */
    public boolean getFileHasRowHeaders() {
        return m_fileHasRowHeaders;
    }

    /**
     * Set a string that will be used as a prefix for each row header. The
     * header generated will have the row number added to the prefix. This
     * prefix - if set - will be used, regardless of any row header read from
     * the file - if there is any.
     *
     * @param rowPrefix the string that will be used to construct the header for
     *            each row. The actual row header will have the row number
     *            added. Specify <code>null</code> to clear the prefix.
     */
    public void setRowHeaderPrefix(final String rowPrefix) {
        m_rowHeaderPrefix = rowPrefix;
    }

    /**
     * @return the string that will be used to construct the header for each
     *         row. The actual row header will have the row number added. If
     *         this returns <code>null</code>, the row header from the file
     *         will be used - if any, otherwise the {@link #DEF_ROWPREFIX}.
     */
    public String getRowHeaderPrefix() {
        return m_rowHeaderPrefix;
    }

    /**
     * @return true if the reader should make rowIDs read from file unique.
     */
    public boolean uniquifyRowIDs() {
        return m_uniquifyRowIDs;
    }

    /**
     * @param uniquify the new value of the "uniquify row IDs from file" flag.
     */
    public void setUniquifyRowIDs(final boolean uniquify) {
        m_uniquifyRowIDs = uniquify;
    }

    /**
     * Will add a delimiter pattern that will terminate a row. Row delimiters
     * are always token (=column) delimiters. Row delimiters will always be
     * returned as separate token by the filereader. You can define a row
     * delimiter that was previously defined a token delimiter. But only, if the
     * delimiter was not set to be included in the token. Otherwise you will get
     * a {@link IllegalArgumentException}.
     *
     * @param rowDelimPattern the row delimiter pattern. Row delimiters will
     *            always be token delimiters and will always be returned as
     *            separate token.
     * @param skipEmptyRows if set <code>true</code>, multiple consecutive
     *            row delimiters will be combined and returned as one
     */
    public void addRowDelimiter(final String rowDelimPattern,
            final boolean skipEmptyRows) {

        Delimiter existingDelim = getDelimiterPattern(rowDelimPattern);

        if (existingDelim != null) {
            if (existingDelim.includeInToken()) {
                // can't do that! Row delimiters need to be returned as
                // separate token. Can't include a delimiter in a token and
                // return it as separate token at the same time.
                throw new IllegalArgumentException("Can't define a row "
                        + "delimiter ('" + rowDelimPattern
                        + "') that was defined as token delimiter before"
                        + " that should be included in the tokens");
            }
        }

        Delimiter newDelim =
                new Delimiter(rowDelimPattern, skipEmptyRows, true, false);
        // returnAsSeparate, includeInToken);

        m_rowDelimiters.add(rowDelimPattern);
        addDelimiterPattern(newDelim);

    }

    /**
     * Removes the row delimiter with the specified pattern. Even though the
     * above method changes an existing column delimiter to being a row delim,
     * this function completely deletes the row delimiter (instead of being
     * aware that it might have been a col delim before and changing it back to
     * a col delim).
     *
     * @param pattern the row delimiter to delete must not be null.
     *            <code>null</code> is always a row delimiter.
     * @return a delimiter object specifying the deleted delimiter, or
     *         <code>null</code> if no row delimiter with the pattern existed
     */
    public Delimiter removeRowDelimiter(final String pattern) {
        if (pattern == null) {
            throw new NullPointerException(
                    "Can't remove <null> as row delimiter.");
        }
        if (isRowDelimiter(pattern)) {
            m_rowDelimiters.remove(pattern);
            return removeDelimiterPattern(pattern);
        }
        return null;
    }

    /**
     * Blows away all defined row delimiters! After a call to this function no
     * row delimiter will be defined (except <code>null</code>).
     */
    public void removeAllRowDelimiters() {
        for (String delim : m_rowDelimiters) {
            removeDelimiterPattern(delim);
        }
        m_rowDelimiters.clear();
    }

    /**
     * @param pattern the pattern to test
     * @return <code>true</code> if the pattern is a row delimiter.
     *         <code>null</code> is always a row delimiter.
     */
    public boolean isRowDelimiter(final String pattern) {
        if (pattern == null) {
            return true;
        }
        assert (!m_rowDelimiters.contains(pattern))
                || (getDelimiterPattern(pattern) != null);
        return m_rowDelimiters.contains(pattern);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAllDelimiters() {
        super.removeAllDelimiters();
        m_rowDelimiters.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Delimiter removeDelimiterPattern(final String pattern) {
        m_rowDelimiters.remove(pattern);
        return super.removeDelimiterPattern(pattern);
    }

    /**
     * @return <code>true</code> if the file reader ignores empty lines
     */
    public boolean getIgnoreEmtpyLines() {
        return m_ignoreEmptyLines;
    }

    /**
     * @param ignoreEm pass <code>true</code> to have the file reader not
     *            return empty lines from the data file
     */
    public void setIgnoreEmptyLines(final boolean ignoreEm) {
        m_ignoreEmptyLines = ignoreEm;
    }

    /**
     * Returns <code>true</code> if the file reader combines multiple
     * consecutive row delimiters with this pattern (i.e. it skips empty rows if
     * it finds multiple if these (and only these) row delimiters). The method
     * throws an {@link IllegalArgumentException} at you if the specified
     * pattern is not a row delimiter.
     *
     * @param pattern the pattern to test for
     * @return true if the filereader skips empty rows for this row delimiter
     */
    public boolean combinesMultipleRowDelimiters(final String pattern) {
        if (!m_rowDelimiters.contains(pattern)) {
            throw new IllegalArgumentException("The specified pattern '"
                    + pattern + "' is not a row delimiter.");
        }

        return getDelimiterPattern(pattern).combineConsecutiveDelims();

    }

    /**
     * Specifies a pattern that, if read in for the specified column, will be
     * considered placeholder for a missing value, and the data table will
     * contain a missing cell instead of that value then.
     *
     * @param colIdx the index of the column this missing value is set for
     * @param pattern the pattern specifying the missing value in the data file
     *            for the specified column. Can be <code>null</code> to delete
     *            a previously set pattern.
     */
    public void setMissingValueForColumn(final int colIdx,
            final String pattern) {
        if (m_missingPatterns.size() <= colIdx) {
            m_missingPatterns.setSize(colIdx + 1);
        }
        m_missingPatterns.set(colIdx, pattern);
    }

    /**
     * Returns the pattern that, if read in for the specified column, will be
     * considered placeholder for a missing value, and the data table will
     * contain a missing cell instead of that value then.
     *
     * @param colIdx the index of the column the missing value is asked for
     * @return the pattern that will be considered placeholder for a missing
     *         value in the specified column. Or <code>null</code> if no
     *         patern is set for that column.
     */
    public String getMissingValueOfColumn(final int colIdx) {
        if (m_missingPatterns.size() <= colIdx) {
            return null;
        }
        return m_missingPatterns.get(colIdx);
    }

    /**
     * @return the character that is considered decimal separator in the data
     *         (token) for a double type column
     */
    public char getDecimalSeparator() {
        return m_decimalSeparator;
    }

    /**
     * Sets the character that will be considered decimal separator in the data
     * (token) read for double type columns.
     *
     * @param sep the new decimal character to set for doubles. Can't be the
     *            same character as the thousands separator.
     */
    public void setDecimalSeparator(final char sep) {
        if (sep == m_thousandsSeparator) {
            throw new IllegalArgumentException("Can't set the decimal "
                    + "separator to the same character as the thousands "
                    + "separator.");

        }
        m_decimalSeparator = sep;
    }

    /**
     * @return the thousandsSeparator. If it is '\0' then it is not set.
     */
    public char getThousandsSeparator() {
        return m_thousandsSeparator;
    }

    /**
     * @param thousandsSeparator the thousandsSeparator to set. If set to '\0'
     *            it will not be applied. Can't be the same as the decimal
     *            separator.
     */
    public void setThousandsSeparator(final char thousandsSeparator) {
        if ((thousandsSeparator != '\0')
            && (thousandsSeparator == m_decimalSeparator)) {
            throw new IllegalArgumentException("Can't set the thousands "
                    + "separator to the same character as the decimal "
                    + "separator.");

        }
        m_thousandsSeparator = thousandsSeparator;
    }

    /**
     * @return <code>true</code> if additional empty tokens should be ignored
     *         at the end of a row (if they are not needed to build the row)
     */
    public boolean ignoreEmptyTokensAtEndOfRow() {
        return m_ignoreEmptyTokensAtEOR;
    }

    /**
     * Sets this flag.
     *
     * @param ignoreThem if <code>true</code>, additional empty tokens will
     *            be ignored at the end of a row (if they are not needed to
     *            build the row)
     */
    public void setIgnoreEmptyTokensAtEndOfRow(final boolean ignoreThem) {
        m_ignoreEmptyTokensAtEOR = ignoreThem;
    }

    /**
     * @param supportShortLines if set true lines with too few data elements
     *            will be accepted and filled with missing values.
     */
    public void setSupportShortLines(final boolean supportShortLines) {
        m_supportShortLines = supportShortLines;
    }

    /**
     * @return true, if lines with too few data items are accepted (they will be
     *         filled with missing values, if read), or false, it the reader
     *         fails when it comes across a short line (the default).
     */
    public boolean getSupportShortLines() {
        return m_supportShortLines;
    }

    /**
     * Sets a new pattern which is translated into a missing value if read from
     * the data file in a string column. Is is used only for columns that don't
     * have their own missing value pattern set (and that are of type string).
     *
     * @param pattern the new pattern to recognize missing values in string
     *            columns. Set to <code>null</code> to clear it.
     */
    public void setMissValuePatternStrCols(final String pattern) {
        m_globalMissPatternStrCols = pattern;
    }

    /**
     * Returns the pattern that, if read in, will be translated into a missing
     * value (in string columns only). It is overridden by the column specific
     * missing value pattern. If it is not defined, null is returned.
     *
     * @return the pattern for missing values, for all string columns. Or null
     *         if not defined.
     */
    public String getMissValuePatternStrCols() {
        return m_globalMissPatternStrCols;
    }

    /**
     * @return the line number in the file that determined the number of
     *         columns. Or -1 if not set yet (or no file analysis took place).
     */
    public int getColumnNumDeterminingLineNumber() {
        return m_columnNumberDeterminingLine;
    }

    /**
     * Sets the line number in the file that determined the number of columns.
     *
     * @param lineNumber the line number in the file that determined the number
     *            of columns.
     */
    public void setColumnNumDeterminingLineNumber(final int lineNumber) {
        m_columnNumberDeterminingLine = lineNumber;
    }

    /**
     * @return the maximum number of lines that should be read from the source.
     *         If -1 is returned all rows should be read.
     */
    public long getMaximumNumberOfRowsToRead() {
        return m_maxNumberOfRowsToRead;
    }

    /**
     * Sets a new maximum for the number of rows to read.
     *
     * @param maxNum the new maximum. If set to -1 all rows of the source will
     *            be read, otherwise no more than the specified number.
     */
    public void setMaximumNumberOfRowsToRead(final long maxNum) {
        m_maxNumberOfRowsToRead = maxNum;
    }

    /**
     * Method to check consistency and completeness of the current settings. It
     * will return a {@link SettingsStatus} object which contains info, warning
     * and error messages. Or if the settings are alright it will return null.
     *
     * @param openDataFile tells whether or not this method should try to access
     *            the data file. This will - if set <code>true</code> - verify
     *            the accessibility of the data.
     * @param tableSpec the spec of the DataTable these settings are for. If set
     *            <code>null</code> only a few checks will be performed - the
     *            ones that are possible without the knowledge of the structure
     *            of the table
     * @return a SettingsStatus object containing info, warning and error
     *         messages, or <code>null</code> if no messages were generated
     *         (i.e. all settings are just fine).
     */
    public SettingsStatus getStatusOfSettings(final boolean openDataFile,
            final DataTableSpec tableSpec) {

        SettingsStatus status = new SettingsStatus();

        addStatusOfSettings(status, openDataFile, tableSpec);

        return status;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SettingsStatus getStatusOfSettings() {
        return getStatusOfSettings(false, null);
    }

    /**
     * Adds its status messages to a passed status object.
     *
     * @param status the object to add messages to - if any
     * @param openDataFile specifies if we should check the accessability of the
     *            data file
     * @param tableSpec the spec of the DataTable these settings are for. If set
     *            <code>null</code> only a few checks will be performed - the
     *            ones that are possible without the knowledge of the structure
     *            of the table
     */
    protected void addStatusOfSettings(final SettingsStatus status,
            final boolean openDataFile, final DataTableSpec tableSpec) {

        // check the data file location. It's required.
        if (m_dataFileLocation == null) {
            status.addError("No data file location specified.");
        } else {
            // see if we can access the data file - if permitted.
            if (openDataFile) {
                BufferedReader reader = null;
                try {
                    reader = createNewInputReader();
                } catch (Exception ioe) {
                    status.addError("I/O Error while connecting to '"
                            + m_dataFileLocation.toString() + "'.");
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ioe) {
                        // then don't close it.
                    }
                }
            }
        }

        if (m_tableName == null) {
            status.addError("No table name set.");
        }

        // check the row headers.
        if (!m_fileHasRowHeaders) {
            // we tell them when we would use the default row header.
            if (m_rowHeaderPrefix == null) {
                status.addInfo("The default row ID ('" + DEF_ROWPREFIX
                        + "+RowIdx') will be used as no row ID prefix"
                        + " is specified and the file doesn't contain row"
                        + " IDs.");
            }
        } else {
            if (m_rowHeaderPrefix != null) {
                status.addInfo("The specified row ID will be used"
                        + " overriding the ones in the data file.");
            }
        }

        // check the row delimiters
        for (String rowDelim : m_rowDelimiters) {
            Delimiter delim = getDelimiterPattern(rowDelim);
            if (delim == null) {
                status.addError("Row delimiter '" + rowDelim
                        + " is not defined" + "being a token delimiter.");
                continue;
            }
            if (delim.includeInToken()) {
                status.addError("Row delimiter '" + rowDelim + "' is set to"
                        + " be included in the token.");
            }
            if (!delim.returnAsToken()) {
                status
                        .addError("Row delimiter '" + rowDelim
                                + "' is not set to"
                                + " be returned as separate token.");
            }
        }
        if (m_rowDelimiters.size() == 0) {
            status.addWarning("No row delimiters are defined! The table will"
                    + " be read into one row (and supernumerous cells will"
                    + " be ignored).");
        }

        // check missing patterns
        if (tableSpec != null) {
            int numCols = tableSpec.getNumColumns();
            if (numCols > m_missingPatterns.size()) {
                status.addInfo("Not all columns have patterns for missing"
                        + "values assigned.");
            } else if (numCols < m_missingPatterns.size()) {
                status.addError("There are more patterns for missing values"
                        + " defined than columns in the table.");
            } else {
                for (Iterator<String> pIter = m_missingPatterns.iterator();
                        pIter.hasNext();) {
                    if (pIter.next() == null) {
                        status.addInfo("Not all columns have patterns for "
                                + "missing values assigned.");
                    }
                    // adding the message once is enough
                    break;
                }
            }
        } else {
            for (Iterator<String> pIter = m_missingPatterns.iterator(); pIter
                    .hasNext();) {
                if (pIter.next() == null) {
                    status.addInfo("Not all columns have patterns for missing"
                            + " values assigned.");
                }
                // adding the message once is enough
                break;
            }
        }

        // let the filetokenizer add its blurb
        super.addStatusOfSettings(status);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer res = new StringBuffer(super.toString());
        res.append("\nReading from:'");
        if (m_dataFileLocation == null) {
            res.append("<null>");
        } else {
            res.append(m_dataFileLocation.toString());
        }
        res.append("'\n");
        if (m_decimalSeparator != '.') {
            res.append("Decimal separator char for doubles: '");
            res.append(m_decimalSeparator);
            res.append("'\n");
        }
        if (m_thousandsSeparator != '\0') {
            res.append("Thousands separator char for doubles: '");
            res.append(m_thousandsSeparator);
            res.append("'\n");
        }
        res.append("Ignore empty tokens at the end of row: "
                + m_ignoreEmptyTokensAtEOR + "\n");
        res.append("Fill short lines with missVals: " + m_supportShortLines
                + "\n");
        res.append("RowPrefix:");
        res.append(m_rowHeaderPrefix + "\n");
        res.append("RowHeaders:" + m_fileHasRowHeaders);
        res.append(", ColHeaders:" + m_fileHasColumnHeaders);
        res.append(", Ignore empty lines:" + m_ignoreEmptyLines + "\n");
        res.append("Row delimiters: ");
        for (Iterator<String> r = m_rowDelimiters.iterator(); r.hasNext();) {
            res.append(printableStr(r.next()));
            if (r.hasNext()) {
                res.append(", ");
            }
        }
        res.append("\n");
        res.append("MissValue patterns: ");
        for (int p = 0; p < m_missingPatterns.size(); p++) {
            res.append(m_missingPatterns.get(p));
            if (p < m_missingPatterns.size() - 1) {
                res.append(", ");
            }
        }
        res.append("\n");
        res.append("Global missing value pattern (string cols): ");
        if (m_globalMissPatternStrCols == null) {
            res.append("<not defined>");
        } else {
            res.append(m_globalMissPatternStrCols);
        }
        res.append("\n");
        return res.toString();
    }
}
