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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;

import javax.swing.JPanel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.PortObjectSpecView;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableView;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class DataColumnPropertiesView extends JPanel implements PortObjectSpecView {

    private final TableView m_propsView;

    /**
     *
     * @param tableSpec data table spec to extract the data column properties from, not null
     */
    public DataColumnPropertiesView(final DataTableSpec tableSpec) {
        setLayout(new BorderLayout());
        m_propsView = new TableView(createPropsTable(CheckUtils.checkArgumentNotNull(tableSpec)));
        m_propsView.setShowIconInColumnHeader(false);
        m_propsView.getHeaderTable().setShowColorInfo(false);
        add(m_propsView);
    }


    private DataTable createPropsTable(final DataTableSpec tableSpec) {
        // output has as many cols
        int numOfCols = tableSpec.getNumColumns();
        String[] colNames = new String[numOfCols];
        DataType[] colTypes = new DataType[numOfCols];
        // colnames are the same as incoming, types are all StringTypes
        for (int c = 0; c < numOfCols; c++) {
            colNames[c] = tableSpec.getColumnSpec(c).getName();
            colTypes[c] = StringCell.TYPE;
        }
        // get keys for ALL props in the table. Each will show in one row.
        HashSet<String> allKeys = new LinkedHashSet<String>();
        for (int c = 0; c < numOfCols; c++) {
            Enumeration<String> props =
                    tableSpec.getColumnSpec(c).getProperties().properties();
            while (props.hasMoreElements()) {
                allKeys.add(props.nextElement());
            }
        }
        DataContainer result =
                new DataContainer(new DataTableSpec(colNames, colTypes));

        // now construct the rows we wanna display
        for (String key : allKeys) {
            DataCell[] cells = new DataCell[numOfCols];
            for (int c = 0; c < numOfCols; c++) {
                String cellValue = "";
                if (tableSpec.getColumnSpec(c).getProperties()
                        .containsProperty(key)) {
                    cellValue =
                            tableSpec.getColumnSpec(c).getProperties()
                            .getProperty(key);
                }
                cells[c] = new StringCell(cellValue);
            }
            result.addRowToTable(new DefaultRow(key, cells));
        }
        result.close();
        return result.getTable();
    }

    @Override
    public String getName() {
        return "Properties";
    }

    /** {@inheritDoc}
     * @since 3.6
     */
    @Override
    public void dispose() {
        TableContentModel contentModel = m_propsView.getContentModel();
        DataTable dataTable = contentModel.getDataTable();
        contentModel.setDataTable(null);
        if (dataTable instanceof ContainerTable) {
            ((ContainerTable)dataTable).clear();
        }
    }
}
