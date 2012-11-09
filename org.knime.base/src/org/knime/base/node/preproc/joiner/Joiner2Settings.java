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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This class hold the settings for the joiner node.
 *
 * @author Heiko Hofer
 */
public class Joiner2Settings {
    private static final String COPOSITION_MODE = "compositionMode";
    private static final String JOIN_MODE = "joinMode";
    private static final String LEFT_JOINING_COLUMNS = "leftTableJoinPredicate";
    private static final String RIGHT_JOINING_COLUMNS =
        "rightTableJoinPredicate";
    private static final String DUPLICATE_COLUMN_HANDLING = "duplicateHandling";
    private static final String DUPLICATE_COLUMN_SUFFIX = "suffix";
    private static final String LEFT_INCLUDE_COLUMNS = "leftIncludeCols";
    private static final String LEFT_INCLUDE_ALL = "leftIncludeAll";
    private static final String REMOVE_LEFT_JOINING_COLUMNS = "rmLeftJoinCols";
    private static final String RIGHT_INCLUDE_COLUMNS = "rightIncludeCols";
    private static final String RIGHT_INCLUDE_ALL = "rightIncludeAll";
    private static final String REMOVE_RIGHT_JOINING_COLUMNS =
        "rmRightJoinCols";
    private static final String MAX_OPEN_FILES = "maxOpenFiles";
    private static final String ROW_KEY_SEPARATOR = "rowKeySeparator";
    private static final String ENABLE_HILITE = "enableHiLite";
    private static final String VERSION = "version";

    /**
     * The version for Joiner nodes until KNIME v2.6.
     * @since 2.7
     */
    public static final String VERSION_1 = "version_1";
    /**
     * The version for Joiner nodes for KNIME v2.7 and later.
     * @since 2.7
     */
    public static final String VERSION_2 = "version_2";

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

    /**
     * This enum holds all ways how join attributes can be combined.
     *
     * @author Heiko Hofer
     */
    public enum CompositionMode {
        /** Join when all join attributes match (logical and). */
        MatchAll,
        /** Join when at least one join attribute matches (logical or). */
        MatchAny;
    }


    /** Internally used row key identifier. */
    static final String ROW_KEY_IDENTIFIER = "$RowID$";



    private DuplicateHandling m_duplicateHandling =
            DuplicateHandling.AppendSuffix;

    private String m_duplicateColSuffix = "(*)";

    private JoinMode m_joinMode = JoinMode.InnerJoin;

    private String[] m_leftJoinColumns = new String[0];
    private String[] m_rightJoinColumns = new String[0];
    private CompositionMode m_compositionMode = CompositionMode.MatchAll;

    private String[] m_leftIncludeCols = new String[0];
    private String[]  m_rightIncludeCols = new String[0];

    private boolean  m_leftIncludeAll = true;
    private boolean m_rightIncludeAll = true;

    private boolean m_rmLeftJoinCols = false;
    private boolean m_rmRightJoinCols = true;

    private int m_maxOpenFiles = 200;
    private String m_rowKeySeparator = "_";
    private boolean m_enableHiLite = false;

    private String m_version = VERSION_2;


    /**
     * Returns true when the version of the settings supports a custom suffix
     * for duplicated columns. The option was removed in version 2
     * (see Bug 3368) in favor of a automatically generated suffix.
     *
     * @return true when this supports a custom suffix for duplicated columns.
     * @since 2.7
     *
     */
    public boolean supportsDuplicateColumnSuffix() {
        return m_version.equals(VERSION_1);
    }

    /**
     * Get the version  either VERSION_1 or VERSION_2.
     * @return the version
     * @since 2.7
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Set the version either VERSION_1 or VERSION_2.
     *
     * @param version the version to set
     * @since 2.7
     */
    public void setVersion(final String version) {
        m_version = version;
    }

    /**
     * Returns the columns of the left table used in the join predicate.
     *
     * @return the leftJoinColumns
     */
    public String[] getLeftJoinColumns() {
        return m_leftJoinColumns;
    }

    /**
     * Sets the columns of the left table used in the join predicate.
     *
     * @param leftJoinColumns the leftJoinColumns to set
     */
    public void setLeftJoinColumns(final String[] leftJoinColumns) {
        m_leftJoinColumns = leftJoinColumns;
    }

