/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *   12.08.2013 (thor): created
 */
package org.knime.testing.internal.nodes.differ;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.knime.base.data.aggregation.dialogutil.DataColumnSpecTableCellRenderer;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.knime.testing.internal.diffcheckers.CheckerUtil;

/**
 * Dialog for the difference checker node.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class DifferenceCheckerNodeDialog extends NodeDialogPane {
    private final DefaultListCellRenderer m_diffCheckerListRenderer = new DefaultListCellRenderer() {
        private static final long serialVersionUID = -3190079420164691890L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                      final boolean isSelected, final boolean cellHasFocus) {
            if (value instanceof DifferenceCheckerFactory) {
                return super.getListCellRendererComponent(list, ((DifferenceCheckerFactory<? extends DataValue>)value)
                        .getDescription(), index, isSelected, cellHasFocus);
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    };

    private final DefaultTableCellRenderer m_diffCheckerTableRenderer = new DefaultTableCellRenderer() {
        private static final long serialVersionUID = -4292093157267296306L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus, final int row,
                                                       final int column) {
            if (value instanceof DifferenceCheckerFactory) {
                return super
                        .getTableCellRendererComponent(table, ((DifferenceCheckerFactory<? extends DataValue>)value)
                                .getDescription(), isSelected, hasFocus, row, column);
            } else {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    };

    private final DefaultTableCellRenderer m_columnSpecTableRenderer = new DataColumnSpecTableCellRenderer();

    private class MyTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 4419441637185906339L;

        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            if (columnIndex == 0) {
                return m_spec.getColumnSpec(rowIndex);
            } else if (columnIndex == 1) {
                return m_settings.checkerFactory(m_spec.getColumnSpec(rowIndex).getName());
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getColumnName(final int column) {
            if (column == 0) {
                return "Column";
            } else if (column == 1) {
                return "Checker";
            } else {
                return null;
            }
        }

        @Override
        public int getRowCount() {
            return m_spec.getNumColumns();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return columnIndex == 1;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
            if (columnIndex == 1) {
                DataColumnSpec colSpec = m_spec.getColumnSpec(rowIndex);
                DifferenceCheckerFactory<? extends DataValue> factory =
                        (DifferenceCheckerFactory<? extends DataValue>)aValue;
                m_settings.checkerFactory(colSpec.getName(), factory);
                DifferenceChecker<? extends DataValue> checker = factory.newChecker();
                m_differenceCheckers.put(colSpec, checker);
                updateInternalsPanel(rowIndex);
            } else {
                super.setValueAt(aValue, rowIndex, columnIndex);
            }
        }
    }

    private class MyTable extends JTable {
        private static final long serialVersionUID = -4798506534974339078L;

        MyTable(final TableModel model) {
            super(model);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TableCellEditor getCellEditor(final int row, final int column) {
            if (column == 1) {
                DataColumnSpec colSpec = m_spec.getColumnSpec(row);

                List<DifferenceCheckerFactory<? extends DataValue>> l =
                        CheckerUtil.instance.getFactoryForType(colSpec.getType());
                JComboBox comboBox = new JComboBox();
                for (DifferenceCheckerFactory<? extends DataValue> fac : l) {
                    comboBox.addItem(fac);
                }
                comboBox.setRenderer(m_diffCheckerListRenderer);
                comboBox.setSelectedItem(m_settings.checkerFactory(colSpec.getName()));
                return new DefaultCellEditor(comboBox);
            } else {
                return super.getCellEditor(row, column);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public TableCellRenderer getCellRenderer(final int row, final int column) {
            if (column == 0) {
                return m_columnSpecTableRenderer;
            } else if (column == 1) {
                return m_diffCheckerTableRenderer;
            } else {
                return super.getCellRenderer(row, column);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getRowHeight() {
            return 24;
        }
    }

    private final AbstractTableModel m_tableModel = new MyTableModel();

    private final JTable m_columnCheckerTable = new MyTable(m_tableModel);

    private final JScrollPane m_columnConfigPanel = new JScrollPane();

    private final DifferenceCheckerSettings m_settings = new DifferenceCheckerSettings();

    private final Map<DataColumnSpec, DifferenceChecker<? extends DataValue>> m_differenceCheckers =
            new HashMap<DataColumnSpec, DifferenceChecker<? extends DataValue>>();

    private DataTableSpec m_spec = new DataTableSpec();

    DifferenceCheckerNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 0.8;

        p.add(new JScrollPane(m_columnCheckerTable), c);

        m_columnCheckerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    updateInternalsPanel(m_columnCheckerTable.getSelectedRow());
                }
            }
        });

        c.gridy++;
        c.weighty = 0.2;
        p.add(m_columnConfigPanel, c);

        addTab("Column Configuration", p);
    }

    void updateInternalsPanel(final int rowIndex) {
        if (rowIndex == -1) {
            m_columnConfigPanel.setViewportView(null);
        } else {
            DataColumnSpec dcs = m_spec.getColumnSpec(rowIndex);
            DifferenceChecker<? extends DataValue> checker = m_differenceCheckers.get(dcs);

            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.anchor = GridBagConstraints.WEST;
            for (DialogComponent dc : checker.getDialogComponents()) {
                p.add(dc.getComponentPanel(), c);
                c.gridy++;
            }

            m_columnConfigPanel.setViewportView(p);
        }
        m_columnConfigPanel.getParent().validate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
            throws NotConfigurableException {
        m_spec = specs[1];
        m_settings.loadSettingsForDialog(settings, m_spec);

        m_differenceCheckers.clear();
        for (DataColumnSpec dcs : m_spec) {
            DifferenceCheckerFactory<? extends DataValue> factory = m_settings.checkerFactory(dcs.getName());
            DifferenceChecker<? extends DataValue> checker = factory.newChecker();
            checker.loadSettingsForDialog(m_settings.internalsForColumn(dcs.getName()));
            m_differenceCheckers.put(dcs, checker);
        }

        updateInternalsPanel(-1);
        m_tableModel.fireTableDataChanged();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        for (Map.Entry<DataColumnSpec, DifferenceChecker<? extends DataValue>> e : m_differenceCheckers.entrySet()) {
            e.getValue().saveSettings(m_settings.internalsForColumn(e.getKey().getName()));
        }

        m_settings.saveSettings(settings);
    }
}
