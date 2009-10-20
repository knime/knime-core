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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class holds the settings for the polynomial regression learner node.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLearnerSettings {
    private int m_degree = 2;

    private int m_maxRowsForView = 10000;

    private String m_targetColumn;

    private final Set<String> m_selectedColumnNames = 
        new LinkedHashSet<String>();
    
    private boolean m_includeAll = false;

    private final Set<String> m_unmodSelectedColumnNames = Collections
            .unmodifiableSet(m_selectedColumnNames);

    /**
     * Returns the maximum degree that polynomial used for regression should
     * have.
     * 
     * @return the maximum degree
     */
    public int getDegree() {
        return m_degree;
    }

    /**
     * Returns the name of the target column that holds the dependent variable.
     * 
     * @return the target column's name
     */
    public String getTargetColumn() {
        return m_targetColumn;
    }

    /**
     * Loads the settings from the node settings object.
     * 
     * @param settings the node settings
     * 
     * @throws InvalidSettingsException if one of the settings is missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_degree = settings.getInt("degree");
        m_targetColumn = settings.getString("targetColumn");
        m_maxRowsForView = settings.getInt("maxViewRows");
        m_includeAll = settings.getBoolean("includeAll", false); // added v2.1
        m_selectedColumnNames.clear();
        if (!m_includeAll) {
            for (String s : settings.getStringArray("selectedColumns")) {
                m_selectedColumnNames.add(s);
            }
        }
    }

    /**
     * Saves the settings to the node settings object.
     * 
     * @param settings the node settings
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_targetColumn != null) {
            settings.addInt("degree", m_degree);
            settings.addString("targetColumn", m_targetColumn);
            settings.addInt("maxViewRows", m_maxRowsForView);
            settings.addBoolean("includeAll", m_includeAll);
            if (!m_includeAll) {
                settings.addStringArray("selectedColumns", m_selectedColumnNames
                        .toArray(new String[m_selectedColumnNames.size()]));
            }
        }
    }

    /**
     * Sets the maximum degree that polynomial used for regression should have.
     * 
     * @param degree the maximum degree
     */
    public void setDegree(final int degree) {
        m_degree = degree;
    }

    /**
     * Sets the name of the target column that holds the dependent variable.
     * 
     * @param targetColumn the target column's name
     */
    public void setTargetColumn(final String targetColumn) {
        m_targetColumn = targetColumn;
    }

    /**
     * Returns the maximum number of rows that are shown in the curve view.
     * 
     * @return the maximum number of rows
     */
    public int getMaxRowsForView() {
        return m_maxRowsForView;
    }

    /**
     * Sets the maximum number of rows that are shown in the curve view.
     * 
     * @param maxRowsForView the maximum number of rows
     */
    public void setMaxRowsForView(final int maxRowsForView) {
        m_maxRowsForView = maxRowsForView;
    }

    /**
     * Sets the names of the columns that should be used for the regression. The
     * target column name must not be among these columns!
     * 
     * @param columnNames a set with the selected column names
     */
    public void setSelectedColumns(final Set<String> columnNames) {
        m_selectedColumnNames.clear();
        for (String s : columnNames) {
            m_selectedColumnNames.add(s);
        }
    }

    /**
     * Returns an (unmodifieable) set of the select column names.
     * 
     * @return a set with the selectec column names
     */
    public Set<String> getSelectedColumns() {
        return m_unmodSelectedColumnNames;
    }
    
    /**
     * @return the includeAll
     */
    public boolean isIncludeAll() {
        return m_includeAll;
    }
    
    /**
     * @param includeAll the includeAll to set
     */
    public void setIncludeAll(final boolean includeAll) {
        m_includeAll = includeAll;
    }
}
