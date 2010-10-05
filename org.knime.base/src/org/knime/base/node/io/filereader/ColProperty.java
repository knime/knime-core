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
 * -------------------------------------------------------------------
 *
 * History
 *   19.01.2005 (ohl): created
 */
package org.knime.base.node.io.filereader;

import java.util.HashSet;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Stores the properties for one column.
 *
 * @author Peter Ohl, University of Konstanz
 */
public class ColProperty {
    /* stores most of the parameters */
    private DataColumnSpec m_colSpec;

    /* the pattern causing a missing cell to be created */
    private String m_missValuePattern;

    /*
     * if true the object contains values set by the user - otherwise its all
     * default settings.
     */
    private boolean m_userSettings;

    /*
     * indicates if we should read possible values from the file (used for
     * integer columns only)
     */
    private boolean m_readPossValsFromFile;

    /*
     * if true, the column will not be added to the output table.
     */
    private boolean m_skipColumn;

    private static final String CFGKEY_USERSETTINGS = "UserSetValues";

    private static final String CFGKEY_MISSVALUE = "MissValuePattern";

    private static final String CFGKEY_COLNAME = "ColumnName";

    private static final String CFGKEY_COLTYPE = "ColumnClass";

    private static final String CFGKEY_POSVALUES = "PossValues";

    private static final String CFGKEY_POSSVAL = "PossValue";

    private static final String CFGKEY_READVALS = "ReadPossValsFromFile";

    private static final String CFGKEY_UPPERBOUND = "UpperBound";

    private static final String CFGKEY_LOWERBOUND = "LowerBound";

    private static final String CFGKEY_SKIP = "SkipThisColumn";

    /**
     * Creates an empty column properties object.
     */
    public ColProperty() {
        m_colSpec = null;
        m_missValuePattern = null;
        m_userSettings = false;
        m_readPossValsFromFile = false;
        m_skipColumn = false;
    }

