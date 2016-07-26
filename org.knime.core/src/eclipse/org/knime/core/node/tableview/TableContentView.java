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
 * ------------------------------------------------------------------------
 */
package org.knime.core.node.tableview;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.renderer.DataValueRenderer;
import org.knime.core.data.renderer.DataValueRendererFamily;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableSortOrder.TableSortKey;



/**
 * Table view on a {@link org.knime.core.data.DataTable}. This
 * implementation uses a caching strategy as described in the
 * {@link org.knime.core.node.tableview.TableContentModel}.
 * <br>
 * Standard renderers are used to display the different types of
 * {@link org.knime.core.data.DataCell}s. This will change in future.
 * <br>
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

    /** The property whether or not the icon in the column header is
     * to be shown.
     */
    private boolean m_isShowIconInColumnHeader;

    /** See {@link #setWrapColumnHeader(boolean)}. */
    private boolean m_isWrapHeader;

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
        setCellSelectionEnabled(true);
        // just initializing the member is not sufficient as the
        // <init> of super initialized the header as well.
        setShowIconInColumnHeader(true);

        /* ******************************************************************
         * Workaround for bug
         * http://bimbug.inf.uni-konstanz.de/show_bug.cgi?id=1219
         * (Unable to move columns in large TableViews)
         * This is caused by a bug introduced in java6
         * (fixed in update 4, build 2, which is not yet available, 6 Oct 07).
         * This workaround is stated below.
         * *****************************************************************/
        final MouseListener bug6503981WorkaroundListener =
            new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mousePressed(final MouseEvent e) {
                JTableHeader header = getTableHeader();
                if (header != null) {
                    int column = header.getColumnModel().getColumnIndexAtX(
                            e.getPoint().x);
                    if (column >= 0) {
                        setColumnSelectionInterval(column, column);
                        ActionMap map = getActionMap();
                        Action a = map != null ? map.get("focusHeader") : null;
                        if (a != null) {
                            a.actionPerformed(new ActionEvent(
                                TableContentView.this, 0, "focusHeader"));
                        }
                   }
                }
            }
        };
        addPropertyChangeListener("tableHeader", new PropertyChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void propertyChange(final PropertyChangeEvent evt) {
                if (evt.getOldValue() instanceof JTableHeader) {
                    JTableHeader oldHead = (JTableHeader)evt.getOldValue();
                    oldHead.removeMouseListener(bug6503981WorkaroundListener);
                }
                if (evt.getNewValue() instanceof JTableHeader) {
                    JTableHeader newHead = (JTableHeader)evt.getNewValue();
                    newHead.addMouseListener(bug6503981WorkaroundListener);
                }
            }
        });
        if (getTableHeader() != null) {
            getTableHeader().addMouseListener(bug6503981WorkaroundListener);
        }
        /* ******************************************************************
         * Workaround ends here.
         * *****************************************************************/
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
        if (m_dataListener == null) { // may be null when in <init>
            m_dataListener = new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent evt) {
                    String id = evt.getPropertyName();
                    if (!id.equals(TableContentModel.PROPERTY_DATA)) {
                        return;
                    }
                    TableColumnModel tcM = getColumnModel();
                    if (hasData()) {
                        DataTable data = getContentModel().getDataTable();
                        DataTableSpec spec = data.getDataTableSpec();
                        for (int i = 0; i < tcM.getColumnCount(); i++) {
                            int colInModel = convertColumnIndexToModel(i);
                            DataColumnSpec headerValue =
                                spec.getColumnSpec(colInModel);
                            tcM.getColumn(i).setHeaderValue(headerValue);
                        }
                    }
                    getTableHeader().revalidate(); // repaint sort icon
                    // property data update to row header view
                    // (only sort icon - data update done via TableModelEvent)
                    firePropertyChange(evt.getPropertyName(),
                            evt.getOldValue(), evt.getNewValue());
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
     * {@inheritDoc}
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
     * @see TableContentModel#getTableContentFilter()
     * @deprecated Implementors should refer to
     * <code>getContentModel().setTableContentFilter(TableContentFilter)</code>
     */
    @Deprecated
    public final void showHiLitedOnly(final boolean showOnlyHilite) {
        getContentModel().showHiLitedOnly(showOnlyHilite);
    }

    /**
     * Get status of filtering for hilited rows.
     *
     * @return <code>true</code> if only hilited rows are shown,
     *         <code>false</code> if all rows are shown.
     * @see TableContentModel#getTableContentFilter()
     * @deprecated Implementors should refer to
     * <code>getContentModel().getTableContentFilter()</code>
     */
    @Deprecated
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

    /** Should the column header names be wrapped if they are too long. Default is false.
     * @param value New value. If set, it makes sense to also set
     * {@link TableView#setColumnHeaderResizingAllowed(boolean)} to true.
     * @since 2.8
     */
    public final void setWrapColumnHeader(final boolean value) {
        if (value != m_isWrapHeader) {
            m_isWrapHeader = value;
            JTableHeader header = getTableHeader();
            if (header == null) {
                return;
            }
            TableCellRenderer r = header.getDefaultRenderer();
            if (r instanceof ColumnHeaderRenderer) {
                ColumnHeaderRenderer cr = (ColumnHeaderRenderer)r;
                cr.setWrapHeader(value);
            }
        }
    }

    /** see {@link #setWrapColumnHeader(boolean)}.
     * @return the isWrapHeader
     * @since 2.8
     */
    public final boolean isWrapHeader() {
        return m_isWrapHeader;
    }

    /** Sets the property whether or not the icon in the column header
     * shall be shown. This typically represents the column's type icon
     * (the cell type contained in the column). Sometimes, this is not
     * desired (for instance in the data outport view).
     * @param show Whether or not this icon should be shown.
     */
    public final void setShowIconInColumnHeader(final boolean show) {
        if (show != m_isShowIconInColumnHeader) {
            m_isShowIconInColumnHeader = show;
            JTableHeader header = getTableHeader();
            if (header == null) {
                return;
            }
            TableCellRenderer r = header.getDefaultRenderer();
            if (r instanceof ColumnHeaderRenderer) {
                ColumnHeaderRenderer cr = (ColumnHeaderRenderer)r;
                cr.setShowIcon(show);
            }
        }
    }

    /**
     * Get the status if the icon in the column header is shown.
     * @return true when the icon is shown, false otherwise.
     */
    public boolean isShowIconInColumnHeader() {
        return m_isShowIconInColumnHeader;
    }

    private ListSelectionListener m_repaintTableHeaderListSelectionListener;

    /**
     * Overridden so that we can attach a mouse listener to it and set
     * the proper renderer. The mouse listener is used to display a popup menu.
     * {@inheritDoc}
     */
    @Override
    public void setTableHeader(final JTableHeader newTableHeader) {
        if (m_repaintTableHeaderListSelectionListener != null) {
            getColumnModel().getSelectionModel().removeListSelectionListener(m_repaintTableHeaderListSelectionListener);
        }
        if (newTableHeader != null) {
            ColumnHeaderRenderer renderer = getNewColumnHeaderRenderer();
            newTableHeader.setDefaultRenderer(renderer);
            renderer.setShowIcon(isShowIconInColumnHeader());
            newTableHeader.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    // don't handle event if table width is being adjusted
                    if (!Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR).
                            equals(newTableHeader.getCursor())) {
                        onMouseClickInHeader(e);
                    }
                }
            });
            m_repaintTableHeaderListSelectionListener = e -> newTableHeader.repaint();
            getColumnModel().getSelectionModel().addListSelectionListener(m_repaintTableHeaderListSelectionListener);
        } else {
            m_repaintTableHeaderListSelectionListener = null;
        }
        super.setTableHeader(newTableHeader);
    }

    /** {@inheritDoc} */
    @Override
    public void setFont(final Font font) {
        super.setFont(font);
        JTableHeader th = getTableHeader();
        if (th != null) {
            th.setFont(font);
        }
    }

    private float[] m_tempHSBColor;
    /**
     * Overridden to avoid event storm. The super implementation will invoke
     * a repaint if the color has changed. Since that happens frequently (and
     * also within the repaint) this causes an infinite loop.
     *
     * {@inheritDoc}
     */
    @Override
    public void setSelectionBackground(final Color back) {
        if (back == null) {
            throw new NullPointerException("Color must not be null!");
        }
        Color fore;
        if (m_tempHSBColor == null) {
            m_tempHSBColor = new float[3];
        }
        float[] hsb = m_tempHSBColor;
        Color.RGBtoHSB(back.getRed(), back.getGreen(), back.getBlue(),
                hsb);
        if (hsb[2] > 0.5f) {
            fore = Color.BLACK;
        } else {
            fore = Color.WHITE;
        }
        // changed in v2.5 (before v2.5 the background was white)
        super.selectionBackground = back;
        super.selectionForeground = fore;
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
        aColumn.sizeWidthToFit();
        DataTable data = getContentModel().getDataTable();
        DataColumnSpec headerValue = data.getDataTableSpec().getColumnSpec(i);
        aColumn.setHeaderValue(headerValue);
        DataValueRendererFamily renderer = getRendererFamily(headerValue);
        String[] descs = renderer.getRendererDescriptions();
        // setting a certain column property will set a preferred renderer
        String preferredRenderer = headerValue.getProperties().getProperty(
                DataValueRenderer.PROPERTY_PREFERRED_RENDERER);
        if (Arrays.asList(descs).contains(preferredRenderer)
                && renderer.accepts(preferredRenderer, headerValue)) {
            renderer.setActiveRenderer(preferredRenderer);
        } else {
            for (String s : descs) {
                if (renderer.accepts(s, headerValue)) {
                    renderer.setActiveRenderer(s);
                    break;
                }
            }
        }
        aColumn.setCellRenderer(renderer);
        super.addColumn(aColumn);
    }

    /** {@inheritDoc} */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new TableContentViewTableHeader(this, columnModel);
    }

    /** Method being invoked when the table is (re-)constructed to get
     * the available renderer for a column. This method may be overwritten
     * to make the list of renderers more specific or general.
     * @param colSpec The spec of the column, never <code>null</code>.
     * @return The renderer family for the argument, not <code>null</code>.
     */
    protected DataValueRendererFamily getRendererFamily(
            final DataColumnSpec colSpec) {
        return colSpec.getType().getRenderer(colSpec);
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
        // get column in which event occurred
        int columnInView = header.columnAtPoint(e.getPoint());
        if (columnInView < 0) { // outside columns
            return;
        }
        Rectangle recOfColumn = header.getHeaderRect(columnInView);
        int horizPos = e.getX() - recOfColumn.x;
        assert (horizPos >= 0);
        if (SwingUtilities.isRightMouseButton(e)) { // right click in header.
            final JPopupMenu popup = getPopUpMenu(columnInView);
            if (popup.getSubElements().length > 0) { // only if it has content
                popup.show(header, e.getX(), e.getY());
            }
        } else if (e.isControlDown() && getContentModel().isSortingAllowed()) { // control pressed.
            onSortRequest(convertColumnIndexToModel(columnInView), null);
        } else if (SwingUtilities.isLeftMouseButton(e) && getContentModel().isSortingAllowed()) { // left click in header.
            TableSortOrder sortOrder = null;
            int colIndexInModel = -1;
            TableModel model = getModel();
            if (model instanceof TableContentModel) {
                TableContentModel cntModel = (TableContentModel)model;
                sortOrder = cntModel.getTableSortOrder();
                colIndexInModel = convertColumnIndexToModel(columnInView);
            } else if (model instanceof TableRowHeaderModel) {
                TableRowHeaderModel rowHeaderModel = (TableRowHeaderModel)model;
                TableContentInterface cntIface = rowHeaderModel.getTableContent();
                if (cntIface instanceof TableContentModel) {
                    TableContentModel cntModel = (TableContentModel)cntIface;
                    sortOrder = cntModel.getTableSortOrder();
                    colIndexInModel = -1;
                }
            }
            TableSortKey sortKey;
            if (sortOrder == null) {
                sortKey = TableSortKey.NONE;
            } else {
                sortKey = sortOrder.getSortKeyForColumn(colIndexInModel);
            }
            final JPopupMenu popup = createSortPopupMenu(columnInView, sortKey);
            popup.show(header, e.getX(), e.getY());
        }
    }

    /** Invoked by the mouse listener on the table header to trigger table
     * sorting on a given column. This call might be ignored if the model
     * does not allow interactive sorting.
     * @param columnIndex The column that was clicked.
     * @see TableContentModel#isSortingAllowed()
     */
    @Deprecated
    protected void onSortRequest(final int columnIndex) {
        onSortRequest(columnIndex, null);
    }

    /** Invoked by the mouse listener on the table header to trigger table
     * sorting on a given column. This call might be ignored if the model
     * does not allow interactive sorting.
     * @param columnIndex The column that was clicked.
     * @param sortKey the new sort key
     * @see TableContentModel#isSortingAllowed()
     * @since 2.8
     */
    protected void onSortRequest(final int columnIndex, final TableSortKey sortKey) {
        getContentModel().requestSort(columnIndex, this, sortKey);
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
                @Override
                public void actionPerformed(final ActionEvent action) {
                    JOptionPane.showMessageDialog(
                            TableContentView.this.getRootPane(), columnValues,
                            "Possible Values",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            popup.add(menuItem);
        }
        // try to figure out the set of available renderers
        TableCellRenderer curRen = tableColumn.getCellRenderer();
        String renderID = null;
        DataValueRendererFamily renFamily = null;
        // should always be true unless someone overrides addColumn
        if (curRen instanceof DataValueRendererFamily) {
            renFamily = (DataValueRendererFamily)curRen;
            renderID = renFamily.getDescription();
        }
        String[] availRender = getAvailableRenderers(column);
        if (availRender != null && availRender.length > 1) {
            JMenu subMenu = new JMenu("Available Renderers");
            popup.add(subMenu);
            // actionlistener which changes the renderer according to the
            // action command
            ActionListener actionListener = new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent action) {
                    changeRenderer(column, action.getActionCommand());
                }
            };
            ButtonGroup buttonGroup = new ButtonGroup();
            for (int i = 0; i < availRender.length; i++) {
                String thisID = availRender[i];
                menuItem = new JRadioButtonMenuItem(thisID);
                menuItem.setEnabled(renFamily != null
                        && renFamily.accepts(thisID, spec));
                buttonGroup.add(menuItem);
                menuItem.setActionCommand(thisID);
                menuItem.addActionListener(actionListener);
                menuItem.setSelected(thisID.equals(renderID));
                subMenu.add(menuItem);
            }
        }
        return popup;
    }

    /** Create a custom popup menu when the mouse was clicked in a column header.
     * This popup menu will contain possible sort options, ascending, descending, and clear sorting.
     * @param columnInView column for which to create the popup menu
     * @param sortKey the new sort key
     * @return a popup menu displaying these properties
     * @see #onMouseClickInHeader(MouseEvent)
     * @since 2.8
     */
    protected JPopupMenu createSortPopupMenu(final int columnInView, final TableSortKey sortKey) {
        final TableColumn tableColumn = getColumnModel().getColumn(columnInView);
        final Object value = tableColumn.getHeaderValue();
        if (!(value instanceof DataColumnSpec)) {
            // only occurs if someone overrides the addColumn method.
            return null;
        }
        return createSortPopupMenu(getContentModel(), this, convertColumnIndexToModel(columnInView), sortKey);
    }

    /** Create a custom popup menu when the mouse was clicked in a column header.
     * This popup menu will contain possible sort options, ascending, descending, and clear sorting.
     * @param contentModel The content model on which to call the sort routines.
     * @param parentComp Component on which the sort-progress bar will be made modal
     * @param columnInModel column (in model), which is going to be sorted.
     * @param sortKey the new sort key
     * @return a popup menu displaying these properties
     * @see #onMouseClickInHeader(MouseEvent)
     * @since 2.8
     */
    static JPopupMenu createSortPopupMenu(final TableContentModel contentModel,
        final JComponent parentComp, final int columnInModel, final TableSortKey sortKey) {
        /** Action listener for all sort actions. */
        final class SortKeyActionListener implements ActionListener {
            private final TableSortKey m_sortKey;
            /** New sort key action listener.  */
            private SortKeyActionListener(final TableSortKey sk) {
                m_sortKey = sk;
            }
            @Override
            public void actionPerformed(final ActionEvent action) {
                contentModel.requestSort(columnInModel, parentComp, m_sortKey);
            }
        }
        final JPopupMenu popup = new JPopupMenu("Column Context Menu");
        final JRadioButtonMenuItem menuItemDesc1 = new JRadioButtonMenuItem("Sort Descending");
        menuItemDesc1.addActionListener(new SortKeyActionListener(TableSortKey.PRIMARY_DESCENDING));
        final JRadioButtonMenuItem menuItemAsc1 = new JRadioButtonMenuItem("Sort Ascending");
        menuItemAsc1.addActionListener(new SortKeyActionListener(TableSortKey.PRIMARY_ASCENDING));
        final JRadioButtonMenuItem menuItemDesc2 = new JRadioButtonMenuItem("Sort Descending");
        menuItemDesc2.addActionListener(new SortKeyActionListener(TableSortKey.SECONDARY_DESCENDING));
        final JRadioButtonMenuItem menuItemAsc2 = new JRadioButtonMenuItem("Sort Ascending");
        menuItemAsc2.addActionListener(new SortKeyActionListener(TableSortKey.SECONDARY_ASCENDING));
        final JRadioButtonMenuItem menuItemNone = new JRadioButtonMenuItem("No Sorting");
        menuItemNone.addActionListener(new SortKeyActionListener(TableSortKey.NONE));
        popup.add(menuItemNone);
        switch (sortKey) {
            case NONE:
                menuItemDesc1.setSelected(false);
                popup.add(menuItemDesc1);
                menuItemAsc1.setSelected(false);
                popup.add(menuItemAsc1);
                menuItemNone.setSelected(true);
                popup.add(menuItemNone);
                break;
            case PRIMARY_DESCENDING:
                menuItemDesc1.setSelected(true);
                popup.add(menuItemDesc1);
                menuItemAsc1.setSelected(false);
                popup.add(menuItemAsc1);
                menuItemNone.setSelected(false);
                popup.add(menuItemNone);
                break;
            case PRIMARY_ASCENDING:
                menuItemDesc1.setSelected(false);
                popup.add(menuItemDesc1);
                menuItemAsc1.setSelected(true);
                popup.add(menuItemAsc1);
                menuItemNone.setSelected(false);
                popup.add(menuItemNone);
                break;
            case SECONDARY_DESCENDING:
                menuItemDesc2.setSelected(true);
                popup.add(menuItemDesc2);
                menuItemAsc2.setSelected(false);
                popup.add(menuItemAsc2);
                menuItemNone.setSelected(false);
                popup.add(menuItemNone);
                break;
            case SECONDARY_ASCENDING:
                menuItemDesc2.setSelected(false);
                popup.add(menuItemDesc2);
                menuItemAsc2.setSelected(true);
                popup.add(menuItemAsc1);
                menuItemNone.setSelected(false);
                popup.add(menuItemNone);
                break;
            default: throw new IllegalArgumentException("TableSortKey '" + sortKey.name() + "' not implemented.");
        }
        return popup;
    }

    /**
     * Changes the renderer in a given column. The column's renderer is
     * retrieved and checked if it is instance of
     * {@link DataValueRendererFamily} (which it is unless a subclass
     * overrides <code>addColumn</code>). In this renderer family the renderer
     * matching the description <code>rendererID</code> is set active.
     * <br>
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

    /** Changes the renderer in all columns whose type is
     * equal to <code>type</code>. This is a convenient way to change the
     * renderer of several columns at once. This method does nothing if
     * the type is unknown or the identifier is invalid.
     * @param type the target type
     * @param ident The identifier for the renderer to use
     * @see #getTypeRendererMap()
     */
    public void changeRenderer(final DataType type, final String ident) {
        for (Enumeration<TableColumn> e = getColumnModel().getColumns();
            e.hasMoreElements();) {
            TableColumn tc = e.nextElement();
            Object headerValue = tc.getHeaderValue();
            TableCellRenderer ren = tc.getCellRenderer();
            if (headerValue instanceof DataColumnSpec
                    && ren instanceof DataValueRendererFamily) {
                DataColumnSpec c = (DataColumnSpec)headerValue;
                DataValueRendererFamily r = (DataValueRendererFamily)ren;
                DataType t = c.getType();
                if (t.equals(type)) {
                    r.setActiveRenderer(ident);
                }
            }
        }
        repaint();
    }

    /** Sets the width of all columns to the argument width.
     * @param width The new width.
     * @see TableColumn#setWidth(int)
     */
    public void setColumnWidth(final int width) {
        for (Enumeration<TableColumn> e = getColumnModel().getColumns();
            e.hasMoreElements();) {
            TableColumn next = e.nextElement();
            if (width < next.getMinWidth()) {
                next.setMinWidth(width);
            }
            next.setPreferredWidth(width);
            next.setWidth(width);
        }
    }

    /** Creates a new map containing DataType&lt;-&gt;available renderer
     * identifiers. The size of this map is equal to the number of different
     * {@link DataColumnSpec#getType() column types}, i.e. if the table
     * only contains, e.g. double values (represented by
     * {@link org.knime.core.data.def.DoubleCell}), this map will have only one
     * entry. The values in this map correspond to the renderer descriptions
     * that are {@link DataType#getRenderer(DataColumnSpec) available for the
     * type at hand}.
     *
     * <p>This map is used to switch the renderer for a set of columns.
     * @return Such a (new) map.
     */
    public Map<DataType, String[]> getTypeRendererMap() {
        LinkedHashMap<DataType, String[]> result =
            new LinkedHashMap<DataType, String[]>();
        for (Enumeration<TableColumn> e = getColumnModel().getColumns();
            e.hasMoreElements();) {
            TableColumn tc = e.nextElement();
            Object headerValue = tc.getHeaderValue();
            TableCellRenderer ren = tc.getCellRenderer();
            if (headerValue instanceof DataColumnSpec
                    && ren instanceof DataValueRendererFamily) {
                DataColumnSpec c = (DataColumnSpec)headerValue;
                DataValueRendererFamily r = (DataValueRendererFamily)ren;
                DataType t = c.getType();
                if (!result.containsKey(t)) {
                    result.put(t, r.getRendererDescriptions());
                }
            }
        }
        return result;
    }

    /**
     * Sets the preferred column width and returns the maximum of the
     * preferred row heights according to each column's renderer.
     * This method should only be called when the table is built up from
     * scratch.
     * @return the best initial row height (this must be set elsewhere)
     */
    int fitCellSizeToRenderer() {
        TableColumnModel colModel = getColumnModel();
        int bestRowHeight = 16;
        for (Enumeration<TableColumn> enu =
            colModel.getColumns(); enu.hasMoreElements();) {
            TableColumn col = enu.nextElement();
            TableCellRenderer renderer = col.getCellRenderer();
            if (renderer instanceof DataValueRenderer) {
                if (getRowCount() > 0) {
                    prepareRenderer(renderer, 0, col.getModelIndex());
                }
                Dimension p = ((DataValueRenderer)renderer).getPreferredSize();
                int prefHeight = p.height;
                int colWidth = p.width;
                if (col.getWidth() < colWidth) {
                    col.setPreferredWidth(colWidth);
                }
                bestRowHeight = Math.max(bestRowHeight, prefHeight);
            }
        }
        return bestRowHeight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void tableChanged(final TableModelEvent e) {
        super.tableChanged(e);
        if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
            int bestRowHeight = fitCellSizeToRenderer();
            if (bestRowHeight != getRowHeight()) {
                firePropertyChange(
                        "requestRowHeight", getRowHeight(), bestRowHeight);
            }
        }
        final JTableHeader header = getTableHeader();
        if (header != null) {
            header.repaint(); // update sort icons
        }
    }

    /**
     * Get the renderer for the column header (never <code>null</code>).
     *
     * @return a new <code>ColumnHeaderRenderer</code>
     * @see ColumnHeaderRenderer
     * @see JTableHeader#setDefaultRenderer(javax.swing.table.TableCellRenderer)
     */
    protected ColumnHeaderRenderer getNewColumnHeaderRenderer() {
        ColumnHeaderRenderer r = new ColumnHeaderRenderer();
        r.setWrapHeader(isWrapHeader());
        return r;
    }
}
