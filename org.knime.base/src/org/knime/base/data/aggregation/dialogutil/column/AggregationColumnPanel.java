/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
package org.knime.base.data.aggregation.dialogutil.column;

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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionAndRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer.ValueRenderer;
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
 * @since 2.11
 */
public class AggregationColumnPanel
    extends AbstractAggregationPanel<AggregationColumnTableModel, ColumnAggregator, DataColumnSpec> {
    /**The default title of the panel to display in a dialog.*/
    public static final String DEFAULT_TITLE = "Manual Aggregation";

    /**This field holds all columns of the input table.*/
    private final List<DataColumnSpec> m_avAggrColSpecs = new LinkedList<>();
    private final String m_key;

    /**
     * {@inheritDoc}
     */
    @Override
    protected JPopupMenu createTablePopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        final JMenuItem invalidRowsMenu = createInvalidRowsSelectionMenu();
        if (invalidRowsMenu != null) {
            menu.add(invalidRowsMenu);
            menu.addSeparator();
        }
        createColumnSelectionMenu(menu);
        menu.addSeparator();
        appendMissingValuesEntry(menu);
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
        final Collection<Class<? extends DataValue>> existingTypes = getAllPresentTypes();
        if (existingTypes.size() < 3) {
            //create no sub menu if their are to few different types
            for (final Class<? extends DataValue> type : existingTypes) {
                if (type == DataValue.class || type == DoubleValue.class) {
                    //skip the general and numerical types
                    continue;
                }
                final JMenuItem selectCompatible =
                    new JMenuItem("Select " + AggregationMethods.getUserTypeLabel(type).toLowerCase() + " columns");
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
                    new JMenuItem(AggregationMethods.getUserTypeLabel(type).toLowerCase() + " columns");
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
            final JMenuItem selectNoneNumerical = new JMenuItem("Select all numerical columns");
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
            final JMenuItem selectNoneNumerical = new JMenuItem("Select all non-numerical columns");
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
        final JMenuItem selectAll = new JMenuItem("Select all columns");
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
     * Adds the aggregation method section to the given menu.
     * This section allows the user to set an aggregation method for
     * all selected columns.
     * @param menu the menu to append the aggregation section
     */
    private void createAggregationSection(final JPopupMenu menu) {
        if (getSelectedRows().length <= 0) {
                final JMenuItem noneSelected = new JMenuItem("Select a column to change method");
                noneSelected.setEnabled(false);
                menu.add(noneSelected);
                return;
        }
        final List<Entry<String, List<AggregationMethod>>> methodList = getMethods4SelectedItems();
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
            for (final Entry<String, List<AggregationMethod>> entry : methodList) {
                final String type = entry.getKey();
                final List<AggregationMethod> methods = entry.getValue();
                final JMenu menuItem = new JMenu(type + " Methods");
                final JMenuItem subMenu = menu.add(menuItem);
                for (final AggregationMethod method : methods) {
                    final JMenuItem methodItem = new JMenuItem(method.getLabel());
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
        this(" Aggregation settings ");
    }

    /**Constructor for class AggregationColumnPanel.
     * @param title the title of the border or <code>null</code> for no border
     * @since 2.10
    */
   public AggregationColumnPanel(final String title) {
        this(title, null);
    }

   /**
     * @param title the title of the border or <code>null</code> for no border
     * @param key the unique settings key
     * @since 2.11
     */
    public AggregationColumnPanel(final String title, final String key) {
       super(title, " Available columns ", new DataColumnSpecListCellRenderer(),
           " To change multiple columns use right mouse click for context menu. ", new AggregationColumnTableModel());
        m_key = key;
   }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
                new AggregationFunctionRowTableCellRenderer<>(new ValueRenderer<ColumnAggregator>() {

                    @Override
                    public void renderComponent(final DefaultTableCellRenderer c, final ColumnAggregator row) {
                        final DataColumnSpec spec = row.getOriginalColSpec();
                        c.setText(spec.getName());
                        c.setIcon(spec.getType().getIcon());
                    }
                }, true, "Double click to remove column. Right mouse click for context menu."));
        columnModel.getColumn(1).setCellEditor(new ColumnAggregatorTableCellEditor());
        columnModel.getColumn(1).setCellRenderer(new AggregationFunctionAndRowTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
    }

    /**
     * @return all supported types with at least one row in the table
     */
    public Collection<Class<? extends DataValue>> getAllPresentTypes() {
        final Collection<Class<? extends DataValue>> supportedTypes = AggregationMethods.getSupportedTypes();
        final Collection<Class<? extends DataValue>> existingTypes = new LinkedList<>();
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
        getTableModel().setAggregationMethod(selectedRows, AggregationMethods.getMethod4Id(methodId));
        final Collection<Integer> idxs = new LinkedList<>();
        for (final int i : selectedRows) {
            idxs.add(Integer.valueOf(i));
        }
        updateSelection(idxs);
    }

    /**
     * Selects all rows that are compatible with the given type.
     * @param type the type to check for compatibility
     */
    protected void selectCompatibleRows(final Class<? extends DataValue> type) {
        final Collection<Integer> idxs = getTableModel().getCompatibleRowIdxs(type);
        updateSelection(idxs);
    }

    /**
     * Returns the number of rows that are compatible to the given type.
     * @param type the type to check for
     * @return the number of compatible rows
     */
    int noOfCompatibleRows(final Class<? extends DataValue> type) {
        return getTableModel().getCompatibleRowIdxs(type).size();
    }

    /**
     * @param excludeColNames the name of all columns that should be
     * excluded from the aggregation panel
     */
    public void excludeColsChange(final Collection<String> excludeColNames) {
        final Set<String> excludeColNameSet = new HashSet<>(excludeColNames);
        final List<DataColumnSpec> newList = new LinkedList<>();
        //include all columns that are not in the exclude list
        for (final DataColumnSpec colSpec : m_avAggrColSpecs) {
            if (!excludeColNameSet.contains(colSpec.getName())) {
                newList.add(colSpec);
            }
        }
        final List<ColumnAggregator> oldAggregators = getTableModel().getRows();
        final List<ColumnAggregator> newAggregators = new LinkedList<>();
        for (final ColumnAggregator aggregator : oldAggregators) {
            if (!excludeColNameSet.contains(aggregator.getOriginalColName())) {
                newAggregators.add(aggregator);
            }
        }
        initialize(newList, newAggregators, getInputTableSpec());
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        ColumnAggregator.saveColumnAggregators(settings, m_key, getTableModel().getRows());
    }

    /**
     * @param settings the settings to read from
     * @param spec initializes the component
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
    throws InvalidSettingsException {
        initialize(spec, ColumnAggregator.loadColumnAggregators(settings, m_key, spec));
    }

    /**
     * Initializes the panel.
     * @param spec the {@link DataTableSpec} of the input table
     * @param colAggrs the {@link List} of {@link ColumnAggregator}s that are
     * initially used
     */
    public void initialize(final DataTableSpec spec, final List<ColumnAggregator> colAggrs) {
        m_avAggrColSpecs.clear();
        final List<DataColumnSpec> listElements = new LinkedList<>();
        for (final DataColumnSpec colSpec : spec) {
            m_avAggrColSpecs.add(colSpec);
            listElements.add(colSpec);
        }
      //remove all invalid column aggregator
        final List<ColumnAggregator> colAggrs2Use = new ArrayList<>(colAggrs.size());
        for (final ColumnAggregator colAggr : colAggrs) {
            final DataColumnSpec colSpec = spec.getColumnSpec(colAggr.getOriginalColName());
            final boolean valid;
            if (colSpec != null && colAggr.getOriginalDataType().isASuperTypeOf(colSpec.getType())) {
                valid = true;
            } else {
                valid = false;
            }
            colAggr.setValid(valid);
            colAggrs2Use.add(colAggr);
        }
        initialize(listElements, colAggrs2Use, spec);
    }

    /**
     * @return a label list of all supported methods for the currently
     * selected rows
     */
    protected List<Entry<String, List<AggregationMethod>>> getMethods4SelectedItems() {
        final int[] selectedColumns = getSelectedRows();
        final Set<DataType> types = new HashSet<>(selectedColumns.length);
        for (final int row : selectedColumns) {
            final ColumnAggregator aggregator = getTableModel().getRow(row);
            types.add(aggregator.getOriginalDataType());
        }
        final DataType superType = CollectionCellFactory.getElementType(types.toArray(new DataType[0]));
        final List<Entry<String, List<AggregationMethod>>> list =
                AggregationMethods.getCompatibleMethodGroupList(superType);
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnAggregator createRow(final DataColumnSpec colSpec) {
        final AggregationMethod defaultMethod = AggregationMethods.getDefaultMethod(colSpec);
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
    @Override
    @Deprecated
    protected void selectAllRows() {
        getTable().selectAll();
    }
}
