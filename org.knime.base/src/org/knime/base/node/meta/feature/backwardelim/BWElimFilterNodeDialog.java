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

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
                        @Override
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
        @Override
        public void addTableModelListener(final TableModelListener l) {
            m_listeners.add(l);
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
        @Override
        public int getColumnCount() {
            return 2;
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
        @Override
        public int getRowCount() {
            return m_featureLevels.size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
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
        @Override
        public boolean isCellEditable(final int rowIndex, final int columnIndex) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void removeTableModelListener(final TableModelListener l) {
            m_listeners.remove(l);
        }

        /**
         * {@inheritDoc}
         */
        @Override
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

    private final JCheckBox m_includeTargetColumn = new JCheckBox(
            "Include target column");

    private final JRadioButton m_manualMode = new JRadioButton(
            "Select features manually");

    private final JRadioButton m_thresholdMode = new JRadioButton(
            "Select features automatically by error threshold");

    private final JSpinner m_errorThreshold = new JSpinner(
            new SpinnerNumberModel(0.5, 0, 1, 0.01));

    private String m_targetColumn;

    private final BWElimFilterSettings m_settings = new BWElimFilterSettings();

    private BWElimModel m_bwElimModel;

    /**
     * Creates a new dialog.
     */
    public BWElimFilterNodeDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;

        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        m_warningMessage.setForeground(Color.ORANGE);
        m_warningMessage.setMinimumSize(new Dimension(100, 20));
        p.add(m_warningMessage, c);

        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        m_includeTargetColumn.addActionListener(new ActionListener() {
            @Override
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

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_manualMode);
        bg.add(m_thresholdMode);
        ActionListener al = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                m_featureLevels.setEnabled(m_manualMode.isSelected());
                m_featureLevels.getSelectionModel().removeSelectionInterval(0,
                        999);

                m_errorThreshold.setEnabled(!m_manualMode.isSelected());
                errorThresholdChanged();
            }
        };
        m_manualMode.addActionListener(al);
        m_thresholdMode.addActionListener(al);

        c.gridy++;
        p.add(m_manualMode, c);
        c.gridy++;
        p.add(m_thresholdMode, c);

        c.gridy++;
        c.gridwidth = 1;
        p.add(new JLabel("      Prediction error threshold   "), c);
        c.gridx = 1;
        m_errorThreshold.setPreferredSize(new Dimension(60, 20));
        m_errorThreshold.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                errorThresholdChanged();
            }
        });

        p.add(m_errorThreshold, c);

        c.gridx = 0;
        c.gridwidth = 2;
        c.gridy++;
        m_featureLevels.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_featureLevels.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
                    @Override
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

    private void errorThresholdChanged() {
        m_warningMessage.setText(" ");
        Pair<Double, Collection<String>> selectedLevel =
                BWElimFilterSettings.findMinimalSet(m_bwElimModel,
                        ((Number)m_errorThreshold.getValue()).doubleValue());

        if (selectedLevel != null) {
            Collection<String> features =
                    new ArrayList<String>(selectedLevel.getSecond());
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
            m_warningMessage.setText("No feature combination with prediction "
                    + "error below the threshold does exist");
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
        m_settings.thresholdMode(m_thresholdMode.isSelected());
        m_settings.errorThreshold(((Number)m_errorThreshold.getValue())
                .doubleValue());
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs) throws NotConfigurableException {
        m_settings.loadSettingsForDialog(settings);

        m_bwElimModel = (BWElimModel)specs[0];
        if (m_bwElimModel == null) {
            throw new NotConfigurableException(
                    "No feature elimination model available.");
        }
        m_targetColumn = m_bwElimModel.targetColumn();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_tableModel.featuresChanged(m_bwElimModel);
                if (specs[1] != null) {
                    m_includedColumns.update((DataTableSpec)specs[1]);
                } else {
                    ((DefaultListModel)m_includedColumns.getModel()).clear();
                }
                for (int i = 0; i < m_tableModel.getRowCount(); i++) {
                    if (m_settings.thresholdMode()) {
                        if (m_tableModel.getNrOfFeatures(i) == m_settings
                                .includedColumns(m_bwElimModel).size()) {
                            m_featureLevels.getSelectionModel()
                                    .setSelectionInterval(i, i);
                            break;
                        }
                    } else {
                        if (m_tableModel.getNrOfFeatures(i) == m_settings
                                .nrOfFeatures()) {
                            m_featureLevels.getSelectionModel()
                                    .setSelectionInterval(i, i);
                            break;
                        }
                    }
                }
                m_includeTargetColumn.setSelected(m_settings
                        .includeTargetColumn());
                m_includedColumns.setSelectedColumns(m_settings
                        .includedColumns(m_bwElimModel));
                m_thresholdMode.setSelected(m_settings.thresholdMode());
                m_manualMode.setSelected(!m_settings.thresholdMode());
                m_errorThreshold.setValue(m_settings.errorThreshold());
            }
        });
    }
}
