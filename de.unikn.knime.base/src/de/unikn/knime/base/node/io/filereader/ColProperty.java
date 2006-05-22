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
 *   19.01.2005 (ohl): created
 */
package de.unikn.knime.base.node.io.filereader;

import java.util.HashSet;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.def.DefaultDataColumnDomain;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettings;

/**
 * 
 * @author ohl, University of Konstanz
 */

/**
 * Stores the properties for one column.
 */
class ColProperty {
    /* stores most of the parameters */
    private DataColumnSpec m_colSpec;

    /* the pattern causing a missing cell to be created */
    private String m_missValuePattern;

    /*
     * if true the object contains values set by the user - otherwise its all
     * default settings.
     */
    private boolean m_userSettings;

    /* indicates if we should read possible values from the file */
    private boolean m_readPossValsFromFile;

    /* indicates if we should read upper and lower bounds from the file */
    private boolean m_readBoundsFromFile;

    /* if more possible vals as this number show up we ignore all of them */
    private int m_maxPossVals;

    private static final String CFGKEY_USERSETTINGS = "UserSetValues";

    private static final String CFGKEY_MISSVALUE = "MissValuePattern";

    private static final String CFGKEY_COLNAME = "ColumnName";

    private static final String CFGKEY_COLTYPE = "ColumnClass";

    private static final String CFGKEY_POSVALUES = "PossValues";

    private static final String CFGKEY_POSSVAL = "PossValue";

    private static final String CFGKEY_READVALS = "ReadPossValsFromFile";

    private static final String CFGKEY_MAXPOSSVALS = "MaxNumOfPossVals";

    private static final String CFGKEY_READBOUNDS = "ReadBoundsFromFile";

    private static final String CFGKEY_UPPERBOUND = "UpperBound";

    private static final String CFGKEY_LOWERBOUND = "LowerBound";

    /**
     * creates an empty column properties object.
     */
    ColProperty() {
        m_colSpec = null;
        m_missValuePattern = null;
        m_userSettings = false;
        m_readPossValsFromFile = false;
        m_readBoundsFromFile = false;
        m_maxPossVals = -1;
    }

    /**
     * Creates a new column properties object initializing its settings from the
     * passed configuration object.
     * 
     * @param cfg a config object to read the internal settings from
     * @throws InvalidSettingsException if the config object did not contain the
     *             expected settings
     */
    ColProperty(final NodeSettings cfg) throws InvalidSettingsException {
        if (cfg == null) {
            throw new NullPointerException("Can't init column property from"
                    + " a null config.");
        }

        m_userSettings = cfg.getBoolean(CFGKEY_USERSETTINGS);
        m_missValuePattern = cfg.getString(CFGKEY_MISSVALUE, null);
        m_readPossValsFromFile = cfg.getBoolean(CFGKEY_READVALS);
        m_maxPossVals = cfg.getInt(CFGKEY_MAXPOSSVALS, -1);
        m_readBoundsFromFile = cfg.getBoolean(CFGKEY_READBOUNDS);

        // read the stuff for the ColumnSpec
        String colName = cfg.getString(CFGKEY_COLNAME);
        DataType colType = cfg.getDataType(CFGKEY_COLTYPE);
        // try reading the possible values - if there are any
        HashSet<DataCell> posValues = null;
        NodeSettings posVcfg = null;
        try {
            posVcfg = cfg.getConfig(CFGKEY_POSVALUES);
        } catch (InvalidSettingsException ice) {
            posVcfg = null;
        }
        if (posVcfg != null) {
            posValues = new HashSet<DataCell>();
            for (String key : posVcfg.keySet()) {
                DataCell pV = posVcfg.getDataCell(key);
                if (posValues.contains(key)) {
                    throw new InvalidSettingsException("Possible value '"
                            + pV.toString() + "' specified twice for column '"
                            + colName + "'.");
                }
                posValues.add(pV);
            }
        }
        DataCell upperBound = null;
        DataCell lowerBound = null;
        // if upper and lower bounds are set, read'em.
        if (cfg.containsKey(CFGKEY_UPPERBOUND)
                || cfg.containsKey(CFGKEY_LOWERBOUND)) {
            upperBound = cfg.getDataCell(CFGKEY_UPPERBOUND);
            lowerBound = cfg.getDataCell(CFGKEY_LOWERBOUND);
        }

        // this is just to make sure null arguments are okay with the
        // constructor. In case somebody changes it in the future.
        assert (new DefaultDataColumnDomain(null, null) != null);

        DataColumnSpecCreator dcsc = 
            new DataColumnSpecCreator(colName, colType);
        if ((posValues != null) && (posValues.size() > 0)) {
            dcsc.setDomain(new DefaultDataColumnDomain(posValues, lowerBound, 
                    upperBound));
        } else {
            dcsc.setDomain(new DefaultDataColumnDomain(lowerBound, upperBound));
        }
        m_colSpec = dcsc.createSpec();

    }

