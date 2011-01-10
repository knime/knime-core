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
 * --------------------------------------------------------------------- *
 * 
 */
package org.knime.core.node.tableview;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.knime.core.data.DataTable;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentModel.TableContentFilter;
import org.knime.core.node.util.ConvenienceMethods;


/** 
 * Panel containing a table view on a generic {@link DataTable}. The
 * table is located in a scroll pane and row and column headers are visible and
 * fixed.
 * <br />
 * For the caching strategy used in the table refer to
 * {@link TableContentModel}.
 *  
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableView extends JScrollPane {
    private static final long serialVersionUID = -5066803221414314340L;

    // TODO adjust header width automatically on start-up 
    // and add functionality to mouse-drag the header width 
    /** Header column's width in pixel. */
    private static final int ROWHEADER_WIDTH = 100;

    /** Cursor that is shown when the column header is resized (north-south). */
    private static final Cursor RESIZE_CURSOR = 
        Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);

    
    /** The popup menu which allows to trigger hilite events. */
    private JPopupMenu m_popup;
    
    /** Enables the resizing of the column header. Instantiated lazily. */
    private ColumnHeaderResizeMouseHandler m_columnHeaderResizeHandler;
    
    /** Whether or not column header resizing is allowed. Defaults to false. */
    private boolean m_isColumnHeaderResizingAllowed;
    
    /** Position (row, col) for "Find" menu entry. It either points to the 
     * location of the last match or (0,0). */
    private FindPositionRowKey m_searchPosition;
    
    /** Last search string, needed for continued search. */
    private String m_searchString;
    
    private TableAction m_findAction;
    
    private TableAction m_findNextAction;
    
    private TableAction m_gotoRowAction;
    
    /** 
     * Creates new empty <code>TableView</code>. Content and handlers are set
     * using the appropriate methods, that is, 
     * {@link #setDataTable(DataTable)} and 
     * {@link #setHiLiteHandler(HiLiteHandler)}. The model for this 
     * view, however, is not <code>null</code>. That is, it's completely legal 
     * to do {@link #getContentModel()} right after calling this 
     * constructor.
     */
    public TableView() {
        this(new TableContentModel());
    }
    
    /** 
     * Creates new instance of a <code>TableView</code> given a content view.
     * A row header is created and displayed. There is no property handler
     * connected to this view at this time.
     * 
     * @param contentView view to display.
     * @throws NullPointerException if contentView is <code>null</code>
     */
    public TableView(final TableContentView contentView) {
        // disallow null arguments
        super(checkNull(contentView));
        m_isColumnHeaderResizingAllowed = false;
        // if not "off", the horizontal scroll bar is never shown (reduces
        // size of columns to minimum)
        contentView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        TableRowHeaderView rowHeadView = 
            TableRowHeaderView.createHeaderView(contentView);
        setRowHeaderView(rowHeadView);
        // set width of the row header
        rowHeadView.setPreferredScrollableViewportSize(
            new Dimension(ROWHEADER_WIDTH, 0));
        setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, 
            rowHeadView.getTableHeader());
        
        // workaround for bug 4202002: The scrolling is out of sync when 
        // scrolled in RowHeader.
        // add a listener to force re-synchronization
        getRowHeader().addChangeListener(new ChangeListener() {
            public void stateChanged(final ChangeEvent e) {
                JViewport jvViewport = (JViewport) e.getSource();
                int iExtent = jvViewport.getExtentSize().height;
                int iMax = jvViewport.getViewSize().height;
                int iValue = Math.max(0, Math.min(
                    jvViewport.getViewPosition().y, iMax - iExtent));
                getVerticalScrollBar().setValues(iValue, iExtent, 0, iMax);
            } // stateChanged(ChangeEvent)
        });
        // listener that opens the popup on the table's row header
        getHeaderTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Point ePoint = e.getPoint();
                    Point thisPoint = SwingUtilities.convertPoint(
                            (Component)e.getSource(), ePoint, TableView.this);
                    showPopup(thisPoint);
                }
            }
        });
        getContentModel().addPropertyChangeListener(
                TableContentModel.PROPERTY_DATA, new PropertyChangeListener() {
           public void propertyChange(final PropertyChangeEvent evt) {
               
           }         
        });
    } // TableView(TableContentView)
    
    /** 
     * Constructs new View by calling 
     * <code>this(new TableContentView(model))</code>.
     * 
     * @param model model to be displayed.
     * @see TableView#TableView(TableContentView)
     * @throws NullPointerException if <code>model</code> is <code>null</code>.
     */ 
    public TableView(final TableContentModel model) {
        this(new TableContentView(model));
    } // TableView(TableContentModel)

    /**
     * Creates new instance of a <code>TableView</code> based on a 
     * {@link DataTable}. A row header is created and displayed.
     * 
     * @param table table to be displayed
     * @throws NullPointerException if <code>table</code> is <code>null</code>.
     */
    public TableView(final DataTable table) {
        this(new TableContentModel(table));
    } // TableView(DataTable)
    
    /**
     * Creates new instance of a <code>TableView</code> based on a 
     * {@link DataTable}. A row header is created and displayed.
     * 
     * @param table table to be displayed
     * @param propHdl used to connect other views, may be <code>null</code>
     * @throws NullPointerException if <code>table</code> is <code>null</code>
     */
    public TableView(final DataTable table, final HiLiteHandler propHdl) {
        this(new TableContentModel(table, propHdl));
    } // TableView(DataTable, HiLiteHandler)

    /**
     * Simply checks if argument is <code>null</code> and throws an exception if
     * it is. Otherwise returns argument. This method is called in the 
     * constructor.
     * 
     * @param content argument to check.
     * @return <code>content</code>
     * @throws NullPointerException if <code>content</code> is <code>null</code>
     */    
    private static TableContentView checkNull(final TableContentView content) {
        if (content == null) {
            throw new NullPointerException("Content View must not be null!");
        }
        return content;
    }

    /** 
     * Get reference to table view that is in the scroll pane's view port.
     * 
     * @return reference to content table
     */
    public TableContentView getContentTable() {
        return (TableContentView) getViewport().getView();
    }
    
    /** 
     * Get reference to underlying <code>TableContentModel</code>. This call 
     * is identical to calling 
     * <code>(TableContentModel)(getContentTable().getModel())</code>.
     * 
     * @return the model displayed.
     */
    public TableContentModel getContentModel() {
        return (TableContentModel)(getContentTable().getModel());
    }
    
    /** 
     * Get reference to row header table, that is the column displaying the
     * row keys from the underlying table.
     * 
     * @return reference to row header.
     */
    public TableRowHeaderView getHeaderTable() {
        return (TableRowHeaderView)getRowHeader().getView();
    }
    
    /** Delegates to super implementation but sets an appropriate preferred
     * size before returning. If the table has no columns (but more than 0 
     * rows), the corner was not shown (because of 0,0 preferred dimension).
     * {@inheritDoc}
     */
    @Override
    public JViewport getColumnHeader() {
        JViewport viewPort = super.getColumnHeader();
        if (viewPort != null && viewPort.getView() != null) { 
            Component view = viewPort.getView();
            int viewHeight = view.getPreferredSize().height;
            boolean hasData = hasData();
            // bug fix #934: header of row header column was not shown when
            // table contains no columns
            if (hasData && viewHeight == 0) {
                view.setPreferredSize(
                        getCorner(UPPER_LEFT_CORNER).getPreferredSize());
            } else if (!hasData && viewHeight > 0) {
                // null is perfectly ok, it seems
                view.setPreferredSize(null);
            }
        }
        return viewPort;
    }
    
    /** Overwritten to add (north-south) resize listener to upper left corner.  
     * {@inheritDoc} */
    @Override
    public void setCorner(final String key, final Component corner) {
        if (UPPER_LEFT_CORNER.equals(key)) {
            Component old = getCorner(UPPER_LEFT_CORNER); 
            if (old != null && m_columnHeaderResizeHandler != null) {
                old.removeMouseListener(m_columnHeaderResizeHandler);
                old.removeMouseMotionListener(m_columnHeaderResizeHandler);
            } 
            if (corner != null && isColumnHeaderResizingAllowed()) {
                if (m_columnHeaderResizeHandler == null) {
                    m_columnHeaderResizeHandler = 
                        new ColumnHeaderResizeMouseHandler();
                }
                corner.addMouseListener(m_columnHeaderResizeHandler);
                corner.addMouseMotionListener(m_columnHeaderResizeHandler);
            }
        }
        super.setCorner(key, corner);
    }

    /** 
     * Checks if a property handler is registered.
     * 
     * @return <code>true</code> if global hiliting is possible (property
     *         handler is available).
     * @see TableContentModel#hasHiLiteHandler()
     */
    public final boolean hasHiLiteHandler() {
        return getContentTable().hasHiLiteHandler();
    }

    /**
     * This table "has data" when there is valid input, i.e. the 
     * {@link DataTable} to display is not <code>null</code>. The 
     * status may changed during runtime by calling the model's 
     * <code>setDataTable(DataTable)</code> method.
     * 
     * @return <code>true</code> when there is data to display,
     *      <code>false</code> otherwise
     * @see TableContentModel#hasData()
     */
    public boolean hasData() {
        return getContentTable().hasData();
    }
    
    /**
     * Sends a request to the content table to hilite all currently selected 
     * rows.
     * 
     * @see TableContentView#hiliteSelected()
     */
    public void hiliteSelected() {
        getContentTable().hiliteSelected();
    }
    
    /**
     * Sends a request to the content table to unhilite all currently selected 
     * rows.
     * 
     * @see TableContentView#unHiliteSelected()
     */
    public void unHiliteSelected() {
        getContentTable().unHiliteSelected();
    }

    /**
     * Sends a request to the content table to reset (unhilite) all rows.
     * 
     * @see TableContentView#resetHilite() 
     */
    public void resetHilite() {
        getContentTable().resetHilite();
    }
    
    /**
     * Sets a new <code>DataTable</code> as content. 
     * 
     * @param data new data to be shown; may be <code>null</code> to have an
     *        empty table.
     * @see TableContentModel#setDataTable(DataTable)
     */
    public void setDataTable(final DataTable data) {
        getContentTable().setDataTable(data);
    }
    
    /**
     * Sets a new <code>HiLiteHandler</code> this view talks to. 
     * The argument may be <code>null</code> to disconnect from the
     * current <code>HiLiteHandler</code>.
     * 
     * @param hiLiteHdl the new <code>HiLiteHandler</code>.
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        getContentTable().setHiLiteHandler(hiLiteHdl);
    }
    
    /**
     * Control behavior to show only hilited rows.
     * 
     * @param showOnlyHilite <code>true</code> Filter and display only
     *        rows whose hilite status is set.
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
     * @return <code>true</code>: only hilited rows are shown, 
     *         <code>false</code>: all rows are shown.
     * @see TableContentModel#getTableContentFilter() 
     * @deprecated Implementors should refer to 
     * <code>getContentModel().getTableContentFilter()</code>
     */
    @Deprecated
    public boolean showsHiLitedOnly() {
        return getContentModel().showsHiLitedOnly();
    }
    
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
     * Get row height from table.
     * 
     * @return current row height
     * @see javax.swing.JTable#getRowHeight() 
     */
    public int getRowHeight() {
        return getHeaderTable().getRowHeight();
    }
   
    /**
     * Returns the width of the first column or -1 if there are no columns.
     * 
     * @return the width of the first column.
     */
    public int getColumnWidth() {
        TableColumnModel colModel = getContentTable().getColumnModel();
        if (colModel.getColumnCount() > 0) {
            return colModel.getColumn(0).getWidth();
        } else {
            return -1;
        }
    }
    
    /**
     * Sets an equal width in all columns.
     * 
     * @param width the new width.
     * @see javax.swing.table.TableColumn#setPreferredWidth(int)
     */
    public void setColumnWidth(final int width) {
        TableColumnModel colModel = getContentTable().getColumnModel();
        for (int i = 0; i < colModel.getColumnCount(); i++) {
            TableColumn c = colModel.getColumn(i);
            if (width < c.getMinWidth()) {
                c.setMinWidth(width);
            }
            c.setPreferredWidth(width);
        }
    }
    
    /** Get the height of the column header view or -1 if none has been set
     * (no data available).
     * @return The height of the column header view.
     */
    public int getColumnHeaderViewHeight() {
        JViewport header = getColumnHeader();
        Component v;
        if (header != null) {
            v = header.getView();
        } else {
            // the header == null if the table has not been completely 
            // initialized (i.e. configureEnclosingScrollPane has not been
            // called the JTable). The header will be the JTableHeader of the
            // table
            v = getContentTable().getTableHeader();
        }
        if (v != null) {
            return v.getHeight();
        }
        return -1;
    }
    
    /** Set the height of the column header view. If none has been set
     * (i.e. no data is available), this method does nothing.
     * @param newHeight The new height.
     */
    public void setColumnHeaderViewHeight(final int newHeight) {
        JViewport header = getColumnHeader();
        Component v;
        if (header != null) {
            v = header.getView();
        } else {
            // the header == null if the table has not been completely 
            // initialized (i.e. configureEnclosingScrollPane has not been
            // called the JTable). The header will be the JTableHeader of the
            // table
            v = getContentTable().getTableHeader();
        }
        if (v != null) {
            Dimension d = v.getSize();
            d.height = newHeight;
            v.setSize(d);
            v.setPreferredSize(d);
        }
    }
    
    /**
     * Set a new row height in the table.
     * 
     * @param newHeight the new height
     * @see javax.swing.JTable#setRowHeight(int)
     */
    public void setRowHeight(final int newHeight) {
        // perform action on header - it makes sure that the event gets
        // propagated to the content view
        getHeaderTable().setRowHeight(newHeight);
    }
    
    /**
     * Shall row header encode the color information in an icon?
     * 
     * @param showIt <code>true</code> for show icon (and thus the color),
     *        <code>false</code> ignore colors
     * @see TableRowHeaderView#setShowColorInfo(boolean)
     */
    public void setShowColorInfo(final boolean showIt) {
        getHeaderTable().setShowColorInfo(showIt);
    }
    
    /**
     * Is color icon shown?
     * 
     * @return <code>true</code> if it is, <code>false</code> otherwise
     * @see TableRowHeaderView#isShowColorInfo()
     */
    public boolean isShowColorInfo() {
        return getHeaderTable().isShowColorInfo();
    }
    
    /** 
     * Set whether or not the icon in the column header is to be displayed.
     * Delegate method to {@link TableView#setShowColorInfo(boolean)}.
     * @param show Whether or not this icon should be shown.
     */
    public void setShowIconInColumnHeader(final boolean show) {
        getContentTable().setShowIconInColumnHeader(show);
    }
    
    /**
     * Get the status if the icon in the column header is shown.
     * Delegate method to {@link TableView#isShowColorInfo()}.
     * @return true when the icon is shown, false otherwise.
     */
    public boolean isShowIconInColumnHeader() {
        return getContentTable().isShowIconInColumnHeader();
    }
    
    /**
     * Whether or not the resizing of the column header height is allowed.
     * The default is <code>false</code>.
     * @return the isColumnHeaderResizingAllowed.
     * @see #setColumnHeaderResizingAllowed(boolean)
     */
    public boolean isColumnHeaderResizingAllowed() {
        return m_isColumnHeaderResizingAllowed;
    }

    /**
     * Enable or disable the resizing of the column header height.
     * @param isColumnHeaderResizingAllowed If <code>true</code> resizing is
     * allowed.
     */
    public void setColumnHeaderResizingAllowed(
            final boolean isColumnHeaderResizingAllowed) {
        if (m_isColumnHeaderResizingAllowed == isColumnHeaderResizingAllowed) {
            return;
        }
        m_isColumnHeaderResizingAllowed = isColumnHeaderResizingAllowed;
        Component corner = getCorner(UPPER_LEFT_CORNER);
        if (m_isColumnHeaderResizingAllowed) {
            if (corner != null) {
                if (m_columnHeaderResizeHandler == null) {
                    m_columnHeaderResizeHandler = 
                        new ColumnHeaderResizeMouseHandler();
                }
                corner.addMouseListener(m_columnHeaderResizeHandler);
                corner.addMouseMotionListener(m_columnHeaderResizeHandler);
            }
        } else {
            if (corner != null && m_columnHeaderResizeHandler != null) {
                corner.removeMouseListener(m_columnHeaderResizeHandler);
                corner.removeMouseMotionListener(m_columnHeaderResizeHandler);
            }
        }
    }
    
    /**
     * Find cells (or row IDs) that match the search string. If a matching 
     * element is found, the view is scrolled to that position. Successive calls
     * of this method with the same arguments will continue the search.
     * 
     * @param search The search string.
     * @param idOnly If only the ID column should be searched.
     * @throws NullPointerException If <code>search</code> argument is null.
     */
    protected void find(final String search, final boolean idOnly) {
        if (search == null) {
            throw new NullPointerException("Search expression is null");
        }
        TableContentView cView = getContentTable();
        if (cView == null) {
            return;
        }
        // if new search options, reset search position and start on top
        if (m_searchPosition == null || idOnly != m_searchPosition.isIDOnly()) {
            initNewSearchPostion(idOnly);
        } else if (!ConvenienceMethods.areEqual(m_searchString, search)) {
            m_searchPosition.reset();
        }
        setLastSearchString(search);
        
        m_searchPosition.mark();
        do {
            if (m_searchPosition.next()) {
                JOptionPane.showMessageDialog(this, 
                    "Reached end of table, continue from top");
            }
            int pos = m_searchPosition.getSearchRow();
            if (pos < 0) {
                // the position object will traverse all rows, then
                // reset the position (pos = -1) and then start from 0, ...
                // this can't be changed because of the marking (we also have
                // to notice if we come from a m_searchPostion.reset() state
                // into a m_searchPosition.reset() state.
                continue;
            }
            int col = m_searchPosition.getSearchColumn();
            String str = null;
            if (col < 0) {
                str = cView.getContentModel().getRowKey(pos).getString();
            } else {
                TableCellRenderer rend = cView.getCellRenderer(pos, col);
                Component comp = rend.getTableCellRendererComponent(cView, 
                        cView.getValueAt(pos, col), false, false, pos, col);
                if (comp instanceof JLabel) {
                    str = ((JLabel)comp).getText();
                }
            }
            if (str != null && str.contains(m_searchString)) {
                gotoCell(pos, col);
                return;
            }
        } while (!m_searchPosition.reachedMark());
        JOptionPane.showMessageDialog(this, "Search string not found");
        m_searchPosition.reset();
    }
    
    private void initNewSearchPostion(final boolean idOnly) {
        TableContentView cView = getContentTable();
        int rowCount = cView.getRowCount();
        int colCount = cView.getColumnCount();
        m_searchPosition = idOnly ? new FindPositionRowKey(rowCount)
                : new FindPositionAll(rowCount, colCount);

    }

    /** Sets a new search string and sends event if it differs from the
     * previous search string. (Event is important for Find Next button's
     * enable status.
     * @param searchString The new search string.
     */
    private void setLastSearchString(final String searchString) {
        if (!ConvenienceMethods.areEqual(m_searchString, searchString)) {
            String old = m_searchString;
            m_searchString = searchString;
            firePropertyChange("search_string", old, searchString);
        }
    }

    /**
     * Scrolls to the given coordinate cell. This method is invoked
     * from the navigation menu. If there is no such coordinate it will 
     * display an error message.
     * 
     * @param row the row to scroll to 
     * @param col the col to scroll to (negative for row key)
     */
    public void gotoCell(final int row, final int col) {
        TableContentView cView = getContentTable();
        try {
            cView.getValueAt(row, Math.max(col, 0));
        } catch (IndexOutOfBoundsException ioe) {
            if (cView.getColumnCount() != 0) {
                JOptionPane.showMessageDialog(this, "No such row/col: (" 
                        + (row + 1) + ", " + (col + 1) + ")", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        Rectangle rec = cView.getCellRect(row, Math.max(col, 0), false);
        cView.getSelectionModel().setSelectionInterval(row, row);
        if (col >= 0) {
            ListSelectionModel colSelModel = 
                cView.getColumnModel().getSelectionModel();
            if (colSelModel != null) {
                colSelModel.setSelectionInterval(col, col);
            }
        }
        cView.scrollRectToVisible(rec);
    }
    
    /**
     * Opens the popup menu on the row header. It allows to trigger hilite
     * events.
     * 
     * @param p location where to open the popup
     */
    protected void showPopup(final Point p) {
        if (!hasHiLiteHandler()) {
            return;
        }
        if (m_popup == null) {
            m_popup = new JPopupMenu();
            for (JMenuItem item : createHiLiteMenuItems()) {
                m_popup.add(item);
            }
        }
        m_popup.show(this, p.x, p.y);
    }
    
    /** Registers the argument action in the components action map.
     * @param action The action to register (does nothing if 
     *        it hasn't a key stroke assigned)
     */
    private void registerAction(final TableAction action) {
        KeyStroke stroke = action.getKeyStroke();
        Object name = action.getValue(TableAction.NAME);
        if (stroke != null) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(stroke, name);
            getActionMap().put(name, action);
        }
    }
    
    /**
     * Create the navigation menu for this table view.
     *  
     * @return a new <code>JMenu</code> with navigation controllers.
     */
    public JMenu createNavigationMenu() {
        final JMenu result = new JMenu("Navigation");
        result.setMnemonic('N');
        TableAction gotoRowAction = registerGotoRowAction();
        JMenuItem goToRowItem = new JMenuItem(gotoRowAction);
        goToRowItem.setAccelerator(gotoRowAction.getKeyStroke());
        goToRowItem.addPropertyChangeListener(
                new EnableListener(this, true, false));
        goToRowItem.setEnabled(hasData());
        result.add(goToRowItem);
        TableAction findAction = registerFindAction();
        JMenuItem findItem = new JMenuItem(findAction);
        findItem.setAccelerator(findAction.getKeyStroke());
        findItem.addPropertyChangeListener(
                new EnableListener(this, true, false));
        findItem.setEnabled(hasData());
        result.add(findItem);
        TableAction findNextAction = registerFindNextAction();
        final JMenuItem findNextItem = new JMenuItem(findNextAction);
        findNextItem.setAccelerator(findNextAction.getKeyStroke());
        findNextItem.addPropertyChangeListener(
                new EnableListener(this, true, false) {
            /** {@inheritDoc} */
            @Override
            protected boolean checkEnabled(final JComponent source) {
                return super.checkEnabled(source) && m_searchString != null;
            }
        });
        addPropertyChangeListener("search_string", 
                new PropertyChangeListener() {
            /** {@inheritDoc} */
            public void propertyChange(final PropertyChangeEvent evt) {
                // firePropertyChange with object args is not visible.
                // the item does not care ... (see above)
                findNextItem.firePropertyChange(evt.getPropertyName(), 0, 1);
            }
        });
        findNextItem.firePropertyChange("update", true, false);
        result.add(findNextItem);
        return result;
    } // createNavigationMenu()
    
    /**
     * Get a new menu to control hiliting for this view.
     * 
     * @return a new JMenu with hiliting buttons
     */
    public JMenu createHiLiteMenu() {
        final JMenu result = new JMenu("Hilite");
        result.setMnemonic('H');
        for (JMenuItem item : createHiLiteMenuItems()) {
            result.add(item);
        }
        return result;
    } // createHiLiteMenu()
    
    /**
     * Helper function to create new JMenuItems that are in the hilite menu.
     * 
     * @return all those items in an array
     */
    Collection<JMenuItem> createHiLiteMenuItems() {
        ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
        JMenuItem hsitem = new JMenuItem("Hilite Selected");
        hsitem.setMnemonic('S');
        hsitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                hiliteSelected();
            }
        });
        hsitem.addPropertyChangeListener(new EnableListener(this, true, true));
        hsitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(hsitem);
        
        JMenuItem usitem = new JMenuItem("Unhilite Selected");
        usitem.setMnemonic('U');
        usitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                unHiliteSelected();
            }
        });
        usitem.addPropertyChangeListener(new EnableListener(this, true, true));
        usitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(usitem);
        
        JMenuItem chitem = new JMenuItem("Clear Hilite");
        chitem.setMnemonic('C');
        chitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                resetHilite();
            }
        });
        chitem.addPropertyChangeListener(new EnableListener(this, true, true));
        chitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(chitem);
        
        JMenu filterSubMenu = new JMenu("Filter");
        
        JRadioButtonMenuItem showAllItem = 
            new JRadioButtonMenuItem("Show All");
        showAllItem.addPropertyChangeListener(
                "ancestor", new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                boolean selected = getContentModel().
                    getTableContentFilter().equals(TableContentFilter.All);
                ((AbstractButton)evt.getSource()).setSelected(selected);
            }
        });
        showAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getContentModel().setTableContentFilter(TableContentFilter.All);
            }
        });
        showAllItem.addPropertyChangeListener(
                new EnableListener(this, true, false));
        showAllItem.setEnabled(hasData());
        filterSubMenu.add(showAllItem);
        
        JRadioButtonMenuItem showHiliteItem = 
            new JRadioButtonMenuItem("Show Hilited Only");
        showHiliteItem.addPropertyChangeListener(
                "ancestor", new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                boolean selected = getContentModel().
                getTableContentFilter().equals(TableContentFilter.HiliteOnly);
                ((AbstractButton)evt.getSource()).setSelected(selected);
            }
        });
        showHiliteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getContentModel().setTableContentFilter(
                        TableContentFilter.HiliteOnly);
            }
        });
        showHiliteItem.addPropertyChangeListener(
                new EnableListener(this, true, true));
        showHiliteItem.setEnabled(hasData() && hasHiLiteHandler());
        filterSubMenu.add(showHiliteItem);
        
        JRadioButtonMenuItem showUnHiliteItem = 
            new JRadioButtonMenuItem("Show UnHilited Only");
        showUnHiliteItem.addPropertyChangeListener(
                "ancestor", new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                boolean selected = getContentModel().
                    getTableContentFilter().equals(
                            TableContentFilter.UnHiliteOnly);
                ((AbstractButton)evt.getSource()).setSelected(selected);
            }
        });
        showUnHiliteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                getContentModel().setTableContentFilter(
                        TableContentFilter.UnHiliteOnly);
            }
        });
        showUnHiliteItem.addPropertyChangeListener(
                new EnableListener(this, true, true));
        showUnHiliteItem.setEnabled(hasData() && hasHiLiteHandler());
        filterSubMenu.add(showUnHiliteItem);

        result.add(filterSubMenu);
        return result;
    }
    
    /**
     * Get a new menu with view controllers (row height, etc.) for this view.
     * 
     * @return a new JMenu with control buttons.
     */
    public JMenu createViewMenu() {
        final JMenu result = new JMenu("View");
        result.setMnemonic('V');
        JMenuItem item = new JMenuItem("Row Height...");
        item.setMnemonic('H');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                while (true) {
                    int curRowHeight = getRowHeight();
                    String in = JOptionPane.showInputDialog(
                            TableView.this, "Enter new row height:", 
                            "" + curRowHeight);
                    if (in == null) { // canceled
                         return;
                    }
                    try {
                        int newHeight = Integer.parseInt(in);
                        if (newHeight <= 0) { // disallow negative values.
                            JOptionPane.showMessageDialog(
                                    TableView.this, "No negative values allowed"
                                    , "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            setRowHeight(newHeight);
                            return;
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(
                                TableView.this, "Can't parse "
                                + in, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        item.addPropertyChangeListener(new EnableListener(this, true, false));
        item.setEnabled(hasData());
        result.add(item);
        
        item = new JMenuItem("Column Width...");
        item.setMnemonic('W');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                while (true) {
                    int curWidth = getColumnWidth();
                    String in = JOptionPane.showInputDialog(
                            TableView.this, "Enter new column width:", 
                            "" + curWidth);
                    if (in == null) { // canceled
                        return;
                    }
                    try {
                        int newWidth = Integer.parseInt(in);
                        if (newWidth <= 0) { // disallow negative values.
                            JOptionPane.showMessageDialog(
                                    TableView.this, 
                                    "No negative values allowed",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        } else {
                            setColumnWidth(newWidth);
                            return;
                        }
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(
                                TableView.this, "Can't parse "
                                + in, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        item.addPropertyChangeListener(new EnableListener(this, true, false));
        item.setEnabled(hasData());
        result.add(item);
        
        item = new JCheckBoxMenuItem("Show Color Information");
        item.setMnemonic('C');
        item.addPropertyChangeListener("ancestor",
        new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                JCheckBoxMenuItem source = (JCheckBoxMenuItem)evt.getSource();
                source.setSelected(isShowColorInfo());
            }
        });
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                boolean v = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                setShowColorInfo(v);
            }
        });
        item.addPropertyChangeListener(new EnableListener(this, true, false));
        item.setEnabled(hasData());
        result.add(item);
        return result;
    } // createViewMenu()
    
    /** Registers all actions for navigation on the table, namely "Find...", 
     * "Find Next" and "Go to Row...". */
    public void registerNavigationActions() {
        registerFindAction();
        registerFindNextAction();
        registerGotoRowAction();
    }
    
    /** Creates and registers the "Find ..." action on this component. Multiple
     * invocation of this method have no effect (lazy initialization).
     * @return The non-null action representing the find task. 
     * @see #registerNavigationActions() */
    public TableAction registerFindAction() {
        if (m_findAction == null) {
            String name = "Find..."; 
            KeyStroke stroke = KeyStroke.getKeyStroke(
                    KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
            TableAction action = new TableAction(stroke, name) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (!hasData()) {
                        return;
                    }
                    if (m_searchPosition == null) {
                        initNewSearchPostion(false);
                    }
                    m_searchPosition.reset();
                    boolean isIDOnly = m_searchPosition.isIDOnly();
                    JCheckBox rowKeyBox = 
                        new JCheckBox("Row ID only", isIDOnly);
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JLabel("Find String: "), BorderLayout.WEST);
                    panel.add(rowKeyBox, BorderLayout.EAST);
                    String in = (String)JOptionPane.showInputDialog(
                            TableView.this, panel, "Search", 
                            JOptionPane.QUESTION_MESSAGE, null, 
                            null, m_searchString);
                    if (in == null || in.isEmpty()) { // canceled
                        return;
                    }
                    find(in, rowKeyBox.isSelected());
                }
            };
            registerAction(action);
            m_findAction = action;
        }
        return m_findAction;
    }
    
    /** Creates and registers the "Find Next" action on this component. Multiple
     * invocation of this method have no effect (lazy initialization).
     * @return The non-null action representing the find next task.
     * @see #registerNavigationActions() */
    public TableAction registerFindNextAction() {
        registerFindAction();
        if (m_findNextAction == null) {
            String name = "Find Next"; 
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
            TableAction action = new TableAction(stroke, name) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (m_searchString == null) {
                        return;
                    }
                    assert m_searchPosition != null 
                    : "Search position is null but search string is non-null";
                    find(m_searchString, m_searchPosition.isIDOnly());
                }
            };
            registerAction(action);
            m_findNextAction = action;
        }
        return m_findNextAction;
    }
    
    /** Creates and registers the "Go to Row" action on this component. Multiple
     * invocation of this method have no effect (lazy initialization).
     * @return The non-null action representing the go to row task.
     * @see #registerNavigationActions() */
    public TableAction registerGotoRowAction() {
        if (m_gotoRowAction == null) {
            String name = "Go to Row..."; 
            KeyStroke stroke = KeyStroke.getKeyStroke(
                    KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK);
            TableAction action = new TableAction(stroke, name) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (!hasData()) {
                        return;
                    }
                    String rowString = JOptionPane.showInputDialog(
                            TableView.this, "Enter row number:", "Go to Row", 
                            JOptionPane.QUESTION_MESSAGE);
                    if (rowString == null) { // canceled
                         return;
                    }
                    try { 
                        int row = Integer.parseInt(rowString);
                        gotoCell(row - 1, 0);
                    } catch (NumberFormatException nfe) {
                        JOptionPane.showMessageDialog(TableView.this, 
                                "Can't parse " + rowString, "Error", 
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            registerAction(action);
            m_gotoRowAction = action;
        }
        return m_gotoRowAction;
    }
    
    /** PropertyChangeListener that will disable/enable the menu items. */
    private static class EnableListener implements PropertyChangeListener {
        private final boolean m_watchData;
        private final boolean m_watchHilite;
        private final TableView m_view;
                
        /**
         * Constructor. Will respect the hasData(), hasHiliteHandler() flag
         * according to the arguments.
         * @param view The view to get status from
         * @param watchData Shall this listener respect data change events.
         * @param watchHilite Shall this listener respect hilite change events.
         * 
         */
        public EnableListener(final TableView view,
                final boolean watchData, final boolean watchHilite) {
            m_view = view;
            m_watchData = watchData;
            m_watchHilite = watchHilite;
        }
        
        /** {@inheritDoc} */
        public void propertyChange(final PropertyChangeEvent evt) {
            JComponent source = (JComponent)evt.getSource();
            boolean isEnabled = checkEnabled(source);
            source.setEnabled(isEnabled);
        }
        
        /** Determines whether component is to be enabled.
         * @param source Event source (for reference)
         * @return if to enable (true) or disable (false);
         */
        protected boolean checkEnabled(final JComponent source) {
            boolean data = !m_watchData || m_view.hasData();
            boolean hilite = !m_watchHilite || m_view.hasHiLiteHandler();
            return data && hilite;
        }
    }
    
    /** Mouse handler that takes care of the column header resizing. */
    private class ColumnHeaderResizeMouseHandler extends MouseInputAdapter {
        
        private int m_oldMouseY;
        
        private Cursor m_swapCursor = RESIZE_CURSOR; 
        
        /** {@inheritDoc} */
        @Override
        public void mouseMoved(final MouseEvent e) {
            Component c = (Component)e.getSource();
            if (canResize(c, e) != c.getCursor().equals(RESIZE_CURSOR)) {
                swapCursor(c);
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void mousePressed(final MouseEvent e) {
            Component c = (Component)e.getSource();
            if (canResize(c, e)) {
                m_oldMouseY = e.getPoint().y;
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void mouseDragged(final MouseEvent e) {
            if (m_oldMouseY > 0) {
                int diff = e.getPoint().y - m_oldMouseY;
                int oldHeight = getColumnHeaderViewHeight();
                setColumnHeaderViewHeight(oldHeight + diff);
                m_oldMouseY = getColumnHeaderViewHeight();
            }
        }
        
        /** {@inheritDoc} */
        @Override
        public void mouseReleased(final MouseEvent e) {
            m_oldMouseY = -1;
        }
        
        private void swapCursor(final Component c) {
            Cursor oldCursor = c.getCursor();
            c.setCursor(m_swapCursor);
            m_swapCursor = oldCursor;
        }
        
        private boolean canResize(final Component c, final MouseEvent e) {
            return (c.getHeight() - e.getPoint().y <= 3);
        }
    }
    
    /** Action associate with the table. There are instances for "Find...", 
     * "Find Next" and "Go to Row". This class has an additional field for the 
     * preferred accelerator key.
     */
    public abstract static class TableAction extends AbstractAction {
        
        private final KeyStroke m_keyStroke;
        
        /**
         * @param keyStroke Key stroke for this actions 
         *        (needs to be registered elsewhere) 
         * @param name Name, see {@link AbstractAction#AbstractAction(String)}
         */
        TableAction(final KeyStroke keyStroke, final String name) {
            super(name);
            m_keyStroke = keyStroke;
        }
        
        /** @return the keyStroke */
        public KeyStroke getKeyStroke() {
            return m_keyStroke;
        }
    }
    
}   // TableView
