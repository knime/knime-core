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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

import org.knime.base.data.aggregation.AggregationMethod;
import org.knime.base.data.aggregation.AggregationMethods;
import org.knime.base.data.aggregation.ColumnAggregator;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumnModel;


/**
 * This class creates the aggregation column panel that allows the user to
 * define the aggregation columns and their aggregation method.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class AggregationColumnPanel extends MouseAdapter {

    /**The initial dimension of this panel.*/
    public static final Dimension PANEL_DIMENSION = new Dimension(650, 200);

    private static final int BUTTON_WIDTH = 125;

    private static final int COMPONENT_HEIGHT = 155;

    private final JPanel m_panel = new JPanel();

    private final List<DataColumnSpec> m_avAggrColSpecs =
        new LinkedList<DataColumnSpec>();

    private final DefaultListModel m_avAggrColListModel =
            new DefaultListModel();

    private final JList m_avAggrColList;

    private final AggregationColumnTableModel m_aggrColTableModel
        = new AggregationColumnTableModel();

    private final JTable m_aggrColTable;

    /**
     * This class implements the context menu functionality of the aggregation
     * column table.
     * @author Tobias Koetter, University of Konstanz
     */
    private class AggregationColumnTableListener extends MouseAdapter {

        /**Constructor for class AggregationColumnTableListener.
         *
         */
        AggregationColumnTableListener() {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                onRemIt();
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mousePressed(final MouseEvent e) {
            maybeShowContextMenu(e);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseReleased(final MouseEvent e) {
            maybeShowContextMenu(e);
        }

        private void maybeShowContextMenu(final MouseEvent e) {
            if (e.isPopupTrigger()) {
                final JPopupMenu menu = createPopupMenu();
                menu.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }

        private JPopupMenu createPopupMenu() {
            final JPopupMenu menu = new JPopupMenu();
            if (getAggregationColumnCount() == 0) {
                //the table contains no rows
                final JMenuItem item =
                    new JMenuItem("No column(s) available");
                item.setEnabled(false);
                menu.add(item);
                return menu;
            }
            createColumnSelectionMenu(menu);
            menu.addSeparator();
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
                    if (type == DataValue.class) {
                        //skip the general type since this one is always present
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
                    if (type == DataValue.class) {
                        //skip the general type since this one is always present
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
            //add the select all columns entry
            final JMenuItem selectAll =
                new JMenuItem("Select all columns");
            selectAll.addActionListener(new ActionListener() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void actionPerformed(final ActionEvent e) {
                    selectAllRows();
                }
            });
            menu.add(selectAll);
        }

        /**
         * @param menu
         */
        private void createMissingValuesMenu(final JPopupMenu menu) {
            if (!rowsSelected()) {
                //show this option only if at least one row is selected
                return;
            }
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
            if (!rowsSelected()) {
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
    }

    /**Constructor for class AggregationColumnPanel.
     *
     */
    public AggregationColumnPanel() {
        m_avAggrColList = new JList(m_avAggrColListModel);
        m_avAggrColList.setCellRenderer(new DataColumnSpecListCellRenderer());
        m_avAggrColList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1
                        && e.getClickCount() == 2) {
                    onAddIt();
                }
            }
        });

        m_aggrColTable = new JTable(m_aggrColTableModel);
        m_aggrColTable.setFillsViewportHeight(true);
        m_aggrColTable.getTableHeader().setReorderingAllowed(false);
        final TableColumnModel columnModel = m_aggrColTable.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(
                new DataColumnSpecTableCellRenderer());
        columnModel.getColumn(1).setCellEditor(
                new AggregationMethodTableCellEditor(m_aggrColTableModel));
        columnModel.getColumn(1).setCellRenderer(
                new AggregationMethodTableCellRenderer());
        columnModel.getColumn(2).setCellRenderer(
                new IncludeMissingCellRenderer(m_aggrColTableModel));
        m_aggrColTable.addMouseListener(new AggregationColumnTableListener());
        columnModel.getColumn(0).setPreferredWidth(170);
        columnModel.getColumn(1).setPreferredWidth(150);
        columnModel.getColumn(2).setPreferredWidth(45);
        columnModel.getColumn(2).setMinWidth(45);
        columnModel.getColumn(2).setMaxWidth(45);

        m_panel.setMinimumSize(PANEL_DIMENSION);
        m_panel.setPreferredSize(PANEL_DIMENSION);
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.X_AXIS));
        final Box rootBox = new Box(BoxLayout.X_AXIS);
        final Border border = BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), " Aggregation settings ");
        rootBox.setBorder(border);
        rootBox.add(createAggrColBox());
        rootBox.add(createButtonBox());
        rootBox.add(createAggrColTable());
        m_panel.add(rootBox);
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

    private Box createAggrColBox() {
        final Box aggrColBox = new Box(BoxLayout.X_AXIS);
        final Border border =
            BorderFactory.createTitledBorder(" Available columns ");
        aggrColBox.setBorder(border);
        final JScrollPane avColList = new JScrollPane(m_avAggrColList);
        final Dimension dimension = new Dimension(125, COMPONENT_HEIGHT);
        avColList.setMinimumSize(dimension);
        avColList.setPreferredSize(dimension);
        aggrColBox.add(avColList);
        return aggrColBox;
    }

    private JComponent createButtonBox() {
        final JPanel buttonBox = new JPanel();
        buttonBox.setBorder(BorderFactory.createTitledBorder(" Select "));
        buttonBox.setLayout(new BoxLayout(buttonBox, BoxLayout.Y_AXIS));
        buttonBox.add(Box.createVerticalGlue());
        buttonBox.add(createButtonFiller(10, 10));
        final JButton addButton = new JButton("add >>");
        addButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(addButton);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddIt();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton addAllButton = new JButton("add all >>");
        addAllButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(addAllButton);
        addAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onAddAll();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton remButton = new JButton("<< remove");
        remButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(remButton);
        remButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onRemIt();
            }
        });
        buttonBox.add(createButtonFiller(15, 10));

        final JButton remAllButton = new JButton("<< remove all");
        remAllButton.setMaximumSize(new Dimension(BUTTON_WIDTH, 25));
        buttonBox.add(remAllButton);
        remAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ae) {
                onRemAll();
            }
        });
        buttonBox.add(createButtonFiller(10, 10));
        buttonBox.add(Box.createVerticalGlue());
        return buttonBox;
    }

    private Component createButtonFiller(final int height, final int width) {
        final Component filler = Box.createVerticalStrut(height);
        final Dimension fillerDimension = new Dimension(width, height);
        filler.setMaximumSize(fillerDimension);
        filler.setPreferredSize(fillerDimension);
        filler.setMinimumSize(fillerDimension);
        return filler;
    }

    private Component createAggrColTable() {
        final JScrollPane pane = new JScrollPane(m_aggrColTable);
        pane.setBorder(BorderFactory.createTitledBorder(null,
        " To change multiple columns use right mouse click for context menu. ",
        TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        return pane;
    }

    /**
     * Removes all columns from the aggregation column table.
     */
    protected void onRemAll() {
        m_aggrColTableModel.removeAll();
    }

    /**
     * Removes the selected columns from the aggregation column table.
     */
    protected void onRemIt() {
        m_aggrColTableModel.removeColumn(m_aggrColTable.getSelectedRows());
    }

    /**
     *  Adds all columns to the aggregation column table.
     */
    protected void onAddAll() {
        final DataColumnSpec[] specs =
            new DataColumnSpec[m_avAggrColListModel.getSize()];
        for (int i = 0, size = m_avAggrColListModel.getSize(); i < size; i++) {
            specs[i] = (DataColumnSpec)m_avAggrColListModel.get(i);
        }
        m_aggrColTableModel.addColumn(specs);
    }

    /**
     * Adds all selected columns to the aggregation column table.
     */
    protected void onAddIt() {
        final Object[] values = m_avAggrColList.getSelectedValues();
        if (values == null || values.length < 1) {
            return;
        }
        final DataColumnSpec[] specs = new DataColumnSpec[values.length];
        for (int i = 0, length = values.length; i < length; i++) {
            specs[i] = (DataColumnSpec)values[i];
        }
        m_aggrColTableModel.addColumn(specs);
    }

    /**
     * Changes the aggregation method of all selected rows to the method
     * with the given label.
     * @param methodId the label of the aggregation method
     */
    protected void changeAggregationMethod(final String methodId) {
        final int[] selectedRows = m_aggrColTable.getSelectedRows();
        m_aggrColTableModel.setAggregationMethod(selectedRows,
                AggregationMethods.getMethod4Id(methodId));
        final Collection<Integer> idxs = new LinkedList<Integer>();
        for (final int i : selectedRows) {
            idxs.add(new Integer(i));
        }
        updateSelection(idxs);
    }

    /**
     * Changes the include missing cell option for the selected rows.
     */
    protected void toggleMissingCellOption() {
        final int[] selectedRows = m_aggrColTable.getSelectedRows();
        m_aggrColTableModel.toggleMissingCellOption(selectedRows);
    }

    /**
     * Selects all rows.
     */
    protected void selectAllRows() {
        m_aggrColTable.selectAll();
    }

    /**
     * Selects all rows that are compatible with the given type.
     * @param type the type to check for compatibility
     */
    protected void selectCompatibleRows(
            final Class<? extends DataValue> type) {
        final Collection<Integer> idxs =
            m_aggrColTableModel.getCompatibleRowIdxs(type);
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
            m_aggrColTableModel.getCompatibleRowIdxs(type);
        return idxs != null ? idxs.size() : 0;
    }

    /**
     * @return <code>true</code> if at least one row is selected
     */
    boolean rowsSelected() {
         final int[] selectedRows = m_aggrColTable.getSelectedRows();
         return (selectedRows != null && selectedRows.length > 0);
    }


    /**
     * @return the number of aggregation columns
     */
    protected int getAggregationColumnCount() {
        return m_aggrColTable.getRowCount();
    }

    /**
     * @param idxs the indices to select
     */
    private void updateSelection(final Collection<Integer> idxs) {
        if (idxs == null || idxs.isEmpty()) {
            m_aggrColTable.clearSelection();
            return;
        }
        boolean first = true;
        for (final Integer idx : idxs) {
            if (idx.intValue() < 0) {
                continue;
            }
            if (first) {
                first = false;
                m_aggrColTable.setRowSelectionInterval(idx.intValue(),
                        idx.intValue());
            } else {
                m_aggrColTable.addRowSelectionInterval(idx.intValue(),
                        idx.intValue());
            }
        }
    }

    /**
     * @return the panel in which all sub-components of this component are
     *         arranged. This panel can be added to the dialog pane.
     */
    public JPanel getComponentPanel() {
        return m_panel;
    }

    /**
     * @param excludeCols the name of all columns that should be excluded from
     * the aggregation panel
     */
    public void excludeColsChange(final Collection<String> excludeCols) {
        final Set<String> excludeColSet =
            new HashSet<String>(excludeCols);
        //update the available aggregation column list
        m_avAggrColListModel.removeAllElements();
        for (final DataColumnSpec colSpec : m_avAggrColSpecs) {
            if (!excludeColSet.contains(colSpec.getName())) {
                m_avAggrColListModel.addElement(colSpec);
            }
        }
        //remove all columns to be excluded from the aggregation column table
        m_aggrColTableModel.removeColumns(excludeCols);
    }

    /**
     * @param settings the settings object to write to
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        ColumnAggregator.saveColumnAggregators(settings,
        m_aggrColTableModel.getColumnAggregators());
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
        for (final DataColumnSpec colSpec : spec) {
            m_avAggrColSpecs.add(colSpec);
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
        m_aggrColTableModel.initialize(colAggrs2Use);
    }

    /**
     * @return a label list of all supported methods for the currently
     * selected rows
     */
    protected List<Entry<String, List<AggregationMethod>>>
        getMethods4SelectedItems() {
        final int[] selectedColumns =
            m_aggrColTable.getSelectedRows();
        final Set<DataType> types =
            new HashSet<DataType>(selectedColumns.length);
        for (final int row : selectedColumns) {
            final ColumnAggregator aggregator =
                m_aggrColTableModel.getColumnAggregator(row);
            types.add(aggregator.getOriginalDataType());
        }
        final DataType superType = CollectionCellFactory.getElementType(
                types.toArray(new DataType[0]));
        final Map<Class<? extends DataValue>, List<AggregationMethod>>
        methodGroups = AggregationMethods.getCompatibleMethodGroups(superType);
        final Set<Entry<Class<? extends DataValue>, List<AggregationMethod>>>
            methodSet = methodGroups.entrySet();
        final List<String> labels = new ArrayList<String>(methodSet.size());
        final Map<String, List<AggregationMethod>> labelSet =
            new HashMap<String, List<AggregationMethod>>(methodSet.size());
        for (final Entry<Class<? extends DataValue>, List<AggregationMethod>>
            entry : methodSet) {
            final String label =
                AggregationMethods.getUserTypeLabel(entry.getKey());
            labels.add(label);
            labelSet.put(label, entry.getValue());
        }
        Collections.sort(labels);
        final List<Entry<String, List<AggregationMethod>>> list =
            new ArrayList<Entry<String, List<AggregationMethod>>>(
                    methodSet.size());
        for (final String label : labels) {
            final List<AggregationMethod> methods = labelSet.get(label);
            final Entry<String, List<AggregationMethod>> entry =
                new Map.Entry<String, List<AggregationMethod>>() {
                    @Override
                    public String getKey() {
                        return label;
                    }
                    @Override
                    public List<AggregationMethod> getValue() {
                        return methods;
                    }
                    @Override
                    public List<AggregationMethod> setValue(
                            final List<AggregationMethod> value) {
                        return methods;
                    }
            };
            list.add(entry);
        }

        return list;
    }
}
