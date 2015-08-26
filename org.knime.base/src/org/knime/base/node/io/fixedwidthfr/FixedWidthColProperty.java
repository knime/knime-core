/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.10.2014 (tibuch): created
 */
package org.knime.base.node.io.fixedwidthfr;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author Tim-Oliver Buchholz
 */
public class FixedWidthColProperty {

    // keys for settings object
    private static final String CFGKEY_COLWIDTH = "ColumnWidth";

    private static final String CFGKEY_INCLUDE = "ColumnInclude";

    private static final String CFGKEY_COLNAME = "ColumnName";

    private static final String CFGKEY_COLTYPE = "ColumnType";

    private static final String CFGKEY_POSVALUES = "PossValues";

    private static final String CFGKEY_POSSVAL = "PossValue";

    private static final String CFGKEY_LOWERBOUND = "LowerBound";

    private static final String CFGKEY_UPPERBOUND = "UpperBound";

    private static final String CFGKEY_MISSVALPAT = "MissingValuePattern";

    private static final String CFGKEY_FORMAT = "FormatParameter";

    private DataColumnSpec m_colSpec;

    private int m_width;

    private boolean m_include;

    private String m_missingValuePattern;

    private String m_formatParameter;

    /**
     *
     * @param colIndex the index of the column
     */
    FixedWidthColProperty(final int colIndex) {

        // create a new name based on the index
        DataColumnSpecCreator c = new DataColumnSpecCreator("column" + colIndex, StringCell.TYPE);

        m_colSpec = c.createSpec();

        m_width = Integer.MAX_VALUE;

        m_include = true;
    }

    /**
     *
     * @param name the name of the new column
     * @param type the type of the new column
     * @param width the width of the new column
     * @param include if the column should be included
     * @param missingValuePattern the pattern for missing values
     * @param formatParameter the optional format parameter
     */
    FixedWidthColProperty(final String name, final DataType type, final int width, final boolean include,
        final String missingValuePattern, final String formatParameter) {
        DataColumnSpecCreator c = new DataColumnSpecCreator(name, type);

        m_colSpec = c.createSpec();

        m_width = width;

        m_include = include;

        m_missingValuePattern = missingValuePattern;

        m_formatParameter = formatParameter;
    }

    /**
     *
     * @param cfg the settings object
     * @throws InvalidSettingsException thrown if settings are not valid
     */
    FixedWidthColProperty(final NodeSettingsRO cfg) throws InvalidSettingsException {

        if (cfg == null) {
            throw new NullPointerException("Can't init column property from a null config.");
        }

        m_width = cfg.getInt(CFGKEY_COLWIDTH);
        m_include = cfg.getBoolean(CFGKEY_INCLUDE);
        m_missingValuePattern = cfg.getString(CFGKEY_MISSVALPAT);
        m_formatParameter = cfg.getString(CFGKEY_FORMAT, "");

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
                if (posValues.contains(new StringCell(key))) {
                    throw new InvalidSettingsException("Possible value '" + pV.toString()
                        + "' specified twice for column '" + colName + "'.");
                }
                posValues.add(pV);
            }
        }
        DataCell upperBound = null;
        DataCell lowerBound = null;
        // if upper and lower bounds are set, read'em.
        if (cfg.containsKey(CFGKEY_UPPERBOUND) || cfg.containsKey(CFGKEY_LOWERBOUND)) {
            upperBound = cfg.getDataCell(CFGKEY_UPPERBOUND);
            lowerBound = cfg.getDataCell(CFGKEY_LOWERBOUND);
        }

        // this is just to make sure null arguments are okay with the
        // constructor. In case somebody changes it in the future.
        assert (new DataColumnDomainCreator(null, null) != null);

        DataColumnSpecCreator dcsc = new DataColumnSpecCreator(colName, colType);
        if ((posValues != null) && (posValues.size() > 0)) {
            dcsc.setDomain(new DataColumnDomainCreator(posValues, lowerBound, upperBound).createDomain());
        } else {
            dcsc.setDomain(new DataColumnDomainCreator(lowerBound, upperBound).createDomain());
        }
        m_colSpec = dcsc.createSpec();
    }

    /**
     * @param fixedWidthColProperty the ColProperty to clone
     */
    public FixedWidthColProperty(final FixedWidthColProperty fixedWidthColProperty) {

        m_colSpec = fixedWidthColProperty.m_colSpec;

        m_width = fixedWidthColProperty.m_width;

        m_include = fixedWidthColProperty.m_include;

        m_missingValuePattern = fixedWidthColProperty.m_missingValuePattern;

        m_formatParameter = fixedWidthColProperty.m_formatParameter;
    }

    /**
     * @param cfg the settings object where we save our properties
     */
    public void saveToConfiguration(final NodeSettingsWO cfg) {
        if (cfg == null) {
            throw new NullPointerException("Can't save column property into null config.");
        }

        cfg.addInt(CFGKEY_COLWIDTH, m_width);
        cfg.addBoolean(CFGKEY_INCLUDE, m_include);
        cfg.addString(CFGKEY_MISSVALPAT, m_missingValuePattern);
        cfg.addString(CFGKEY_FORMAT, m_formatParameter);

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
        if ((m_colSpec.getDomain().getLowerBound() != null) || (m_colSpec.getDomain().getUpperBound() != null)) {
            cfg.addDataCell(CFGKEY_LOWERBOUND, m_colSpec.getDomain().getLowerBound());
            cfg.addDataCell(CFGKEY_UPPERBOUND, m_colSpec.getDomain().getUpperBound());
        }
    }

    /**
     * @return column width
     */
    public int getColWidth() {
        return m_width;
    }

    /**
     * @return column spec
     */
    public DataColumnSpec getColSpec() {
        return m_colSpec;
    }

    /**
     * @param colSpec the colSpec to set
     */
    public void setColSpec(final DataColumnSpec colSpec) {
        this.m_colSpec = colSpec;
    }

    /**
     * @return if this column should be included
     */
    public boolean getInclude() {
        return m_include;
    }

    /**
     * @param b set if this column should be included
     */
    public void setInclude(final boolean b) {
        m_include = b;
    }

    /**
     * @return the missing-value pattern
     */
    public String getMissingValuePattern() {
        return m_missingValuePattern;
    }

    /**
     * @param str the missing value pattern
     */
    public void setMissingValuePattern(final String str) {
        m_missingValuePattern = str;
    }

    /**
     * @return the formatParameter
     */
    public Optional<String> getFormatParameter() {
        return Optional.ofNullable(m_formatParameter);
    }

    /**
     * @param formatParameter the formatParameter to set
     */
    public void setFormatParameter(final String formatParameter) {
        m_formatParameter = formatParameter;
    }
}