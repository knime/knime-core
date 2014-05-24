/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   May 6, 2014 ("Patrick Winter"): created
 */
package org.knime.base.node.io.database;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.util.Pair;

/**
 * @author Patrick Winter, KNIME.com, Zurich, Switzerland
 */
final class DBGroupByAggregationPanel extends JPanel {

    private static final long serialVersionUID = -3007016099590067613L;

    private static final int COMPONENT_HEIGHT = 155;

    private static final int COLUMN_INDEX = 0;

    private static final int METHOD_INDEX = 1;

    private JList<DataColumnSpec> m_columns = new JList<DataColumnSpec>();

    private DefaultListModel<DataColumnSpec> m_columnsModel = new DefaultListModel<DataColumnSpec>();

    private DataTableSpec m_spec = new DataTableSpec();

    private List<String> m_excluded = new ArrayList<String>(0);

    private JTable m_aggregatedColumns = new JTable();

    private DefaultTableModel m_aggregatedColumnsModel = new DefaultTableModel() {
        private static final long serialVersionUID = -2005832659273670911L;
        @Override
        public boolean isCellEditable(final int row, final int column) {
            return column == 1;
        }
    };

    /**
     * Creates the group by aggregation panel.
     */
    DBGroupByAggregationPanel() {
        m_aggregatedColumns.setModel(m_aggregatedColumnsModel);
        m_aggregatedColumnsModel.addColumn("Column");
        m_aggregatedColumnsModel.addColumn("Aggregation");
        // Make aggregation method selectable through combo box
        m_aggregatedColumns.getColumnModel().getColumn(METHOD_INDEX)
            .setCellEditor(new DefaultCellEditor(
                new JComboBox<String>(DBGroupByAggregationMethod.getAllMethodNames())));
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createTitledBorder(" Aggregation settings "));
        m_columns.setModel(m_columnsModel);
        m_columns.setCellRenderer(new DataColumnSpecListCellRenderer());
        // Add column on double click
        m_columns.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    onAddIt();
                }
            }
        });
        // Remove column on double click
        m_aggregatedColumns.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    onRemIt();
                }
            }
        });
        add(createListComponent());
        add(createButtonComponent());
        add(createTableComponent());
    }

    /**
     * @param excludedColumns Columns that are excluded from the list of columns available for aggregation
     */
    void excludeColsChange(final List<String> excludedColumns) {
        m_excluded = excludedColumns;
        Vector<?> rows = m_aggregatedColumnsModel.getDataVector();
        for (int i = 0; i < rows.size(); i++) {
            String column = ((Vector<?>)rows.elementAt(i)).elementAt(0).toString();
            if (m_excluded.contains(column)) {
                m_aggregatedColumnsModel.removeRow(i--);
            }
        }
        refreshAvailableColumns();
    }

    /**
     * @param settings The settings to load from
     * @param spec The spec containing the available columns
     */
    void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec) {
        m_spec = spec;
        m_aggregatedColumnsModel.setRowCount(0);
        String[] columns = settings.getStringArray(DBGroupByNodeModel.CFG_AGGREGATED_COLUMNS, new String[0]);
        String[] methods = settings.getStringArray(DBGroupByNodeModel.CFG_AGGREGATION_METHODS, new String[0]);
        for (int i = 0; i < columns.length; i++) {
            m_aggregatedColumnsModel.addRow(new Object[]{columns[i], methods[i]});
        }
        refreshAvailableColumns();
    }

    /**
     * @param settings The settings to save to
     * @throws InvalidSettingsException If the current settings are invalid
     */
    void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        List<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
        String[] columns = new String[m_aggregatedColumnsModel.getRowCount()];
        String[] methods = new String[m_aggregatedColumnsModel.getRowCount()];
        for (int i = 0; i < m_aggregatedColumnsModel.getRowCount(); i++) {
            Vector<?> row = (Vector<?>)m_aggregatedColumnsModel.getDataVector().elementAt(i);
            Pair<String, String> pair =
                new Pair<String, String>(row.elementAt(COLUMN_INDEX).toString(),
                        row.elementAt(METHOD_INDEX).toString());
            if (pairs.contains(pair)) {
                throw new InvalidSettingsException("Duplicate settings: Column " + pair.getFirst()
                    + " with aggregation method " + pair.getSecond());
            }
            pairs.add(pair);
            columns[i] = pair.getFirst();
            methods[i] = pair.getSecond();
        }
        settings.addStringArray(DBGroupByNodeModel.CFG_AGGREGATED_COLUMNS, columns);
        settings.addStringArray(DBGroupByNodeModel.CFG_AGGREGATION_METHODS, methods);
    }

    private void refreshAvailableColumns() {
        m_columnsModel.clear();
        for (DataColumnSpec colSpec : m_spec) {
            if (!m_excluded.contains(colSpec.getName())) {
                m_columnsModel.addElement(colSpec);
            }
        }
    }

    private void onAddIt() {
        if (m_columns.getSelectedValue() != null) {
            String selection = m_columns.getSelectedValue().getName();
            m_aggregatedColumnsModel.addRow(new Object[]{selection, DBGroupByAggregationMethod.AVG.name()});
        }
    }

    private void onRemIt() {
        int selectedRow = m_aggregatedColumns.getSelectedRow();
        if (selectedRow >= 0) {
            m_aggregatedColumnsModel.removeRow(selectedRow);
        }
    }

    private void onAddAll() {
        Enumeration<DataColumnSpec> specs = m_columnsModel.elements();
        while (specs.hasMoreElements()) {
            String column = specs.nextElement().getName();
            m_aggregatedColumnsModel.addRow(new Object[]{column, DBGroupByAggregationMethod.AVG.name()});
        }
    }

    private void onRemAll() {
        m_aggregatedColumnsModel.setRowCount(0);
    }

    /**
     * Creates the list component that contains the possible columns to choose from (displayed on the left hand).
     *
     * @return the list component which contains the list of available columns to choose from
     */
    private Component createListComponent() {
        final Box avMethodsBox = new Box(BoxLayout.X_AXIS);
        final Border border = BorderFactory.createTitledBorder(" Available columns ");
        avMethodsBox.setBorder(border);
        final JScrollPane compMethodsList = new JScrollPane(m_columns);
        final Dimension dimension = new Dimension(150, COMPONENT_HEIGHT);
        compMethodsList.setMinimumSize(dimension);
        compMethodsList.setPreferredSize(dimension);
        avMethodsBox.add(compMethodsList);
        return avMethodsBox;
    }

    /**
     * @return the {@link Component} that contains the buttons (add, add all, remove and remove all) in the middle of
     *         the dialog.
     */
    private Component createButtonComponent() {
        final JPanel buttonBox = new JPanel();
        buttonBox.setBorder(BorderFactory.createTitledBorder(" Select "));
        buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.Y_AXIS));
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(createButtonFiller(10, 10));
        final JButton addButton = new JButton("add >>");
        addButton.setMaximumSize(new Dimension(135, 25));
        buttonBox.add(addButton);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton addAllButton = new JButton("add all >>");
        addAllButton.setMaximumSize(new Dimension(135, 25));
        buttonBox.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton remButton = new JButton("<< remove");
        remButton.setMaximumSize(new Dimension(135, 25));
        buttonBox.add(remButton);
        remButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton remAllButton = new JButton("<< remove all");
        remAllButton.setMaximumSize(new Dimension(135, 25));
        buttonBox.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonBox.add(createButtonFiller(10, 10));
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    private Component createButtonFiller(final int height, final int width) {
        final Component filler = Box.createVerticalStrut(height);
        final Dimension fillerDimension = new Dimension(width, height);
        filler.setMaximumSize(fillerDimension);
        filler.setPreferredSize(fillerDimension);
        filler.setMinimumSize(fillerDimension);
        return filler;
    }

    private Component createTableComponent() {
        JScrollPane pane = new JScrollPane(m_aggregatedColumns);
        pane.setBorder(BorderFactory.createTitledBorder("Aggregated columns"));
        return pane;
    }

}