    /**
     * Returns the columns of the right table used in the join predicate.
     *
     * @return the rightJoinColumns
     */
    public String[] getRightJoinColumns() {
        return m_rightJoinColumns;
    }

    /**
     * Sets the columns of the right table used in the join predicate.
     *
     * @param rightJoinColumns the rightJoinColumns to set
     */
    public void setRightJoinColumns(final String[] rightJoinColumns) {
        m_rightJoinColumns = rightJoinColumns;
    }


    /**
     * @return the compositionMode
     */
    public CompositionMode getCompositionMode() {
        return m_compositionMode;
    }

    /**
     * @param compositionMode the compositionMode to set
     */
    public void setCompositionMode(final CompositionMode compositionMode) {
        m_compositionMode = compositionMode;
    }

    /**
     * Returns how duplicate column names should be handled.
     *
     * @return the duplicate handling method
     */
    public DuplicateHandling getDuplicateHandling() {
        return m_duplicateHandling;
    }

    /**
     * Sets how duplicate column names should be handled.
     *
     * @param duplicateHandling the duplicate handling method
     */
    public void setDuplicateHandling(
            final DuplicateHandling duplicateHandling) {
        m_duplicateHandling = duplicateHandling;
    }

    /**
     * Returns the mode how the two tables should be joined.
     *
     * @return the join mode
     */
    public JoinMode getJoinMode() {
        return m_joinMode;
    }

    /**
     * Sets the mode how the two tables should be joined.
     *
     * @param joinMode the join mode
     */
    public void setJoinMode(final JoinMode joinMode) {
        m_joinMode = joinMode;
    }

    /**
     * Returns the suffix that is appended to duplicate columns from the right
     * table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @return the suffix
     */
    public String getDuplicateColumnSuffix() {
        return m_duplicateColSuffix;
    }

    /**
     * Sets the suffix that is appended to duplicate columns from the right
     * table if the duplicate handling method is
     * <code>JoinMode.AppendSuffix</code>.
     *
     * @param suffix the suffix
     */
    public void setDuplicateColumnSuffix(final String suffix) {
        m_duplicateColSuffix = suffix;
    }



    /**
     * Returns the columns of the left table that should be included to the
     * joining table.
     *
     * @return the leftIncludeCols
     */
    public String[] getLeftIncludeCols() {
        return m_leftIncludeCols;
    }

    /**
     * Sets the columns of the left table that should be included to the
     * joining table.
     *
     * @param leftIncludeCols the leftIncludeCols to set
     */
    public void setLeftIncludeCols(final String[] leftIncludeCols) {
        m_leftIncludeCols = leftIncludeCols;
    }

    /**
     * Returns the columns of the right table that should be included to the
     * joining table.
     *
     * @return the rightIncludeCols
     */
    public String[] getRightIncludeCols() {
        return m_rightIncludeCols;
    }

    /**
     * Sets the columns of the right table that should be included to the
     * joining table.
     *
     * @param rightIncludeCols the rightIncludeCols to set
     */
    public void setRightIncludeCols(final String[] rightIncludeCols) {
        m_rightIncludeCols = rightIncludeCols;
    }

    /**
     * Returns true when all columns of the left table should be added to
     * the joining table.
     *
     * @return the leftIncludeAll
     */
    public boolean getLeftIncludeAll() {
        return m_leftIncludeAll;
    }

    /**
     * Pass true when all columns of the left table should be added to
     * the joining table.
     *
     * @param leftIncludeAll the leftIncludeAll to set
     */
    public void setLeftIncludeAll(final boolean leftIncludeAll) {
        m_leftIncludeAll = leftIncludeAll;
    }

    /**
     * Returns true when all columns of the right table should be added to
     * the joining table.
     *
     * @return the rightIncludeAll
     */
    public boolean getRightIncludeAll() {
        return m_rightIncludeAll;
    }

    /**
     * Pass true when all columns of the right table should be added to
     * the joining table.
     *
     * @param rightIncludeAll the rightIncludeAll to set
     */
    public void setRightIncludeAll(final boolean rightIncludeAll) {
        m_rightIncludeAll = rightIncludeAll;
    }

