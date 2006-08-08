/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed 
 */
package org.knime.core.node.tableview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.node.property.hilite.HiLiteHandler;



/** 
 * Table view on a {@link org.knime.core.data.DataTable}. This
 * implementation uses a caching strategy as described in the
 * {@link org.knime.core.node.tableview.TableContentModel}.
 * <br />
 * Standard renderers are used to display the different types of 
 * {@link org.knime.core.data.DataCell}s. This will change in future.
 * <br />
 * This view typically resides in a
 * {@link org.knime.core.node.tableview.TableView} (wrapping it in a
 * scroll pane and providing lots of delegating methods). If you want to use a 
 * table view somewhere else than in this package, e.g. in a different node, 
 * refer to the {@link org.knime.core.node.tableview.TableView}
 * implementation.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableContentView extends JTable {
    private static final long serialVersionUID = -3503118869778091484L;
    
    private PropertyChangeListener m_dataListener;

    /**
     * Creates empty content view. Consider 
     * {@link #setDataTable(DataTable)} to set a new data table to be 
     * displayed.
     */ 
    public TableContentView() {
        this(new TableContentModel());
    } // TableContentView()
    
    /**
     * Creates new <code>TableContentView</code> based on a given 
     * <code>TableContentModel</code>. A standard renderer that prints the
     * <code>toString()</code> result of <code>DataCell</code> is used.
     * 
     * @param model to be displayed
     */
    public TableContentView(final TableContentModel model) {
        super(model);
    } // TableContentView(TableModel)

    /**
     * Creates new <code>TableContentView</code> based on a 
     * <code>DataTable</code>. The view's table model is a 
     * <code>TableContentModel</code>.
     * 
     * @param data <code>DataTable</code> to be displayed
     * @throws NullPointerException if <code>data</code> is <code>null</code>.
     * @see TableContentModel#TableContentModel(DataTable)
     */
    public TableContentView(final DataTable data) {
        this(new TableContentModel());
        getContentModel().setDataTable(data);
    } // TableContentView(DataTable)

    /**
     * Checks that the given argument is of type {@link TableContentModel} 
     * and throws exception if it is not. Otherwise it sets the new model and
     * updates the table.
     * 
     * @param tableModel the new model, necessarily of type 
     *        {@link TableContentModel} 
     * @see javax.swing.JTable#setModel(javax.swing.table.TableModel)
     * @throws ClassCastException if dataModel not of type 
     *         {@link TableContentModel} 
     * @throws IllegalArgumentException if <code>dataModel</code> is 
     *         <code>null</code> as done by 
     *         <code>JTable.setModel(TableModel)</code>
     */
    @Override
    public void setModel(final TableModel tableModel) {
        TableContentModel tblModel = (TableContentModel)tableModel;
        if (m_dataListener == null) { // may be null whin in <init>
            m_dataListener = new PropertyChangeListener() {
                public void propertyChange(final PropertyChangeEvent evt) {
                    String id = evt.getPropertyName();
                    if (!id.equals(TableContentModel.PROPERTY_DATA)) {
                        return;
                    }
                    scrollRectToVisible(new Rectangle());
                    TableColumnModel tcM = getColumnModel();
                    if (hasData()) {
                        for (int i = 0; i < tcM.getColumnCount(); i++) {
                            DataTable data = getContentModel().getDataTable();
                            DataColumnSpec headerValue = 
                                data.getDataTableSpec().getColumnSpec(i);
                            tcM.getColumn(i).setHeaderValue(headerValue);
                        }
                    }
                }  
            };
        } else {
            getContentModel().removePropertyChangeListener(m_dataListener);
        }
        tblModel.addPropertyChangeListener(m_dataListener);
        super.setModel(tblModel);
    } // setModel(TableModel)
    
    
    
    /**
     * Returns a reference to the TableContentModel.
     * 
     * @return the reference to the table model
     */
    public TableContentModel getContentModel() {
        return (TableContentModel)getModel();
    }
    
    /**
     * Overridden in order to set the correct selection color (depending on
     * hilite status).
     * 
     * @see JTable#prepareRenderer(TableCellRenderer, int, int)
     */
    @Override
    public Component prepareRenderer(final TableCellRenderer renderer, 
            final int row, final int column) {
        final TableContentModel cntModel = (TableContentModel)getModel();
        final boolean isHiLit = cntModel.isHiLit(row);
        Color selColor = (isHiLit 
                ? ColorAttr.SELECTED_HILITE : ColorAttr.SELECTED);
        setSelectionBackground(selColor);
        return super.prepareRenderer(renderer, row, column);
    }
    
    /**
     * Is the row count returned by {@link #getRowCount()} final?
     * 
     * @return <code>true</code> if row count won't change anymore (all rows
     *         have been seen), <code>false</code> if more rows are expected to
     *         come
     * @see TableContentModel#isRowCountFinal()
     */
    public boolean isRowCountFinal() {
        return getContentModel().isRowCountFinal();
    } // isRowCountFinal()

    /** 
     * Sets a new <code>DataTable</code> as content. 
     * 
     * @param data New data to be shown. May be <code>null</code> to have an 
     * empty table.
     * @see TableContentModel#setDataTable(DataTable)
     */
    public void setDataTable(final DataTable data) {
        getContentModel().setDataTable(data);
    } // setDataTable(DataTable)

    /** 
     * Sets a new <code>HiLiteHandler</code> that this view talks to. The
     * argument may be <code>null</code> to disconnect the current
     * <code>HiLiteHandler</code>.
     * 
     * @param hiLiteHdl the new <code>HiLiteHandler</code>.
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        getContentModel().setHiLiteHandler(hiLiteHdl);
    } // setHiLiteHandler(HiLiteHandler)
    
    /**
     * Control behaviour to show only hilited rows.
     * 
     * @param showOnlyHilite <code>true</code>: filter and display only
     *        rows whose hilite status is set
     * @see TableContentModel#showHiLitedOnly(boolean)
     */
    public final void showHiLitedOnly(final boolean showOnlyHilite) {
        getContentModel().showHiLitedOnly(showOnlyHilite);
    }
    
    /**
     * Get status of filtering for hilited rows.
     * 
     * @return <code>true</code> if only hilited rows are shown, 
     *         <code>false</code> if all rows are shown.
     * @see TableContentModel#showsHiLitedOnly() 
     */
    public boolean showsHiLitedOnly() {
        return getContentModel().showsHiLitedOnly();
    }

    /** 
     * Is there a HiLiteHandler connected?
     * 
     * @return <code>true</code> if global hiliting is possible
     * @see TableContentModel#hasHiLiteHandler()
     */
    public final boolean hasHiLiteHandler() {
        return getContentModel().hasHiLiteHandler();
    } // hasHiLiteHandler()

    /** 
     * This table "hasData" when there is valid input, i.e. the 
     * <code>DataTable</code> to be displayed is not <code>null</code>. The 
     * status may be changed during runtime by calling the models 
     * <code>setDataTable</code> method.
     * 
     * @return <code>true</code> if there is data to be displayed
     * @see TableContentModel#hasData()
     */
    public boolean hasData() {
        return getContentModel().hasData();
    } // hasData()
    
    /**
     * Delegate method to cancel row counting.
     * 
     * @see TableContentModel#cancelRowCountingInBackground()
     */
    public void cancelRowCountingInBackground() {
        getContentModel().cancelRowCountingInBackground();
    }

    /**
     * Delegate method to start row counting.
     * 
     * @see TableContentModel#countRowsInBackground()
     */
    public void countRowsInBackground() {
        getContentModel().countRowsInBackground();
    }
    
    /** 
     * Requests to the <code>HiLiteHandler</code> that all rows that are 
     * currently selected are added to the set of hilited patterns. This 
     * method does nothing if the view is not connected to any 
     * <code>HiLiteHandler</code>.
     */
    public void hiliteSelected() {
        getContentModel().requestHiLite(getSelectionModel());
    } // hiliteSelected()
    
    /** 
     * Requests to the <code>HiLiteHandler</code> that all rows that are 
     * currently selected are removed from the set of hilited patterns. This
     * method does nothing if the view is not connected to any 
     * <code>HiLiteHandler</code>.
     */
    public void unHiliteSelected() {
        getContentModel().requestUnHiLite(getSelectionModel());
    } // hiliteSelected()
    
    /** 
     * Requests to the <code>HiLiteHandler</code> that the hilite status of
     * all rows is reset. This method does nothing if the view is not connected
     * to any <code>HiLiteHandler</code>.
     */
    public void resetHilite() {
        getContentModel().requestResetHiLite();
    } // hiliteSelected()

    /**
     * Overridden so that we can attach a mouse listener to it and set
     * the proper renderer. The mouse listener is used to display a popup menu.
     * @see javax.swing.JTable#setTableHeader(javax.swing.table.JTableHeader)
     */
    @Override
    public void setTableHeader(final JTableHeader newTableHeader) {
        if (newTableHeader != null) {
            TableCellRenderer renderer = getNewColumnHeaderRenderer();
            newTableHeader.setDefaultRenderer(renderer);
            newTableHeader.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    onMouseClickInHeader(e);
                }
            });
        }
        super.setTableHeader(newTableHeader);
    }

    /**
     * Overridden to avoid event storm. The super implementation will invoke
     * a repaint if the color has changed. Since that happens frequently (and
     * also within the repaint) this causes an infinite loop.
     * 
     * @see javax.swing.JTable#setSelectionBackground(java.awt.Color)
     */
    @Override
    public void setSelectionBackground(final Color newColor) {
        if (newColor == null) {
            throw new NullPointerException("Color must not be null!");  
        }
        super.selectionBackground = newColor;
    }
    
    /**
     * Overridden to set proper header content and apply renderer. The
     * header of the column will be set to the <code>DataTable</code>'s
     * <code>DataColumnSpec</code> and for the renderer the
     * type's <code>getNewRenderer()</code> is used
     * 
     * @param aColumn column to be added
     * @see javax.swing.JTable#addColumn(javax.swing.table.TableColumn)
     * @see org.knime.core.data.DataType#getRenderer(DataColumnSpec)
     * @see DataColumnSpec
     */
    @Override
    public void addColumn(final TableColumn aColumn) {
        assert (hasData());
        int i = aColumn.getModelIndex();
        DataTable data = getContentModel().getDataTable();
        DataColumnSpec headerValue = data.getDataTableSpec().getColumnSpec(i);
        aColumn.setHeaderValue(headerValue);
        DataValueRendererFamily renderer = 
            headerValue.getType().getRenderer(headerValue);
        aColumn.setCellRenderer(renderer);
        super.addColumn(aColumn);
    }
    
    /**
     * Invoked when a mouse event in the header occurs. This implementation 
     * will create a popup menu when there is more than one renderer for that
     * column available and will also list all possible values in the column
     * where the mouse was clicked (as it is provided in the column spec).
     * The event's source is the table header
     * 
     * @param e the mouse event in the table header
     */
    protected void onMouseClickInHeader(final MouseEvent e) {
        JTableHeader header = getTableHeader();
        // get column in which event occured
        int column = header.columnAtPoint(e.getPoint());
        Rectangle recOfColumn = header.getHeaderRect(column);
        int horizPos = e.getX() - recOfColumn.x;
        assert (horizPos >= 0);
        // right click in header.
        if (SwingUtilities.isRightMouseButton(e)) {
            JPopupMenu popup = getPopUpMenu(column);
            if (popup.getSubElements().length > 0) { // only if it has content
                popup.show(header, e.getX(), e.getY());
            }
        }
    }
    
    /** 
     * Create a custom popup menu when the mouse was clicked in a column header.
     * This popup menu will contain the possible values in that column (when
     * available) and a set of buttons which let the user change the renderer
     * (again: when available).
     * 
     * @param column column for which to create the popup menu
     * @return a popup menu displaying these properties
     * @see #onMouseClickInHeader(MouseEvent)
     */
    protected JPopupMenu getPopUpMenu(final int column) {
        final TableColumn tableColumn = getColumnModel().getColumn(column); 
        Object value = tableColumn.getHeaderValue();
        if (!(value instanceof DataColumnSpec)) {
            // only occurs if someone overrides the addColumn method.
            return null;
        }
        final DataColumnSpec spec = (DataColumnSpec)value;
        JPopupMenu popup = new JPopupMenu("Column Context Menu");
        JMenuItem menuItem;
        // first menu item will allow to show all possible values
        final Set<DataCell> valueList = spec.getDomain().getValues();
        if (valueList != null && !valueList.isEmpty()) {
            menuItem = new JMenuItem("Show possible values");
            final String[] columnValues = new String[valueList.size()];
            int i = 0;
            for (DataCell cell : valueList) {
                columnValues[i++] = cell.toString();
            }
            menuItem.addActionListener(new ActionListener() {
                // TODO: must be put in a scroll pane?
                public void actionPerformed(final ActionEvent action) {
                    JOptionPane.showMessageDialog(
                            TableContentView.this, columnValues, 
                            "Possible Values", 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            popup.add(menuItem);
        }
        // try to figure out the set of available renderers 
        TableCellRenderer curRen = tableColumn.getCellRenderer();
        String renderID = null; 
        // should always be true unless someone overrides addColumn
        if (curRen instanceof DataValueRendererFamily) { 
            DataValueRendererFamily renFamily = (DataValueRendererFamily)curRen;
            renderID = renFamily.getDescription();
        }
        String[] availRender = getAvailableRenderers(column);
        if (availRender != null && availRender.length > 1) {
            JMenu subMenu = new JMenu("Available Renderers");
            popup.add(subMenu);
            // actionlistener which changes the renderer according to the 
            // action command
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(final ActionEvent action) {
                    changeRenderer(column, action.getActionCommand());
                }
            };
            ButtonGroup buttonGroup = new ButtonGroup();
            for (int i = 0; i < availRender.length; i++) {
                String thisID = availRender[i];
                menuItem = new JRadioButtonMenuItem(thisID);
                buttonGroup.add(menuItem);
                menuItem.setActionCommand(thisID);
                menuItem.addActionListener(actionListener);
                menuItem.setSelected(thisID.equals(renderID));
                subMenu.add(menuItem);
            }
        }
        return popup;
    }
    
    /**
     * Changes the renderer in a given column. The column's renderer is 
     * retrieved and checked if it is instance of 
     * {@link DataValueRendererFamily} (which it is unless a subclass 
     * overrides <code>addColumn</code>). In this renderer family the renderer
     * matching the description <code>rendererID</code> is set active.
     * <br />
     * If the description is not valid (<code>null</code> or unknown), this
     * method does nothing.
     * 
     * @param column the column of interest
     * @param rendererID the name of the renderer
     * @see DataValueRendererFamily#getRendererDescriptions()
     * @throws IndexOutOfBoundsException if <code>column</code> violates its 
     *         range
     */
    public void changeRenderer(final int column, final String rendererID) {
        final TableColumn aColumn = getColumnModel().getColumn(column);
        TableCellRenderer curRen = aColumn.getCellRenderer();
        if (!(curRen instanceof DataValueRendererFamily)) {
            return;
        }
        DataValueRendererFamily renFamily = (DataValueRendererFamily)curRen;
        renFamily.setActiveRenderer(rendererID);
        repaint();
    }
    
    /**
     * Get the description of all available renderers in a column. The returned
     * array simply contains all description in the 
     * <code>DataValueRendererFamily</code> (which should be the default 
     * renderer in each column.) 
     * 
     * @param column the column of interest.
     * @return a new array containing the description of all available renderer
     *         or an empty array to address no available renderer
     * @throws IndexOutOfBoundsException if <code>column</code> violates its 
     *         range
     */
    public String[] getAvailableRenderers(final int column) {
        final TableColumn tableColumn = getColumnModel().getColumn(column); 
        TableCellRenderer curRen = tableColumn.getCellRenderer();
        String[] availRenderer; 
        if (curRen instanceof DataValueRendererFamily) {
            DataValueRendererFamily renFamily = (DataValueRendererFamily)curRen;
            availRenderer = renFamily.getRendererDescriptions();
        } else {
            availRenderer = new String[0];
        }
        return availRenderer;
    }
    
    /**
     * Calls super, sets proper row height.
     * 
     * @see javax.swing.JTable#initializeLocalVars()
     */
    @Override
    protected void initializeLocalVars() {
        super.initializeLocalVars();
        TableColumnModel colModel = getColumnModel();
        int bestRowHeight = getRowHeight();
        for (Enumeration<TableColumn> enu = colModel.getColumns(); enu.hasMoreElements();) {
            TableColumn col = enu.nextElement();
            TableCellRenderer renderer = col.getCellRenderer();
            if (renderer instanceof DataValueRenderer) {
                int prefHeight = (int)((DataValueRenderer)renderer).
                    getPreferredSize().getHeight();
                bestRowHeight = Math.max(bestRowHeight, prefHeight);
            }
        }
        setRowHeight(bestRowHeight);
    }
    
    /**
     * Get the renderer for the column header (never <code>null</code>).
     * 
     * @return a new <code>ColumnHeaderRenderer</code>
     * @see ColumnHeaderRenderer
     * @see JTableHeader#setDefaultRenderer(javax.swing.table.TableCellRenderer)
     */
    protected TableCellRenderer getNewColumnHeaderRenderer() {
        return new ColumnHeaderRenderer();
    }
}
