/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
    }
}
