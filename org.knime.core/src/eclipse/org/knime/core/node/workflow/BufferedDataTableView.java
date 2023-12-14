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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JMenu;

import org.knime.core.data.DataTable;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObjectView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.AsyncTable;
import org.knime.core.node.tableview.TableView;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class BufferedDataTableView extends JComponent implements PortObjectView {

    private final TableView m_dataView;

    private DataTable m_table;

    /**
     * Updates are synchronized on this object. Declaring the methods
     * as synchronized (i.e. using "this" as mutex) does not work as swing
     * also acquires locks on this graphical object.
     */
    private final Object m_updateLock = new Object();

    /**
     * A view showing the data stored in the specified output port.
     * @param table table to display
     */
    public BufferedDataTableView(final DataTable table) {
        m_table = table;

        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);

        m_dataView = new TableView();
        m_dataView.setWrapColumnHeader(CorePlugin.getInstance().isWrapColumnHeaderInTableViews());
        m_dataView.getContentModel().setSortingAllowed(table instanceof AsyncTable ? false : true);
        m_dataView.registerNavigationActions();
        updateDataTable();
    }

    JMenu[] getMenus() {
        return new JMenu[] {
            m_dataView.createEditMenu(),
            m_dataView.createHiLiteMenu(),
            m_dataView.createNavigationMenu(),
            m_dataView.createViewMenu()
        };
    }

    private void updateDataTable() {
        synchronized (m_updateLock) {
            m_dataView.setDataTable(m_table);
            add(m_dataView);
            revalidate();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.4
     */
    @Override
    public void setHiliteHandler(final HiLiteHandler handler) {
        synchronized (m_updateLock) {
            m_dataView.setHiLiteHandler(handler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        if (m_table == null) {
            return "No Table";
        }
        StringBuilder result = new StringBuilder("");
        String tableName = m_table.getDataTableSpec().getName();
        if (tableName != null) {
            result.append("Table \"" + tableName + "\"");
        } else {
            result.append("");
        }
        if (m_table instanceof BufferedDataTable) {
            result.append(" - Rows: " + ((BufferedDataTable) m_table).size());
        }
        return result.toString();
    }

    @Override
    public void close() {
        if (m_table instanceof AsyncTable) {
            ((AsyncTable)m_table).cancel();
            m_dataView.setDataTable(null);
        }
    }

    @Override
    public void open() {
        if (m_table instanceof AsyncTable) {
            //the loading process of a async data table might have
            //been canceled on close -> re-initialize the table to
            //get rid of rows stuck in the 'loading' state
            if (!m_dataView.hasData()) {
                updateDataTable();
            }
        }
    }

    /** {@inheritDoc}
     * Reset internal data table and reset data out-port view.
     */
    @Override
    public void dispose() {
        m_table = null;
        m_dataView.setDataTable(null);
        m_dataView.setHiLiteHandler(null);
    }

    /**
     * @return the panel containing the table view
     * @since 4.1
     */
    public TableView getTableView() {
        return m_dataView;
    }


}
