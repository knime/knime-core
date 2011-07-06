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
 * History
 *   12.05.2011 (hofer): created
 */
package org.knime.base.node.viz.crosstable;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings object for the crosstab node.
 *
 * @author Heiko Hofer
 */
public class CrosstabNodeSettings {
    private static final String ROW_VAR_COLUMN = "rowVariableColumn";
    private static final String COL_VAR_COLUMN = "columnVariableColumn";
    private static final String WEIGHT_COLUMN = "weightColumn";
    private static final String NAMING_VERSION = "namingVersion";
    private static final String ENALE_HILITING = "enableHiliting";


    private String m_rowVarColumn = null;
    private String m_colVarColumn = null;
    private String m_weightColumn = null;
    private String m_namingVersion = "1.0";
    private boolean m_enableHiliting = false;

    /**
     * @return the rowVarColumn
     */
    String getRowVarColumn() {
        return m_rowVarColumn;
    }

    /**
     * @param rowVarColumn the rowVarColumn to set
     */
    void setRowVarColumn(final String rowVarColumn) {
        m_rowVarColumn = rowVarColumn;
    }

    /**
     * @return the colVarColumn
     */
    String getColVarColumn() {
        return m_colVarColumn;
    }

    /**
     * @param colVarColumn the colVarColumn to set
     */
    void setColVarColumn(final String colVarColumn) {
        m_colVarColumn = colVarColumn;
    }

    /**
     * @return the weightColumn
     */
    String getWeightColumn() {
        return m_weightColumn;
    }

    /**
     * @param weightColumn the weightColumn to set
     */
    void setWeightColumn(final String weightColumn) {
        m_weightColumn = weightColumn;
    }

    /**
     * @return the enableHiliting
     */
    boolean getEnableHiliting() {
        return m_enableHiliting;
    }

    /**
     * @param enableHiliting the enableHiliting to set
     */
    void setEnableHiliting(final boolean enableHiliting) {
        m_enableHiliting = enableHiliting;
    }

    /**
     * @return the namingVersion
     */
    String getNamingVersion() {
        return m_namingVersion;
    }

    /**
     * Get the columns that should be in the output beside the
     * values return by getRowVarColumn() and getColVarColumn() which will
     * always be in the output.
     * These are the properties displayed in the cross tabulation view.
     *
     * @return additional columns that should be in the output
     */
    List<String> getProperties() {
        CrosstabProperties naming = CrosstabProperties.create(
                getNamingVersion());
        return naming.getProperties();
    }

    /** Called from dialog when settings are to be loaded.
     * @param settings To load from
     * @param inSpec Input spec
     */
    void loadSettingsDialog(final NodeSettingsRO settings,
            final DataTableSpec inSpec) {
        m_rowVarColumn = settings.getString(ROW_VAR_COLUMN, null);
        m_colVarColumn = settings.getString(COL_VAR_COLUMN, null);
        m_weightColumn = settings.getString(WEIGHT_COLUMN, null);
        m_namingVersion = settings.getString(NAMING_VERSION, null);
        m_enableHiliting = settings.getBoolean(ENALE_HILITING, false);
    }

    /** Called from model when settings are to be loaded.
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        m_rowVarColumn = settings.getString(ROW_VAR_COLUMN);
        m_colVarColumn = settings.getString(COL_VAR_COLUMN);
        m_weightColumn = settings.getString(WEIGHT_COLUMN);
        m_namingVersion = settings.getString(NAMING_VERSION);
        m_enableHiliting = settings.getBoolean(ENALE_HILITING);
    }

    /** Called from model and dialog to save current settings.
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(ROW_VAR_COLUMN, m_rowVarColumn);
        settings.addString(COL_VAR_COLUMN, m_colVarColumn);
        settings.addString(WEIGHT_COLUMN, m_weightColumn);
        settings.addString(NAMING_VERSION, m_namingVersion);
        settings.addBoolean(ENALE_HILITING, m_enableHiliting);
    }

}
