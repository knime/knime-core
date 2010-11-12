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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.addemptyrows;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration object for "add empty rows" node.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class AddEmptyRowsConfig {

    private boolean m_atLeastMode = true;

    private int m_rowCount = 15;

    private double m_fillValueDouble;

    private boolean m_useMissingDouble = true;

    private int m_fillValueInt;

    private boolean m_useMissingInt = true;

    private String m_fillValueString;

    private boolean m_useMissingString = true;

    private String m_newRowKeyPrefix = "Empty ";

    /**
     * @return the atLeastMode
     */
    boolean isAtLeastMode() {
        return m_atLeastMode;
    }

    /**
     * @param atLeastMode true to have number of "at least" mode, otherwise
     *            "additional" mode.
     */
    void setAtLeastMode(final boolean atLeastMode) {
        m_atLeastMode = atLeastMode;
    }

    /**
     * @return the rowCount
     */
    int getRowCount() {
        return m_rowCount;
    }

    /**
     * @param rowCount the rowCount to set
     */
    void setRowCount(final int rowCount) {
        if (rowCount < 0) {
            throw new IndexOutOfBoundsException("Invalid row count: "
                    + rowCount);
        }
        m_rowCount = rowCount;
    }

    /**
     * @return the fillValueDouble
     */
    double getFillValueDouble() {
        return m_fillValueDouble;
    }

    /**
     * @param isUseMissing Whether to use missing value
     * @param fillValueDouble the fillValueDouble to set
     */
    void setFillValueDouble(final boolean isUseMissing,
            final double fillValueDouble) {
        m_useMissingDouble = isUseMissing;
        m_fillValueDouble = fillValueDouble;
    }

    /**
     * @return the useMissingDouble
     */
    boolean isUseMissingDouble() {
        return m_useMissingDouble;
    }

    /**
     * @return the fillValueInt
     */
    int getFillValueInt() {
        return m_fillValueInt;
    }

    /**
     * @param isUseMissing Whether to use missing value
     * @param fillValueInt the fillValueInt to set
     */
    void setFillValueInt(final boolean isUseMissing, final int fillValueInt) {
        m_useMissingInt = isUseMissing;
        m_fillValueInt = fillValueInt;
    }

    /**
     * @return the useMissingInt
     */
    boolean isUseMissingInt() {
        return m_useMissingInt;
    }

    /**
     * @return the fillValueString
     */
    String getFillValueString() {
        return m_fillValueString;
    }

    /**
     * @param isUseMissing Whether to use missing value
     * @param fillValueString the fillValueString to set
     */
    void setFillValueString(final boolean isUseMissing,
            final String fillValueString) {
        m_useMissingString = isUseMissing;
        m_fillValueString = fillValueString;
    }

    /**
     * @return the useMissingString
     */
    boolean isUseMissingString() {
        return m_useMissingString;
    }

    /**
     * @param newRowKeyPrefix prefix of newly generated keys
     */
    void setNewRowKeyPrefix(final String newRowKeyPrefix) {
        if (newRowKeyPrefix == null) {
            m_newRowKeyPrefix = "";
        } else {
            m_newRowKeyPrefix = newRowKeyPrefix;
        }
    }

    /**
     * @return prefix of newly generated keys
     */
    public String getNewRowKeyPrefix() {
        return m_newRowKeyPrefix;
    }

    /**
     * Saves current configuration.
     *
     * @param settings To save to.
     */
    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean("atLeastMode", m_atLeastMode);
        settings.addInt("rowCount", m_rowCount);
        settings.addString("newRowKeyPrefix", m_newRowKeyPrefix);

        NodeSettingsWO doubleSet = settings.addNodeSettings("double");
        doubleSet.addBoolean("useMissing", m_useMissingDouble);
        doubleSet.addDouble("fillValue", m_fillValueDouble);

        NodeSettingsWO intSet = settings.addNodeSettings("int");
        intSet.addBoolean("useMissing", m_useMissingInt);
        intSet.addInt("fillValue", m_fillValueInt);

        NodeSettingsWO stringSet = settings.addNodeSettings("String");
        stringSet.addBoolean("useMissing", m_useMissingString);
        stringSet.addString("fillValue", m_fillValueString);
    }

    /**
     * Loads configuration.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If that fails for any reason.
     */
    void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_atLeastMode = settings.getBoolean("atLeastMode");
        m_rowCount = settings.getInt("rowCount");
        m_newRowKeyPrefix = settings.getString("newRowKeyPrefix");

        NodeSettingsRO doubleSet = settings.getNodeSettings("double");
        m_useMissingDouble = doubleSet.getBoolean("useMissing");
        m_fillValueDouble = doubleSet.getDouble("fillValue");

        NodeSettingsRO intSet = settings.getNodeSettings("int");
        m_useMissingInt = intSet.getBoolean("useMissing");
        m_fillValueInt = intSet.getInt("fillValue");

        NodeSettingsRO stringSet = settings.getNodeSettings("String");
        m_useMissingString = stringSet.getBoolean("useMissing");
        m_fillValueString = stringSet.getString("fillValue");
    }
}
