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

import org.knime.base.data.aggregation.AggregationMethodDecorator;

import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


/**
 * Abstract class that creates a panel which contains a list of Objects defined
 * by the parameter <code>O</code> to select from on the left side of the
 * panel, a box with add an remove buttons in the middle and a table
 * of type {@link AggregationTableModel} that contains the selected options
 * from the left with additional information on the right.
 *
 * @author Tobias Koetter, University of Konstanz
 * @param <T> the {@link AggregationTableModel} implementation to work with
 * @param <O> the {@link AggregationMethodDecorator} implementation to work
 * with
 * @param <L> the class of the list elements the user can choose from
 * @since 2.6
 * @see AggregationTableModel
 * @see AggregationMethodDecorator
 */
public abstract class AbstractAggregationPanel
<T extends AggregationTableModel<O>, O extends AggregationMethodDecorator,
    L extends Object> extends MouseAdapter {

    /**The size of the incl./excl. missing cells column.*/
    public static final int MISSING_CELL_OPTION_SIZE = 45;

    /**The initial dimension of this panel.*/
    public static final Dimension PANEL_DIMENSION = new Dimension(650, 200);

    private static final int BUTTON_WIDTH = 125;

    private static final int COMPONENT_HEIGHT = 155;

    private final JPanel m_panel = new JPanel();

    private final DefaultListModel m_listModel = new DefaultListModel();

    private final JList m_list;

    private final T m_tableModel;

    private final JTable m_table;

    /**
     * This class implements the context menu functionality of the table.
     * @author Tobias Koetter, University of Konstanz
     */
    private class TableListener extends MouseAdapter {

        /**Constructor for class AggregationColumnTableListener.
         *
         */
        TableListener() {
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
                JPopupMenu menu = createTablePopupMenu();
                if (menu != null) {
                    menu.show(e.getComponent(),
                            e.getX(), e.getY());
                } else {
                    menu = new JPopupMenu();
                }
                appendMissingValuesEntry(menu);
            }
        }
    }

    /**Constructor for class AbstractAggregatorPanel.
     * @param title the title of the panel.
     * (<code>null</code> = no border, empty string = border)
     * @param listTitle the title of the list component on the left.
     * (<code>null</code> = no border, empty string = border)
     * @param cellRenderer the {@link ListCellRenderer}
     * @param tableTitle the title of the table component on the right.
     * (<code>null</code> = no border, empty string = border)
     * @param tableModel the {@link AggregationTableModel} to use
     */
    protected  AbstractAggregationPanel(final String title,
            final String listTitle, final ListCellRenderer cellRenderer,
            final String tableTitle, final T tableModel) {
        m_tableModel = tableModel;
        m_list = new JList(getListModel());
        m_list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1
                        && e.getClickCount() == 2) {
                    onAddIt();
                }
            }
        });
        m_list.setCellRenderer(cellRenderer);
        m_table = new JTable(m_tableModel);
        m_table.setFillsViewportHeight(true);
        m_table.getTableHeader().setReorderingAllowed(false);
        m_table.addMouseListener(getTableMouseAdapter());
        final TableColumnModel columnModel = m_table.getColumnModel();
        final int missingCellOptionColIdx =
            m_tableModel.getMissingCellOptionColIdx();
        if (missingCellOptionColIdx >= 0) {
            columnModel.getColumn(missingCellOptionColIdx).setCellRenderer(
                    getMissingCellRenderer());
            columnModel.getColumn(missingCellOptionColIdx).setMinWidth(
                    MISSING_CELL_OPTION_SIZE);
            columnModel.getColumn(missingCellOptionColIdx).setMaxWidth(
                    MISSING_CELL_OPTION_SIZE);
        }
        adaptTableColumnModel(columnModel);

        m_panel.setMinimumSize(PANEL_DIMENSION);
        m_panel.setPreferredSize(PANEL_DIMENSION);
        m_panel.setLayout(new BoxLayout(m_panel, BoxLayout.X_AXIS));
        final Box rootBox = new Box(BoxLayout.X_AXIS);
        if (title != null) {
            final Border border = BorderFactory.createTitledBorder(BorderFactory
                    .createEtchedBorder(), title);
            rootBox.setBorder(border);
        }
        rootBox.add(createListComponent(listTitle));
        rootBox.add(createButtonComponent());
        rootBox.add(createTableComponent(tableTitle));
        m_panel.add(rootBox);
    }

    /**
     * @return the {@link JPopupMenu} or <code>null</code> for no popup menu
     */
    protected abstract JPopupMenu createTablePopupMenu();

    /**
     * @param size the size of the array to create
     * @return an array of the given size that can store objects of the
     * used {@link AggregationMethodDecorator} class
     */
    protected abstract O[] createEmptyOperatorArray(int size);

    /**
     * @param selectedListElement the list element to create the
     * {@link AggregationMethodDecorator} for
     * @return the concrete implementation of the used
     * {@link AggregationMethodDecorator} class
     */
    protected abstract O getOperator(final L selectedListElement);

    /**
     * @return the {@link MouseAdapter} that should be registered for
     * listening to mouse events on the table
     */
    protected MouseAdapter getTableMouseAdapter() {
        return new TableListener();
    }

    /**
     * Override this method to set specific renderer and column widths.
     * The missing cell option column is
     * @param columnModel the {@link TableColumnModel} to adapt
     */
    protected void adaptTableColumnModel(final TableColumnModel columnModel) {
        //nothing to do
    }

    /**
     * @return helper class that creates an {@link IncludeMissingCellRenderer}
     * with the actual {@link TableModel}
     */
    public IncludeMissingCellRenderer getMissingCellRenderer() {
        return new IncludeMissingCellRenderer(getTableModel());
    }

    /**
     * Appends the toggle missing values entry at the end of the given menu
     * if at least one row is selected and the table model contains
     * the missing cell option column.
     *
     * @param menu the {@link JPopupMenu} to create the toggle missing values
     * entry in
     */
    protected void appendMissingValuesEntry(final JPopupMenu menu) {
        if (!tableRowsSelected()
                || getTableModel().getMissingCellOptionColIdx() < 0) {
            //show this option only if at least one row is selected and
            //the table contains the missing option
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
     * Creates the list component that contains the possible options to
     * choose from (displayed on the left hand).
     * @param title the optional title of the list component.
     * (<code>null</code> = no border, empty string = border)
     * @return the list component which contains the list of available
     * options to choose from
     */
    protected Component createListComponent(final String title) {
        final Box avMethodsBox = new Box(BoxLayout.X_AXIS);
        if (title != null) {
            final Border border = BorderFactory.createTitledBorder(title);
            avMethodsBox.setBorder(border);
        }
        final JScrollPane compMethodsList =
            new JScrollPane(getList());
        final Dimension dimension = new Dimension(125, COMPONENT_HEIGHT);
        compMethodsList.setMinimumSize(dimension);
        compMethodsList.setPreferredSize(dimension);
        avMethodsBox.add(compMethodsList);
        return avMethodsBox;
    }

    /**
     * @return the {@link JList} that is displayed on the left hand side
     * which contains the available options
     * @see AbstractAggregationPanel#getListModel()
     */
    protected JList getList() {
        return m_list;
    }

    /**
     * @return the {@link DefaultListModel} of the list that is displayed on
     * the left hand side which contains the available options
     * @see #getList()
     */
    protected DefaultListModel getListModel() {
        return m_listModel;
    }

    /**
     * @return the {@link JTable} that contains all selected options and that
     * is displayed on the right hand side
     * @see #getTableModel()
     */
    protected JTable getTable() {
        return m_table;
    }

    /**
     * @return the {@link AggregationTableModel} that contains all selected
     * options and that is displayed on the right hand side
     * @see #getTable()
     */
    protected T getTableModel() {
        return m_tableModel;
    }

    /**
     * @return the {@link Component} that contains the buttons
     * (add, add all, remove and remove all) in the middle of the dialog.
     */
    protected Component createButtonComponent() {
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

    /**
     * Creates the table {@link Component} that contains all selected options
     * (displayed on the right).
     * @param title the optional title of the list component.
     * (<code>null</code> = no border, empty string = border)
     * @return the {@link Component} that contains the table which displays
     * all selected items
     */
    protected Component createTableComponent(final String title) {
        final JScrollPane pane = new JScrollPane(getTable());
        if (title != null) {
            pane.setBorder(BorderFactory.createTitledBorder(null, title,
                    TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
        }
        return pane;
    }

    /**
     * Adds all selected columns to the aggregation column table.
     */
    protected void onAddIt() {
        @SuppressWarnings("unchecked")
        final L[] values = (L[])getList().getSelectedValues();
        if (values == null || values.length < 1) {
            return;
        }
        final O[] methods = getOperators(values);
        getTableModel().add(methods);
    }

    /**
     *  Adds all columns to the aggregation column table.
     */
    protected void onAddAll() {
        final O[] methods = getOperators(getListModel());
        getTableModel().add(methods);
    }

    /**
     * Removes the selected columns from the aggregation column table.
     */
    protected void onRemIt() {
        getTableModel().remove(getTable().getSelectedRows());
    }

    /**
     * Removes all columns from the aggregation column table.
     */
    protected void onRemAll() {
        getTableModel().removeAll();
    }

    private O[] getOperators(
            final DefaultListModel listModel) {
      final O[] methods = createEmptyOperatorArray(listModel.size());
      for (int i = 0, size = listModel.getSize(); i < size; i++) {
          @SuppressWarnings("unchecked")
          final L listEntry = (L)listModel.get(i);
          final O operator = getOperator(listEntry);
          methods[i] = operator;
      }
      return methods;
    }

    /**
     * @param values the user selected values to add
     * @return the wrapped objects to add to the table model
     */
    private O[] getOperators(final L[] values) {
        final O[] methods = createEmptyOperatorArray(values.length);
        for (int i = 0, length = values.length; i < length; i++) {
            methods[i] = getOperator(values[i]);
        }
        return methods;
    }

    /**
     * Changes the include missing cell option for the selected rows.
     */
    protected void toggleMissingCellOption() {
        final int[] selectedRows = getTable().getSelectedRows();
        getTableModel().toggleMissingCellOption(selectedRows);
    }

    /**
     * Selects all selected methods.
     */
    protected void selectAllSelectedMethods() {
        getTable().selectAll();
    }

    /**
     * Selects all compatible methods.
     */
    protected void selectAllCompatibleMethods() {
        final int size = getListModel().size();
        final int[] indices = new int[size];
        for (int i = 0; i < size; i++) {
            indices[i] = i;
        }
        getList().setSelectedIndices(indices);
    }

    /**
     * @return <code>true</code> if at least one row is selected
     */
    protected boolean tableRowsSelected() {
         return getNoOfSelectedRows() > 0;
    }

    /**
     * @return the number of rows in the table
     */
    protected int getNoOfTableRows() {
        return getTableModel().getRowCount();
    }

    /**
    * Returns the indices of all selected rows.
    *
    * @return an array of integers containing the indices of all selected rows,
    *         or an empty array if no row is selected
    */
    protected int[] getSelectedRows() {
        return getTable().getSelectedRows();
    }

    /**
     * @return the number of selected table rows
     */
    protected int getNoOfSelectedRows() {
        final int[] selectedRows = getSelectedRows();
        return selectedRows == null ? 0 : selectedRows.length;
    }

    /**
     * @return the panel in which all sub-components of this component are
     *         arranged. This panel can be added to the dialog pane.
     */
    public JPanel getComponentPanel() {
        return m_panel;
    }

    /**
     * @return {@link Collection} of all elements the user can choose from
     */
    protected Collection<L> getListElements() {
        final LinkedList<L> elements = new LinkedList<L>();
        final Enumeration<?> listElements = getListModel().elements();
        while (listElements.hasMoreElements()) {
            @SuppressWarnings("unchecked")
            final L listElement = (L)listElements.nextElement();
            elements.add(listElement);
        }
        return elements;
    }

    /**
     * Initializes the panel.
     * @param listElements the elements the user can choose from
     * @param operators of {@link AggregationMethodDecorator}s
     * that are initially used
     */
    public void initialize(final List<L> listElements,
            final List<O> operators) {
        getListModel().clear();
        final Collection<L> types = listElements;
        for (final L type : types) {
            getListModel().addElement(type);
        }
        getTableModel().initialize(operators);
    }
}
