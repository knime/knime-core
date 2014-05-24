/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created on 29.10.2013 by NanoTec
 */
package org.knime.core.node.util;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.util.ListModelFilterUtils;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent.Type;

/**
 * A panel comprising a column list, search field and some search customizers for the user. The list should be
 * initialized using {@link #update(DataTableSpec)} afterwards the returned {@link ListModifier} can be used to
 * add/remove additional {@link DataColumnSpec}s. Usually additional columns are used to add 'invalid' columns, for
 * which a model specific configuration does exist but which does actually not exist any more in the input table. See
 * {@link ListModifier} to get more information about additional columns.
 *
 * @author Marcel Hanser
 * @since 2.10
 */
@SuppressWarnings("serial")
public final class ColumnSelectionSearchableListPanel extends JPanel {

    private static final int DEFAULT_LIST_WIDTH = 70;

    private final ColumnSelectionList m_columnList;

    private final SearchedItemsSelectionMode m_selectionMode;

    private final JComboBox m_filters;

    private final ConfiguredColumnDeterminer m_container;

    private ModifierImpl m_currentModifier;

    private ListCellRenderer m_cellRenderer;

    private final Set<Integer> m_lastSearchHits = new HashSet<Integer>();

    private JTextField m_searchField;

    /**
     * @param searchedItemsSelectionMode the {@link SearchedItemsSelectionMode} to use
     * @param configuredColumnDeterminer callback for determining if a certain column is configured
     */
    public ColumnSelectionSearchableListPanel(final SearchedItemsSelectionMode searchedItemsSelectionMode,
        final ConfiguredColumnDeterminer configuredColumnDeterminer) {
        this(searchedItemsSelectionMode, configuredColumnDeterminer, DEFAULT_LIST_WIDTH, true);
    }

