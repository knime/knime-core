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
 * History
 *   16.11.2005 (mb): created
 *   2006-05-26 (tm): reviewed
 */
package org.knime.core.node.defaultnodedialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * Default component for dialogs allowing to select a subset of the available
 * columns.
 * 
 * @author M. Berthold, University of Konstanz
 */
public class DialogComponentColumnFilter extends DialogComponent {

    /* name for XML config entry holing list of included columns */
    private final String m_configName;

    /* Include list. */
    private final JList m_inclList;

    /* Include model. */
    private final DefaultListModel m_inclMdl;

    /* Exclude list. */
    private final JList m_exclList;

    /* Exclude model. */
    private final DefaultListModel m_exclMdl;

    /* List of DataCellColumnSpecs to keep initial ordering of DataCells. */
    private final LinkedHashSet<Object> m_order = new LinkedHashSet<Object>();

    /* Store the excluded or the included cols into the settings? */
    private final boolean m_storeExcluded;

    /**
     * Creates a new filter column panel with three components which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * 
     * @param configName name of entry in setting object
     * @param label description of this panel
     * @see #update(DataTableSpec, Set, boolean)
     */
    public DialogComponentColumnFilter(final String configName,
            final String label) {
        this(configName, label, true);
    } // FilterColumnNodeDialogPanel()

    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     * 
     * @param configName name of entry in setting object
     * @param label description of this panel
     * @param excluded true if the excluded columns should be stored into the
     *            spec, false otherwise.
     * @see #update(DataTableSpec, Set, boolean)
     */
    public DialogComponentColumnFilter(final String configName,
            final String label, final boolean excluded) {
        m_storeExcluded = excluded;
        m_configName = configName;
        // keeps buttons such add 'add', 'add all', 'remove', and 'remove all'
        final JPanel buttonPan = new JPanel(new GridLayout(0, 1, 5, 5));
        // add an empty label to move buttons down a bit
        buttonPan.add(new JLabel(""));
        // now add four buttons to move columns from left to right or vice versa
        final JButton remButton = new JButton("> remove >");
        buttonPan.add(remButton);
        remButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        final JButton remAllButton = new JButton(">> remove all >>");
        buttonPan.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        final JButton addButton = new JButton("< add <");
        buttonPan.add(addButton);
        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        final JButton addAllButton = new JButton("<< add all <<");
        buttonPan.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
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
        Border includeBorder = BorderFactory.createTitledBorder(" Include ");
        includePanel.setBorder(includeBorder);
        includePanel.add(inclSearchPanel, BorderLayout.NORTH);
        includePanel.add(jspIncl, BorderLayout.CENTER);

        // exclude list
        m_exclMdl = new DefaultListModel();
        m_exclMdl.addElement("<empty>");
        m_exclList = new JList(m_exclMdl);
        m_exclList.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
        Border excludeBorder = BorderFactory.createTitledBorder(" Exclude ");
        excludePanel.setBorder(excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        // set renderer so that icon and column name are displayed nicely
        // (for both lists)
        setListCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList list,
                    final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                assert (c == this);
                if (value instanceof DataColumnSpec) {
                    setText(((DataColumnSpec)value).getName().toString());
                    setIcon(((DataColumnSpec)value).getType().getIcon());
                } else {
                    setText("unknown type...");
                }
                return this;
            }
        });

        // adds include, button, exclude component
        JPanel buttonPan2 = new JPanel(new FlowLayout());
        buttonPan2.add(buttonPan);

        JPanel overallPan = new JPanel();
        overallPan.setLayout(new BoxLayout(overallPan, BoxLayout.X_AXIS));
        overallPan.add(includePanel);
        overallPan.add(buttonPan2);
        overallPan.add(excludePanel);
        overallPan.setBorder(new TitledBorder(label));
        super.add(overallPan);
    }

    /**
     * Called by the '> remove >' button to exclude the selected elements from
     * the include list.
     */
    private void onRemIt() {
        Object[] incls = m_inclList.getSelectedValues();
        for (int i = 0; i < incls.length; i++) {
            m_exclMdl.addElement(incls[i]);
            m_inclMdl.removeElement(incls[i]);
        }
        List list = Arrays.asList(m_exclMdl.toArray());
        m_exclMdl.removeAllElements();
        for (Object c : m_order) {
            if (list.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
    }

    /**
     * Called by the 'remove >>' button to exclude all elements from the include
     * list.
     */
    private void onRemAll() {
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (Object c : m_order) {
            m_exclMdl.addElement(c);
        }
    }

    /**
     * Called by the '< add <' button to include the selected elements from the
     * exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        Object[] o = m_exclList.getSelectedValues();
        for (int i = 0; i < o.length; i++) {
            m_inclMdl.addElement(o[i]);
            m_exclMdl.removeElement(o[i]);
        }

        // again, remove all from the include list and start adding them from
        // the table spec by double-checking the include list
        List l = Arrays.asList(m_inclMdl.toArray());
        m_inclMdl.removeAllElements();
        for (Object c : m_order) {
            if (l.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the
     * exclude list.
     */
    private void onAddAll() {
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (Object c : m_order) {
            m_inclMdl.addElement(c);
        }
    }

    /**
     * Read contents of this dialog from config file (the list of excluded
     * columns only in this case - everything else can be reconstructed from
     * that).
     * 
     * @param settings config to read from
     * @param specs table specs for the inports
     * @throws InvalidSettingsException if something fails
     * @see DialogComponent#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws InvalidSettingsException {
        LinkedHashSet<String> excl = new LinkedHashSet<String>();
        try {
            String[] excludedCells = settings.getStringArray(m_configName);
            if (excludedCells != null) {
                for (String s : excludedCells) {
                    excl.add(s);
                }
            }
        } finally {
            this.update(specs[0], excl, m_storeExcluded);
        }
    }

    /**
     * Store contents of this dialog to config file (the list of excluded
     * columns only in this case - everything else can be reconstructed from
     * that).
     * 
     * @param settings config to write to
     * @see DialogComponent#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        List<String> colList;
        if (m_storeExcluded) {
            colList = this.getExcludedColumnList();
        } else {
            colList = this.getIncludedColumnList();
        }
        String[] colCells = new String[colList.size()];
        int i = 0;
        for (String cell : colList) {
            colCells[i] = cell;
            i++;
        }
        settings.addStringArray(m_configName, colCells);
    }

    /**
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contain all column names
     * from the spec afterwards.
     * 
     * @param spec the spec to retrieve the column names from
     * @param excl the list of columns to exclude or include
     * @param exclude the flag if <code>excl</code> contains the columns to
     *            exclude otherwise include.
     */
    public void update(final DataTableSpec spec, final Set<String> excl,
            final boolean exclude) {
        assert (spec != null && excl != null);
        m_order.clear();
        m_inclMdl.removeAllElements();
        m_exclMdl.removeAllElements();
        for (int i = 0; i < spec.getNumColumns(); i++) {
            final DataColumnSpec cSpec = spec.getColumnSpec(i);
            final String c = cSpec.getName();
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
     * @return a list of all columns from the exclude list
     */
    public List<String> getExcludedColumnList() {
        return getColumnList(m_exclMdl);
    }

    /**
     * Returns all columns from the include list.
     * 
     * @return a list of all columns from the include list
     */
    public List<String> getIncludedColumnList() {
        return getColumnList(m_inclMdl);
    }

    /**
     * Helper for the get***ColumnList methods.
     * 
     * @param model the list from which to retrieve the elements
     * @return all elements if the given model in a lists
     */
    private static List<String> getColumnList(final ListModel model) {
        final ArrayList<String> list = new ArrayList<String>();
        // this is kind of quick & dirty here: We don't know which update
        // method was used and hence we don't know if the elements in the
        // lists are of type DataCell or DataColumnSpec
        for (int i = 0; i < model.getSize(); i++) {
            Object o = model.getElementAt(i);
            String cell;
            if (o instanceof DataColumnSpec) {
                cell = ((DataColumnSpec)o).getName();
            } else {
                cell = (String)o;
            }
            list.add(cell);
        }
        return list;
    }

    /*
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
           throw new IllegalArgumentException("Paremeter str must not be null");
        }
        if (startIndex < 0 || startIndex >= max) {
            throw new IllegalArgumentException("Invalid start index: "
                    + startIndex);
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
     * Set the renderer that is used for both lists in this panel.
     * 
     * @param renderer new renderer being used
     * @see JList#setCellRenderer(javax.swing.ListCellRenderer)
     */
    protected final void setListCellRenderer(final ListCellRenderer renderer) {
        m_inclList.setCellRenderer(renderer);
        m_exclList.setCellRenderer(renderer);
    }

    /**
     * @see org.knime.core.node.defaultnodedialog.DialogComponent
     *      #setEnabledComponents(boolean)
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        recSetEnabledContainer(this, enabled);
    }

    private void recSetEnabledContainer(final Container cont, final boolean b) {
        cont.setEnabled(b);
        for (Component c : cont.getComponents()) {
            if (c instanceof Container) {
                recSetEnabledContainer((Container)c, b);
            } else {
                c.setEnabled(b);
            }
        }
    }
}