    /**
     * Returns true when the columns returned by getLeftJoinColumns() should
     * not be added to the joining table.
     *
     * @return the rmLeftJoinCols
     */
    public boolean getRemoveLeftJoinCols() {
        return m_rmLeftJoinCols;
    }

    /**
     * Pass true when the columns returned by getLeftJoinColumns() should
     * not be added to the joining table.
     *
     * @param rmLeftJoinCols the rmLeftJoinCols to set
     */
    public void setRemoveLeftJoinCols(final boolean rmLeftJoinCols) {
        m_rmLeftJoinCols = rmLeftJoinCols;
    }

    /**
     * Returns true when the columns returned by getRightJoinColumns() should
     * not be added to the joining table.
     *
     * @return the rmRightJoinCols
     */
    public boolean getRemoveRightJoinCols() {
        return m_rmRightJoinCols;
    }

    /**
     * Pass true when the columns returned by getRightJoinColumns() should
     * not be added to the joining table.
     *
     * @param rmRightJoinCols the rmRightJoinCols to set
     */
    public void setRemoveRightJoinCols(final boolean rmRightJoinCols) {
        m_rmRightJoinCols = rmRightJoinCols;
    }

    /**
     * Return number of files that are allowed to be openend by the Joiner.
     *
     * @return the maxOpenFiles
     */
    public int getMaxOpenFiles() {
        return m_maxOpenFiles;
    }

    /**
     * Set number of files that are allowed to be openend by the Joiner.
     *
     * @param maxOpenFiles the maxOpenFiles to set
     */
    public void setMaxOpenFiles(final int maxOpenFiles) {
        m_maxOpenFiles = maxOpenFiles;
    }

    /**
     * Return Separator of the RowKeys in the joined table.
     *
     * @return the rowKeySeparator
     */
    public String getRowKeySeparator() {
        return m_rowKeySeparator;
    }

    /**
     * Set Separator of the RowKeys in the joined table.
     *
     * @param rowKeySeparator the rowKeySeparator to set
     */
    public void setRowKeySeparator(final String rowKeySeparator) {
        m_rowKeySeparator = rowKeySeparator;
    }

    /**
     * Returns true when hiliting should be supported.
     *
     * @return the enableHiLite
     */
    public boolean getEnableHiLite() {
        return m_enableHiLite;
    }

    /**
     * Set if hiliting should be supported.
     *
     * @param enableHiLite the enableHiLite to set
     */
    public void setEnableHiLite(final boolean enableHiLite) {
        m_enableHiLite = enableHiLite;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        // load version introduced in 2.7
        m_version = settings.getString(VERSION, VERSION_1);


        m_duplicateHandling =
                DuplicateHandling.valueOf(settings
                        .getString(DUPLICATE_COLUMN_HANDLING));
        m_compositionMode =
            CompositionMode.valueOf(settings.getString(COPOSITION_MODE));
        m_joinMode = JoinMode.valueOf(settings.getString(JOIN_MODE));
        m_leftJoinColumns =
            settings.getStringArray(LEFT_JOINING_COLUMNS);
        m_rightJoinColumns =
            settings.getStringArray(RIGHT_JOINING_COLUMNS);
        m_duplicateColSuffix = settings.getString(DUPLICATE_COLUMN_SUFFIX);
        m_leftIncludeCols = settings.getStringArray(LEFT_INCLUDE_COLUMNS);
        m_rightIncludeCols = settings.getStringArray(RIGHT_INCLUDE_COLUMNS);
        m_leftIncludeAll = settings.getBoolean(LEFT_INCLUDE_ALL);
        m_rightIncludeAll = settings.getBoolean(RIGHT_INCLUDE_ALL);
        m_rmLeftJoinCols = settings.getBoolean(REMOVE_LEFT_JOINING_COLUMNS);
        m_rmRightJoinCols = settings.getBoolean(REMOVE_RIGHT_JOINING_COLUMNS);
        m_maxOpenFiles = settings.getInt(MAX_OPEN_FILES);
        m_rowKeySeparator = settings.getString(ROW_KEY_SEPARATOR);
        m_enableHiLite = settings.getBoolean(ENABLE_HILITE);


    }

