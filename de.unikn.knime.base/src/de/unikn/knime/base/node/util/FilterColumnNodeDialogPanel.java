/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.DataValue;
import de.unikn.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * Panel is used to select/filter a certain number of columns.
 * 
 * <p>
 * You can add a property change listener to this class that is notified when
 * the inlude list changes.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class FilterColumnNodeDialogPanel extends JPanel {

    /** Settings key for the excluded columns. */
    public static final String INCLUDED_COLUMNS = "included_columns";

    /** Settings key for the excluded columns. */
    public static final String EXCLUDED_COLUMNS = "excluded_columns";

    /** String for the change event when the include list changes. */
    public static final String PROP_CHANGE_INCLUDE_LIST = "include_changed";

    /** Include list. */
    private final JList m_inclList;

    /** Include model. */
    private final DefaultListModel m_inclMdl;

    /** Exclude list. */
    private final JList m_exclList;

    /** Exclude model. */
    private final DefaultListModel m_exclMdl;

    /** List of DataCellColumnSpecss to keep initial ordering of DataCells. */
    private final LinkedHashSet<DataColumnSpec> m_order = 
        new LinkedHashSet<DataColumnSpec>();

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_includeBorder;

    /** Border of the include panel, keep it so we can change the title. */
    private final TitledBorder m_excludeBorder;
    
    /** Show only columns of types that are compatible 
     * to one of theses classes. */
    private final Class<? extends DataValue>[] m_filterClasses;
    
    private final HashSet<DataColumnSpec> m_hideColumns =
        new HashSet<DataColumnSpec>();

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * 
     * @see #update(DataTableSpec, boolean, Set)
     * @see #update(DataTableSpec, boolean, DataCell...)
     */
    public FilterColumnNodeDialogPanel() {
        this(DataValue.class);
    }
    
    /** Only used to init the filter class array. */
    private static Class<? extends DataValue>[] init(
            final Class<? extends DataValue>... filterValueClasses) {
        if (filterValueClasses == null || filterValueClasses.length == 0) {
            throw new NullPointerException("Classes must not be null");
        }
        List<Class<? extends DataValue>> list = 
            Arrays.asList(filterValueClasses);
        if (list.contains(null)) {
            throw new NullPointerException("List of value classes must not " 
                    + "contain null elements.");
        }
        return filterValueClasses;
    }

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * @param filterValueClasses An array of type <code>DataValue</code> classes
     *        only allowed for selection. Will be check during update.
     * 
     * @see #update(DataTableSpec, boolean, Set)
     * @see #update(DataTableSpec, boolean, DataCell...)
     */
    public FilterColumnNodeDialogPanel(
            final Class<? extends DataValue>... filterValueClasses) {
        m_filterClasses = init(filterValueClasses);
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel();
        buttonPan.setLayout(new BoxLayout(buttonPan, BoxLayout.Y_AXIS));
        buttonPan.add(new JPanel());

        final JButton remButton = new JButton("remove >>");
        remButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(remButton);
        remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonPan.add(new JPanel());

        final JButton remAllButton = new JButton("remove all >>");
        remAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonPan.add(new JPanel());

        final JButton addButton = new JButton("<< add");
        addButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(addButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonPan.add(new JPanel());
        
        final JButton addAllButton = new JButton("<< add all");
        addAllButton.setMaximumSize(new Dimension(125, 25));
        buttonPan.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonPan.add(new JPanel());

        // include list
        m_inclMdl = new DefaultListModel();
        m_inclMdl.addElement("<empty>");
        m_inclList = new JList(m_inclMdl);
        m_inclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        final JScrollPane jspIncl = new JScrollPane(m_inclList);
        jspIncl.setMinimumSize(new Dimension(150, 155));

        final JTextField searchFieldIncl = new JTextField(8);
        JButton searchButtonIncl = new JButton("...");
        ActionListener actionListenerIncl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String text = searchFieldIncl.getText();
                if (m_inclMdl.isEmpty() || text.equals("")) {
                    return;
                }
                int start = Math.max(0, m_inclList.getSelectedIndex() + 1);
                if (start >= m_inclMdl.getSize()) {
                    start = 0;
                }
                int f = searchInList(m_inclList, text, start);
                if (f >= 0) {
                    m_inclList.scrollRectToVisible(m_inclList.getCellBounds(f,
                            f));
                    m_inclList.setSelectedIndex(f);
                }
            }
        };
        searchFieldIncl.addActionListener(actionListenerIncl);
        searchButtonIncl.addActionListener(actionListenerIncl);
        JPanel inclSearchPanel = new JPanel(new FlowLayout());
        inclSearchPanel.add(new JLabel("Search: "));
        inclSearchPanel.add(searchFieldIncl);
        inclSearchPanel.add(searchButtonIncl);
        JPanel includePanel = new JPanel(new BorderLayout());
        m_includeBorder = BorderFactory.createTitledBorder(" Include ");
        includePanel.setBorder(m_includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(jspIncl, BorderLayout.CENTER);

        // exclude list
        m_exclMdl = new DefaultListModel();
        m_exclMdl.addElement("<empty>");
        m_exclList = new JList(m_exclMdl);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setListCellRenderer(new DataColumnSpecListCellRenderer());
        final JScrollPane jspExcl = new JScrollPane(m_exclList);
        jspExcl.setMinimumSize(new Dimension(150, 155));

        final JTextField searchFieldExcl = new JTextField(8);
        JButton searchButtonExcl = new JButton("...");
        ActionListener actionListenerExcl = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String text = searchFieldExcl.getText();
                if (m_exclMdl.isEmpty() || text.equals("")) {
                    return;
                }
                int start = Math.max(0, m_exclList.getSelectedIndex() + 1);
                if (start >= m_exclMdl.getSize()) {
                    start = 0;
                }
                int f = searchInList(m_exclList, text, start);
                if (f >= 0) {
                    m_exclList.scrollRectToVisible(m_exclList.getCellBounds(f,
                            f));
                    m_exclList.setSelectedIndex(f);
                }
            }
        };
        searchFieldExcl.addActionListener(actionListenerExcl);
        searchButtonExcl.addActionListener(actionListenerExcl);
        JPanel exclSearchPanel = new JPanel(new FlowLayout());
        exclSearchPanel.add(new JLabel("Search: "));
        exclSearchPanel.add(searchFieldExcl);
        exclSearchPanel.add(searchButtonExcl);
        JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(" Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        JPanel buttonPan2 = new JPanel(new GridLayout());
        Border border = BorderFactory.createTitledBorder(" Select ");
        buttonPan2.setBorder(border);
        buttonPan2.add(buttonPan);
        
        // adds include, button, exclude component
        JPanel center = new JPanel(new BorderLayout());
        super.setLayout(new BorderLayout());
        center.add(includePanel, BorderLayout.CENTER);
        center.add(buttonPan2, BorderLayout.EAST);
        super.add(center, BorderLayout.WEST);
        super.add(excludePanel, BorderLayout.CENTER);
    } // FilterColumnNodeDialogPanel()

    /**
     * Called by the 'remove >>' button to exclude the selected elements from
     * the include list.
     */
    private void onRemIt() {
        Object[] incls = m_inclList.getSelectedValues();
        boolean changed = incls.length > 0;
        for (int i = 0; i < incls.length; i++) {
            m_exclMdl.addElement(incls[i]);
            m_inclMdl.removeElement(incls[i]);
        }
        List list = Arrays.asList(m_exclMdl.toArray());
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (list.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        if (changed) {
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        }
    }

    /**
     * Called by the 'remove >>' button to exclude all elements from the include
     * list.
     */
    private void onRemAll() {
        boolean changed = !m_inclMdl.isEmpty();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            m_exclMdl.addElement(c);
        }
        if (changed) {
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        }
    }

    /**
     * Called by the '<< add' button to include the selected elements from the
     * exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        Object[] o = m_exclList.getSelectedValues();
        boolean changed = o.length > 0;
        if (o != null) {
            for (int i = 0; i < o.length; i++) {
                m_inclMdl.addElement(o[i]);
                m_exclMdl.removeElement(o[i]);
            }
        }
        // again, remove all from the include list and start adding them from
        // the table spec by double-checking the include list
        List l = Arrays.asList(m_inclMdl.toArray());
        m_inclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            if (l.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
        if (changed) {
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the
     * exclude list.
     */
    private void onAddAll() {
        boolean changed = !m_exclMdl.isEmpty();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (DataColumnSpec c : m_order) {
            m_inclMdl.addElement(c);
        }
        if (changed) {
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        }
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     * 
     * @param spec The spec to retrieve the column names from.
     * @param exclude The flag if <code>excl</code> contains the columns to
     *            exclude otherwise include.
     * @param cells An array of data cell to either in- or exclude.
     */
    public void update(final DataTableSpec spec, final boolean exclude,
            final DataCell... cells) {
        this.update(spec, exclude, Arrays.asList(cells));
    }
    
    /** Checks if the given type is included in the list of allowed types. If 
     * the list is empty, all types are valid.
     */
    private boolean typeAllowed(final DataType type) {
        for (Class<? extends DataValue> cl : m_filterClasses) {
            if (type.isCompatible(cl)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     * 
     * @param spec The spec to retrieve the column names from.
     * @param exclude The flag if <code>excl</code> contains the columns to
     *            exclude otherwise include.
     * @param excl The list of columns to exclude or include.
     */
    public void update(final DataTableSpec spec, final boolean exclude,
            final Set<DataCell> excl) {
        this.update(spec, exclude, (Collection<DataCell>) excl);
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     * 
     * @param spec The spec to retrieve the column names from.
     * @param exclude The flag if <code>excl</code> contains the columns to
     *            exclude otherwise include.
     * @param excl The list of columns to exclude or include.
     */
    private void update(final DataTableSpec spec, final boolean exclude,
            final Collection<DataCell> excl) {
        assert (spec != null && excl != null);
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            if (!typeAllowed(cSpec.getType())) {
                continue;
            }
            final DataCell c = cSpec.getName();
            m_order.add(cSpec);
            if (exclude) {
                if (excl.contains(c)) {
                    m_exclMdl.addElement(cSpec);
                } else {
                    m_inclMdl.addElement(cSpec);
                }
            } else {
                if (excl.contains(c)) {
                    m_inclMdl.addElement(cSpec);
                } else {
                    m_exclMdl.addElement(cSpec);
                }
            }
        }
        repaint();
     }
    
    /**
     * Returns all columns from the exclude list.
     * 
     * @return A set of all columns from the exclude list.
     */
    public Set<DataCell> getExcludedColumnList() {
        return getColumnList(m_exclMdl);
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return A list of all columns from the include list.
     */
    public Set<DataCell> getIncludedColumnList() {
        return getColumnList(m_inclMdl);
    }

    /**
     * Helper for the get***ColumnList methods.
     * 
     * @param list The list from which to retrieve the elements
     */
    private static Set<DataCell> getColumnList(final ListModel model) {
        final Set<DataCell> list = new LinkedHashSet<DataCell>();
        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            DataCell cell = ((DataColumnSpec)o).getName();
            list.add(cell);
        }
        return list;
    }
    
    /**
     * Returns the DataType for the given cell retrieving it from the
     * initial DataTableSpec. If this name could not found, return null.
     * @param name The DataCell name to get the DataType for.
     * @return The DataType or null.
     */
    public DataType getType(final DataCell name) {
        for (DataColumnSpec spec : m_order) {
            if (spec.getName().equals(name)) {
                return spec.getType();
            }
        }
        return null;
    }

    /** 
     * Finds in the list any occurence of the argument string (as substring). 
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

                if (o instanceof DataCell) {
                    string = ((DataCell)o).toString().toUpperCase();
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
     * Set the renderer that is used for both list in this panel.
     * 
     * @param renderer New renderer being used.
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     */
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        m_inclList.setCellRenderer(renderer);
        m_exclList.setCellRenderer(renderer);
    }
    
    /**
     * Renoves the given column form either include or exclude list and notfies
     * all listeners.
     * @param column The column to remove.
     */
    public final void hideColumn(final DataColumnSpec column) {
        if (m_inclMdl.contains(column)) {
            m_hideColumns.add(column);
            m_inclMdl.removeElement(column);
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        } else {
            if (m_exclMdl.contains(column)) {
                m_hideColumns.add(column);
                m_exclMdl.removeElement(column);
                firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
            }
        }
    }
    
    /**
     * Re-adds all remove/hidden columns to the exclude list and notofies all
     * listeners.
     */
    public final void resetHiding() {
        if (m_hideColumns.size() > 0) {
            for (DataColumnSpec column : m_hideColumns) {
                m_exclMdl.addElement(column);
            }
            m_hideColumns.clear();
            firePropertyChange(PROP_CHANGE_INCLUDE_LIST, null, null);
        }
    }

    /**
     * Sets the title of the include panel.
     * 
     * @param title The new title
     */
    public final void setIncludeTitle(final String title) {
        m_includeBorder.setTitle(title);
    }

    /**
     * Sets the title of the exclude panel.
     * 
     * @param title The new title
     */
    public final void setExcludeTitle(final String title) {
        m_excludeBorder.setTitle(title);
    }
}
