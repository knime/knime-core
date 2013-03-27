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
            /** {@inheritDoc} */
            @Override
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
        tabPanel.add(new JScrollPane(m_colList), BorderLayout.CENTER);
        m_individualsPanel = new IndividualsPanel();
        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        tabPanel.add(scroller, BorderLayout.EAST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_addButton = new JButton("Add");
        m_addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
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
        defaults = ColSetting.loadMetaColSetting(settings, specs[0]);
        individuals = ColSetting.loadIndividualColSetting(settings, specs[0]);

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
        ColSetting.saveMetaColSetting(defaults, settings);
        ColSetting.saveIndividualsColSetting(individuals, settings);
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
                    /** {@inheritDoc} */
                    @Override
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
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize(); 
        }

        /** {@inheritDoc} */
        @Override
        public int getScrollableBlockIncrement(final Rectangle visibleRect, 
                final int orientation, final int direction) {
            int rh = getComponentCount() > 0 
                ? getComponent(0).getHeight() : 0;
            return (rh > 0) ? Math.max(rh, (visibleRect.height / rh) * rh) 
                    : visibleRect.height;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
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