    /**
     * Loads the settings from the node settings object using default values if
     * some settings are missing.
     *
     * @param settings a node settings object
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        // load version introduced in 2.7
        m_version = settings.getString(VERSION, VERSION_1);

        m_duplicateHandling =
                DuplicateHandling.valueOf(settings.getString(
                        DUPLICATE_COLUMN_HANDLING,
                        DuplicateHandling.AppendSuffix.toString()));
        m_compositionMode =
            CompositionMode.valueOf(settings.getString(
                    COPOSITION_MODE, CompositionMode.MatchAll.toString()));
        m_joinMode =
                JoinMode.valueOf(settings.getString(JOIN_MODE,
                        JoinMode.InnerJoin.toString()));
        m_leftJoinColumns =
            settings.getStringArray(LEFT_JOINING_COLUMNS,
                    new String[] {Joiner2Settings.ROW_KEY_IDENTIFIER});
        m_rightJoinColumns =
            settings.getStringArray(RIGHT_JOINING_COLUMNS,
                    new String[] {Joiner2Settings.ROW_KEY_IDENTIFIER});
        m_duplicateColSuffix = settings.getString(DUPLICATE_COLUMN_SUFFIX,
                                                  "");

        m_leftIncludeCols = settings.getStringArray(LEFT_INCLUDE_COLUMNS,
                new String[0]);
        m_rightIncludeCols = settings.getStringArray(RIGHT_INCLUDE_COLUMNS,
                new String[0]);

        m_leftIncludeAll = settings.getBoolean(LEFT_INCLUDE_ALL, true);
        m_rightIncludeAll = settings.getBoolean(RIGHT_INCLUDE_ALL, true);

        m_rmLeftJoinCols = settings.getBoolean(REMOVE_LEFT_JOINING_COLUMNS,
                false);
        m_rmRightJoinCols = settings.getBoolean(REMOVE_RIGHT_JOINING_COLUMNS,
                true);
        m_maxOpenFiles = settings.getInt(MAX_OPEN_FILES, 200);
        m_rowKeySeparator = settings.getString(ROW_KEY_SEPARATOR, "_");
        m_enableHiLite = settings.getBoolean(ENABLE_HILITE, false);


    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addString(DUPLICATE_COLUMN_HANDLING,
                m_duplicateHandling.toString());
        settings.addString(COPOSITION_MODE, m_compositionMode.toString());
        settings.addString(JOIN_MODE, m_joinMode.name());
        settings.addStringArray(LEFT_JOINING_COLUMNS
                , m_leftJoinColumns);
        settings.addStringArray(RIGHT_JOINING_COLUMNS
                , m_rightJoinColumns);
        settings.addString(DUPLICATE_COLUMN_SUFFIX, m_duplicateColSuffix);
        settings.addStringArray(LEFT_INCLUDE_COLUMNS, m_leftIncludeCols);
        settings.addStringArray(RIGHT_INCLUDE_COLUMNS, m_rightIncludeCols);
        settings.addBoolean(LEFT_INCLUDE_ALL, m_leftIncludeAll);
        settings.addBoolean(RIGHT_INCLUDE_ALL, m_rightIncludeAll);
        settings.addBoolean(REMOVE_LEFT_JOINING_COLUMNS, m_rmLeftJoinCols);
        settings.addBoolean(REMOVE_RIGHT_JOINING_COLUMNS, m_rmRightJoinCols);
        settings.addInt(MAX_OPEN_FILES, m_maxOpenFiles);
        settings.addString(ROW_KEY_SEPARATOR, m_rowKeySeparator);
        settings.addBoolean(ENABLE_HILITE, m_enableHiLite);
        // save default values for settings that were removed in 2.5, so that
        // a workflow created with 2.5 can be opened in 2.4.
        settings.addInt("numBitsInitial", 6);
        settings.addInt("numBitsMaximal", Integer.SIZE);
        settings.addDouble("usedMemoryThreshold", 0.85);
        settings.addLong("minAvailableMemory", 10000000L);
        settings.addBoolean("memUseCollectionUsage", true);

        // save version introduced in 2.7
        settings.addString(VERSION, m_version);
    }
}
