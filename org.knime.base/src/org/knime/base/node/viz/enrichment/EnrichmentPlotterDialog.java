/* Created on Oct 23, 2006 9:22:50 AM by thor
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * ------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.enrichment;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;

import org.knime.base.node.viz.enrichment.EnrichmentPlotterSettings.PlotMode;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * This is the dialog for the enrichment plotter in which the two columns for
 * the curves are selected.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class EnrichmentPlotterDialog extends NodeDialogPane {
    /**
     * The model for the list with the curves.
     *
     * @author Thorsten Meinl, University of Konstanz
     */
    private class MyListModel extends AbstractListModel {
        /**
         * {@inheritDoc}
         */
        public Object getElementAt(final int index) {
            return m_settings.getCurve(index);
        }

        /**
         * {@inheritDoc}
         */
        public int getSize() {
            return m_settings.getCurveCount();
        }

        /**
         * Adds a new curve to the list and the settings.
         */
        public void addCurve() {
            String sort = m_sortColumn.getSelectedColumn();
            String hit = m_hitColumn.getSelectedColumn();
            if (sort.equals(hit)) {
                JOptionPane.showMessageDialog(getPanel().getParent(),
                        "Please select two different columns.",
                        "Wrong selection", JOptionPane.WARNING_MESSAGE);
                return;
            }

            m_settings.addCurve(sort, hit, m_sortDescending.isSelected());
            fireIntervalAdded(this, m_settings.getCurveCount() - 1, m_settings
                    .getCurveCount() - 1);
            m_sortDescending.setSelected(false);
        }

        /**
         * Removes the selected curve from the list and the settings.
         */
        public void removeCurve() {
            int index = m_curves.getSelectedIndex();
            if ((m_curves.getModel().getSize() != 0) && (index != -1)) {
                m_settings.removeCurve(m_settings.getCurve(index));
                fireContentsChanged(this, 0, getSize() - 1);
            }
        }

        /**
         * Forces a refresh of the list.
         */
        void refresh() {
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    private final MyListModel m_listModel = new MyListModel();

    private final JList m_curves = new JList(m_listModel);

    private EnrichmentPlotterSettings m_settings =
            new EnrichmentPlotterSettings();

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_sortColumn =
            new ColumnSelectionComboxBox((Border)null, DoubleValue.class);

    @SuppressWarnings("unchecked")
    private final ColumnSelectionComboxBox m_hitColumn =
            new ColumnSelectionComboxBox((Border)null, DataValue.class);

    private final JCheckBox m_sortDescending = new JCheckBox("Sort descending");

    private final JRadioButton m_plotSum =
            new JRadioButton("Plot sum of hit values");

    private final JRadioButton m_plotHits = new JRadioButton("Plot hits ");

    private final JRadioButton m_plotClusters =
            new JRadioButton("Plot discovered clusters");

    private final JFormattedTextField m_hitThreshold =
            new JFormattedTextField(new DecimalFormat("###0.0##"));

    private final JSpinner m_minClusterMembers =
        new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));

    private final JLabel m_hitClusterLabel = new JLabel("Activity column");

    /**
     * Creates a dialog for the enrichment plotter settings.
     */
    public EnrichmentPlotterDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(2, 2, 0, 2);
        c.gridx = 0;
        c.gridy = 0;

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_plotHits);
        bg.add(m_plotSum);
        bg.add(m_plotClusters);
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;

        p.add(m_plotSum, c);

        c.gridy++;
        c.insets = new Insets(0, 2, 0, 2);
        p.add(m_plotHits, c);

        c.gridy++;
        JPanel p2 = new JPanel();
        m_hitThreshold.setColumns(3);
        p2.add(new JLabel("    Hit threshold"));
        p2.add(m_hitThreshold);
        p.add(p2, c);

        c.gridy++;
        c.insets = new Insets(0, 2, 0, 2);
        p.add(m_plotClusters, c);


        c.gridy++;
        p2 = new JPanel();
        p2.add(new JLabel("    Minimum molecules per cluster"));
        p2.add(m_minClusterMembers);
        c.insets = new Insets(2, 2, 6, 2);
        p.add(p2, c);

        c.insets = new Insets(2, 2, 2, 2);

        ActionListener al = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_hitThreshold.setEnabled(m_plotHits.isSelected());
                m_minClusterMembers.setEnabled(m_plotClusters.isSelected());
                if (m_plotClusters.isSelected()) {
                    m_hitClusterLabel.setText("Cluster column");
                } else {
                    m_hitClusterLabel.setText("Activity column");
                }
            }
        };
        m_plotSum.addActionListener(al);
        m_plotHits.addActionListener(al);
        m_plotClusters.addActionListener(al);

        c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        p.add(new JLabel("Sort column"), c);
        c.gridx = 1;
        p.add(m_hitClusterLabel, c);

        c.gridy++;
        c.gridx = 0;
        p.add(m_sortColumn, c);

        c.gridx = 1;
        p.add(m_hitColumn, c);

        c.gridy++;
        c.gridx = 0;
        p.add(m_sortDescending, c);

        c.gridy++;
        c.gridx = 0;
        JButton b = new JButton("  Add curve  ");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_listModel.addCurve();
            }
        });
        p.add(b, c);

        c.gridx = 1;
        b = new JButton("Remove curve");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_listModel.removeCurve();
            }
        });
        p.add(b, c);

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        m_curves.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(m_curves), c);


        addTab("Curves to plot", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings = new EnrichmentPlotterSettings();
        m_settings.loadSettingsForDialog(settings);

        m_sortColumn.update(specs[0], null);
        m_hitColumn.update(specs[0], null);
        m_listModel.refresh();
        m_plotHits.setSelected(m_settings.plotMode().equals(PlotMode.PlotHits));
        m_plotSum.setSelected(m_settings.plotMode().equals(PlotMode.PlotSum));
        m_plotClusters.setSelected(m_settings.plotMode().equals(
                PlotMode.PlotClusters));
        m_hitThreshold.setEnabled(m_settings.plotMode().equals(
                PlotMode.PlotHits));
        m_minClusterMembers.setEnabled(m_settings.plotMode().equals(
                PlotMode.PlotClusters));

        m_hitThreshold.setText(Double.toString(m_settings.hitThreshold()));
        m_minClusterMembers.setValue(m_settings.minClusterMembers());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        if (m_plotHits.isSelected()) {
            m_settings.plotMode(PlotMode.PlotHits);
        } else if (m_plotSum.isSelected()) {
            m_settings.plotMode(PlotMode.PlotSum);
        } else {
            m_settings.plotMode(PlotMode.PlotClusters);
        }
        m_settings.hitThreshold(((Number)m_hitThreshold.getValue())
                .doubleValue());
        m_settings.minClusterMembers(((Number)m_minClusterMembers.getValue())
                .intValue());
        m_settings.saveSettings(settings);
    }
}