    /**
     * @param searchedItemsSelectionMode the {@link SearchedItemsSelectionMode} to use
     * @param configuredColumnDeterminer callback for determining if a certain column is configured
     * @param listFixedWidth sets the maximal width on the JList component if the value is bigger than 0
     * @param autoDeleteAdditionalColumns <code>true</code> says that selected columns should be automatically deleted
     *            if the user presses 'Del'
     */
    public ColumnSelectionSearchableListPanel(final SearchedItemsSelectionMode searchedItemsSelectionMode,
        final ConfiguredColumnDeterminer configuredColumnDeterminer, final int listFixedWidth,
        final boolean autoDeleteAdditionalColumns) {

        m_container = configuredColumnDeterminer;
        m_columnList = new ColumnSelectionList();
        m_columnList.setUserSelectionAllowed(true);
        if (listFixedWidth > 0) {
            // Setting the fixed width also improves the performance, as it
            // prevents the list to render all components. (See javadoc of JList#setFixedCellWidth)
            m_columnList.setFixedCellWidth(listFixedWidth);
        }
        m_selectionMode = CheckUtils.checkNotNull(searchedItemsSelectionMode);

        m_searchField = new JTextField(14);
        switch (m_selectionMode) {
            case SELECT_FIRST:
                m_columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                break;
            case SELECT_ALL:
                m_columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                break;
            case SELECT_NONE:
                m_columnList.setUserSelectionAllowed(false);
                break;
            default:
                break;
        }

        if (autoDeleteAdditionalColumns) {
            addConfigurationRequestListener(new ConfigurationRequestListener() {

                @Override
                public void configurationRequested(final ConfigurationRequestEvent creationEvent) {
                    if (Type.DELETION.equals(creationEvent.getType())) {
                        for (DataColumnSpec cspec : getSelectedColumns()) {
                            if (isAdditionalColumn(cspec)) {
                                innerGetModifier().removeAdditionalColumn(cspec.getName());
                            }
                        }
                    }
                }
            });
        }
        m_searchField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                processSearch();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                processSearch();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                processSearch();
            }

        });

        addKeyMouseSelectionListerersToColumnList();

        setDefaultCellRenderer();

        final JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createTitledBorder(" Column Search "));
        searchPanel.add(m_searchField, BorderLayout.CENTER);

        JPanel jPanel = new JPanel(new GridLayout(0, 1));
        jPanel.setBorder(BorderFactory.createTitledBorder("Filter Options"));
        m_filters = new JComboBox(FilterOption.values());
        // simulate a change in the search field to trigger a new search.
        m_filters.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                processSearch();
            }
        });
        jPanel.add(m_filters);

        searchPanel.add(jPanel, BorderLayout.SOUTH);
        setLayout(new BorderLayout());

        add(searchPanel, BorderLayout.NORTH);
        add(new JScrollPane(m_columnList), BorderLayout.CENTER);
    }

    /**
     * Clears the current list, adds the {@link DataColumnSpec}s of the given spec and invalidates the current
     * {@link ListModifier}. The {@link DataColumnSpec}s added by this function cannot be deleted from list using the
     * {@link ListModifier}.
     *
     * @param spec containing the immutable part of the column list
     * @return a ListModifier for managing the mutable part of the column list
     */
    public ListModifier update(final DataTableSpec spec) {
        return update(spec, null);
    }

    /**
     * Clears the current list, adds the {@link DataColumnSpec}s of the given spec and invalidates the current
     * {@link ListModifier}. The {@link DataColumnSpec}s added by this function cannot be deleted from list using the
     * {@link ListModifier}. This variant creates for each column given by allConfiguredColumns, which does not exists
     * in the input spec an {@link DataColumnSpecListCellRenderer#createInvalidSpec(String)} and adds it as an
     * additional column. See {@link ListModifier} for more information about additional and invalid columns.
     *
     * @param spec containing the immutable part of the column list
     * @param filter the column filter for filtering the spec
     * @param allConfiguredColumns all configured columns
     * @return a ListModifier for managing the mutable part of the column list
     */
    public ListModifier update(final DataTableSpec spec, final ColumnFilter filter,
        final Collection<String> allConfiguredColumns) {
        return update(spec, filter, allConfiguredColumns.toArray(new String[allConfiguredColumns.size()]));
    }

    /**
     * Clears the current list, adds the {@link DataColumnSpec}s of the given spec and invalidates the current
     * {@link ListModifier}. The {@link DataColumnSpec}s added by this function cannot be deleted from list using the
     * {@link ListModifier}. This variant creates for each column given by allConfiguredColumns, which does not exists
     * in the input spec an {@link DataColumnSpecListCellRenderer#createInvalidSpec(String)} and adds it as an
     * additional column. See {@link ListModifier} for more information about additional and invalid columns.
     *
     * @param spec containing the immutable part of the column list
     * @param filter the column filter for filtering the spec
     * @param allConfiguredColumns all configured columns
     * @return a ListModifier for managing the mutable part of the column list
     */
    public ListModifier update(final DataTableSpec spec, final ColumnFilter filter,
        final String... allConfiguredColumns) {
        ListModifier modifier = update(spec, null);
        for (String name : allConfiguredColumns) {
            if (!spec.containsName(name)) {
                modifier.addInvalidColumns(name);
            }
        }
        return modifier;
    }

    /**
     * Clears the current list, adds the {@link DataColumnSpec}s of the given spec and invalidates the current
     * {@link ListModifier}. The {@link DataColumnSpec}s added by this function cannot be deleted from list using the
     * {@link ListModifier}.
     *
     * @param spec containing the immutable part of the column list
     * @param filter the column filter for filtering the spec
     * @return a ListModifier for managing the mutable part of the column list
     */
    public ListModifier update(final DataTableSpec spec, final ColumnFilter filter) {
        m_lastSearchHits.clear();
        setUnkownFilterVisible(false);
        m_columnList.update(spec, filter);
        if (m_currentModifier != null) {
            m_currentModifier.invalidate();
        }
        m_currentModifier = new ModifierImpl(this, spec);
        return m_currentModifier;
    }

    /**
     * Clears the current list, adds the {@link DataColumnSpec}s of the given spec and invalidates the current
     * {@link ListModifier}. The {@link DataColumnSpec}s added by this function cannot be deleted from list using the
     * {@link ListModifier}.
     *
     * @param specs containing the immutable part of the column list
     * @return a ListModifier for managing the mutable part of the column list
     */
    public ListModifier update(final Iterable<DataColumnSpec> specs) {
        m_lastSearchHits.clear();
        setUnkownFilterVisible(false);
        m_columnList.update(specs);
        if (m_currentModifier != null) {
            m_currentModifier.invalidate();
        }
        DataTableSpecCreator dataTableSpecCreator = new DataTableSpecCreator();
        for (DataColumnSpec spec : specs) {
            dataTableSpecCreator.addColumns(spec);
        }
        m_currentModifier = new ModifierImpl(this, dataTableSpecCreator.createSpec());
        return m_currentModifier;
    }

    /**
     * @return the currently selected {@link DataColumnSpec}, i.e. the selected one with the highest index
     */
    public DataColumnSpec getSelectedColumn() {
        return (DataColumnSpec)m_columnList.getSelectedValue();
    }

    /**
     * @return the currently selected {@link DataColumnSpec}s
     */
    public List<DataColumnSpec> getSelectedColumns() {
        List<DataColumnSpec> list = new ArrayList<DataColumnSpec>();
        for (Object o : m_columnList.getSelectedValues()) {
            list.add((DataColumnSpec)o);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * @param columns sets the selected columns
     */
    public void setSelectedColumns(final String... columns) {
        m_columnList.setSelectedColumns(columns);
    }

    /**
     * @param columns sets the selected columns
     */
    public void setSelectedColumns(final Collection<String> columns) {
        m_columnList.setSelectedColumns(columns);
    }

    /**
     * Ensures that one of the currently selected items is visible.
     */
    public void ensureSelectedColumnsVisible() {
        m_columnList.ensureIndexIsVisible(m_columnList.getSelectedIndex());
    }

    /**
     * @param spec the spec to check
     * @return <code>true</code> if the given column has been added using the {@link ListModifier}, which also enables
     *         that {@link DataColumnSpec} to be deleted later on
     */
    public boolean isAdditionalColumn(final DataColumnSpec spec) {
        return isAdditionalColumn(spec.getName());
    }

    /**
     * @param spec the spec to check
     * @return <code>true</code> if the given column has been added using the {@link ListModifier}, which also enables
     *         that {@link DataColumnSpec} to be deleted later on
     */
    public boolean isAdditionalColumn(final String spec) {
        return innerGetModifier().m_additionalNames.containsKey(spec);
    }

    /**
     * @param listener listener to add
     */
    public void addSearchListener(final SearchListener listener) {
        listenerList.add(SearchListener.class, listener);
    }

    /**
     * @param listener listener to remove
     */
    public void removeSearchListener(final SearchListener listener) {
        listenerList.remove(SearchListener.class, listener);
    }

    /**
     * @param listener listener to add
     */
    public void addConfigurationRequestListener(final ConfigurationRequestListener listener) {
        listenerList.add(ConfigurationRequestListener.class, listener);
    }

    /**
     * @param listener listener to remove
     */
    public void removeConfigurationRequestListener(final ConfigurationRequestListener listener) {
        listenerList.remove(ConfigurationRequestListener.class, listener);
    }

    /**
     * @param cellRenderer the cell renderer used to render the list entries
     */
    public void setCellRenderer(final ListCellRenderer cellRenderer) {
        m_cellRenderer = cellRenderer;
    }

    /**
     * The selection type which appears if the user press enter while the search field has the focus.
     *
     * @author Marcel Hanser
     */
    public enum SearchedItemsSelectionMode {
        /**
         * Selects the first entry which matches the search string.
         */
        SELECT_FIRST,
        /**
         * Selects all entries which matches the search string.
         */
        SELECT_ALL,
        /**
         * Selects no entries.
         */
        SELECT_NONE;
    }

    private enum FilterOption {
        NONE, MODIFIED, UNMODIFIED, UNKNOWN;

        @Override
        public String toString() {
            return name().substring(0, 1) + name().substring(1).toLowerCase();
        }
    }

    /**
     * Determines if there is already a configuration for a given {@link DataColumnSpec}.
     *
     * @author Marcel Hanser
     */
    public static interface ConfiguredColumnDeterminer {
        /**
         * @param spec the spec for which a configuration may exist
         * @return <code>true</code> if there is already a configuration for the given spec
         */
        public boolean isConfiguredColumn(DataColumnSpec spec);
    }

    /**
     * Listener called if the user presses 'Enter' in the searchfield.
     *
     * @author Marcel Hanser
     */
    public static interface SearchListener extends EventListener {
        /**
         * Called if the user presses 'Enter' in the searchfield.
         *
         * @param searchEvent the event containing e.g. the matching items
         */
        public void searchEvaluated(SearchEvent searchEvent);
    }

    /**
     * Listener called if the user presses 'Enter' or 'Del' while the list has the focus.
     *
     * @author Marcel Hanser
     */
    public static interface ConfigurationRequestListener extends EventListener {
        /**
         * Called if the user presses 'Enter' or 'Del' while the list has the focus.
         *
         * @param creationEvent the creation event
         */
        public void configurationRequested(ConfigurationRequestEvent creationEvent);
    }

    /**
     * The ListModifier is returned by calling {@link ColumnSelectionSearchableListPanel#update(DataTableSpec)} or
     * {@link ColumnSelectionSearchableListPanel#update(DataTableSpec, ColumnFilter)} and provides the functions to
     * add/remove additional {@link DataColumnSpec}s. An additional column is one which is not part of the actual
     * DataTableSpec, which also indicates that it is possible to remove them. A special variant of additional columns
     * are the 'invalid' ones created by {@link DataColumnSpecListCellRenderer#createInvalidSpec(String, DataType)}
     * indicating columns which are previously configured but actually do not exist in the input table. These columns
     * are rendered with a red border on default. Other types of additional columns and a customized
     * {@link ListCellRenderer} may be used for a arbitrary highlighting of certain additional columns.
     *
     * @author Marcel Hanser
     */
    public static interface ListModifier {
        /**
         * Creates and adds invalid column specs of the given type for the given column names. See {@link ListModifier}
         * for more information about invalid columns.
         *
         * @see DataColumnSpecListCellRenderer#createInvalidSpec(String, DataType)
         * @param type the type of the invalud column
         * @param columns the column names
         * @return this modifier
         * @throws IllegalArgumentException if one column does already exist in the list, including as well as the
         *             additional columns as well as the original {@link DataTableSpec}
         * @throws IllegalStateException if this modfier is invalid
         */
        ListModifier addInvalidColumns(DataType type, final String... columns);

        /**
         * Creates and adds invalid columns pecs for the given column names.See {@link ListModifier} for more
         * information about invalid columns.
         *
         * @see DataColumnSpecListCellRenderer#createInvalidSpec(String)
         * @param columns the column names
         * @return this modifier
         * @throws IllegalArgumentException if one column does already exist in the list, including as well as the
         *             additional columns as well as the original {@link DataTableSpec}
         * @throws IllegalStateException if this modfier is invalid
         */
        ListModifier addInvalidColumns(final String... columns);

        /**
         * Adds additional columns to the model. See {@link ListModifier} for more information about additional columns.
         *
         * @param columns the columns to add
         * @return this modfier
         * @throws IllegalArgumentException if the column does already exist in the list, including as well as the
         *             additional columns as well as the original {@link DataTableSpec}
         * @throws IllegalStateException if this modfier is invalid
         */
        ListModifier addAdditionalColumns(final Iterable<DataColumnSpec> columns);

        /**
         * Adds additional columns to the model. See {@link ListModifier} for more information about additional columns.
         *
         * @param columns the columns to add
         * @return this modfier
         * @throws IllegalArgumentException if one column does already exist in the list, including as well as the
         *             additional columns as well as the original {@link DataTableSpec}
         * @throws IllegalStateException if this modfier is invalid
         */
        ListModifier addAdditionalColumns(final DataColumnSpec... columns);

        /**
         * Adds an additional column to the model. See {@link ListModifier} for more information about additional
         * columns.
         *
         * @param column the column to add
         * @return this modfier
         * @throws IllegalArgumentException if one column does already exist in the list, including as well as the
         *             additional columns as well as the original {@link DataTableSpec}
         * @throws IllegalStateException if this modfier is invalid
         */
        ListModifier addAdditionalColumn(DataColumnSpec column);

        /**
         * Removes additional columns from the model. See {@link ListModifier} for more information about additional
         * columns.
         *
         * @param columns the columns to remove
         * @return this modfier
         * @throws IllegalStateException if this modfier is invalid
         * @throws IllegalArgumentException if one column is contained in the original {@link DataTableSpec}
         */
        ListModifier removeAdditionalColumns(final String... columns);

        /**
         * Removes additional columns from the model. See {@link ListModifier} for more information about additional
         * columns.
         *
         * @param columns the columns to remove
         * @return this modfier
         * @throws IllegalStateException if this modfier is invalid
         * @throws IllegalArgumentException if one column is contained in the original {@link DataTableSpec}
         */
        ListModifier removeAdditionalColumns(final Iterable<String> columns);

        /**
         * Removes an additional column from the model. See {@link ListModifier} for more information about additional
         * columns.
         *
         * @param column the column to remove
         * @return this modfier
         * @throws IllegalStateException if this modfier is invalid
         * @throws IllegalArgumentException if the column is contained in the original {@link DataTableSpec}
         */
        ListModifier removeAdditionalColumn(final String column);

    }

    /**
     * Comprises the search results after the user performed a search.
     *
     * @author Marcel Hanser
     */
    public static final class SearchEvent extends EventObject {

        private final int[] m_searchHitIndices;

        private final DataColumnSpec[] m_searchHits;

        private final String m_searchString;

        private SearchEvent(final Object theSource, final String searchString, final int[] searchHitIndices,
            final DataColumnSpec[] searchHits) {
            super(theSource);
            m_searchString = searchString;
            this.m_searchHitIndices = searchHitIndices;
            this.m_searchHits = searchHits;
        }

        /**
         * @return the searchString
         */
        public String getSearchString() {
            return m_searchString;
        }

        /**
         * @return the matching items indices
         */
        public int[] getSearchHitIndices() {
            return m_searchHitIndices;
        }

        /**
         * @return the matching items
         */
        public DataColumnSpec[] getSearchHits() {
            return m_searchHits;
        }
    }

    /**
     * Comprises if the user either want to delete, to create a configuration for the selected items. Or if one changed
     * the selection on the list.
     *
     * @author Marcel Hanser
     */
    public static final class ConfigurationRequestEvent extends EventObject {
        private final Type m_type;

        private ConfigurationRequestEvent(final Object theSource, final Type type) {
            super(theSource);
            m_type = type;
        }

        /**
         * @return the type
         */
        public Type getType() {
            return m_type;
        }

        /**
         * The type of a {@link ConfigurationRequestEvent}.
         *
         * @author Marcel Hanser
         */
        public enum Type {
            /**
             * The user wants to create a configuration.
             */
            CREATION,
            /**
             * The user wants to delete a configuration.
             */
            DELETION,
            /**
             * The user changed the selection.
             */
            SELECTION;
        }

    }

    /**
     * Implementation for the ListModifier.
     *
     * @author Marcel Hanser
     */
    private static final class ModifierImpl implements ListModifier {
        private ColumnSelectionSearchableListPanel m_parent;

        private final DataTableSpec m_spec;

        private final Map<String, DataColumnSpec> m_additionalNames = new HashMap<String, DataColumnSpec>();

        /**
         * @param parent
         */
        private ModifierImpl(final ColumnSelectionSearchableListPanel parent, final DataTableSpec spec) {
            super();
            m_parent = parent;
            m_spec = spec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier addInvalidColumns(final DataType type, final String... columns) {
            checkValid();
            for (String column : columns) {
                addAdditionalColumn(DataColumnSpecListCellRenderer.createInvalidSpec(column, type));
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier addInvalidColumns(final String... columns) {
            checkValid();
            for (String column : columns) {
                addAdditionalColumn(DataColumnSpecListCellRenderer.createInvalidSpec(column));
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier addAdditionalColumns(final DataColumnSpec... columns) {
            checkValid();
            return addAdditionalColumns(Arrays.asList(columns));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier addAdditionalColumns(final Iterable<DataColumnSpec> cspecs) {
            checkValid();
            for (DataColumnSpec cspec : cspecs) {
                addAdditionalColumn(cspec);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier addAdditionalColumn(final DataColumnSpec additionalColumn) {
            checkValid();
            String name = additionalColumn.getName();
            CheckUtils.checkArgument(!m_additionalNames.containsKey(name),
                "column '%s' has been already added as additional column ", name);
            CheckUtils.checkArgument(!m_spec.containsName(name), "column '%s' already exists in input table ", name);
            m_parent.setUnkownFilterVisible(true);
            m_additionalNames.put(name, additionalColumn);
            ((DefaultListModel)m_parent.m_columnList.getModel()).addElement(additionalColumn);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier removeAdditionalColumns(final String... columns) {
            checkValid();
            removeAdditionalColumns(Arrays.asList(columns));
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier removeAdditionalColumns(final Iterable<String> columns) {
            checkValid();
            for (String col : columns) {
                removeAdditionalColumn(col);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ListModifier removeAdditionalColumn(final String columns) {
            checkValid();
            CheckUtils.checkArgument(!m_spec.containsName(columns),
                "column: '%s' cannot be deleted as it is part of the original DataTableSpec", columns);
            DataColumnSpec dataColumnSpec = m_additionalNames.remove(columns);
            if (dataColumnSpec != null) {
                ((DefaultListModel)m_parent.m_columnList.getModel()).removeElement(dataColumnSpec);
            }
            if (m_additionalNames.isEmpty()) {
                m_parent.setUnkownFilterVisible(false);
            }
            return this;
        }

        private void invalidate() {
            m_parent = null;
        }

        private void checkValid() {
            if (m_parent == null) {
                throw new IllegalStateException("Modifier is not longer valid, a new has already been created");
            }
        }
    }

    /**
     * Notifies {@code SearchListener}s added of searches evaluated by the user.
     * <p>
     * This method constructs a {@code SearchEvent} with this as the source, the matching items/indeces and the original
     * search string and sends it to the registered {@code SearchListener}.
     *
     * @param searchString the original search string
     * @param searchHitIndices indeces of the matching items
     * @param searchHits the mathing items
     *
     * @see #addSearchListener
     * @see #removeSearchListener
     * @see SearchEvent
     */
    protected void fireSearchEvaluated(final String searchString, final int[] searchHitIndices,
        final DataColumnSpec[] searchHits) {
        Object[] listeners = listenerList.getListenerList();
        SearchEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SearchListener.class) {
                if (e == null) {
                    e = new SearchEvent(this, searchString, searchHitIndices, searchHits);
                }
                ((SearchListener)listeners[i + 1]).searchEvaluated(e);
            }
        }
    }

    /**
     * Notifies {@code SearchListener}s added of searches evaluated by the user.
     * <p>
     * This method constructs a {@code ConfigurationRequestEvent} with this as the source and the information if the
     * user want to create or delete an configuration and sends it to the registered {@code SearchListener}.
     *
     * @param requestType the request type
     *
     * @see #addConfigurationRequestListener
     * @see #removeConfigurationRequestListener
     * @see ConfigurationRequestEvent
     */
    protected void fireConfigurationRequested(final Type requestType) {
        Object[] listeners = listenerList.getListenerList();
        ConfigurationRequestEvent e = null;

        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ConfigurationRequestListener.class) {
                if (e == null) {
                    e = new ConfigurationRequestEvent(this, requestType);
                }
                ((ConfigurationRequestListener)listeners[i + 1]).configurationRequested(e);
            }
        }
        m_columnList.repaint();
    }

    private void addKeyMouseSelectionListerersToColumnList() {
        m_columnList.addKeyListener(new KeyAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void keyTyped(final KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyChar()) {
                    fireConfigurationRequested(Type.CREATION);
                } else if (KeyEvent.VK_DELETE == e.getKeyChar()) {
                    fireConfigurationRequested(Type.DELETION);
                }
            }
        });

        m_columnList.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    fireConfigurationRequested(Type.CREATION);
                }
            }

        });

        m_columnList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                fireConfigurationRequested(Type.SELECTION);
            }
        });
    }

    private void setDefaultCellRenderer() {
        m_cellRenderer = new DataColumnSpecListCellRenderer() {
            //            private static final Font DEFAULT_FONT = new Font() ;
            /** {@inheritDoc} */
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
                final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                //                if (isSelected) {
                //                    return comp;
                //                }

                final DataColumnSpec cspec = (DataColumnSpec)value;
                if (m_container.isConfiguredColumn(cspec)) {
                    Font font2 = new Font(comp.getFont().getName(), Font.ITALIC | Font.BOLD, comp.getFont().getSize());
                    comp.setFont(font2);
                }

                if (isInvalid(cspec)) {
                    setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                }
                return comp;
            }
        };

        m_columnList.setCellRenderer(new DefaultListCellRenderer() {
            /**
             * {@inheritDoc}
             */
            @Override
            public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                final boolean isSelected, final boolean cellHasFocus) {
                return m_cellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
    }

    // Here is the actual search happening, so called the heart of this class ;)
    private void processSearch() {
        int[] searchHits =
            ListModelFilterUtils.getAllSearchHits(m_columnList.getUnfilteredModel(), m_searchField.getText());
        DataColumnSpec[] searchedColumns;

        List<Integer> goodIndices = new ArrayList<Integer>();
        List<DataColumnSpec> goodSpecs = new ArrayList<DataColumnSpec>();
        // go over all search hits and filter them according to the FilterOption
        for (int i = 0; i < searchHits.length; i++) {
            int selectedIndex = searchHits[i];
            DataColumnSpec currentCol = (DataColumnSpec)m_columnList.getUnfilteredModel().getElementAt(selectedIndex);

            switch ((FilterOption)m_filters.getSelectedItem()) {
                case UNKNOWN:
                    if (isInvalid(currentCol)) {
                        goodIndices.add(selectedIndex);
                        goodSpecs.add(currentCol);
                    }
                    break;
                case MODIFIED:
                    if (m_container.isConfiguredColumn(currentCol)) {
                        goodIndices.add(selectedIndex);
                        goodSpecs.add(currentCol);
                    }
                    break;
                case UNMODIFIED:
                    if (!isInvalid(currentCol) && !m_container.isConfiguredColumn(currentCol)) {
                        goodIndices.add(selectedIndex);
                        goodSpecs.add(currentCol);
                    }
                    break;
                default:
                    goodIndices.add(selectedIndex);
                    goodSpecs.add(currentCol);
            }

        }

        searchHits = toPrimitive(goodIndices);

        if (differsFromLastSearchHits(searchHits)) {
            m_columnList.filterItems(searchHits);
            m_columnList.setSelectedIndex(0);

            m_lastSearchHits.clear();
            for (int i : searchHits) {
                m_lastSearchHits.add(i);
            }
            searchedColumns = goodSpecs.toArray(new DataColumnSpec[goodSpecs.size()]);

        } else {
            searchedColumns = new DataColumnSpec[0];
        }

        fireSearchEvaluated(m_searchField.getText(), createIncreasingIntegerArray(searchHits.length), searchedColumns);
    }

    private int[] createIncreasingIntegerArray(final int length) {
        int[] toReturn = new int[length];
        for (int i = 0; i < length; i++) {
            toReturn[i] = i;
        }
        return toReturn;
    }

    private boolean differsFromLastSearchHits(final int[] searchHits) {
        if (m_lastSearchHits.isEmpty()) {
            return true;
        }
        if (searchHits.length != m_lastSearchHits.size()) {
            return true;
        }
        for (int i : searchHits) {
            if (!m_lastSearchHits.contains(i)) {
                return true;
            }
        }
        return false;
    }

    private ModifierImpl innerGetModifier() {
        if (m_currentModifier == null) {
            throw new IllegalStateException("#update has not been called previously.");
        }
        return m_currentModifier;
    }

    /**
     * @param b <code>true</code> if the unkonwn filter otion should be visible
     */
    private void setUnkownFilterVisible(final boolean b) {
        DefaultComboBoxModel model = (DefaultComboBoxModel)m_filters.getModel();
        model.removeElement(FilterOption.UNKNOWN);
        if (b) {
            model.addElement(FilterOption.UNKNOWN);
        }
    }

    private static int[] toPrimitive(final List<Integer> toConvert) {
        int[] arr = new int[toConvert.size()];

        for (int i = 0; i < toConvert.size(); i++) {
            if (toConvert.get(i) != null) {
                arr[i] = toConvert.get(i);
            }
        }
        return arr;
    }
}
