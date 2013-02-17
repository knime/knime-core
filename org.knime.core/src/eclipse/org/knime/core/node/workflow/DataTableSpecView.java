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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.DataTableSpecExtractor;
import org.knime.core.data.util.DataTableSpecExtractor.PossibleValueOutputFormat;
import org.knime.core.data.util.DataTableSpecExtractor.PropertyHandlerOutputFormat;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DataTableSpecView extends JPanel {

//    private static final NodeLogger LOGGER =
//        NodeLogger.getLogger(DataTableSpecView.class);


    private final TableView m_specView;

    private final DataTableSpec m_tableSpec;

    /** Updates are synchronized on this object. Declaring the methods
     * as synchronized (i.e. using "this" as mutex) does not work as swing
     * also acquires locks on this graphical object.
     */
    private final Object m_updateLock = new Object();


    /**
     *
     * @param tableSpec data table spec to display
     */
    public DataTableSpecView(final DataTableSpec tableSpec) {
        m_tableSpec = tableSpec;
        setLayout(new BorderLayout());
        setBackground(NodeView.COLOR_BACKGROUND);

        m_specView = new TableView();
        m_specView.getContentModel().setSortingAllowed(true);
        m_specView.setShowIconInColumnHeader(false);
        m_specView.setShowColorInfo(false);
        m_specView.getHeaderTable().getModel().setColumnName("Column Name");
        // in the data view our columns are all of type string. Don't show that.
        // Users confuse it with the type of their table.
        // m_dataView.getHeaderTable().setShowTypeInfo(false);
        m_specView.getHeaderTable().setShowColorInfo(false);

        setName("DataTableSpec");
        add(m_specView);

        updateDataTableSpec();
    }

    /**
     * Sets a new DataTableSpec to display.
     *
     * @param newTableSpec The new data table spec (or null) to display in the
     *            view.
     */
    private void updateDataTableSpec() {
        synchronized (m_updateLock) {
            m_specView.setDataTable(createTableSpecTable(m_tableSpec));
            m_specView.getHeaderTable().sizeWidthToFit();
            // display the number of columns in the upper left corner
            if (m_tableSpec != null) {
                String title = createWindowTitle(m_tableSpec.getNumColumns());
                m_specView.getHeaderTable().setColumnName(title);
                setName("Spec - " + title);
            } else {
                m_specView.getHeaderTable().setColumnName("");
                setName("No Spec");
            }
        }
    }


    private DataTable createTableSpecTable(final DataTableSpec spec) {
        if (spec != null) {
            DataTableSpecExtractor e = new DataTableSpecExtractor();
            e.setExtractColumnNameAsColumn(false);
            e.setPossibleValueOutputFormat(PossibleValueOutputFormat.Columns);
            e.setPropertyHandlerOutputFormat(PropertyHandlerOutputFormat.ToString);
            return e.extract(spec);
        } else {
            String[] names = new String[]{"No outgoing table spec"};
            DataType[] types = new DataType[]{StringCell.TYPE};
            DataContainer result = new DataContainer(new DataTableSpec(names, types));
            result.close();
            return result.getTable();
        }
    }

    private static String createWindowTitle(final int numOfCols) {
        return "Column" + (numOfCols > 1 ? "s: " : ": ") + numOfCols;
    }


}
