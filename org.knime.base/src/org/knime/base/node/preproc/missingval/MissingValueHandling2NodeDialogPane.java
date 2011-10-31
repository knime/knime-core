/*
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.util.ListModelFilterUtils;
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
public class MissingValueHandling2NodeDialogPane extends NodeDialogPane {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(MissingValueHandling2NodeDialogPane.class);

    private final JList m_colList;
    private final DefaultListModel m_colListModel;

    private final JPanel m_defaultsPanel;
    private final JPanel m_individualsPanel;
    
    private final JButton m_addButton;

    /**
     * Constructs new dialog with an appropriate dialog title.
     */
    public MissingValueHandling2NodeDialogPane() {
        super();
        // Default handling, first tab
        m_defaultsPanel = new JPanel(new GridLayout(0, 1));
        addTab("Default", m_defaultsPanel);

        // Individual Handling, second tab
        m_colListModel = new DefaultListModel();
        m_colList = new JList(m_colListModel);
        m_colList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
                    if (!m_addButton.isEnabled()) {
                        return;
                    }
                    DataColumnSpec selected = 
                        (DataColumnSpec)m_colList.getSelectedValue();
                    onAdd(selected);
                }
            } 
        });
        m_colList.setCellRenderer(new DataColumnSpecListCellRenderer() {
            /** {@inheritDoc} */
            @Override
            public Component getListCellRendererComponent(final JList list,
                    final Object value, final int index, 
                    final boolean isSelected, final boolean cellHasFocus) {
                final Component comp = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    return comp;
                }
                final DataColumnSpec cspec = (DataColumnSpec) value;
                final Component[] c = m_individualsPanel.getComponents();
                for (int i = 0; i < c.length; i++) {
                    MissingValueHandling2Panel p = (MissingValueHandling2Panel)c[i];
                    if (p.getSettings().isMetaConfig()) {
                        continue;
                    }
                    final List<String> names = Arrays.asList(
                            p.getSettings().getNames());
                    if (names.contains(cspec.getName())) {
                        comp.setEnabled(false);
                    }
                }
                return comp; 
            } 
        });
        
        final JTextField searchField = new JTextField(8);
        final ActionListener actionListener = new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                int[] searchHits = ListModelFilterUtils.getAllSearchHits(
                        m_colList, searchField.getText());
                m_colList.clearSelection();
                if (searchHits.length == 0) {
                    // nothing to select
                    return;
                }
                final ArrayList<Integer> hitList = new ArrayList<Integer>(); 
                for (int i = 0; i < searchHits.length; i++) {
                    int hit = searchHits[i];
                    DataColumnSpec cspec = (DataColumnSpec) 
                            m_colListModel.getElementAt(hit);
                    final Component[] c = m_individualsPanel.getComponents();
                    for (int j = 0; j < c.length; j++) {
                        final MissingValueHandling2Panel p = (MissingValueHandling2Panel) c[j];
                        if (p.getSettings().isMetaConfig()) {
                            continue;
                        }
                        final List<String> names = Arrays.asList(
                                p.getSettings().getNames());
                        if (names.contains(cspec.getName())) {
                            // no hit: item is already in use
                            hit = -1;
                            break;
                        }
                    }
                    if (hit >= 0) { // only add element here when enabled
                        hitList.add(hit);
                    }
                }
                if (hitList.size() > 0) {
                    final int[] hits = new int[hitList.size()];
                    for (int i = 0; i < hits.length; i++) {
                        hits[i] = hitList.get(i);
                    }
                    m_colList.setSelectedIndices(hits);
                    m_colList.scrollRectToVisible(
                            m_colList.getCellBounds(hits[0], hits[0]));
                }
            }
        };
        searchField.addActionListener(actionListener);
        final JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder(
                " Column Search "));
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        final JPanel colPanel = new JPanel(new BorderLayout());
        colPanel.add(searchPanel, BorderLayout.NORTH);
        colPanel.add(new JScrollPane(m_colList), BorderLayout.CENTER);
        final JPanel tabPanel = new JPanel(new BorderLayout());
        tabPanel.add(colPanel, BorderLayout.CENTER);        
        
        m_individualsPanel = new IndividualsPanel();
        JScrollPane scroller = new JScrollPane(m_individualsPanel);
        tabPanel.add(scroller, BorderLayout.EAST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        m_addButton = new JButton("Add");
        m_addButton.addActionListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void actionPerformed(final ActionEvent e) {
                final Object[] selectedCols = m_colList.getSelectedValues();
                final List<DataColumnSpec> selectList = 
                        new ArrayList<DataColumnSpec>();
                for (Object o : selectedCols) {
                    final DataColumnSpec dcs = (DataColumnSpec) o;
                    selectList.add(dcs);
                }
                onAdd(selectList);
            }
        });
        buttonPanel.add(m_addButton);
        tabPanel.add(buttonPanel, BorderLayout.SOUTH);
        addTab("Individual", tabPanel);
    }

    /** Enables/disables the button according to list selection. */
    private void checkButtonStatus() {
        if (m_colList.isSelectionEmpty()) {
            m_addButton.setEnabled(false);
        } else {
            final Object[] selectedCols = m_colList.getSelectedValues();
            // at least one item is selected, get its type and compare it
            // with all the other selected elements
            final DataType type = ((DataColumnSpec) selectedCols[0]).getType();
            for (Object o : selectedCols) {
                final DataColumnSpec dcs = (DataColumnSpec) o;
                if (!type.equals(dcs.getType())) {
                    m_addButton.setEnabled(false);
                    return;
                }
            }
            final Component[] c = m_individualsPanel.getComponents();
            for (int i = 0; i < c.length; i++) {
                MissingValueHandling2Panel p = (MissingValueHandling2Panel)c[i];
                if (p.getSettings().isMetaConfig()) {
                    continue;
                }
                final List<String> names = Arrays.asList(
                        p.getSettings().getNames());
                for (Object o : selectedCols) {
                    final DataColumnSpec dcs = (DataColumnSpec) o;
                    if (names.contains(dcs.getName())) {
                        m_addButton.setEnabled(false);
                        return;
                    }
                }
            }
            m_addButton.setEnabled(true);
        }
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
        MissingValueHandling2ColSetting[] defaults = new MissingValueHandling2ColSetting[0];
        MissingValueHandling2ColSetting[] individuals = new MissingValueHandling2ColSetting[0];
        defaults = MissingValueHandling2ColSetting.loadMetaColSettings(settings, specs[0]);
        individuals = MissingValueHandling2ColSetting.loadIndividualColSettings(settings, specs[0]);

        m_defaultsPanel.removeAll();
        for (int i = 0; i < defaults.length; i++) {
            m_defaultsPanel.add(new MissingValueHandling2Panel(
                    defaults[i], (DataColumnSpec) null));
        }
        m_individualsPanel.removeAll();
        for (int i = 0; i < individuals.length; i++) {
            String[] names = individuals[i].getNames();
            ArrayList<DataColumnSpec>colSpecs = new ArrayList<DataColumnSpec>();
            for (int j = 0; j < names.length; j++) {
                final DataColumnSpec cspec = specs[0].getColumnSpec(names[j]);
                if (cspec == null) {
                    LOGGER.debug("No such column in spec: " + names[j]);
                } else { 
                    colSpecs.add(cspec);
                }
            }
            if (!colSpecs.isEmpty()) {
                names = new String[colSpecs.size()];
                for (int j = 0; j < names.length; j++) {
                    names[j] = colSpecs.get(j).getName();
                }
                individuals[i].setNames(names);
                addToIndividualPanel(new MissingValueHandling2Panel(individuals[i], 
                    colSpecs.toArray(new DataColumnSpec[0])));
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
        MissingValueHandling2ColSetting[] defaults = new MissingValueHandling2ColSetting[cs.length];
        for (int i = 0; i < defaults.length; i++) {
            defaults[i] = ((MissingValueHandling2Panel)cs[i]).getSettings();
        }
        cs = m_individualsPanel.getComponents();
        MissingValueHandling2ColSetting[] individuals = new MissingValueHandling2ColSetting[cs.length];
        for (int i = 0; i < individuals.length; i++) {
            individuals[i] = ((MissingValueHandling2Panel)cs[i]).getSettings();
        }
        MissingValueHandling2ColSetting.saveMetaColSettings(defaults, settings);
        MissingValueHandling2ColSetting.saveIndividualsColSettings(individuals, settings);
    }

    private void onAdd(final DataColumnSpec spec) {
        if (spec == null) {
            return;
        }
        MissingValueHandling2Panel p = new MissingValueHandling2Panel(spec);
        addToIndividualPanel(p);
        checkButtonStatus();
    }
    
    private void onAdd(final List<DataColumnSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            return;
        }
        MissingValueHandling2Panel p = new MissingValueHandling2Panel(specs);
        addToIndividualPanel(p);
        checkButtonStatus();
    }

    private void removeFromIndividualPanel(final MissingValueHandling2Panel panel) {
        m_individualsPanel.remove(panel);
        m_individualsPanel.revalidate();
        m_individualsPanel.repaint();
        m_colList.revalidate();
        m_colList.repaint();
        checkButtonStatus();
    }

    private void addToIndividualPanel(final MissingValueHandling2Panel panel) {
        panel.addPropertyChangeListener(MissingValueHandling2Panel.REMOVE_ACTION,
                new PropertyChangeListener() {
                    /** {@inheritDoc} */
                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {
                        removeFromIndividualPanel((MissingValueHandling2Panel)evt
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
