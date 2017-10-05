/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * -------------------------------------------------------------------
 *
 * History
 *   02.02.2005 (cebron): created
 */
package org.knime.base.node.io.database;

import java.util.Arrays;
import java.util.List;

import javax.swing.JScrollPane;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObjectSpec;

/**
 * Dialog for choosing the columns that will be sorted. It is also possible to set the order of columns
 *
 * @author Nicolas Cebron, University of Konstanz
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DBSorterNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBSorterNodeDialog.class);

    /**
     * The tab's name.
     */
    private static final String TAB = "Sorting Filter";

    /*
     * Hold the Panel
     */
    private final DBSorterNodeDialogPanel m_panel;

    /*
     * The initial number of SortItems that the SorterNodeDialogPanel should
     * show.
     */
    private static final int NRSORTITEMS = 3;

    /**
     * Creates a new {@link NodeDialogPane} for the Sorter Node in order to choose the desired columns and the sorting
     * order (ascending/ descending).
     */
    DBSorterNodeDialog() {
        super();
        m_panel = new DBSorterNodeDialogPanel();
        super.addTab(TAB, new JScrollPane(m_panel));
    }

    /**
     * Calls the update method of the underlying update method of the {@link DBSorterNodeDialogPanel} using the input
     * data table spec from this {@link DBSorterNodeModel}.
     *
     * @param settings the node settings to read from
     * @param ports the input port objects
     *
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     * @throws NotConfigurableException if the dialog cannot be opened.
     */
    @SuppressWarnings("javadoc")
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] ports)
        throws NotConfigurableException {
        List<String> list = null;
        boolean[] sortOrder = null;

        if (settings.containsKey(DBSorterNodeModel.COLUMNS_KEY)) {
            try {
                String[] alist = settings.getStringArray(DBSorterNodeModel.COLUMNS_KEY);
                if (alist != null) {
                    list = Arrays.asList(alist);
                }
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage());
            }
        }

        if (settings.containsKey(DBSorterNodeModel.ASCENDING_KEY)) {
            try {
                sortOrder = settings.getBooleanArray(DBSorterNodeModel.ASCENDING_KEY);
            } catch (InvalidSettingsException ise) {
                LOGGER.error(ise.getMessage());
            }
        }
        if (list == null || sortOrder == null || list.size() == 0 || list.size() != sortOrder.length) {
            list = null;
            sortOrder = null;
        }

        // set the values on the panel
        DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec)ports[0];
        final DataTableSpec spec;
        if (dbSpec == null) {
            spec = null;
        } else {
            spec = dbSpec.getDataTableSpec();
        }
        m_panel.update(spec, list, sortOrder, NRSORTITEMS);
    }

    /**
     * Sets the list of columns to include and the sorting order list inside the underlying {@link DBSorterNodeModel}
     * retrieving them from the {@link DBSorterNodeDialogPanel}.
     *
     * @param settings the node settings to write into
     * @throws InvalidSettingsException if settings are not valid
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @SuppressWarnings("javadoc")
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        assert (settings != null);
        m_panel.checkValid();
        List<String> inclList = m_panel.getIncludedColumnList();
        settings.addStringArray(DBSorterNodeModel.COLUMNS_KEY, inclList.toArray(new String[inclList.size()]));
        settings.addBooleanArray(DBSorterNodeModel.ASCENDING_KEY, m_panel.getSortOrder());
    }
}
