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
 * ---------------------------------------------------------------------
 *
 * History
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class hold the settings for the joiner node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class NewJoinerSettings {
    /**
     * This enum holds all ways of handling duplicate column names in the two
     * input tables.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public enum DuplicateHandling {
        /** Filter out duplicate columns from the second table. */
        Filter,
        /** Append a suffix to the columns from the second table. */
        AppendSuffix,
        /** Don't execute the node. */
        DontExecute;
    }

    /**
     * This enum holds all ways of joining the two tables.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    public enum JoinMode {
        /** Make an INNER JOIN. */
        InnerJoin("Inner Join"),
        /** Make a LEFT OUTER JOIN. */
        LeftOuterJoin("Left Outer Join"),
        /** Make a RIGHT OUTER JOIN. */
        RightOuterJoin("Right Outer Join"),
        /** Make a FULL OUTER JOIN. */
        FullOuterJoin("Full Outer Join");

        private final String m_text;

        private JoinMode(final String text) {
            m_text = text;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_text;
        }
    }

    /** Name of the row key column in the dialog. */
    static final String ROW_KEY_COL_NAME = "Row ID";
    /** Internally used row key identifier. */
    static final String ROW_KEY_IDENTIFIER = "$RowID$";

    private String m_secondTableColumn;

    private DuplicateHandling m_duplicateHandling =
            DuplicateHandling.DontExecute;

    private String m_suffix = "";

    private String m_keySuffix = "_";

    private JoinMode m_joinMode = JoinMode.InnerJoin;

    /**
     * Returns the join column's name from the second table.
     *
     * @return the column's name
     */
    public String secondTableColumn() {
        return m_secondTableColumn;
    }

    /**
     * Sets the join column's name from the second table.
     *
     * @param secondTableColumn the column's name
     */
    public void secondTableColumn(final String secondTableColumn) {
        m_secondTableColumn = secondTableColumn;
    }

    /**
     * Returns how duplicate column names should be handled.
     *
     * @return the duplicate handling method
     */
    public DuplicateHandling duplicateHandling() {
        return m_duplicateHandling;
    }

    /**
     * Sets how duplicate column names should be handled.
     *
     * @param duplicateHandling the duplicate handling method
     */
    public void duplicateHandling(final DuplicateHandling duplicateHandling) {
        m_duplicateHandling = duplicateHandling;
    }

    /**
     * Returns the mode how the two tables should be joined.
     *
     * @return the join mode
     */
    public JoinMode joinMode() {
        return m_joinMode;
    }

    /**
     * Sets the mode how the two tables should be joined.
     *
     * @param joinMode the join mode
     */
    public void joinMode(final JoinMode joinMode) {
        m_joinMode = joinMode;
    }

    /**
     * Returns the suffix that is appended to duplicate columns from the second
     * table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @return the suffix
     */
    public String suffix() {
        return m_suffix;
    }

    /**
     * Sets the suffix that is appended to duplicate columns from the second
     * table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @param suffix the suffix
     */
    public void suffix(final String suffix) {
        m_suffix = suffix;
    }

    /**
     * Returns the suffix that is appended to row keys from the first
     * table if multiple rows from the second table match.
     *
     * @return the suffix
     */
    public String keySuffix() {
        return m_keySuffix;
    }

    /**
     * Sets the suffix that is appended to row keys from the first
     * table if multiple rows from the second table match.
     *
     * @param suffix the suffix
     */
    public void keySuffix(final String suffix) {
        m_keySuffix = suffix;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_duplicateHandling =
                DuplicateHandling.valueOf(settings
                        .getString("duplicateHandling"));
        m_joinMode = JoinMode.valueOf(settings.getString("joinMode"));
        m_secondTableColumn = settings.getString("secondTableColumn");
        m_suffix = settings.getString("suffix");
        // since 2.1
        m_keySuffix = settings.getString("keySuffix", "_");
    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_duplicateHandling =
                DuplicateHandling.valueOf(settings.getString(
                        "duplicateHandling", DuplicateHandling.DontExecute
                                .toString()));
        m_joinMode =
                JoinMode.valueOf(settings.getString("joinMode",
                        JoinMode.InnerJoin.toString()));
        m_secondTableColumn =
                settings.getString("secondTableColumn",
                        NewJoinerSettings.ROW_KEY_IDENTIFIER);
        m_suffix = settings.getString("suffix", "");
        m_keySuffix = settings.getString("keySuffix", "_");
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString("duplicateHandling", m_duplicateHandling.toString());
        settings.addString("joinMode", m_joinMode.name());
        settings.addString("secondTableColumn", m_secondTableColumn);
        settings.addString("suffix", m_suffix);
        settings.addString("keySuffix", m_keySuffix);
    }
}