    /**
     * writes all settings from this object into the passed configuration
     * object.
     * 
     * @param cfg the configuration object to write the settings into.
     */
    void saveToConfiguration(final NodeSettings cfg) {

        if (cfg == null) {
            throw new NullPointerException("Can't save column property into"
                    + "null config.");
        }

        cfg.addBoolean(CFGKEY_USERSETTINGS, m_userSettings);
        cfg.addString(CFGKEY_MISSVALUE, m_missValuePattern);
        cfg.addBoolean(CFGKEY_READVALS, m_readPossValsFromFile);
        cfg.addInt(CFGKEY_MAXPOSSVALS, m_maxPossVals);
        cfg.addBoolean(CFGKEY_READBOUNDS, m_readBoundsFromFile);

        // add the stuff from the ColumnSpec
        cfg.addString(CFGKEY_COLNAME, m_colSpec.getName());
        cfg.addDataType(CFGKEY_COLTYPE, m_colSpec.getType());
        Set<DataCell> posValues = m_colSpec.getDomain().getValues();
        if ((posValues != null) && (posValues.size() > 0)) {
            NodeSettings pVCfg = cfg.addConfig(CFGKEY_POSVALUES);
            int count = 0;
            for (DataCell cell : posValues) {
                pVCfg.addDataCell(CFGKEY_POSSVAL + count, cell);
                count++;
            }
        }
        if ((m_colSpec.getDomain().getLowerBound() != null)
                || (m_colSpec.getDomain().getUpperBound() != null)) {
            cfg.addDataCell(CFGKEY_LOWERBOUND, m_colSpec.getDomain().
                    getLowerBound());
            cfg.addDataCell(CFGKEY_UPPERBOUND, m_colSpec.getDomain().
                    getUpperBound());
        }
    }

    /**
     * @param cSpec the column spec to store in this property object
     */
    void setColumnSpec(final DataColumnSpec cSpec) {
        m_colSpec = cSpec;
    }

    /**
     * @return the column spec containing most properties of this column
     */
    DataColumnSpec getColumnSpec() {
        return m_colSpec;
    }

    /**
     * @return the pattern that indicates missing data in this column
     */
    String getMissingValuePattern() {
        return m_missValuePattern;
    }

    /**
     * @param missValue the missing value pattern to store
     */
    void setMissingValuePattern(final String missValue) {
        m_missValuePattern = missValue;
    }

    /**
     * @return true if settings in this property object are set by the user,
     *         false if all settings are default/guessed values.
     */
    boolean getUserSettings() {
        return m_userSettings;
    }

    /**
     * @param setByUser flag indicating that the values in this object are
     *            settings specified by the user - as opposed to default/guessed
     *            settings.
     */
    void setUserSettings(final boolean setByUser) {
        m_userSettings = setByUser;
    }

    /**
     * @return a number greater than 0 if the possible values for this column
     *         should be read from file. The number returned is the maximum
     *         number of rows to extract the values from.
     */
    boolean getReadPossibleValuesFromFile() {
        return m_readPossValsFromFile;
    }

    /**
     * Determines if the possible values of this column will be read from file.
     * 
     * @param readThem the new value of the flag. True (the default) if possible
     *            values should be read. False otherwise.
     */
    void setReadPossibleValuesFromFile(final boolean readThem) {
        m_readPossValsFromFile = readThem;
    }

    /**
     * @return the maximum number of possible values for this column. If more
     *         values than this are read this column is not considered being
     *         nominal and all values are discarded. If -1 is returned then
     *         there is no maximum specified.
     */
    int getMaxNumberOfPossibleValues() {
        return m_maxPossVals;
    }

    /**
     * Sets the maximum number of possible values allowed for this column. If
     * more values than this are read this column is not considered being
     * nominal and all values are discarded. If set to -1 no maximum is
     * specified. The default value is -1.
     * 
     * @param maxPosVals the maximum number of values allowed.
     */
    void setMaxNumberOfPossibleValues(final int maxPosVals) {
        m_maxPossVals = maxPosVals;
    }

    /**
     * Determines if all values of this column should be examined and lower and
     * upper bounds should be stored.
     * 
     * @param readThem set true if the data should be read and lower and upper
     *            bounds should be stored for this column, otherwise false.
     */
    void setReadBoundsFromFile(final boolean readThem) {
        m_readBoundsFromFile = readThem;
    }

    /**
     * @return true if the data will be read and lower and upper bounds will be
     *         stored for this column, otherwise false.
     */
    boolean getReadBoundsFromFile() {
        return m_readBoundsFromFile;
    }

    /**
     * sets a new column name for this column.
     * 
     * @param colName the new name
     */
    void changeColumnName(final String colName) {
        // must replace the column spec
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setName(colName);
        m_colSpec = dcsc.createSpec();
    }

    /**
     * sets a new column type for this column.
     * 
     * @param newType the new type
     */
    void changeColumnType(final DataType newType) {
        // must replace the column spec
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setType(newType);
        m_colSpec = dcsc.createSpec();
    }

    /**
     * replaces the list of possible values for this columns.
     * 
     * @param newDomain the new domain to set in the column spec of this col
     *            property
     */
    void changeDomain(final DataColumnDomain newDomain) {
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setDomain(newDomain);
        m_colSpec = dcsc.createSpec();
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (m_colSpec != null) {
            sb.append(m_colSpec);
        } else {
            sb.append("<null ColSpec>");
        }
        sb.append(", MissVal: " + m_missValuePattern);
        sb.append(", userSetSettings: " + m_userSettings);
        sb.append(", readVals: " + m_readPossValsFromFile);
        if (m_readPossValsFromFile) {
            sb.append(" (max. " + m_maxPossVals + " Vals)");
        }
        sb.append(", readBounds: " + m_readBoundsFromFile);
        return sb.toString();
    }

    /**
     * returns a new ColProperty object containing a deep copy of this one.
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        
        ColProperty result = new ColProperty();
        // column specs are read only, we reuse it
        result.m_colSpec = m_colSpec;
        // Strings as well
        result.m_missValuePattern = m_missValuePattern;
        
        result.m_userSettings = m_userSettings;
        result.m_readPossValsFromFile = m_readPossValsFromFile;
        result.m_readBoundsFromFile = m_readBoundsFromFile;
        result.m_maxPossVals = m_maxPossVals;

        return result;
    }
    
} // class ColProperty