    /**
     * Creates a new column properties object initializing its settings from the
     * passed configuration object.
     *
     * @param cfg a config object to read the internal settings from
     * @throws InvalidSettingsException if the config object did not contain the
     *             expected settings
     */
    public ColProperty(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        if (cfg == null) {
            throw new NullPointerException("Can't init column property from"
                    + " a null config.");
        }

        m_userSettings = cfg.getBoolean(CFGKEY_USERSETTINGS);
        m_missValuePattern = cfg.getString(CFGKEY_MISSVALUE, null);
        m_readPossValsFromFile = cfg.getBoolean(CFGKEY_READVALS);
        // default to false for backward compatibility
        m_skipColumn = cfg.getBoolean(CFGKEY_SKIP, false);

        // read the stuff for the ColumnSpec
        String colName = cfg.getString(CFGKEY_COLNAME);
        DataType colType = cfg.getDataType(CFGKEY_COLTYPE);
        // try reading the possible values - if there are any
        HashSet<DataCell> posValues = null;
        NodeSettingsRO posVcfg = null;
        try {
            posVcfg = cfg.getNodeSettings(CFGKEY_POSVALUES);
        } catch (InvalidSettingsException ice) {
            // do nothing
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
        assert (new DataColumnDomainCreator(null, null) != null);

        DataColumnSpecCreator dcsc =
            new DataColumnSpecCreator(colName, colType);
        if ((posValues != null) && (posValues.size() > 0)) {
            dcsc.setDomain(new DataColumnDomainCreator(posValues, lowerBound,
                    upperBound).createDomain());
        } else {
            dcsc.setDomain(new DataColumnDomainCreator(lowerBound, upperBound)
                    .createDomain());
        }
        m_colSpec = dcsc.createSpec();

    }

    /**
     * Writes all settings from this object into the passed configuration
     * object.
     *
     * @param cfg the configuration object to write the settings into
     */
    public void saveToConfiguration(final NodeSettingsWO cfg) {

        if (cfg == null) {
            throw new NullPointerException("Can't save column property into"
                    + "null config.");
        }

        cfg.addBoolean(CFGKEY_USERSETTINGS, m_userSettings);
        cfg.addString(CFGKEY_MISSVALUE, m_missValuePattern);
        cfg.addBoolean(CFGKEY_READVALS, m_readPossValsFromFile);
        cfg.addBoolean(CFGKEY_SKIP, m_skipColumn);

        // add the stuff from the ColumnSpec
        cfg.addString(CFGKEY_COLNAME, m_colSpec.getName());
        cfg.addDataType(CFGKEY_COLTYPE, m_colSpec.getType());
        Set<DataCell> posValues = m_colSpec.getDomain().getValues();
        if ((posValues != null) && (posValues.size() > 0)) {
            NodeSettingsWO pVCfg = cfg.addNodeSettings(CFGKEY_POSVALUES);
            int count = 0;
            for (DataCell cell : posValues) {
                pVCfg.addDataCell(CFGKEY_POSSVAL + count, cell);
                count++;
            }
        }
        if ((m_colSpec.getDomain().getLowerBound() != null)
                || (m_colSpec.getDomain().getUpperBound() != null)) {
            cfg.addDataCell(CFGKEY_LOWERBOUND, m_colSpec.getDomain()
                    .getLowerBound());
            cfg.addDataCell(CFGKEY_UPPERBOUND, m_colSpec.getDomain()
                    .getUpperBound());
        }
    }

    /**
     * @param cSpec the column spec to store in this property object
     */
    public void setColumnSpec(final DataColumnSpec cSpec) {
        m_colSpec = cSpec;
    }

    /**
     * @return the column spec containing most properties of this column
     */
    public DataColumnSpec getColumnSpec() {
        return m_colSpec;
    }

    /**
     * @return the pattern that indicates missing data in this column
     */
    public String getMissingValuePattern() {
        return m_missValuePattern;
    }

    /**
     * @param missValue the missing value pattern to store
     */
    public void setMissingValuePattern(final String missValue) {
        m_missValuePattern = missValue;
    }

    /**
     * @return <code>true</code> if settings in this property object are set
     *         by the user, <code>false</code> if all settings are
     *         default/guessed values.
     */
    public boolean getUserSettings() {
        return m_userSettings;
    }

    /**
     * @param setByUser flag indicating that the values in this object are
     *            settings specified by the user - as opposed to default/guessed
     *            settings.
     */
    public void setUserSettings(final boolean setByUser) {
        m_userSettings = setByUser;
    }

    /**
     * @return <code>true</code> if the possible values should be read from
     *         file. This is set only for integer columns.
     */
    public boolean getReadPossibleValuesFromFile() {
        return m_readPossValsFromFile;
    }

    /**
     * Determines if the possible values of this column will be read from file
     * (used only with integer columns).
     *
     * @param readThem the new value of the flag. <code>true</code> if
     *            possible values should be read, <code>false</code>
     *            otherwise.
     */
    public void setReadPossibleValuesFromFile(final boolean readThem) {
        m_readPossValsFromFile = readThem;
    }

    /**
     * @return true if this column is not included in the reader's table.
     */
    public boolean getSkipThisColumn() {
        return m_skipColumn;
    }

    /**
     * @param skipIt specify true, if this column should not be included in
     * the reader's file table. Set to false (the default), if the column should
     * appear in the file reader's output table.
     */
    public void setSkipThisColumn(final boolean skipIt) {
        m_skipColumn = skipIt;
    }

    /**
     * Sets a new column name for this column.
     *
     * @param colName the new name
     */
    public void changeColumnName(final String colName) {
        // must replace the column spec
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setName(colName);
        m_colSpec = dcsc.createSpec();
    }

    /**
     * Sets a new column type for this column.
     *
     * @param newType the new type
     */
    public void changeColumnType(final DataType newType) {
        // must replace the column spec
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setType(newType);
        m_colSpec = dcsc.createSpec();
    }

    /**
     * Replaces the list of possible values for this columns.
     *
     * @param newDomain the new domain to set in the column spec of this col
     *            property
     */
    public void changeDomain(final DataColumnDomain newDomain) {
        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(m_colSpec);
        dcsc.setDomain(newDomain);
        m_colSpec = dcsc.createSpec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
        return sb.toString();
    }

    /**
     * Returns a new ColProperty object containing a deep copy of this one.
     *
     * {@inheritDoc}
     */
    @Override
    public Object clone() {

        ColProperty result = new ColProperty();
        // column specs are read only, we reuse it
        result.m_colSpec = m_colSpec;
        // Strings as well
        result.m_missValuePattern = m_missValuePattern;
        result.m_skipColumn = m_skipColumn;
        result.m_userSettings = m_userSettings;
        result.m_readPossValsFromFile = m_readPossValsFromFile;

        return result;
    }
}
