/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.core.node.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;


/**
 * Panel is used to select/filter a certain number of columns.
 *
 * <p>
 * You can add a property change listener to this class that is notified when
 * the include list changes.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
public class ColumnFilterPanel extends JPanel {

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

    /** Checkbox that enabled to keep all column in include list. */
    private final JCheckBox m_keepAllBox;

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

    /**
     * Class that filters all columns based on a given set of compatible
     * <code>DataValue</code> classes.
     */
    public static class ValueClassFilter implements ColumnFilter {
        /**
         * Show only columns of types that are compatible to one of theses
         * classes.
         */
        private final Class<? extends DataValue>[] m_filterClasses;
        /**
         * Creates a new value class filter.
         * @param filterValueClasses all classes that are compatible with
         *        the type allowed in {@link #isValidColumn(DataColumnSoec)}.
         */
        public ValueClassFilter(
                final Class<? extends DataValue>... filterValueClasses) {
            if (filterValueClasses == null || filterValueClasses.length == 0) {
                throw new NullPointerException("Classes must not be null");
            }
            List<Class<? extends DataValue>> list = Arrays
                    .asList(filterValueClasses);
            if (list.contains(null)) {
                throw new NullPointerException("List of value classes must not "
                        + "contain null elements.");
            }
            m_filterClasses = filterValueClasses;
        }
        /**
         * Checks if the given column type is included in the list of allowed
         * types. If the list is empty, all types are valid.
         * @param cspec {@link ColumnFilterPanel} checked
         * @return true, if given column should be visible in column filter
         */
        public final boolean includeColumn(final DataColumnSpec cspec) {
            for (Class<? extends DataValue> cl : m_filterClasses) {
                if (cspec.getType().isCompatible(cl)) {
                    return true;
                }
            }
            return false;
        }
        /** {@inheritDoc} */
        @Override
        public String allFilteredMsg() {
            return "No columns compatible with the specific column types.";
        }
    };

    /** The filter used to filter out/in valid column types. */
    private final ColumnFilter m_filter;

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
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String[])
     */
    @SuppressWarnings("unchecked")
    public ColumnFilterPanel() {
        this(DataValue.class);
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     *
     * @param filterValueClasses an array of type {@link DataValue} classes only
     *            allowed for selection. Will be check during update
     * @param showKeepAllBox true, if an check box to keep all columns is shown
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    public ColumnFilterPanel(final boolean showKeepAllBox,
            final Class<? extends DataValue>... filterValueClasses) {
        this(showKeepAllBox, new ValueClassFilter(filterValueClasses));
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     *
     * @param filterValueClasses an array of type {@link DataValue} classes only
     *            allowed for selection. Will be check during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    public ColumnFilterPanel(
            final Class<? extends DataValue>... filterValueClasses) {
        this(new ValueClassFilter(filterValueClasses));
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     *
     * @param filter specifies valid column values. Will be check during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    public ColumnFilterPanel(final ColumnFilter filter) {
        this(false, filter);
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * @param showKeepAllBox true, if an check box to keep all columns is shown
     * @param filter specifies valid column values. Will be check during update
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String...)
     */
    public ColumnFilterPanel(final boolean showKeepAllBox,
            final ColumnFilter filter) {
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
                onSearch(m_inclList, m_inclMdl, m_searchFieldIncl,
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
                onSearch(m_inclList, m_inclMdl, m_searchFieldIncl,
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
                onSearch(m_exclList, m_exclMdl, m_searchFieldExcl,
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
                onSearch(m_exclList, m_exclMdl, m_searchFieldExcl,
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

        // add 'keep all' checkbox
        if (showKeepAllBox) {
            m_keepAllBox = new JCheckBox("Always include all columns");
            m_keepAllBox.setToolTipText("If the set of input columns changes, "
                    + "all columns stay included.");
            m_keepAllBox.addItemListener(new ItemListener() {
                /** {@inheritDoc} */
                @Override
                public void itemStateChanged(final ItemEvent ie) {
                    boolean keepAll = m_keepAllBox.isSelected();
                    if (keepAll) {
                        onAddAll();
                    }
                    enabledComponents(!keepAll);
                }
            });
        } else {
            m_keepAllBox = null;
        }

        // adds include, button, exclude component
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        JPanel all = new JPanel();
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
        all.add(center);
        if (m_keepAllBox != null) {
            JPanel keepAllPanel = new JPanel(
                    new FlowLayout(FlowLayout.RIGHT, 0, 0));
            keepAllPanel.add(m_keepAllBox);
            all.add(keepAllPanel);
        }
        super.setLayout(new GridLayout(1, 1));
        super.add(all);
    }

    /**
     * Enables or disables all components on this panel.
     * {@inheritDoc}
     */
    @Override
    public void setEnabled(final boolean enabled) {
        super.setEnabled(enabled);
        if (m_keepAllBox != null) {
            m_keepAllBox.setEnabled(enabled);
        }
        boolean newEnabled = enabled && !isKeepAllSelected();
        enabledComponents(newEnabled);
    }

    private void enabledComponents(final boolean newEnabled) {
        m_searchFieldIncl.setEnabled(newEnabled);
        m_searchButtonIncl.setEnabled(newEnabled);
        m_searchFieldExcl.setEnabled(newEnabled);
        m_searchButtonExcl.setEnabled(newEnabled);
        m_inclList.setEnabled(newEnabled);
        m_exclList.setEnabled(newEnabled);
        m_markAllHitsIncl.setEnabled(newEnabled);
        m_markAllHitsExcl.setEnabled(newEnabled);
        m_remAllButton.setEnabled(newEnabled);
        m_remButton.setEnabled(newEnabled);
        m_addAllButton.setEnabled(newEnabled);
        m_addButton.setEnabled(newEnabled);
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
     * If the keep all box is visible and is selected.
     * @return true, if keep all columns check box is selected, otherwise false
     */
    public final boolean isKeepAllSelected() {
        return m_keepAllBox != null && m_keepAllBox.isSelected();
    }

    /**
     * Sets a new selection for the keep all columns box.
     * @param select true, if the box should be selected
     */
    public final void setKeepAllSelected(final boolean select) {
        if (m_keepAllBox != null) {
            m_keepAllBox.setSelected(select);
              enabledComponents(m_keepAllBox.isEnabled() && !select);
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
        setKeepAllSelected(false);
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
        setKeepAllSelected(false);
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
     * include and exclude list. The include list will contain all column names
     * from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param exclude the flag if <code>cells</code> contains the columns to
     *            exclude (otherwise include).
     * @param cells an array of data cells to either in- or exclude.
     */
    public void update(final DataTableSpec spec, final boolean exclude,
            final String... cells) {
        this.update(spec, exclude, Arrays.asList(cells));
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param exclude the flag if <code>list</code> contains the columns to
     *            exclude otherwise include
     * @param list the list of columns to exclude or include
     */
    public void update(final DataTableSpec spec, final boolean exclude,
            final Collection<String> list) {
        assert (spec != null && list != null);
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        m_hideColumns.clear();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            if (!m_filter.includeColumn(cSpec)) {
                continue;
            }
            final String c = cSpec.getName();
            m_order.add(cSpec);
            if (isKeepAllSelected()) {
                m_inclMdl.addElement(cSpec);
            } else {
                if (exclude) {
                    if (list.contains(c)) {
                        m_exclMdl.addElement(cSpec);
                    } else {
                        m_inclMdl.addElement(cSpec);
                    }
                } else {
                    if (list.contains(c)) {
                        m_inclMdl.addElement(cSpec);
                    } else {
                        m_exclMdl.addElement(cSpec);
                    }
                }
            }
        }
        repaint();
    }

    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     * @deprecated Use {@link #getExcludedColumnSet()} instead
     */
    @Deprecated
    public Set<String> getExcludedColumnList() {
        return getExcludedColumnSet();
    }

    /**
     * Returns all columns from the exclude list.
     *
     * @return a set of all columns from the exclude list
     */
    public Set<String> getExcludedColumnSet() {
        return getColumnList(m_exclMdl);
    }


    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     * @deprecated Use {@link #getIncludedColumnSet()} instead
     */
    @Deprecated
    public Set<String> getIncludedColumnList() {
        return getIncludedColumnSet();
    }

    /**
     * Returns all columns from the include list.
     *
     * @return a list of all columns from the include list
     */
    public Set<String> getIncludedColumnSet() {
        return getColumnList(m_inclMdl);
    }


    /**
     * Helper for the get***ColumnList methods.
     *
     * @param model The list from which to retrieve the elements
     */
    private static Set<String> getColumnList(final ListModel model) {
        final Set<String> list = new LinkedHashSet<String>();
        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            String cell = ((DataColumnSpec)o).getName();
            list.add(cell);
        }
        return list;
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
     * This method is called when the user wants to search the given
     * {@link JList} for the text of the given {@link JTextField}.
     *
     * @param list the list to search in
     * @param model the list model on which the list is based on
     * @param searchField the text field with the text to search for
     * @param markAllHits if set to <code>true</code> the method will mark all
     *            occurrences of the given search text in the given list. If set
     *            to <code>false</code> the method will mark the next
     *            occurrences of the search text after the current marked list
     *            element.
     */
    private static void onSearch(final JList list,
            final DefaultListModel model, final JTextField searchField,
            final boolean markAllHits) {
        if (list == null || model == null || searchField == null) {
            return;
        }
        final String searchStr = searchField.getText().trim();
        if (model.isEmpty() || searchStr.equals("")) {
            list.clearSelection();
            return;
        }
        if (markAllHits) {
            int[] searchHits = getAllSearchHits(list, searchStr);
            list.clearSelection();
            if (searchHits.length > 0) {
                list.setSelectedIndices(searchHits);
                list.scrollRectToVisible(list.getCellBounds(searchHits[0],
                        searchHits[0]));
            }
        } else {
            int start = Math.max(0, list.getSelectedIndex() + 1);
            if (start >= model.getSize()) {
                start = 0;
            }
            int f = searchInList(list, searchStr, start);
            if (f >= 0) {
                list.scrollRectToVisible(list.getCellBounds(f, f));
                list.setSelectedIndex(f);
            }
        }
    }

    /*
     * Finds in the list any occurrence of the argument string (as substring).
     */
    private static int searchInList(final JList list, final String str,
            final int startIndex) {
        // this method was (slightly modified) copied from
        // JList#getNextMatch
        ListModel model = list.getModel();
        int max = model.getSize();
        String prefix = str;
        if (prefix == null) {
            throw new IllegalArgumentException();
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();

        int index = startIndex;
        do {
            Object o = model.getElementAt(index);

            if (o != null) {
                String string;
                if (o instanceof String) {
                    string = ((String)o).toUpperCase();
                } else if (o instanceof DataColumnSpec) {
                    string = ((DataColumnSpec)o).getName().toString()
                            .toUpperCase();
                } else {
                    string = o.toString();
                    if (string != null) {
                        string = string.toUpperCase();
                    }
                }

                if (string != null && string.indexOf(prefix) >= 0) {
                    return index;
                }
            }
            index = (index + 1 + max) % max;
        } while (index != startIndex);
        return -1;
    }

    /**
     * Uses the {@link #searchInList(JList, String, int)} method to get all
     * occurrences of the given string in the given list and returns the index
     * off all occurrences as a <code>int[]</code>.
     *
     * @see #searchInList(JList, String, int)
     * @param list the list to search in
     * @param str the string to search for
     * @return <code>int[]</code> with the indices off all objects from the
     *         given list which match the given string. If no hits exists the
     *         method returns an empty <code>int[]</code>.
     *
     */
    private static int[] getAllSearchHits(final JList list, final String str) {

        ListModel model = list.getModel();
        int max = model.getSize();
        final ArrayList<Integer> hits = new ArrayList<Integer>(max);
        int index = 0;
        do {
            int tempIndex = searchInList(list, str, index);
            // if the search returns no hit or returns a hit before the
            // current search position exit the while loop
            if (tempIndex < index || tempIndex < 0) {
                break;
            }
            index = tempIndex;
            hits.add(new Integer(index));
            // increase the index to start the search from the next position
            // after the current hit
            index++;
        } while (index < max);

        if (hits.size() > 0) {
            final int[] resultArray = new int[hits.size()];
            for (int i = 0, length = hits.size(); i < length; i++) {
                resultArray[i] = hits.get(i).intValue();
            }
            return resultArray;
        }
        return new int[0];
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
