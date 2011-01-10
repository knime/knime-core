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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Aug 11, 2008 (wiswedel): created
 */
package org.knime.base.collection.list.split;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Settings object to keep paramaters for split operation.
 * @author Bernd Wiswedel, University of Konstanz
 */
final class CollectionSplitSettings {
    
    /** Different ways of identifying how many elements there are in a column.*/
    public enum CountElementsPolicy {
        /** Do one scan on the input table and count. */
        Count,
        /** Use the count (and names) from the column's element names field. */
        UseElementNamesOrFail,
        /** Try UseElementNamesOrFail, if that fails use Count. */
        BestEffort
    };
    
    private boolean m_determineMostSpecificDataType;
    private String m_collectionColName;
    private CountElementsPolicy m_countElementsPolicy;
    private boolean m_replaceInputColumn;
    
    /**
     * @return the determineMostSpecificDataType
     */
    public final boolean isDetermineMostSpecificDataType() {
        return m_determineMostSpecificDataType;
    }
    
    /** Save current settings.
     * @param settings To save to.
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_collectionColName == null) {
            return;
        }
        settings.addBoolean("determineMostSpecificDataType", 
                m_determineMostSpecificDataType);
        settings.addString("collectionColName", m_collectionColName);
        settings.addString("countElementsPolicy", m_countElementsPolicy.name());
        settings.addBoolean("replaceInputColumn", m_replaceInputColumn);
    }
    
    /** Load settings, called in NodeModel.
     * @param settings To load from
     * @throws InvalidSettingsException If any setting is invalid.
     */
    public void loadSettingsInModel(final NodeSettingsRO settings) 
        throws InvalidSettingsException {
        m_determineMostSpecificDataType = 
            settings.getBoolean("determineMostSpecificDataType");
        m_collectionColName = settings.getString("collectionColName");
        String policy = settings.getString("countElementsPolicy");
        if (policy == null) {
            throw new InvalidSettingsException(
                    "No count elements policy defined");
        }
        try {
            m_countElementsPolicy = CountElementsPolicy.valueOf(policy);
        } catch (IllegalArgumentException iae) {
            throw new InvalidSettingsException("Invalid policy " + policy);
        }
        m_replaceInputColumn = settings.getBoolean("replaceInputColumn");
    }
    
    /** Load settings, used in dialog.
     * @param settings To load from.
     * @param spec To guess default settings from.
     */
    public void loadSettingsInDialog(final NodeSettingsRO settings, 
            final DataTableSpec spec) {
        m_determineMostSpecificDataType = 
            settings.getBoolean("determineMostSpecificDataType", true);
        m_collectionColName = settings.getString("collectionColName", null);
        String policy = settings.getString("countElementsPolicy", null);
        if (policy == null) {
            m_countElementsPolicy = CountElementsPolicy.BestEffort;
        } else {
            try {
                m_countElementsPolicy = CountElementsPolicy.valueOf(policy);
            } catch (IllegalArgumentException iae) {
                m_countElementsPolicy = CountElementsPolicy.BestEffort;
            }
        }
        m_replaceInputColumn = settings.getBoolean("replaceInputColumn", false);
    }
    
    /** Do auto-configuration.
     * @param spec To guess defaults from.
     */
    public void initDefaults(final DataTableSpec spec) {
        m_determineMostSpecificDataType = false;
        for (DataColumnSpec s : spec) {
            if (s.getType().isCompatible(CollectionDataValue.class)) {
                m_collectionColName = s.getName();
            }
        }
        m_countElementsPolicy = CountElementsPolicy.BestEffort;
        m_replaceInputColumn = false;
    }
    
    /**
     * @param determineMostSpecificDataType 
     * the determineMostSpecificDataType to set
     */
    public final void setDetermineMostSpecificDataType(
            final boolean determineMostSpecificDataType) {
        m_determineMostSpecificDataType = determineMostSpecificDataType;
    }
    
    /**
     * @return the collectionColName
     */
    public final String getCollectionColName() {
        return m_collectionColName;
    }
    
    /**
     * @param collectionColName the collectionColName to set
     */
    public final void setCollectionColName(final String collectionColName) {
        m_collectionColName = collectionColName;
    }

    /**
     * @return the countElementsPolicy
     */
    public final CountElementsPolicy getCountElementsPolicy() {
        return m_countElementsPolicy;
    }

    /**
     * @param countElementsPolicy the countElementsPolicy to set
     */
    public final void setCountElementsPolicy(
            final CountElementsPolicy countElementsPolicy) {
        m_countElementsPolicy = countElementsPolicy;
    }

    /**
     * @return the replaceInputColumn
     */
    public final boolean isReplaceInputColumn() {
        return m_replaceInputColumn;
    }

    /**
     * @param replaceInputColumn the replaceInputColumn to set
     */
    public final void setReplaceInputColumn(final boolean replaceInputColumn) {
        m_replaceInputColumn = replaceInputColumn;
    }
}
