/* Created on Dec 5, 2006 8:17:28 PM by thor
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.knn;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class stores the settings for the kNN node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class KnnSettings {
    private String m_classColumn;

    private int m_k = 3;

    private boolean m_weightByDistance;
    
    
    /**
     * Returns if the nearest neighbours should be weighted by their distance
     * to the query pattern.
     * 
     * @return <code>true</code> if the neighbours should be weighted by their
     * distance, <code>false</code> otherwise
     */
    public boolean weightByDistance() {
        return m_weightByDistance;
    }
    
    
    /**
     * Sets if the nearest neighbours should be weighted by their distance
     * to the query pattern.
     * 
     * @param b <code>true</code> if the neighbours should be weighted by their
     * distance, <code>false</code> otherwise
     */
    public void weightByDistance(final boolean b) {
        m_weightByDistance = b;
    }
    
    /**
     * Returns the number of neighbours to consider.
     * 
     * @return the number of neighbours
     */
    public int k() {
        return m_k;
    }

    /**
     * Sets the number of neighbours to consider.
     * 
     * @param k the number of neighbours
     */
    public void k(final int k) {
        this.m_k = k;
    }

    /**
     * Returns the name of the column with the class labels.
     * 
     * @return the class column's name
     */
    public String classColumn() {
        return m_classColumn;
    }

    /**
     * sets the name of the column with the class labels.
     * 
     * @param classColumn the class column's name
     */
    public void classColumn(final String classColumn) {
        m_classColumn = classColumn;
    }

    /**
     * Saves the settings into the given node settings object.
     * 
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("classColumn", m_classColumn);
        settings.addInt("k", m_k);
        settings.addBoolean("weightByDistance", m_weightByDistance);
    }

    /**
     * Loads the settings from the given node settings object.
     * 
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing or invalid
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_classColumn = settings.getString("classColumn");
        m_k = settings.getInt("k");
        m_weightByDistance = settings.getBoolean("weightByDistance");
    }
}
