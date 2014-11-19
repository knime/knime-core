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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.io.database.groupby.dialog.type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.knime.base.data.aggregation.ColumnAggregator;
import org.knime.base.data.aggregation.dialogutil.AbstractAggregationPanel;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionAndRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer;
import org.knime.base.data.aggregation.dialogutil.AggregationFunctionRowTableCellRenderer.ValueRenderer;
import org.knime.base.data.aggregation.dialogutil.type.DataTypeNameSorter;
import org.knime.base.node.io.database.groupby.dialog.DBAggregationFunctionProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.database.DatabaseUtility;
import org.knime.core.node.port.database.aggregation.AggregationFunctionProvider;
import org.knime.core.node.port.database.aggregation.DBAggregationFunction;
import org.knime.core.node.port.database.aggregation.function.CountDBAggregationFunction;
import org.knime.core.node.util.DataTypeListCellRenderer;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class DBDataTypeAggregationFunctionPanel
extends AbstractAggregationPanel<DBDataTypeAggregationFunctionTableModel,
DBDataTypeAggregationFunctionRow, DataType> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DBDataTypeAggregationFunctionPanel.class);

    /**This field holds all columns of the input table.*/
    private final List<DataColumnSpec> m_avAggrColSpecs = new LinkedList<>();
    private final String m_key;

    private DBDataTypeAggregationFunctionRowTableCellEditor m_aggregationFunctionCellEditor;

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
        final Collection<DataType> existingTypes = getAllPresentTypes();
        if (existingTypes.size() < 3) {
            //create no sub menu if their are to few different types
            for (final DataType type : existingTypes) {
                if (type == DoubleCell.TYPE || type == StringCell.TYPE) {
                    //skip the general and numerical types
                    continue;
                }
                final JMenuItem selectCompatible =
                    new JMenuItem("Select " + type.toString() + " columns");
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
            for (final DataType type : existingTypes) {
                if (type == DoubleCell.TYPE) {
                    //skip the general and numerical types
                    continue;
                }
                final JMenuItem selectCompatible =
                    new JMenuItem(type.toString() + " columns");
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
            getTableModel().getCompatibleRowIdxs(DoubleCell.TYPE);
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
            getTableModel().getNotCompatibleRowIdxs(DoubleCell.TYPE);
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
     * @return all {@link DataType}s with at least one row in the table
     */
    private Collection<DataType> getAllPresentTypes() {
        final List<DBDataTypeAggregationFunctionRow> rows = getTableModel().getRows();
        final Set<DataType> types = new HashSet<>();
        for (DBDataTypeAggregationFunctionRow row : rows) {
            types.add(row.getDataType());
        }
        return types;
    }

    /**
     * Adds the aggregation method section to the given menu.
     * This section allows the user to set an aggregation method for
     * all selected columns.
     * @param menu the menu to append the aggregation section
     */
    private void createAggregationSection(final JPopupMenu menu) {
        if (getSelectedRows().length <= 0) {
                final JMenuItem noneSelected = new JMenuItem("Select a row to change method");
                noneSelected.setEnabled(false);
                menu.add(noneSelected);
                return;
        }
        final List<DBAggregationFunction> methodList = getMethods4SelectedItems();
        //we need no sub menu for a single group
        for (final DBAggregationFunction method : methodList) {
            final JMenuItem methodItem = new JMenuItem(method.getLabel());
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
    }


    /**Constructor for class AggregationColumnPanel.
     * @param key the unique settings key
     *
     */
    public DBDataTypeAggregationFunctionPanel(final String key) {
        this(" Aggregation settings ", key);
    }

   /**
     * @param title the title of the border or <code>null</code> for no border
     * @param key the unique settings key
     */
    public DBDataTypeAggregationFunctionPanel(final String title, final String key) {
       super(title, " Available data type ", new DataTypeListCellRenderer(),
           " To change multiple columns use right mouse click for context menu. ",
           new DBDataTypeAggregationFunctionTableModel());
       if (key == null) {
           throw new IllegalArgumentException("key must not be null");
       }
        m_key = key;
   }

    /**
     * @param provider the {@link AggregationFunctionProvider}
     */
    public void setAggregationFunctionProvider(final AggregationFunctionProvider<DBAggregationFunction> provider) {
        getTableModel().setAggregationFunctionProvider(provider);
        m_aggregationFunctionCellEditor.setAggregationFunctionProvider(provider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        columnModel.getColumn(0).setCellRenderer(
                new AggregationFunctionRowTableCellRenderer<>(new ValueRenderer<DBDataTypeAggregationFunctionRow>() {
                    @Override
                    public void renderComponent(final DefaultTableCellRenderer c,
                        final DBDataTypeAggregationFunctionRow row) {
                        final DataType dataType = row.getDataType();
                        c.setText(dataType.toString());
                        c.setIcon(dataType.getIcon());
                    }
                }, true, "Double click to remove column. Right mouse click for context menu."));
        m_aggregationFunctionCellEditor = new DBDataTypeAggregationFunctionRowTableCellEditor(null);
        columnModel.getColumn(1).setCellEditor(m_aggregationFunctionCellEditor);
        columnModel.getColumn(1).setCellRenderer(new AggregationFunctionAndRowTableCellRenderer());
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
    }

    /**
     * Changes the aggregation method of all selected rows to the method
     * with the given label.
     * @param methodId the label of the aggregation method
     */
    protected void changeAggregationMethod(final String methodId) {
        final int[] selectedRows = getSelectedRows();
        AggregationFunctionProvider<DBAggregationFunction> provider = getTableModel().getAggregationFunctionProvider();
        if (provider != null) {
            getTableModel().setAggregationFunction(selectedRows, provider.getFunction(methodId));
            final Collection<Integer> idxs = new LinkedList<>();
            for (final int i : selectedRows) {
                idxs.add(Integer.valueOf(i));
            }
            updateSelection(idxs);
        } else {
            LOGGER.error("Aggregation function provider shouldn not be null");
        }
    }

    /**
     * Selects all rows that are compatible with the given type.
     * @param type the type to check for compatibility
     */
    protected void selectCompatibleRows(final DataType type) {
        final Collection<Integer> idxs = getTableModel().getCompatibleRowIdxs(type);
        updateSelection(idxs);
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        DBDataTypeAggregationFunctionRow.saveFunctions(settings, m_key, getTableModel().getRows());
    }

    /**
     * @param settings the settings to read from
     * @param dbIdentifier the database identifier
     * @param spec initializes the component
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final String dbIdentifier, final DataTableSpec spec)
                throws InvalidSettingsException {
        final DatabaseUtility utility = DatabaseUtility.getUtility(dbIdentifier);
        setAggregationFunctionProvider(new DBAggregationFunctionProvider(utility));
        initialize(spec, DBDataTypeAggregationFunctionRow.loadFunctions(settings, m_key, dbIdentifier, spec));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() throws InvalidSettingsException {
        final List<DBDataTypeAggregationFunctionRow> rows = getTableModel().getRows();
        for (final DBDataTypeAggregationFunctionRow row : rows) {
            DBAggregationFunction function = row.getFunction();
            try {
                function.validate();
            } catch (InvalidSettingsException e) {
                throw new InvalidSettingsException("Exception for data type '" + row.getDataType().toString()
                    + "': " + e.getMessage());
            }
            if (!row.isValid()) {
                throw new InvalidSettingsException("Row for data type '" + row.getDataType().toString()
                    + "' is invalid.");
            }
        }
    }

    /**
     * Initializes the panel.
     * @param spec the {@link DataTableSpec} of the input table
     * @param colAggrs the {@link List} of {@link ColumnAggregator}s that are
     * initially used
     */
    public void initialize(final DataTableSpec spec, final List<DBDataTypeAggregationFunctionRow> colAggrs) {
        m_avAggrColSpecs.clear();
        final List<DataType> listElements = getTypeList(spec);
        initialize(listElements, colAggrs, spec);
    }

    /**
     * @param spec
     * @return the {@link List} of {@link DataType}s the user can choose from
     */
    private List<DataType> getTypeList(final DataTableSpec spec) {
        final Set<DataType> types = new HashSet<>();
        final DataType generalType = DataType.getType(DataCell.class);
        types.add(generalType);
        types.add(BooleanCell.TYPE);
        types.add(IntCell.TYPE);
        types.add(LongCell.TYPE);
        types.add(DoubleCell.TYPE);
        types.add(StringCell.TYPE);
        types.add(DateAndTimeCell.TYPE);
        types.add(ListCell.getCollectionType(generalType));
        types.add(SetCell.getCollectionType(generalType));
        if (spec != null) {
            for (DataColumnSpec colSpec : spec) {
                types.add(colSpec.getType());
            }
        }
        final List<DataType> typeList = new ArrayList<>(types);
        Collections.sort(typeList, DataTypeNameSorter.getInstance());
        return typeList;
    }

    /**
     * @return a label list of all supported methods for the currently
     * selected rows
     */
    protected List<DBAggregationFunction> getMethods4SelectedItems() {
        final int[] selectedRowIdxs = getSelectedRows();
        final Set<DataType> types = new HashSet<>(selectedRowIdxs.length);
        for (final int rowIdx : selectedRowIdxs) {
            final DBDataTypeAggregationFunctionRow row = getTableModel().getRow(rowIdx);
            types.add(row.getDataType());
        }
        final DataType superType = CollectionCellFactory.getElementType(types.toArray(new DataType[0]));
        final List<DBAggregationFunction> list =
                getTableModel().getAggregationFunctionProvider().getCompatibleFunctions(superType, true);
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DBDataTypeAggregationFunctionRow createRow(final DataType type) {
        AggregationFunctionProvider<DBAggregationFunction> provider = getTableModel().getAggregationFunctionProvider();
        final DBAggregationFunction defaultFunction;
        if (provider != null) {
            defaultFunction = provider.getDefaultFunction(type);
        } else {
            defaultFunction = new CountDBAggregationFunction.Factory().createInstance();
        }
        return new DBDataTypeAggregationFunctionRow(type, defaultFunction);
    }
}
