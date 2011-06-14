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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnheaderinsert;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class ColumnHeaderInsertConfig {

    private String m_lookupColumn;
    private String m_valueColumn;
    private boolean m_failIfNoMatch;


    /** @return the lookupColumn */
    String getLookupColumn() {
        return m_lookupColumn;
    }

    /** @param lookupColumn the lookupColumn to set */
    void setLookupColumn(final String lookupColumn) {
        m_lookupColumn = lookupColumn;
    }

    /** @return the valueColumn */
    String getValueColumn() {
        return m_valueColumn;
    }

    /** @param valueColumn the valueColumn to set */
    void setValueColumn(final String valueColumn) {
        m_valueColumn = valueColumn;
    }

    /** @return the failIfNoMatch */
    boolean isFailIfNoMatch() {
        return m_failIfNoMatch;
    }

    /** @param failIfNoMatch the failIfNoMatch to set */
    void setFailIfNoMatch(final boolean failIfNoMatch) {
        m_failIfNoMatch = failIfNoMatch;
    }

    void saveConfiguration(final NodeSettingsWO settings) {
        settings.addString("lookupColumn", m_lookupColumn);
        settings.addString("valueColumn", m_valueColumn);
        settings.addBoolean("failIfNoMatch", m_failIfNoMatch);
    }

    void loadConfigurationInModel(
            final NodeSettingsRO settings) throws InvalidSettingsException {
        m_lookupColumn = settings.getString("lookupColumn");
        // allow null for row key column
        if (m_lookupColumn != null && m_lookupColumn.length() == 0) {
            throw new InvalidSettingsException(
                    "Invalid (empty) key for lookup column");
        }
        m_valueColumn = settings.getString("valueColumn");
        if (m_valueColumn == null || m_valueColumn.length() == 0) {
            throw new InvalidSettingsException(
                    "Invalid (empty) value for value column name");
        }
        m_failIfNoMatch = settings.getBoolean("failIfNoMatch");
    }

    void loadConfigurationInDialog(final NodeSettingsRO settings,
            final DataTableSpec dictionaryTable)
    throws NotConfigurableException {
        m_lookupColumn = settings.getString("lookupColumn", null);
        String firstStringCol = null;
        String fallbackCol = null;
        for (DataColumnSpec col : dictionaryTable) {
            if (col.getType().isCompatible(StringValue.class)) {
                fallbackCol = col.getName();
            }
            if (col.getType().equals(StringCell.TYPE)) { // exclude sdf etc
                firstStringCol = col.getName();
                break;
            }
        }
        if (fallbackCol == null) {
            throw new NotConfigurableException("Not configuration possible: "
                    + "No string compatible column in dictionary table");
        }
        String defCol = firstStringCol != null ? firstStringCol : fallbackCol;
        m_valueColumn = settings.getString("valueColumn", defCol);
        m_failIfNoMatch = settings.getBoolean("failIfNoMatch", true);
    }

}
