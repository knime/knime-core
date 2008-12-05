/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * 
 */
package org.knime.base.node.preproc.missingval;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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

    private final JList m_colList;
    private final DefaultListModel m_colListModel;

    private final JPanel m_defaultsPanel;
    private final JPanel m_individualsPanel;
    
    private final JButton m_addButton;

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
        m_colList = new JList(m_colListModel);
        m_colList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_colList.addListSelectionListener(new ListSelectionListener() {
           public void valueChanged(final ListSelectionEvent e) {
               checkButtonStatus();
           } 
        });
        m_colList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DataColumnSpec selected = 
                        (DataColumnSpec)m_colList.getSelectedValue();
                    onAdd(selected);
                }
            } 
        });
        m_colList.setCellRenderer(new DataColumnSpecListCellRenderer());
        JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(new JScrollPane(m_colList), BorderLayout.WEST);
        m_individualsPanel = new IndividualsPanel();
        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        tabPanel.add(scroller, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_addButton = new JButton("Add");
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                DataColumnSpec colSpec = 
                    (DataColumnSpec)m_colList.getSelectedValue();
                onAdd(colSpec);
            }
        });
        buttonPanel.add(m_addButton);
        tabPanel.add(buttonPanel, BorderLayout.SOUTH);
        addTab("Individual", tabPanel);
    }
    
    /** Enables/disables the button according to list selection. */
    private void checkButtonStatus() {
        m_addButton.setEnabled(!m_colList.isSelectionEmpty());
    }

    /**
     * {@inheritDoc}
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
        defaults = ColSetting.loadMetaColSettings(settings, specs[0]);
        individuals = ColSetting.loadIndividualColSettings(settings, specs[0]);

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
        m_individualsPanel.setPreferredSize(m_defaultsPanel.getPreferredSize());
        checkButtonStatus();
    }

    /**
     * {@inheritDoc}
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
    
    /** Panel hosting the individual panels. It implements {@link Scrollable}
     * to allow for correct jumping to the next enclosed panel. It allows
     * overwrites getPreferredSize() to return the sum of all individual
     * heights. 
     */
    private static class IndividualsPanel extends JPanel implements Scrollable {
        
        /** Set box layout. */
        public IndividualsPanel() {
            BoxLayout layout = new BoxLayout(this, BoxLayout.Y_AXIS);
            setLayout(layout);
        }

        /** {@inheritDoc} */
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize(); 
        }

        /** {@inheritDoc} */
        public int getScrollableBlockIncrement(final Rectangle visibleRect, 
                final int orientation, final int direction) {
            int rh = getComponentCount() > 0 
                ? getComponent(0).getHeight() : 0;
            return (rh > 0) ? Math.max(rh, (visibleRect.height / rh) * rh) 
                    : visibleRect.height;
        }

        /** {@inheritDoc} */
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** {@inheritDoc} */
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        /** {@inheritDoc} */
        public int getScrollableUnitIncrement(final Rectangle visibleRect, 
                final int orientation, final int direction) {
            return getComponentCount() > 0 ? getComponent(0).getHeight() : 100;
        }
        
        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredSize() {
            int height = 0;
            for (Component c : getComponents()) {
                Dimension h = c.getPreferredSize();
                height += h.height;
            }
            int width = super.getPreferredSize().width;
            return new Dimension(width, height);
        }
        
    }
}
