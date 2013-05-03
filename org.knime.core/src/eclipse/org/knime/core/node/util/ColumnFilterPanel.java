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
import java.util.LinkedList;
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
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.util.ListModelFilterUtils;


/**
 * Panel is used to select/filter a certain number of columns.
 *
 * <p>
 * You can add a property change listener to this class that is notified when
 * the include list changes.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
@SuppressWarnings("serial")
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

    private boolean m_showInvalidInclCols = false;

    private boolean m_showInvalidExclCols = false;

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
         *        the type allowed in {@link #includeColumn(DataColumnSpec)}
         */
        public ValueClassFilter(
                final Class<? extends DataValue>... filterValueClasses) {
            if (filterValueClasses == null || filterValueClasses.length == 0) {
                throw new NullPointerException("Classes must not be null");
            }
            final List<Class<? extends DataValue>> list = Arrays
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
        @Override
        public final boolean includeColumn(final DataColumnSpec cspec) {
            for (final Class<? extends DataValue> cl : m_filterClasses) {
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
    }

    /** The filter used to filter out/in valid column types. */
    private ColumnFilter m_filter;

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

    private DataTableSpec m_spec;


    /**
     * Creates a new filter column panel with three component which are the
     * include list, button panel to shift elements between the two lists, and
     * the exclude list. The include list then will contain all values to
     * filter.
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String[])
     *
     * @deprecated Use the constructor {@link #ColumnFilterPanel(boolean)}
     * instead
     */
    @Deprecated
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
     * @param showKeepAllBox <code>true</code>, if an check box to keep all
     * columns is shown
     *
     * @see #update(DataTableSpec, boolean, Collection)
     * @see #update(DataTableSpec, boolean, String[])
     */
    @SuppressWarnings("unchecked")
    public ColumnFilterPanel(final boolean showKeepAllBox) {
        this(showKeepAllBox, DataValue.class);
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
     *
     * @deprecated Use the constructor
     * {@link #ColumnFilterPanel(boolean, Class...)} instead
     */
    @Deprecated
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
     *
     * @deprecated Use the constructor
     * {@link #ColumnFilterPanel(boolean, ColumnFilter)} instead
     */
    @Deprecated
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
                onAddAll(true);
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
        final ActionListener actionListenerIncl = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ListModelFilterUtils.onSearch(m_inclList, m_inclMdl,
                        m_searchFieldIncl.getText(),
                        m_markAllHitsIncl.isSelected());
            }
        };
        m_searchFieldIncl.addActionListener(actionListenerIncl);
        m_searchButtonIncl.addActionListener(actionListenerIncl);
        final JPanel inclSearchPanel = new JPanel(new BorderLayout());
        inclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        inclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        inclSearchPanel.add(m_searchFieldIncl, BorderLayout.CENTER);
        inclSearchPanel.add(m_searchButtonIncl, BorderLayout.EAST);
        m_markAllHitsIncl = new JCheckBox("Select all search hits");
        final ActionListener actionListenerAllIncl = new ActionListener() {
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
        final JPanel includePanel = new JPanel(new BorderLayout());
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
        final ActionListener actionListenerExcl = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                ListModelFilterUtils.onSearch(m_exclList, m_exclMdl,
                        m_searchFieldExcl.getText(),
                        m_markAllHitsExcl.isSelected());
            }
        };
        m_searchFieldExcl.addActionListener(actionListenerExcl);
        m_searchButtonExcl.addActionListener(actionListenerExcl);
        final JPanel exclSearchPanel = new JPanel(new BorderLayout());
        exclSearchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15,
                15));
        exclSearchPanel.add(new JLabel("Column(s): "), BorderLayout.WEST);
        exclSearchPanel.add(m_searchFieldExcl, BorderLayout.CENTER);
        exclSearchPanel.add(m_searchButtonExcl, BorderLayout.EAST);
        m_markAllHitsExcl = new JCheckBox("Select all search hits");
        final ActionListener actionListenerAllExcl = new ActionListener() {
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
        final JPanel excludePanel = new JPanel(new BorderLayout());
        m_excludeBorder = BorderFactory.createTitledBorder(
                EXCLUDE_BORDER, " Exclude ");
        excludePanel.setBorder(m_excludeBorder);
        excludePanel.add(exclSearchPanel, BorderLayout.NORTH);
        excludePanel.add(jspExcl, BorderLayout.CENTER);

        final JPanel buttonPan2 = new JPanel(new GridLayout());
        final Border border = BorderFactory.createTitledBorder(" Select ");
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
                    final boolean keepAll = m_keepAllBox.isSelected();
                    if (keepAll) {
                        onAddAll(false);
                    }
                    enabledComponents(!keepAll);
                }
            });
        } else {
            m_keepAllBox = null;
        }

        // adds include, button, exclude component
        final JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        center.add(excludePanel);
        center.add(buttonPan2);
        center.add(includePanel);
        final JPanel all = new JPanel();
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));
        all.add(center);
        if (m_keepAllBox != null) {
            final JPanel keepAllPanel = new JPanel(
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
        final boolean newEnabled = enabled && !isKeepAllSelected();
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
     * @param show set to <code>true</code> to show invalid exclude columns
     * @since 2.8
     */
    public void setShowInvalidExcludeColumns(final boolean show) {
        m_showInvalidExclCols = show;
    }

    /**
     * @param show set to <code>true</code> to show invalid include columns
     * @since 2.8
     */
    public void setShowInvalidIncludeColumns(final boolean show) {
        m_showInvalidInclCols = show;
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
            for (final ChangeListener listener : m_listeners) {
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
        final Object[] o = m_inclList.getSelectedValues();
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        for (final Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            DataColumnSpec spec = (DataColumnSpec) e.nextElement();
            if (m_showInvalidInclCols && DataColumnSpecListCellRenderer.isInvalid(spec)) {
                invalidSpecs.add(spec);
            }
            hash.add(spec);
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_inclMdl.removeElement(o[i]);
        }
        m_exclMdl.removeAllElements();
        for (final DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_exclMdl.addElement(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all remaining invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_exclMdl.addElement(invalidSpec);
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
        if (m_inclMdl.elements().hasMoreElements()) {
            //perform the operation only if the exclude model contains at least one column to add
            final List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
            if (m_showInvalidExclCols) {
                for (final Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
                    final DataColumnSpec spec = (DataColumnSpec)e.nextElement();
                    if (DataColumnSpecListCellRenderer.isInvalid(spec)) {
                        invalidSpecs.add(spec);
                    }
                }
            }
            m_inclMdl.removeAllElements();
            m_exclMdl.removeAllElements();
            for (final DataColumnSpec c : m_order) {
                if (!m_hideColumns.contains(c)) {
                    m_exclMdl.addElement(c);
                }
            }
            if (!invalidSpecs.isEmpty()) {
                //add all invalid specs at the end of the list
                for (DataColumnSpec invalidSpec : invalidSpecs) {
                    m_exclMdl.addElement(invalidSpec);
                }
            }
            setKeepAllSelected(false);
            fireFilteringChangedEvent();
        }
    }

    /**
     * Called by the '<< add' button to include the selected elements from the
     * exclude list.
     */
    private void onAddIt() {
        // add all selected elements from the exclude to the include list
        final Object[] o = m_exclList.getSelectedValues();
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(Arrays.asList(o));
        List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
        for (final Enumeration<?> e = m_inclMdl.elements(); e.hasMoreElements();) {
            final DataColumnSpec spec = (DataColumnSpec)e.nextElement();
            if (m_showInvalidInclCols && DataColumnSpecListCellRenderer.isInvalid(spec)) {
                invalidSpecs.add(spec);
            }
            hash.add(spec);
        }
        boolean changed = false;
        for (int i = 0; i < o.length; i++) {
            changed |= m_exclMdl.removeElement(o[i]);
        }
        m_inclMdl.removeAllElements();
        for (final DataColumnSpec c : m_order) {
            if (hash.contains(c)) {
                m_inclMdl.addElement(c);
            }
        }
        if (!invalidSpecs.isEmpty()) {
            //add all remaining invalid specs at the end of the list
            for (DataColumnSpec invalidSpec : invalidSpecs) {
                m_inclMdl.addElement(invalidSpec);
            }
        }
        if (changed) {
            fireFilteringChangedEvent();
        }
    }

    /**
     * Called by the '<< add all' button to include all elements from the
     * exclude list.
     * @param showInvalid <code>true</code> if the invalid columns should still be shown
     */
    private void onAddAll(final boolean showInvalid) {
        if (m_exclMdl.elements().hasMoreElements() || !showInvalid) {
            //perform the operation only if the exclude model contains at least one column to add
            final List<DataColumnSpec> invalidSpecs = new LinkedList<DataColumnSpec>();
            if (showInvalid && m_showInvalidInclCols) {
                for (final Enumeration<?> e = m_inclMdl.elements(); e.hasMoreElements();) {
                    final DataColumnSpec spec = (DataColumnSpec)e.nextElement();
                    if (DataColumnSpecListCellRenderer.isInvalid(spec)) {
                        invalidSpecs.add(spec);
                    }
                }
            }
            m_inclMdl.removeAllElements();
            m_exclMdl.removeAllElements();
            for (final DataColumnSpec c : m_order) {
                if (!m_hideColumns.contains(c)) {
                    m_inclMdl.addElement(c);
                }
            }
            if (!invalidSpecs.isEmpty()) {
                //add all invalid specs at the end of the list
                for (DataColumnSpec invalidSpec : invalidSpecs) {
                    m_inclMdl.addElement(invalidSpec);
                }
            }
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
    public void update(final DataTableSpec spec, final boolean exclude, final Collection<String> list) {
        assert (spec != null && list != null);
        m_spec = spec;
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
     * Updates this filter panel by removing all current selections from the
     * include and exclude list. The include list will contains all column names
     * from the spec afterwards.
     *
     * @param spec the spec to retrieve the column names from
     * @param inclList the list of columns to include
     * @param exclList the list of columns to exclude
     * @since 2.8
     */
    public void update(final DataTableSpec spec, final Collection<String> inclList,
                       final Collection<String> exclList) {
        assert (spec != null && inclList != null && exclList != null);
        m_spec = spec;
        m_order.clear();
        final Set<String> exclCols = new HashSet<String>(exclList);
        final Set<String> inclCols = new HashSet<String>(inclList);
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
                inclCols.remove(c);
            } else {
                if (exclCols.remove(c)) {
                    m_exclMdl.addElement(cSpec);
                } else {
                    m_inclMdl.addElement(cSpec);
                    inclCols.remove(c);
                }
            }
        }
        addInvalidSpecs(m_showInvalidExclCols, spec, exclCols, m_exclMdl);
        addInvalidSpecs(m_showInvalidInclCols, spec, inclCols, m_inclMdl);
        repaint();
    }

    private static void addInvalidSpecs(final boolean showInvalidCols, final DataTableSpec spec,
                                        final Set<String> colNames, final DefaultListModel model) {
        if (showInvalidCols && !colNames.isEmpty()) {
            for (String colName : colNames) {
                final DataColumnSpec invalidSpec = DataColumnSpecListCellRenderer.createInvalidSpec(colName);
                model.addElement(invalidSpec);
            }
        }
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
        return ListModelFilterUtils.getColumnList(m_exclMdl);
    }

    /**
     * Returns all invalid columns from the exclude list.
     *
     * @return a set of all invalid columns from the exclude list
     * @since 2.8
     */
    public Set<String> getInvalidExcludeColumnSet() {
        return ListModelFilterUtils.getInvalidColumnList(m_exclMdl);
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
        return ListModelFilterUtils.getColumnList(m_inclMdl);
    }

    /**
     * Returns all invalid columns from the include list.
     *
     * @return a list of all invalid columns from the include list
     * @since 2.8
     */
    public Set<String> getInvalidIncludedColumnSet() {
        return ListModelFilterUtils.getInvalidColumnList(m_inclMdl);
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
        for (final DataColumnSpec spec : m_order) {
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
        for (final DataColumnSpec column : columns) {
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
        final HashSet<Object> hash = new HashSet<Object>();
        hash.addAll(m_hideColumns);
        for (final Enumeration<?> e = m_exclMdl.elements(); e.hasMoreElements();) {
            hash.add(e.nextElement());
        }
        m_exclMdl.removeAllElements();
        for (final DataColumnSpec c : m_order) {
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

    /**
     * Sets the internal used {@link ColumnFilter} to the given one and calls
     * the {@link #update(DataTableSpec, boolean, Collection)} method to
     * update the column panel.
     *
     * @param filter the new {@link ColumnFilter} to use
     */
    public void setColumnFilter(final ColumnFilter filter) {
        m_filter = filter;
        if (m_spec == null) {
            //the spec is not available that's why we do not need to call
            //the update method
            return;
        }
        update(m_spec, true, getExcludedColumnSet());
    }
}
