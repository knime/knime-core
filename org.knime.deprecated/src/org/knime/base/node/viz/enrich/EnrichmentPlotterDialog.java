/* Created on Oct 23, 2006 9:22:50 AM by thor
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.enrich;

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
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;

import org.knime.core.data.DataTableSpec;
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
            new ColumnSelectionComboxBox((Border)null, DoubleValue.class);

    private final JCheckBox m_sortDescending = new JCheckBox("Sort descending");

    private final JRadioButton m_plotSum =
            new JRadioButton("Plot sum of hit values");

    private final JRadioButton m_plotHits = new JRadioButton("Hit threshold");

    private final JFormattedTextField m_hitThreshold =
            new JFormattedTextField(new DecimalFormat("###0.0##"));

    /**
     * Creates a dialog for the enrichment plotter settings.
     */
    public EnrichmentPlotterDialog() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Sort column"), c);

        c.gridx = 1;
        p.add(new JLabel("Hit column"), c);

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
        JButton b = new JButton("Add curve");
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
        m_curves.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        p.add(new JScrollPane(m_curves), c);


        ButtonGroup bg = new ButtonGroup();
        bg.add(m_plotHits);
        bg.add(m_plotSum);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;

        JPanel p2 = new JPanel();
        p2.add(m_plotSum);
        p2.add(m_plotHits);
        p2.add(m_hitThreshold);
        m_hitThreshold.setColumns(3);
        p.add(p2, c);

        m_plotHits.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_hitThreshold.setEnabled(m_plotHits.isSelected());
            }
        });
        m_plotSum.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_hitThreshold.setEnabled(m_plotHits.isSelected());
            }
        });


        addTab("Curves to plot", p);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_settings = new EnrichmentPlotterSettings();
        try {
            m_settings.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            // LOGGER.error("Cannot read settings, using defaults", ex);
        }

        m_sortColumn.update(specs[0], null);
        m_hitColumn.update(specs[0], null);
        m_listModel.refresh();
        m_plotHits.setSelected(!m_settings.sumHitValues());
        m_plotSum.setSelected(m_settings.sumHitValues());
        m_hitThreshold.setText(Double.toString(m_settings.hitThreshold()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_settings.sumHitValues(m_plotSum.isSelected());
        m_settings.hitThreshold(((Number)m_hitThreshold.getValue())
                .doubleValue());
        m_settings.saveSettingsTo(settings);
    }
}
