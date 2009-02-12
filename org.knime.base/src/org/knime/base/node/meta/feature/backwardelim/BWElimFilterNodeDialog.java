/*
 * ------------------------------------------------------------------ *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   27.02.2008 (thor): created
 */
package org.knime.base.node.meta.feature.backwardelim;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ColumnSelectionList;
import org.knime.core.util.Pair;

/**
 * This class is the dialog for the feature filter node.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class BWElimFilterNodeDialog extends NodeDialogPane {
    private static class MyTableModel implements TableModel {
        private final CopyOnWriteArrayList<TableModelListener> m_listeners =
                new CopyOnWriteArrayList<TableModelListener>();

        private final List<Pair<Double, Collection<String>>> m_featureLevels =
                new ArrayList<Pair<Double, Collection<String>>>();

        /**
         * Call this if the feature elimination model has changed.
         *
         * @param bwModel the new model
         */
        public void featuresChanged(final BWElimModel bwModel) {
            m_featureLevels.clear();
            m_featureLevels.addAll(bwModel.featureLevels());
            Collections.sort(m_featureLevels,
                    new Comparator<Pair<Double, Collection<String>>>() {
                        public int compare(
                                final Pair<Double, Collection<String>> o1,
                                final Pair<Double, Collection<String>> o2) {
                            int diff = o1.getFirst().compareTo(o2.getFirst());
                            if (diff != 0) {
                                return diff;
                            }
                            return -o1.getSecond().size()
                                    + o2.getSecond().size();
                        }
                    });
            TableModelEvent ev = new TableModelEvent(this);
            for (TableModelListener l : m_listeners) {
                l.tableChanged(ev);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void addTableModelListener(final TableModelListener l) {
            m_listeners.add(l);
        }

        /**
         * {@inheritDoc}
         */
        public Class<?> getColumnClass(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Double.class;
                case 1:
                    return Integer.class;
                default:
                    return Object.class;
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getColumnCount() {
            return 2;
        }

        /**
         * {@inheritDoc}
         */
        public String getColumnName(final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Error";
                case 1:
                    return "Nr. of features";
                default:
                    return "???";
            }
        }

        /**
         * {@inheritDoc}
         */
        public int getRowCount() {
            return m_featureLevels.size();
        }

        /**
         * {@inheritDoc}
         */
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return m_featureLevels.get(rowIndex).getFirst();
                case 1:
                    return m_featureLevels.get(rowIndex).getSecond().size();
                default:
                    return null;
            }
        }

        /**
         * Returns the number of included feature for the level shown in the
         * given row.
         *
         * @param rowIndex the row's index
         *
         * @return the number of features
         */
        public int getNrOfFeatures(final int rowIndex) {
            return m_featureLevels.get(rowIndex).getSecond().size();
        }

        /**
         * Returns a collection with all included feature names for the level
         * shown in the given row.
         *
         * @param rowIndex the row's index
         *
         * @return a collection with column names
         */
        public Collection<String> getFeatures(final int rowIndex) {
            return m_featureLevels.get(rowIndex).getSecond();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public void removeTableModelListener(final TableModelListener l) {
            m_listeners.remove(l);
        }

        /**
         * {@inheritDoc}
         */
        public void setValueAt(final Object value, final int rowIndex,
                final int columnIndex) {
            // not editable
        }
    }

    private final ColumnSelectionList m_includedColumns =
            new ColumnSelectionList();

    private final MyTableModel m_tableModel = new MyTableModel();

    private final JTable m_featureLevels = new JTable(m_tableModel);

    private final JLabel m_warningMessage = new JLabel(" ");

    private final JCheckBox m_includeTargetColumn =
            new JCheckBox("Include target column");

    private String m_targetColumn;

    private final BWElimFilterSettings m_settings = new BWElimFilterSettings();

    /**
     * Creates a new dialog.
     */
    public BWElimFilterNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        c.anchor = GridBagConstraints.CENTER;
        m_warningMessage.setForeground(Color.ORANGE);
        m_warningMessage.setMinimumSize(new Dimension(100, 20));
        p.add(m_warningMessage, c);

        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        m_includeTargetColumn.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Collection<String> l = m_includedColumns.getSelectedColumns();
                if (m_includeTargetColumn.isSelected()) {
                    l.add(m_targetColumn);
                } else {
                    l.remove(m_targetColumn);
                }
                m_includedColumns.setSelectedColumns(l);
            }
        });
        p.add(m_includeTargetColumn, c);

        m_featureLevels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_featureLevels.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    public void valueChanged(final ListSelectionEvent e) {
                        listSelectionChanged(e);
                    }
                });
        m_includedColumns.setUserSelectionAllowed(false);

        JPanel p2 = new JPanel(new GridLayout(1, 2));
        p2.add(new JScrollPane(m_featureLevels));
        p2.add(new JScrollPane(m_includedColumns));

        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.8;
        c.weightx = 0.5;
        p.add(p2, c);

        addTab("Column Selection", p);
    }

    private void listSelectionChanged(final ListSelectionEvent ev) {
        int selRow = m_featureLevels.getSelectionModel().getMinSelectionIndex();
        m_warningMessage.setText(" ");
        if (selRow >= 0) {
            Collection<String> features =
                    new ArrayList<String>(m_tableModel.getFeatures(selRow));
            if (m_includeTargetColumn.isSelected()) {
                features.add(m_targetColumn);
            }
            m_includedColumns.setSelectedColumns(features);
            if (m_includedColumns.getSelectedIndices().length < features.size()) {
                m_warningMessage.setText("Warning: Some features are missing "
                        + "in the input table");
            }
        } else {
            m_includedColumns.clearSelection();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        int selRow = m_featureLevels.getSelectedRow();
        if (selRow >= 0) {
            m_settings.nrOfFeatures(m_tableModel.getNrOfFeatures(selRow));
        } else {
            m_settings.nrOfFeatures(-1);
        }
        m_settings.includeTargetColumn(m_includeTargetColumn.isSelected());
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        final BWElimModel model = (BWElimModel)specs[0];
        if (model == null) {
            throw new NotConfigurableException(
                    "No feature elimination model available.");
        }
        m_targetColumn = model.targetColumn();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                m_tableModel.featuresChanged((BWElimModel)specs[0]);
                if (specs[1] != null) {
                    m_includedColumns.update((DataTableSpec)specs[1]);
                } else {
                    ((DefaultListModel) m_includedColumns.getModel()).clear();
                }
                for (int i = 0; i < m_tableModel.getRowCount(); i++) {
                    if (m_tableModel.getNrOfFeatures(i) == m_settings
                            .nrOfFeatures()) {
                        m_featureLevels.getSelectionModel()
                                .setSelectionInterval(i, i);
                        break;
                    }
                }
                m_includeTargetColumn.setSelected(m_settings
                        .includeTargetColumn());
                m_includedColumns.setSelectedColumns(m_settings
                        .includedColumns(model));
            }
        });
    }
}
