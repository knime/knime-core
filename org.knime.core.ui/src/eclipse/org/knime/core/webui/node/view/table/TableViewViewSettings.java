/*
 * ------------------------------------------------------------------------
 *
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
 *   Dec 10, 2021 (konrad-amtenbrink): created
 */
package org.knime.core.webui.node.view.table;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;

/**
 * @author Konrad Amtenbrink, KNIME GmbH, Berlin, Germany
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
public class TableViewViewSettings implements DefaultNodeSettings {

    static final class ColumnChoices implements ChoicesProvider {
        @Override
        public String[] choices(final PortObjectSpec[] specs) {
            if (specs[1] == null) {
                return new String[0];
            }
            return ((DataTableSpec)specs[1]).getColumnNames();
        }
    }

    /**
     * The selected columns to be displayed.
     */
    @Schema(title = "Displayed columns", description = "Select the columns that should be displayed in the table",
        choices = ColumnChoices.class, multiple = true)
    public String[] m_displayedColumns;

    /**
     * If the rows keys should be displayed
     */
    @Schema(title = "Show row keys", description = "Whether to display the row keys or not")
    protected boolean m_showRowKeys = true;

    /**
     * Whether to show the data type of every column in the header or not
     */
    @Schema(title = "Show column data type in header",
        description = "Whether to display the data type of the " + "columns in the header or not")
    protected boolean m_showColumnDataType = true;

    /**
     * If the row indices should be displayed
     */
    @Schema(title = "Show row indices", description = "Whether to display the row indices or not")
    protected boolean m_showRowIndices;

    /**
     * The title of the table
     */
    @Schema(title = "Title",
        description = "The title of the table shown above the generated image. If left blank, no title will be shown.")
    protected String m_title = "Table View";

    /**
     * whether to display the title or not
     */
    @Schema(title = "Show title", description = "Whether to display the title or not.")
    protected boolean m_showTitle = true;

    /**
     * If true only a certain number of rows is shown
     */
    @Schema(title = "Pagination",
        description = "Enables or disables the ability to only show a certain number of rows.")
    public boolean m_enablePagination = true;

    /**
     * The page size, i.e., number of rows to be displayed.
     */
    @Schema(title = "Page size", description = "Select the amount of rows shown per page", min = 1)
    public int m_pageSize = 10;

    @Schema(title = "Compact rows", description = "Whether to display the rows in a more compact form or not")
    boolean m_compactMode;

    /**
     * If global search is enabled
     */
    @Schema(title = "Enable global search",
        description = "Enables or disables the ability to perform a global search inside the table.")
    protected boolean m_enableGlobalSearch = true;

    /**
     * If column search is enabled
     */
    @Schema(title = "Enable column search",
        description = "Enables or disables the ability to perform a column search inside the table.")
    protected boolean m_enableColumnSearch = true;

    /**
     * If sorting should be enabled
     */
    @Schema(title = "Enable sorting by header",
        description = "Enables or disables the ability to sort the table by clicking on the column headers")
    protected boolean m_enableSortingByHeader = true;

    /**
     * If this view notifies other views when the users do a selection action
     */
    @Schema(title = "Publish Selection",
        description = "When checked, the view notifies other interactive views when the user changes the selection in"
            + " the current view.")
    protected boolean m_publishSelection = true;

    /**
     * If this view should react on selection events from other views
     */
    @Schema(title = "Subscribe to Selection",
        description = "When checked, the view reacts on notifications from other interactive views that the selection"
            + " has been changed.")
    protected boolean m_subscribeToSelection = true;

    /**
     *  Create a new {@link TableViewViewSettings} with default values
     */
    protected TableViewViewSettings() {
    }

    TableViewViewSettings(final PortObjectSpec[] specs) {
        this((DataTableSpec)specs[1]);
    }

    /**
     * @param spec table spec to determine the selected column from
     */
    public TableViewViewSettings(final DataTableSpec spec) {
        if (spec == null) {
            m_displayedColumns = new String[0];
        } else {
            m_displayedColumns = spec.getColumnNames();
        }
    }

    /**
     * @return the displayedColumns
     */
    public String[] getDisplayedColumns() {
        return m_displayedColumns;
    }
}
