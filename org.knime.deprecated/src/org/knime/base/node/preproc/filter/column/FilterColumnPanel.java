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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.preproc.filter.column;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.util.ListModelFilterUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;
import org.knime.core.node.util.filter.DataColumnSpecFilterPanel;


/** Copy of the column filter panel with additional enforce exclude/include
 * buttons. This copy is pending API, the functionality will eventually be moved
 * to the public 
 * {@link org.knime.core.node.util.ColumnFilterPanel}.
 * @author Thomas Gabriel, KNIME.com AG, Zurich, Switzerland
 * @deprecated use {@link DataColumnSpecFilterPanel}
 */
@Deprecated
class FilterColumnPanel extends JPanel {
    
    
    public enum SelectionOption {
        EnforceInclusion,
        EnforceExclusion;
        
        public static SelectionOption parse(final String str) 
            throws InvalidSettingsException {
            try {
                return SelectionOption.valueOf(str);
            } catch (Exception iae) {
                throw new InvalidSettingsException(
                        "Can't parse selection option: " + str, iae);
            }
        }
        
        public static SelectionOption parse(final String str, 
                final SelectionOption defaultOption) {
            try {
                return SelectionOption.parse(str);
            } catch (InvalidSettingsException iae) {
                return defaultOption;
            }
        }
    }

    /** Settings key for the excluded columns. */
    public static final String INCLUDED_COLUMNS = "included_columns";

    /** Settings key for the excluded columns. */
    public static final String EXCLUDED_COLUMNS = "excluded_columns";

    /** Include list. */
    private final JList m_inclList;

    /** Include model. */
    private final DefaultListModel m_inclMdl;

    /** Exclude list. */
    private final JList m_exclList;

    /** Exclude model. */
    private final DefaultListModel m_exclMdl;

    /** Highlight all search hits in the include model. */
    private final JCheckBox m_markAllHitsIncl;

    /** Highlight all search hits in the exclude model. */
    private final JCheckBox m_markAllHitsExcl;

    /** Radio button for the exclusion option. */
    private final JRadioButton m_enforceExclusion;
    
    /** Radio button for the inclusion option. */
    private final JRadioButton m_enforceInclusion;

    /** Remove all button. */
    private final JButton m_remAllButton;

    /** Remove button. */
    private final JButton m_remButton;

    /** Add all button. */
    private final JButton m_addAllButton;

    /** Add button. */
    private final JButton m_addButton;

    /** Search Field in include list. */
    private final JTextField m_searchFieldIncl;

    /** Search Button for include list. */
    private final JButton m_searchButtonIncl;

    /** Search Field in exclude list. */
    private final JTextField m_searchFieldExcl;

    /** Search Button for exclude list. */
    private final JButton m_searchButtonExcl;

    /** List of DataCellColumnSpecss to keep initial ordering of DataCells. */
    private final LinkedHashSet<DataColumnSpec> m_order =
        new LinkedHashSet<DataColumnSpec>();

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;

    private final HashSet<DataColumnSpec> m_hideColumns =
        new HashSet<DataColumnSpec>();

    private List<ChangeListener>m_listeners;

    /**
     * Line border for include columns.
     */
    private static final Border INCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /**
     * Line border for exclude columns.
     */
    private static final Border EXCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     */
    public FilterColumnPanel() {
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(20));

