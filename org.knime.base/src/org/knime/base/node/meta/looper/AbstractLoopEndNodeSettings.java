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
 */
package org.knime.base.node.meta.looper;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/** Abstract class for Loop End node settings.
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.9
 */
public class AbstractLoopEndNodeSettings {

    /** Specifies how to generate the row keys */
    enum RowKeyPolicy {

        /** Generates entirely new row keys, Row0, Row1, ... */
        GENERATE_NEW("Generate new row IDs"),

            /** Appends a suffix (#1, #2, ...) in order to unique the row keys */
        APPEND_SUFFIX("Unique row IDs by appending a suffix"),

            /** Leaves the row keys unmodified. Node execution fails if duplicates are detected. */
        UNMODIFIED("Leave row IDs unmodified");

        private String m_label;

        /**
         * Creates a new row key policy with a given label that
         * will appear in the node dialog.
         */
        private RowKeyPolicy(final String label) {
            m_label = label;
        }

        /**
         * @return the label that will appear in the node dialog
         */
        String label() {
            return m_label;
        }

    }

    private boolean m_addIterationColumn = true;

    /** @since 3.1 */
    private RowKeyPolicy m_rowKeyPolicy = RowKeyPolicy.APPEND_SUFFIX;


    /**
     *
     */
    public AbstractLoopEndNodeSettings() {
        super();
    }

    /**
     * Sets if row keys are made unique by adding the iteration number.
     *
     * @param b <code>true</code> if the iteration number is added,
     *            <code>false</code> otherwise
     *
     * Deprecated: use {@link #rowKeyPolicy(RowKeyPolicy)} instead
     *
     * @since 2.3
     */
    @Deprecated
    public void uniqueRowIDs(final boolean b) {
        if(b) {
            m_rowKeyPolicy = RowKeyPolicy.APPEND_SUFFIX;
        } else {
            m_rowKeyPolicy = RowKeyPolicy.UNMODIFIED;
        }
    }

    /**
     * Returns if row keys are made unique by adding the iteration number.
     *
     * @return <code>true</code> if the iteration number is added,
     *         <code>false</code> otherwise
     * Deprecated: use {@link #rowKeyPolicy()} instead
     *
     * @since 2.3
     */
    @Deprecated
    public boolean uniqueRowIDs() {
        return m_rowKeyPolicy == RowKeyPolicy.APPEND_SUFFIX;
    }

    /**
     * @param the row key policy specifying whether row keys are to be newly generated, a suffix is appended or remain
     *            unmodified
     * @since 3.1
     */
    public void rowKeyPolicy(final RowKeyPolicy policy) {
        m_rowKeyPolicy = policy;
    }

    /**
     * @return the row key policy specifying whether row keys are to be newly generated, a suffix is appended or remain
     *         unmodified
     * @since 3.1
     */
    public RowKeyPolicy rowKeyPolicy() {
        return m_rowKeyPolicy;
    }

    /**
     * Sets if a column containing the iteration number should be appended to
     * the output table.
     *
     * @param add <code>true</code> if a column should be added,
     *            <code>false</code> otherwise
     */
    public void addIterationColumn(final boolean add) {
        m_addIterationColumn = add;
    }

    /**
     * Returns if a column containing the iteration number should be appended to
     * the output table.
     *
     * @return <code>true</code> if a column should be added, <code>false</code>
     *         otherwise
     */
    public boolean addIterationColumn() {
        return m_addIterationColumn;
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        settings.addBoolean("addIterationColumn", m_addIterationColumn);
        settings.addString("rowKeyPolicy", m_rowKeyPolicy.name());
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    public void loadSettings(final NodeSettingsRO settings) {
        m_addIterationColumn = settings.getBoolean("addIterationColumn", true);

        //old settings -> check for backwards compatibility
        if (settings.containsKey("uniqueRowIDs")) {
            boolean uniqueRowIDs = settings.getBoolean("uniqueRowIDs", false);
            if (uniqueRowIDs) {
                m_rowKeyPolicy = RowKeyPolicy.APPEND_SUFFIX;
            } else {
                m_rowKeyPolicy = RowKeyPolicy.UNMODIFIED;
            }
        } else {
            String rowKeyPolicy = settings.getString("rowKeyPolicy", RowKeyPolicy.APPEND_SUFFIX.name());
            m_rowKeyPolicy = RowKeyPolicy.valueOf(rowKeyPolicy);
        }
    }

}
