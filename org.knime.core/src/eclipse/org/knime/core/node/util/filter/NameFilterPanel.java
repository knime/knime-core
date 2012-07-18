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
package org.knime.core.node.util.filter;

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
import java.util.Collections;
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

import org.knime.core.data.util.ListModelFilterUtils;
import org.knime.core.node.util.filter.NameFilterConfiguration.EnforceOption;

/** Name filter panel with additional enforce include/exclude radio buttons.
 *
 * Thomas Gabriel, KNIME.com AG, Zurich, Switzerland
 * @since 2.6
 *
 * @param <T> the instance T this object is parametrized on
 */
@SuppressWarnings("serial")
public abstract class NameFilterPanel<T> extends JPanel {

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

    /** List of T elements to keep initial ordering of names. */
    private final LinkedHashSet<T> m_order =
        new LinkedHashSet<T>();

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;

    private final HashSet<T> m_hideNames = new HashSet<T>();

    private List<ChangeListener>m_listeners;

    /** Line border for include names. */
    private static final Border INCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(0, 221, 0), 2);

    /** Line border for exclude names. */
    private static final Border EXCLUDE_BORDER =
        BorderFactory.createLineBorder(new Color(240, 0, 0), 2);

    /** The filter used to filter out/in valid elements. */
    private InputFilter<T> m_filter;

    /**
     * Creates a panel allowing the user to select elements.
     */
    protected NameFilterPanel() {
        this(false, null);
    }

    /**
     * Creates a new filter  panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * @param showSelectionListsOnly if set, the component shows only the basic
     * include/exclude selection panel - no additional search boxes, force-include-options, etc.
     */
    protected NameFilterPanel(final boolean showSelectionListsOnly) {
        this(showSelectionListsOnly, null);
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter. Additionally a {@link InputFilter} can be specified, based on
     * which the shown items are shown or not. The filter can be <code>null
     * </code>, in which case it is simply not used at all.
     * @param showSelectionListsOnly if set, the component shows only the basic
     * include/exclude selection panel - no additional search boxes, force-include-options, etc.
     * @param filter A filter that specifies which items are shown in the
     * panel (and thus are possible to include or exclude) and which are not
     * shown.
     */
    protected NameFilterPanel(final boolean showSelectionListsOnly,
            final InputFilter<T> filter) {
        super(new GridLayout(1, 1));
        m_filter = filter;

        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(Box.createVerticalStrut(20));

        m_addButton = new JButton("add >>");
        m_addButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addButton);
        m_addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_addAllButton = new JButton("add all >>");
        m_addAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_addAllButton);
        m_addAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remButton = new JButton("<< remove");
        m_remButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remButton);
        m_remButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonPan.add(Box.createVerticalStrut(25));

        m_remAllButton = new JButton("<< remove all");
        m_remAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(m_remAllButton);
        m_remAllButton.addActionListener(new ActionListener() {
            @Override
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
            @Override
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
            @Override
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

        // set renderer for items in the in- and exclude list
        m_inclList.setCellRenderer(getListCellRenderer());
        m_exclList.setCellRenderer(getListCellRenderer());

        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setMinimumSize(new Dimension(150, 155));

        m_searchFieldExcl = new JTextField(8);
        m_searchButtonExcl = new JButton("Search");
        ActionListener actionListenerExcl = new ActionListener() {
            @Override
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
            @Override
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
        m_enforceInclusion = new JRadioButton("Enforce inclusion");
        m_enforceExclusion = new JRadioButton("Enforce exclusion");
        if (!showSelectionListsOnly) {
            final ButtonGroup forceGroup = new ButtonGroup();
            m_enforceInclusion.setToolTipText(
                    "Force the set of included names to stay the same.");
            forceGroup.add(m_enforceInclusion);
            includePanel.add(m_enforceInclusion, BorderLayout.SOUTH);
            m_enforceExclusion.setToolTipText(
                    "Force the set of excluded names to stay the same.");
            forceGroup.add(m_enforceExclusion);
            m_enforceExclusion.doClick();
            excludePanel.add(m_enforceExclusion, BorderLayout.SOUTH);
        }

        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        super.add(center);
    }

    /** @return a list cell renderer from items to be rendered in the filer */
    protected abstract ListCellRenderer getListCellRenderer();

    /**
     * Get the a T for the given name.
     * @param name a string to retrieve T for.
     * @return an instance of T
     */
    protected abstract T getTforName(final String name);

    /**
     * Returns the name for the given T.
     * @param t to retrieve the name for
     * @return the name represented by T
     */
    protected abstract String getNameForT(final T t);

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
        for (T c : m_order) {
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
        for (T c : m_order) {
            if (!m_hideNames.contains(c)) {
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
        for (T c : m_order) {
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
        for (T c : m_order) {
            if (!m_hideNames.contains(c)) {
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
     * @param config to be loaded from
     * @param names array of names to be included or excluded (preserve order)
     */
    public void loadConfiguration(final NameFilterConfiguration config,
            final String[] names) {
        final List<String> ins = Arrays.asList(config.getIncludeList());
        final List<String> exs = Arrays.asList(config.getExcludeList());
        this.update(ins, exs, names);
        switch (config.getEnforceOption()) {
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
     * Update this panel with the given include, exclude lists and the array of
     * all possible values.
     * @param ins include list
     * @param exs exclude list
     * @param names all available names
     * @noreference This method is not intended to be referenced by clients.
     * @nooverride This method is not intended to be re-implemented or extended by clients.
     */
    public void update(final List<String> ins, final List<String> exs,
            final String[] names) {
        // clear internal member
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_hideNames.clear();

        for (final String name : names) {
            final T t = getTforName(name);

            // continue if filter is set and current item t is filtered out
            if (m_filter != null) {
                if (!m_filter.include(t)) {
                    continue;
                }
            }

            // if item is not filtered out, add it to include or exclude list
            if (ins.contains(name)) {
                m_inclMdl.addElement(t);
            } else if (exs.contains(name)) {
                m_exclMdl.addElement(t);
            }
            m_order.add(t);
        }
        repaint();
    }

    /**
     * Save this configuration.
     * @param config settings to be saved into
     */
    public void saveConfiguration(final NameFilterConfiguration config) {
        // save enforce option
        if (m_enforceExclusion.isSelected()) {
            config.setEnforceOption(EnforceOption.EnforceExclusion);
        } else {
            config.setEnforceOption(EnforceOption.EnforceInclusion);
        }

        // save include list
        final Set<T> incls = getIncludeList();
        final String[] ins = new String[incls.size()];
        int index = 0;
        for (T t : incls) {
            ins[index++] = getNameForT(t);
        }
        config.setIncludeList(ins);

        // save exclude option
        final Set<T> excls = getExcludeList();
        final String[] exs = new String[excls.size()];
        index = 0;
        for (T t : excls) {
            exs[index++] = getNameForT(t);
        }
        config.setExcludeList(exs);
    }

    /** @return list of all included T's */
    public Set<T> getIncludeList() {
        final Set<T> list = new LinkedHashSet<T>();
        for (int i = 0; i < m_inclMdl.getSize(); i++) {
            @SuppressWarnings("unchecked")
            T t = (T) m_inclMdl.getElementAt(i);
            list.add(t);
        }
        return list;
    }

    /** @return list of all excluded T's */
    public Set<T> getExcludeList() {
        final Set<T> list = new LinkedHashSet<T>();
        for (int i = 0; i < m_exclMdl.getSize(); i++) {
            @SuppressWarnings("unchecked")
            T t = (T) m_exclMdl.getElementAt(i);
            list.add(t);
        }
        return list;
    }

    /**
     * Removes the given columns form either include or exclude list and
     * notifies all listeners. Does not throw an exception if the argument
     * contains <code>null</code> elements or is not contained in any of the
     * lists.
     * @param names a list of names to hide from the filter
     */
    public void hideNames(final T... names) {
        boolean changed = false;
        for (T name : names) {
            if (m_inclMdl.contains(name)) {
                m_hideNames.add(name);
                changed |= m_inclMdl.removeElement(name);
            } else if (m_exclMdl.contains(name)) {
                m_hideNames.add(name);
                changed |= m_exclMdl.removeElement(name);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /** Re-adds all remove/hidden names to the exclude list. */
    public void resetHiding() {
        if (m_hideNames.isEmpty()) {
            return;
        }
        // add all selected elements from the include to the exclude list
        HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideNames);
        for (Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        m_exclMdl.removeAllElements();
        for (T name : m_order) {
            if (hash.contains(name)) {
                m_exclMdl.addElement(name);
            }
        }
        m_hideNames.clear();
    }

    /**
     * Sets the title of the include panel.
     *
     * @param title the new title
     */
    public void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     *
     * @param title the new title
     */
    public void setExcludeTitle(final String title) {
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

    /**
     * Sets the internal used {@link InputFilter} and calls
     * the {@link #update(List, List, String[])} method to update the
     * panel.
     *
     * @param filter the new {@link InputFilter} to use
     */
    public void setNameFilter(final InputFilter<T> filter) {
        m_filter = filter;

        List<String> inclList = new ArrayList<String>(getIncludedNamesAsSet());
        List<String> exclList = new ArrayList<String>(getExcludedNamesAsSet());
        String[] allNames = new String[inclList.size() + exclList.size()];
        for (int i = 0; i < allNames.length; i++) {
            allNames[i] = inclList.get(i);
        }
    }

    /**
     * Returns a set of the names of all included items.
     * @return a set of the names of all included items.
     */
    public Set<String> getIncludedNamesAsSet() {
        Set<T> inclList = getIncludeList();
        Set<String> inclNames = new LinkedHashSet<String>(inclList.size());
        for (T t : inclList) {
            inclNames.add(getNameForT(t));
        }
        return inclNames;
    }

    /**
     * Returns a set of the names of all excluded items.
     * @return a set of the names of all excluded items.
     */
    public Set<String> getExcludedNamesAsSet() {
        Set<T> exclList = getExcludeList();
        Set<String> exclNames = new LinkedHashSet<String>(exclList.size());
        for (T t : exclList) {
            exclNames.add(getNameForT(t));
        }
        return exclNames;
    }

    /**
     * Returns all values include and exclude in its original order they have added to this panel.
     * @return a set of string containing all values from the in- and exclude list
     */
    public Set<String> getAllValues() {
        final Set<String> set = new LinkedHashSet<String>();
        for (T t : m_order) {
            set.add(getNameForT(t));
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Returns all objects T in its original order.
     * @return a set of T objects retrieved from the in- and exclude list
     */
    public Set<T> getAllValuesT() {
        return Collections.unmodifiableSet(m_order);
    }
}

