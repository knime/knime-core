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
 * History
 *    27.08.2008 (Tobias Koetter): created
 */

package org.knime.base.data.aggregation.dialogutil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnPanel
extends AbstractAggregationPanel<AggregationColumnTableModel,
ColumnAggregator, DataColumnSpec> {

    /**This field holds all columns of the input table.*/
    private final List<DataColumnSpec> m_avAggrColSpecs =
        new LinkedList<DataColumnSpec>();

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        if (getNoOfTableRows() == 0) {
            //the table contains no rows
            final JMenuItem item =
                new JMenuItem("No column(s) available");
            item.setEnabled(false);
            menu.add(item);
            return menu;
        }
        createColumnSelectionMenu(menu);
        createMissingValuesMenu(menu);
        menu.addSeparator();
        createAggregationSection(menu);
        return menu;
    }

    /**
     * Adds the column selection section to the given menu.
     * This section allows the user to select all rows that are compatible
     * to the chosen data type at once.
     * @param menu the menu to append the column selection section
     */
    private void createColumnSelectionMenu(final JPopupMenu menu) {
        final Collection<Class<? extends DataValue>> existingTypes =
            getAllPresentTypes();
        if (existingTypes.size() < 3) {
            //create no sub menu if their are to few different types
            for (final Class<? extends DataValue> type : existingTypes) {
                if (type == DataValue.class || type == DoubleValue.class) {
                    //skip the general and numerical types
                    continue;
                }
                final JMenuItem selectCompatible =
                    new JMenuItem("Select "
                            + AggregationMethods.getUserTypeLabel(
                                    type).toLowerCase()
                            + " columns");
                selectCompatible.addActionListener(new ActionListener() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        selectCompatibleRows(type);
                    }
                });
                menu.add(selectCompatible);
            }
        } else {
            //create a column selection sub menu
            final JMenu menuItem = new JMenu("Select all...");
            final JMenuItem supportedMenu = menu.add(menuItem);
            for (final Class<? extends DataValue> type : existingTypes) {
                if (type == DataValue.class || type == DoubleValue.class) {
                    //skip the general and numerical types
                    continue;
                }
                final JMenuItem selectCompatible =
                    new JMenuItem(AggregationMethods.getUserTypeLabel(
                                    type).toLowerCase()
                            + " columns");
                selectCompatible.addActionListener(new ActionListener() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        selectCompatibleRows(type);
                    }
                });
                supportedMenu.add(selectCompatible);
            }
        }
      //add the select numerical columns entry if they are available
        final Collection<Integer> numericIdxs =
            getTableModel().getCompatibleRowIdxs(DoubleValue.class);
        if (numericIdxs != null && !numericIdxs.isEmpty()) {
            final JMenuItem selectNoneNumerical =
                new JMenuItem("Select all numerical columns");
            selectNoneNumerical.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    updateSelection(numericIdxs);
                }
            });
            menu.add(selectNoneNumerical);
        }
        //add the select none numerical columns entry if they are available
        final Collection<Integer> nonNumericIdxs =
            getTableModel().getNotCompatibleRowIdxs(DoubleValue.class);
        if (nonNumericIdxs != null && !nonNumericIdxs.isEmpty()) {
            final JMenuItem selectNoneNumerical =
                new JMenuItem("Select all non-numerical columns");
            selectNoneNumerical.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    updateSelection(nonNumericIdxs);
                }
            });
            menu.add(selectNoneNumerical);
        }
        //add the select all columns entry
        final JMenuItem selectAll =
            new JMenuItem("Select all columns");
        selectAll.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                getTable().selectAll();
            }
        });
        menu.add(selectAll);
    }

    /**
     * @param menu
     */
    private void createMissingValuesMenu(final JPopupMenu menu) {
        if (getSelectedRows().length <= 0) {
            //show this option only if at least one row is selected
            return;
        }
        menu.addSeparator();
      //add the select all columns entry
        final JMenuItem toggleMissing =
            new JMenuItem("Toggle missing cell option");
        toggleMissing.setToolTipText(
                "Changes the include missing cell option");
        toggleMissing.addActionListener(new ActionListener() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void actionPerformed(final ActionEvent e) {
                toggleMissingCellOption();
            }
        });
        menu.add(toggleMissing);
    }

    /**
     * Adds the aggregation method section to the given menu.
     * This section allows the user to set an aggregation method for
     * all selected columns.
     * @param menu the menu to append the aggregation section
     */
    private void createAggregationSection(final JPopupMenu menu) {
        if (getSelectedRows().length <= 0) {
                final JMenuItem noneSelected =
                    new JMenuItem("Select a column to change method");
                noneSelected.setEnabled(false);
                menu.add(noneSelected);
                return;
        }
        final List<Entry<String, List<AggregationMethod>>>
            methodList = getMethods4SelectedItems();
        if (methodList.size() == 1) {
            //we need no sub menu for a single group
            for (final AggregationMethod method
                    : methodList.get(0).getValue()) {
                final JMenuItem methodItem =
                    new JMenuItem(method.getLabel());
                methodItem.setToolTipText(method.getDescription());
                methodItem.addActionListener(new ActionListener() {
                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        changeAggregationMethod(method.getId());
                    }
                });
                menu.add(methodItem);
            }
        } else {
            for (final Entry<String, List<AggregationMethod>> entry
                    : methodList) {
                final String type = entry.getKey();
                final List<AggregationMethod> methods =
                    entry.getValue();
                final JMenu menuItem = new JMenu(type + " Methods");
                final JMenuItem subMenu = menu.add(menuItem);
                for (final AggregationMethod method : methods) {
                    final JMenuItem methodItem =
                        new JMenuItem(method.getLabel());
                    methodItem.setToolTipText(method.getDescription());
                    methodItem.addActionListener(new ActionListener() {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        public void actionPerformed(
                                final ActionEvent e) {
                            changeAggregationMethod(method.getId());
                        }
                    });
                    subMenu.add(methodItem);
                }
            }
        }
    }



    /**Constructor for class AggregationColumnPanel.
     *
     */
    public AggregationColumnPanel() {
        super(" Aggregation settings ", " Available columns ",
                new DataColumnSpecListCellRenderer(), " To change multiple "
                + "columns use right mouse click for context menu. ",
                new AggregationColumnTableModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
                new DataColumnSpecTableCellRenderer());
        columnModel.getColumn(1).setCellEditor(
                new AggregationMethodTableCellEditor(getTableModel()));
        columnModel.getColumn(1).setCellRenderer(
                new AggregationMethodTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
    }
    /**
     * @return all supported types with at least one row in the table
     */
    public Collection<Class<? extends DataValue>> getAllPresentTypes() {
        final Collection<Class<? extends DataValue>> supportedTypes =
            AggregationMethods.getSupportedTypes();
        final Collection<Class<? extends DataValue>> existingTypes =
            new LinkedList<Class<? extends DataValue>>();
        for (final Class<? extends DataValue> type : supportedTypes) {
            if (noOfCompatibleRows(type) > 0) {
                //add only types that have at least one row
                existingTypes.add(type);
            }
        }
        return existingTypes;
    }

    /**
     * Changes the aggregation method of all selected rows to the method
     * with the given label.
     * @param methodId the label of the aggregation method
     */
    protected void changeAggregationMethod(final String methodId) {
        final int[] selectedRows = getSelectedRows();
        getTableModel().setAggregationMethod(selectedRows,
                AggregationMethods.getMethod4Id(methodId));
        final Collection<Integer> idxs = new LinkedList<Integer>();
        for (final int i : selectedRows) {
            idxs.add(i);
        }
        updateSelection(idxs);
    }

    /**
     * Selects all rows that are compatible with the given type.
     * @param type the type to check for compatibility
     */
    protected void selectCompatibleRows(
            final Class<? extends DataValue> type) {
        final Collection<Integer> idxs =
            getTableModel().getCompatibleRowIdxs(type);
        updateSelection(idxs);
    }

    /**
     * Returns the number of rows that are compatible to the given type.
     * @param type the type to check for
     * @return the number of compatible rows
     */
    int noOfCompatibleRows(
            final Class<? extends DataValue> type) {
        final Collection<Integer> idxs =
            getTableModel().getCompatibleRowIdxs(type);
        return idxs != null ? idxs.size() : 0;
    }

    /**
     * @param idxs the indices to select
     */
    void updateSelection(final Collection<Integer> idxs) {
        if (idxs == null || idxs.isEmpty()) {
            getTable().clearSelection();
            return;
        }
        boolean first = true;
        for (final Integer idx : idxs) {
            if (idx.intValue() < 0) {
                continue;
            }
            if (first) {
                first = false;
                getTable().setRowSelectionInterval(idx.intValue(),
                        idx.intValue());
            } else {
                getTable().addRowSelectionInterval(idx.intValue(),
                        idx.intValue());
            }
        }
    }

    /**
     * @param excludeColNames the name of all columns that should be
     * excluded from the aggregation panel
     */
    public void excludeColsChange(final Collection<String> excludeColNames) {
        final Set<String> excludeColNameSet =
            new HashSet<String>(excludeColNames);
        final List<DataColumnSpec> newList = new LinkedList<DataColumnSpec>();
        //include all columns that are not in the exclude list
        for (final DataColumnSpec colSpec : m_avAggrColSpecs) {
            if (!excludeColNameSet.contains(colSpec.getName())) {
                newList.add(colSpec);
            }
        }
        final List<ColumnAggregator> oldAggregators = getTableModel().getRows();
        final List<ColumnAggregator> newAggregators =
            new LinkedList<ColumnAggregator>();
        for (final ColumnAggregator aggregator : oldAggregators) {
            if (!excludeColNameSet.contains(aggregator.getOriginalColName())) {
                newAggregators.add(aggregator);
            }
        }
        initialize(newList, newAggregators);
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        ColumnAggregator.saveColumnAggregators(settings,
                getTableModel().getRows());
    }

    /**
     * @param settings the settings to read from
     * @param spec initializes the component
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec spec)
    throws InvalidSettingsException {
        initialize(spec, ColumnAggregator.loadColumnAggregators(settings));
    }

    /**
     * Initializes the panel.
     * @param spec the {@link DataTableSpec} of the input table
     * @param colAggrs the {@link List} of {@link ColumnAggregator}s that are
     * initially used
     */
    public void initialize(final DataTableSpec spec,
            final List<ColumnAggregator> colAggrs) {
        m_avAggrColSpecs.clear();
        final List<DataColumnSpec> listElements =
            new LinkedList<DataColumnSpec>();
        for (final DataColumnSpec colSpec : spec) {
            m_avAggrColSpecs.add(colSpec);
            listElements.add(colSpec);
        }
      //remove all invalid column aggregator
        final List<ColumnAggregator> colAggrs2Use =
            new ArrayList<ColumnAggregator>(colAggrs.size());
        for (final ColumnAggregator colAggr : colAggrs) {
            final DataColumnSpec colSpec =
                spec.getColumnSpec(colAggr.getOriginalColName());
            if (colSpec != null
                    && colSpec.getType().equals(
                            colAggr.getOriginalDataType())) {
                colAggrs2Use.add(colAggr);
            }
        }
        initialize(listElements, colAggrs2Use);
    }

    /**
     * @return a label list of all supported methods for the currently
     * selected rows
     */
    protected List<Entry<String, List<AggregationMethod>>>
        getMethods4SelectedItems() {
        final int[] selectedColumns = getSelectedRows();
        final Set<DataType> types =
            new HashSet<DataType>(selectedColumns.length);
        for (final int row : selectedColumns) {
            final ColumnAggregator aggregator = getTableModel().getRow(row);
            types.add(aggregator.getOriginalDataType());
        }
        final DataType superType = CollectionCellFactory.getElementType(
                types.toArray(new DataType[0]));
        final List<Entry<String, List<AggregationMethod>>> list =
                AggregationMethods.getCompatibleMethodGroupList(superType);
        return list;
    }

    /**
     * {@inheritDoc}
     * @since 2.6
     */
    @Override
    protected ColumnAggregator[] createEmptyOperatorArray(final int size) {
        return new ColumnAggregator[size];
    }

    /**
     * {@inheritDoc}
     * @since 2.6
     */
    @Override
    protected ColumnAggregator getOperator(
            final DataColumnSpec colSpec) {
        final AggregationMethod defaultMethod =
            AggregationMethods.getDefaultMethod(colSpec);
        return new ColumnAggregator(colSpec, defaultMethod);
    }


    /**Use the {@link #getTableModel()} method instead to get the
     * table model where you can call the <code>getRowCount()</code> method.
     * @return the number of aggregation columns
     * @see #getTableModel()
     */
    @Deprecated
    protected int getAggregationColumnCount() {
        return getTableModel().getRowCount();
    }


    /**
     * Selects all rows. Use the {@link #getTable()} method instead to get the
     * table model where you can call the <code>selectAll()</code> method.
     * @see #getTable()
     */
    @Deprecated
    protected void selectAllRows() {
        getTable().selectAll();
    }
}