        m_addButton = new JButton("add >>");
        m_addButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_addAllButton = new JButton("add all >>");
        m_addAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remButton = new JButton("<< remove");
        m_remButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remAllButton = new JButton("<< remove all");
        m_remAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(20));
        buttonPan.add(Box.createGlue());

        // include list
        m_inclMdl = new DefaultListModel();
        m_inclList = new JList(m_inclMdl);
        m_inclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_inclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onRemIt();
                    me.consume();
                }
            }
        });
        final JScrollPane jspIncl = new JScrollPane(m_inclList);
        jspIncl.setMinimumSize(new Dimension(150, 155));

        m_searchFieldIncl = new JTextField(8);
        m_searchButtonIncl = new JButton("Search");
        ActionListener actionListenerIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ListModelFilterUtils.onSearch(m_inclList, m_inclMdl, 
                        m_searchFieldIncl.getText(), 
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_searchFieldIncl.addActionListener(actionListenerIncl);
        m_searchButtonIncl.addActionListener(actionListenerIncl);
        JPanel inclSearchPanel = new JPanel(new BorderLayout());
        inclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        inclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        inclSearchPanel.add(m_searchFieldIncl, BorderLayout.CENTER);
        inclSearchPanel.add(m_searchButtonIncl, BorderLayout.EAST);
        m_markAllHitsIncl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_inclList.clearSelection();
                ListModelFilterUtils.onSearch(m_inclList, m_inclMdl, 
                        m_searchFieldIncl.getText(), 
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_markAllHitsIncl.addActionListener(actionListenerAllIncl);
        inclSearchPanel.add(m_markAllHitsIncl, BorderLayout.PAGE_END);
        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(
                INCLUDE_BORDER, " Include ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(jspIncl, BorderLayout.CENTER);

        // exclude list
        m_exclMdl = new DefaultListModel();
        m_exclList = new JList(m_exclMdl);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        m_exclList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent me) {
                if (me.getClickCount() == 2) {
                    onAddIt();
                    me.consume();
                }
            }
        });
        setListCellRenderer(new DataColumnSpecListCellRenderer());
        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setMinimumSize(new Dimension(150, 155));

        m_searchFieldExcl = new JTextField(8);
        m_searchButtonExcl = new JButton("Search");
        ActionListener actionListenerExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                ListModelFilterUtils.onSearch(m_exclList, m_exclMdl, 
                        m_searchFieldExcl.getText(), 
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_searchFieldExcl.addActionListener(actionListenerExcl);
        m_searchButtonExcl.addActionListener(actionListenerExcl);
        JPanel exclSearchPanel = new JPanel(new BorderLayout());
        exclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        exclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        exclSearchPanel.add(m_searchFieldExcl, BorderLayout.CENTER);
        exclSearchPanel.add(m_searchButtonExcl, BorderLayout.EAST);
        m_markAllHitsExcl = new JCheckBox("Select all search hits");
        ActionListener actionListenerAllExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_exclList.clearSelection();
                ListModelFilterUtils.onSearch(m_exclList, m_exclMdl, 
                        m_searchFieldExcl.getText(), 
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_markAllHitsExcl.addActionListener(actionListenerAllExcl);
        exclSearchPanel.add(m_markAllHitsExcl, BorderLayout.PAGE_END);
        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(
                EXCLUDE_BORDER, " Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        JPanel buttonPan2 = new JPanel(new GridLayout());
        Border border = BorderFactory.createTitledBorder(" Select ");
        buttonPan2.setBorder(border);
        buttonPan2.add(buttonPan);

        // add force incl/excl buttons
        final ButtonGroup forceGroup = new ButtonGroup();
        m_enforceInclusion = new JRadioButton("Enforce inclusion");
        m_enforceInclusion.setToolTipText(
                "Force the set of included columns to stay the same.");
        forceGroup.add(m_enforceInclusion);
        includePanel.add(m_enforceInclusion, BorderLayout.SOUTH);
        m_enforceExclusion = new JRadioButton("Enforce exclusion");
        m_enforceExclusion.setToolTipText(
                "Force the set of excluded columns to stay the same.");
        forceGroup.add(m_enforceExclusion);
        m_enforceExclusion.doClick();
        excludePanel.add(m_enforceExclusion, BorderLayout.SOUTH);

        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        super.setLayout(new GridLayout(1, 1));
        super.add(center);
    }

    /**
     * Enables or disables all components on this panel.
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        m_searchFieldIncl.setEnabled(enabled);
        m_searchButtonIncl.setEnabled(enabled);
        m_searchFieldExcl.setEnabled(enabled);
        m_searchButtonExcl.setEnabled(enabled);
        m_inclList.setEnabled(enabled);
        m_exclList.setEnabled(enabled);
        m_markAllHitsIncl.setEnabled(enabled);
        m_markAllHitsExcl.setEnabled(enabled);
        m_remAllButton.setEnabled(enabled);
        m_remButton.setEnabled(enabled);
        m_addAllButton.setEnabled(enabled);
        m_addButton.setEnabled(enabled);
        m_enforceInclusion.setEnabled(enabled);
        m_enforceExclusion.setEnabled(enabled);
    }

    /**
     * Adds a listener which gets informed whenever the column filtering
     * changes.
     * @param listener the listener
     */
    public void addChangeListener(final ChangeListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList<ChangeListener>();
        }
        m_listeners.add(listener);
    }

    /**
     * Removes the given listener from this filter column panel.
     * @param listener the listener.
     */
    public void removeChangeListener(final ChangeListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Removes all column filter change listener.
     */
    public void removeAllColumnFilterChangeListener() {
        if (m_listeners != null) {
            m_listeners.clear();
        }
    }

    private void fireFilteringChangedEvent() {
        if (m_listeners != null) {
            for (ChangeListener listener : m_listeners) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }

    /**
     * Called by the 'remove >>' button to exclude the selected elements from
     * the include list.
     */
    private void onRemIt() {
        // add all selected elements from the include to the exclude list
        Object[] o = m_inclList.getSelectedValues();
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        for (Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_inclMdl.removeElement(o[i]);
        }
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }



    /**
     * Called by the 'remove >>' button to exclude all elements from the include
     * list.
     */
    private void onRemAll() {
        boolean changed = m_inclMdl.elements().hasMoreElements();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (!m_hideColumns.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Called by the '<< add' button to include the selected elements from the
     * exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        Object[] o = m_exclList.getSelectedValues();
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        for (Enumeration<?> e = m_inclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_exclMdl.removeElement(o[i]);
        }
        m_inclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the
     * exclude list.
     */
    private void onAddAll() {
        boolean changed = m_exclMdl.elements().hasMoreElements();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (!m_hideColumns.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     * @param spec the spec to retrieve the column names from
     * @param selOption selection option {@link SelectionOption}
     * @param list the list of columns to exclude or include depending on the
     *        inclusion/exclusion option that need to be set before
     */
    public void update(final DataTableSpec spec, 
            final SelectionOption selOption, 
            final Collection<String> list) {
        assert (spec != null && list != null);
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_hideColumns.clear();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            final String c = cSpec.getName();
            m_order.add(cSpec);
            switch (selOption) {
            case EnforceExclusion:
                if (list.contains(c)) {
                    m_exclMdl.addElement(cSpec);
                } else {
                    m_inclMdl.addElement(cSpec);
                }
                break;
            case EnforceInclusion:
                if (list.contains(c)) {
                    m_inclMdl.addElement(cSpec);
                } else {
                    m_exclMdl.addElement(cSpec);
                }
                break;
            }
        }
        switch (selOption) {
        case EnforceExclusion:
            m_enforceExclusion.doClick();
            break;
        case EnforceInclusion:
            m_enforceInclusion.doClick();
            break;
        }
        repaint();
    }
    
    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     */
    public Set<String> getExcludedColumnSet() {
        return ListModelFilterUtils.getColumnList(m_exclMdl);
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public Set<String> getIncludedColumnSet() {
        return ListModelFilterUtils.getColumnList(m_inclMdl);
    }
    
    public SelectionOption getSelectionOption() {
        if (m_enforceExclusion.isSelected()) {
            return SelectionOption.EnforceExclusion;
        } else {
            return SelectionOption.EnforceInclusion;
        }
    }
    
    /**
     * Returns the data type for the given cell retrieving it from the initial
     * {@link DataTableSpec}. If this name could not found, return
     * <code>null</code>.
     *
     * @param name the column name to get the data type for
     * @return the data type or <code>null</code>
     */
    public DataType getType(final String name) {
        for (DataColumnSpec spec : m_order) {
            if (spec.getName().equals(name)) {
                return spec.getType();
            }
        }
        return null;
    }


    /**
     * Set the renderer that is used for both list in this panel.
     *
     * @param renderer the new renderer being used
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     */
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        m_inclList.setCellRenderer(renderer);
        m_exclList.setCellRenderer(renderer);
    }

    /**
     * Removes the given columns form either include or exclude list and
     * notifies all listeners. Does not throw an exception if the argument
     * contains <code>null</code> elements or is not contained in any of the
     * lists.
     *
     * @param columns the columns to remove
     */
    public final void hideColumns(final DataColumnSpec... columns) {
        boolean changed = false;
        for (DataColumnSpec column : columns) {
            if (m_inclMdl.contains(column)) {
                m_hideColumns.add(column);
                changed |= m_inclMdl.removeElement(column);
            } else if (m_exclMdl.contains(column)) {
                m_hideColumns.add(column);
                changed |= m_exclMdl.removeElement(column);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Re-adds all remove/hidden columns to the exclude list.
     */
    public final void resetHiding() {
        if (m_hideColumns.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideColumns);
        for (Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        m_hideColumns.clear();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public final void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public final void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }

    /**
     * Setter for the original "Remove All" button.
     *
     * @param text the new button title
     */
    public void setRemoveAllButtonText(final String text) {
        m_remAllButton.setText(text);
    }

    /**
     * Setter for the original "Add All" button.
     *
     * @param text the new button title
     */
    public void setAddAllButtonText(final String text) {
        m_addAllButton.setText(text);
    }

    /**
     * Setter for the original "Remove" button.
     *
     * @param text the new button title
     */
    public void setRemoveButtonText(final String text) {
        m_remButton.setText(text);
    }

    /**
     * Setter for the original "Add" button.
     *
     * @param text the new button title
     */
    public void setAddButtonText(final String text) {
        m_addButton.setText(text);
    }
}
