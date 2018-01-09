/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 8, 2014 ("Patrick Winter"): created
 */
package org.knime.base.node.io.database;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Patrick Winter, KNIME AG, Zurich, Switzerland
 */
final class DBJoinerSettings {

    /**
     * This enum holds all ways of handling duplicate column names in the two input tables.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    enum DuplicateHandling {
        /** Filter out duplicate columns from the second table. */
        Filter,
        /** Append a suffix to the columns from the second table. */
        AppendSuffixAutomatic,
        /** Append a custom suffix to the columns from the second table. */
        AppendSuffix,
        /** Don't execute the node. */
        DontExecute;
    }

    /**
     * This enum holds all ways of joining the two tables.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @author Patrick Winter, KNIME AG, Zurich, Switzerland
     */
    enum JoinMode {
        /** Make an INNER JOIN. */
        InnerJoin("Inner Join", "JOIN"),
        /** Make a LEFT OUTER JOIN. */
        LeftOuterJoin("Left Outer Join", "LEFT JOIN"),
        /** Make a RIGHT OUTER JOIN. */
        RightOuterJoin("Right Outer Join", "RIGHT JOIN"),
        /** Make a FULL OUTER JOIN. */
        FullOuterJoin("Full Outer Join", "FULL OUTER JOIN");

        private final String m_text;

        private final String m_keyword;

        private JoinMode(final String text, final String keyword) {
            m_text = text;
            m_keyword = keyword;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_text;
        }

        /**
         * @return The SQL keyword for this join mode
         */
        String getKeyword() {
            return m_keyword;
        }
    }

    private static final String CFG_JOIN_MODE = "joinMode";

    private static final String CFG_AND_COMPOSITION = "andComposition";

    private static final String CFG_LEFT_JOIN_ON_COLUMNS = "leftJoinOnColumns";

    private static final String CFG_RIGHT_JOIN_ON_COLUMNS = "rightJoinOnColumns";

    private static final String CFG_LEFT_COLUMNS = "leftColumns";

    private static final String CFG_RIGHT_COLUMNS = "rightColumns";

    private static final String CFG_ALL_LEFT_COLUMNS = "allLeftColumns";

    private static final String CFG_ALL_RIGHT_COLUMNS = "allRightColumns";

    private static final String CFG_FILTER_LEFT_JOIN_ON_COLUMNS = "filterLeftJoinOnColumns";

    private static final String CFG_FILTER_RIGHT_JOIN_ON_COLUMNS = "filterRightJoinOnColumns";

    private static final String CFG_DUPLICATE_HANDLING = "duplicateHandling";

    private static final String CFG_CUSTOM_SUFFIX = "customSuffix";

    private JoinMode m_joinMode = JoinMode.InnerJoin;

    private boolean m_andComposition = true;

    private String[] m_leftJoinOnColumns = new String[0];

    private String[] m_rightJoinOnColumns = new String[0];

    private String[] m_leftColumns = new String[0];

    private String[] m_rightColumns = new String[0];

    private boolean m_allLeftColumns = true;

    private boolean m_allRightColumns = true;

    private boolean m_filterLeftJoinOnColumns = false;

    private boolean m_filterRightJoinOnColumns = true;

    private DuplicateHandling m_duplicateHandling = DuplicateHandling.AppendSuffixAutomatic;

    private String m_customSuffix = "(*)";

    /**
     * @param settings The settings to load from
     */
    void loadSettingsInDialog(final NodeSettingsRO settings) {
        m_joinMode = JoinMode.valueOf(settings.getString(CFG_JOIN_MODE, JoinMode.InnerJoin.name()));
        m_andComposition = settings.getBoolean(CFG_AND_COMPOSITION, true);
        m_leftJoinOnColumns = settings.getStringArray(CFG_LEFT_JOIN_ON_COLUMNS, new String[0]);
        m_rightJoinOnColumns = settings.getStringArray(CFG_RIGHT_JOIN_ON_COLUMNS, new String[0]);
        m_leftColumns = settings.getStringArray(CFG_LEFT_COLUMNS, new String[0]);
        m_rightColumns = settings.getStringArray(CFG_RIGHT_COLUMNS, new String[0]);
        m_allLeftColumns = settings.getBoolean(CFG_ALL_LEFT_COLUMNS, true);
        m_allRightColumns = settings.getBoolean(CFG_ALL_RIGHT_COLUMNS, true);
        m_filterLeftJoinOnColumns = settings.getBoolean(CFG_FILTER_LEFT_JOIN_ON_COLUMNS, false);
        m_filterRightJoinOnColumns = settings.getBoolean(CFG_FILTER_RIGHT_JOIN_ON_COLUMNS, true);
        m_duplicateHandling =
            DuplicateHandling.valueOf(settings.getString(CFG_DUPLICATE_HANDLING,
                DuplicateHandling.AppendSuffixAutomatic.name()));
        m_customSuffix = settings.getString(CFG_CUSTOM_SUFFIX, "(*)");
    }

    /**
     * @param settings The settings to load from
     * @throws InvalidSettingsException If the settings are invalid
     */
    void loadSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_joinMode = JoinMode.valueOf(settings.getString(CFG_JOIN_MODE));
        m_andComposition = settings.getBoolean(CFG_AND_COMPOSITION);
        m_leftJoinOnColumns = settings.getStringArray(CFG_LEFT_JOIN_ON_COLUMNS);
        m_rightJoinOnColumns = settings.getStringArray(CFG_RIGHT_JOIN_ON_COLUMNS);
        m_leftColumns = settings.getStringArray(CFG_LEFT_COLUMNS);
        m_rightColumns = settings.getStringArray(CFG_RIGHT_COLUMNS);
        m_allLeftColumns = settings.getBoolean(CFG_ALL_LEFT_COLUMNS);
        m_allRightColumns = settings.getBoolean(CFG_ALL_RIGHT_COLUMNS);
        m_filterLeftJoinOnColumns = settings.getBoolean(CFG_FILTER_LEFT_JOIN_ON_COLUMNS);
        m_filterRightJoinOnColumns = settings.getBoolean(CFG_FILTER_RIGHT_JOIN_ON_COLUMNS);
        m_duplicateHandling = DuplicateHandling.valueOf(settings.getString(CFG_DUPLICATE_HANDLING));
        m_customSuffix = settings.getString(CFG_CUSTOM_SUFFIX);
    }

    /**
     * @param settings The settings to save from
     */
    void saveSettings(final NodeSettingsWO settings) {
        settings.addString(CFG_JOIN_MODE, m_joinMode.name());
        settings.addBoolean(CFG_AND_COMPOSITION, m_andComposition);
        settings.addStringArray(CFG_LEFT_JOIN_ON_COLUMNS, m_leftJoinOnColumns);
        settings.addStringArray(CFG_RIGHT_JOIN_ON_COLUMNS, m_rightJoinOnColumns);
        settings.addStringArray(CFG_LEFT_COLUMNS, m_leftColumns);
        settings.addStringArray(CFG_RIGHT_COLUMNS, m_rightColumns);
        settings.addBoolean(CFG_ALL_LEFT_COLUMNS, m_allLeftColumns);
        settings.addBoolean(CFG_ALL_RIGHT_COLUMNS, m_allRightColumns);
        settings.addBoolean(CFG_FILTER_LEFT_JOIN_ON_COLUMNS, m_filterLeftJoinOnColumns);
        settings.addBoolean(CFG_FILTER_RIGHT_JOIN_ON_COLUMNS, m_filterRightJoinOnColumns);
        settings.addString(CFG_DUPLICATE_HANDLING, m_duplicateHandling.name());
        settings.addString(CFG_CUSTOM_SUFFIX, m_customSuffix);
    }

    /**
     * @return the joinMode
     */
    JoinMode getJoinMode() {
        return m_joinMode;
    }

    /**
     * @param joinMode the joinMode to set
     */
    void setJoinMode(final JoinMode joinMode) {
        m_joinMode = joinMode;
    }

    /**
     * @return the andComposition
     */
    boolean getAndComposition() {
        return m_andComposition;
    }

    /**
     * @param andComposition the andComposition to set
     */
    void setAndComposition(final boolean andComposition) {
        m_andComposition = andComposition;
    }

    /**
     * @return the leftJoinOnColumns
     */
    String[] getLeftJoinOnColumns() {
        return m_leftJoinOnColumns;
    }

    /**
     * @param leftJoinOnColumns the leftJoinOnColumns to set
     */
    void setLeftJoinOnColumns(final String[] leftJoinOnColumns) {
        m_leftJoinOnColumns = leftJoinOnColumns;
    }

    /**
     * @return the rightJoinOnColumns
     */
    String[] getRightJoinOnColumns() {
        return m_rightJoinOnColumns;
    }

    /**
     * @param rightJoinOnColumns the rightJoinOnColumns to set
     */
    void setRightJoinOnColumns(final String[] rightJoinOnColumns) {
        m_rightJoinOnColumns = rightJoinOnColumns;
    }

    /**
     * @return the leftColumns
     */
    String[] getLeftColumns() {
        return m_leftColumns;
    }

    /**
     * @param leftColumns the leftColumns to set
     */
    void setLeftColumns(final String[] leftColumns) {
        m_leftColumns = leftColumns;
    }

    /**
     * @return the rightColumns
     */
    String[] getRightColumns() {
        return m_rightColumns;
    }

    /**
     * @param rightColumns the rightColumns to set
     */
    void setRightColumns(final String[] rightColumns) {
        m_rightColumns = rightColumns;
    }

    /**
     * @return the allLeftColumns
     */
    boolean getAllLeftColumns() {
        return m_allLeftColumns;
    }

    /**
     * @param allLeftColumns the allLeftColumns to set
     */
    void setAllLeftColumns(final boolean allLeftColumns) {
        m_allLeftColumns = allLeftColumns;
    }

    /**
     * @return the allRightColumns
     */
    boolean getAllRightColumns() {
        return m_allRightColumns;
    }

    /**
     * @param allRightColumns the allRightColumns to set
     */
    void setAllRightColumns(final boolean allRightColumns) {
        m_allRightColumns = allRightColumns;
    }

    /**
     * @return the filterLeftJoinOnColumns
     */
    boolean getFilterLeftJoinOnColumns() {
        return m_filterLeftJoinOnColumns;
    }

    /**
     * @param filterLeftJoinOnColumns the filterLeftJoinOnColumns to set
     */
    void setFilterLeftJoinOnColumns(final boolean filterLeftJoinOnColumns) {
        m_filterLeftJoinOnColumns = filterLeftJoinOnColumns;
    }

    /**
     * @return the filterRightJoinOnColumns
     */
    boolean getFilterRightJoinOnColumns() {
        return m_filterRightJoinOnColumns;
    }

    /**
     * @param filterRightJoinOnColumns the filterRightJoinOnColumns to set
     */
    void setFilterRightJoinOnColumns(final boolean filterRightJoinOnColumns) {
        m_filterRightJoinOnColumns = filterRightJoinOnColumns;
    }

    /**
     * @return the duplicateHandling
     */
    DuplicateHandling getDuplicateHandling() {
        return m_duplicateHandling;
    }

    /**
     * @param duplicateHandling the duplicateHandling to set
     */
    void setDuplicateHandling(final DuplicateHandling duplicateHandling) {
        m_duplicateHandling = duplicateHandling;
    }

    /**
     * @return the customSuffix
     */
    String getCustomSuffix() {
        return m_customSuffix;
    }

    /**
     * @param customSuffix the customSuffix to set
     */
    void setCustomSuffix(final String customSuffix) {
        m_customSuffix = customSuffix;
    }

}
