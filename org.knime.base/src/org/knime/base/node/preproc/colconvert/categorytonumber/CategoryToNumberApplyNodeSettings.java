/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   25.08.2011 (hofer): created
 */
package org.knime.base.node.preproc.colconvert.categorytonumber;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * The settings object of the Category2Number node.
 *
 * @author Heiko Hofer
 */
public class CategoryToNumberApplyNodeSettings {
    private static final String APPEND_COLUMN = "append_columns";
    private static final String COLUMN_SUFFIX = "column_suffix";

    /** Define names of new columns. */
    private boolean m_appendColumns;
    private String m_columnSuffix;

    /**
     * Create an instance with default values.
     */
    public CategoryToNumberApplyNodeSettings() {
        m_appendColumns = true;
        m_columnSuffix = " (to number)";
    }

    /**
     * @return the appendColumns
     */
    boolean getAppendColumns() {
        return m_appendColumns;
    }

    /**
     * @param appendColumns the appendColumns to set
     */
    void setAppendColumns(final boolean appendColumns) {
        m_appendColumns = appendColumns;
    }

    /**
     * @return the columnSuffix
     */
    String getColumnSuffix() {
        return m_columnSuffix;
    }

    /**
     * @param columnSuffix the columnSuffix to set
     */
    void setColumnSuffix(final String columnSuffix) {
        m_columnSuffix = columnSuffix;
    }


    /** Called from dialog when settings are to be loaded.
     * @param settings To load from
     */
    void loadSettingsForDialog(final NodeSettingsRO settings) {
        /** Define names of new columns. */
        m_appendColumns = settings.getBoolean(APPEND_COLUMN, true);
        m_columnSuffix = settings.getString(COLUMN_SUFFIX, " (to number)");
    }

    /** Called from model when settings are to be loaded.
     * @param settings To load from
     * @throws InvalidSettingsException If settings are invalid.
     */
    void loadSettingsForModel(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        /** Define names of new columns. */
        m_appendColumns = settings.getBoolean(APPEND_COLUMN);
        m_columnSuffix = settings.getString(COLUMN_SUFFIX);
    }

    /** Called from model and dialog to save current settings.
     * @param settings To save to.
     */
    void saveSettings(final NodeSettingsWO settings) {
        /** Define names of new columns. */
        settings.addBoolean(APPEND_COLUMN, m_appendColumns);
        settings.addString(COLUMN_SUFFIX, m_columnSuffix);
    }

}
