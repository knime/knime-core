/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 * 
 */
package org.knime.base.node.preproc.missingval;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * Dialog to the missing value handling node.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class MissingValueHandlingNodeDialog extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MissingValueHandlingNodeDialog.class);

    private final DefaultListModel m_colListModel;

    private final JPanel m_defaultsPanel;

    private final JPanel m_individualsPanel;

    /**
     * Constructs new dialog with an appropriate dialog title.
     */
    public MissingValueHandlingNodeDialog() {
        super();
        // Default handling, first tab
        m_defaultsPanel = new JPanel(new GridLayout(0, 1));
        addTab("Default", m_defaultsPanel);

        // Individual Handling, second tab
        m_colListModel = new DefaultListModel();
        final JList colList = new JList(m_colListModel);
        colList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        colList.setCellRenderer(new DataColumnSpecListCellRenderer());
        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(new JScrollPane(colList), BorderLayout.WEST);
        m_individualsPanel = new JPanel(new GridLayout(0, 1));
        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        tabPanel.add(scroller, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec colSpec = (DataColumnSpec)colList
                        .getSelectedValue();
                onAdd(colSpec);
            }
        });
        buttonPanel.add(addButton);
        tabPanel.add(buttonPanel, BorderLayout.SOUTH);
        addTab("Individual", tabPanel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_colListModel.removeAllElements();
        for (int i = 0; i < specs[0].getNumColumns(); i++) {
            DataColumnSpec spec = specs[0].getColumnSpec(i);
            m_colListModel.addElement(spec);
        }
        ColSetting[] defaults = new ColSetting[0];
        ColSetting[] individuals = new ColSetting[0];
        try {
            defaults = ColSetting.loadMetaColSettings(settings);
            individuals = ColSetting.loadIndividualColSettings(settings);
        } catch (InvalidSettingsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        m_defaultsPanel.removeAll();
        for (int i = 0; i < defaults.length; i++) {
            m_defaultsPanel.add(new MissingValuePanel(defaults[i], null));
        }
        m_individualsPanel.removeAll();
        for (int i = 0; i < individuals.length; i++) {
            String name = individuals[i].getName();
            DataColumnSpec colSpec = specs[0].getColumnSpec(name);
            if (colSpec == null) {
                LOGGER.debug("No such column in spec: " + name);
            } else {
                addToIndividualPanel(new MissingValuePanel(individuals[i],
                        colSpec));
            }
        }
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        Component[] cs = m_defaultsPanel.getComponents();
        ColSetting[] defaults = new ColSetting[cs.length];
        for (int i = 0; i < defaults.length; i++) {
            defaults[i] = ((MissingValuePanel)cs[i]).getSettings();
        }
        cs = m_individualsPanel.getComponents();
        ColSetting[] individuals = new ColSetting[cs.length];
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = ((MissingValuePanel)cs[i]).getSettings();
        }
        ColSetting.saveMetaColSettings(defaults, settings);
        ColSetting.saveIndividualsColSettings(individuals, settings);
    }

    private void onAdd(final DataColumnSpec spec) {
        if (spec == null) {
            return;
        }
        Component[] c = m_individualsPanel.getComponents();
        for (int i = 0; i < c.length; i++) {
            MissingValuePanel p = (MissingValuePanel)c[i];
            if (p.getSettings().getName().equals(spec.getName())) {
                return;
            }
        }
        MissingValuePanel p = new MissingValuePanel(spec);
        addToIndividualPanel(p);
    }

    private void removeFromIndividualPanel(final MissingValuePanel panel) {
        m_individualsPanel.remove(panel);
        m_individualsPanel.revalidate();
        m_individualsPanel.repaint();
    }

    private void addToIndividualPanel(final MissingValuePanel panel) {
        panel.addPropertyChangeListener(MissingValuePanel.REMOVE_ACTION,
                new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        removeFromIndividualPanel((MissingValuePanel)evt
                                .getSource());
                    }
                });
        m_individualsPanel.add(panel);
        m_individualsPanel.revalidate();
    }
}
