/* ------------------------------------------------------------------
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
