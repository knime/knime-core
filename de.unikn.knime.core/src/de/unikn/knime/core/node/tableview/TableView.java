/* 
 * --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed 
 */
package de.unikn.knime.core.node.tableview;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumnModel;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;

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
    
    /** The popup menu which allows to trigger hilite events. */
    private JPopupMenu m_popup;
    
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
    
    /** 
     * Checks if a property handler is registered.
     * 
     * @return <code>true</code> if global highlighting is possible (property
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
     * Control behaviour to show only highlighted rows.
     * 
     * @param showOnlyHilit <code>true</code> Filter and display only
     *        rows whose highlight status is set.
     * @see TableContentModel#showHighlightedOnly(boolean)
     */
    public final void showHighlightedOnly(final boolean showOnlyHilit) {
        getContentTable().showHighlightedOnly(showOnlyHilit);
    }

    /**
     * Get status of filtering for highlighted rows.
     * 
     * @return <code>true</code>: only highlighted rows are shown, 
     *         <code>false</code>: all rows are shown.
     * @see TableContentModel#showsHighlightedOnly() 
     */
    public boolean showsHighlightedOnly() {
        return getContentTable().showsHighlightedOnly();
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
            colModel.getColumn(i).setPreferredWidth(width);
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
     * Tries to find a row key that matches on the given pattern starting
     * at position <code>startRow</code>. If a matching row is found, the view
     * is scrolled to that position.
     * 
     * @param pattern pattern to look for
     * @param startRow row, where to start the search
     */
    protected void findRow(final String pattern, final int startRow) {
        final int start = Math.min(0, startRow);
        final TableRowHeaderView view = getHeaderTable();
        for (int i = start; i < view.getRowCount(); i++) {
            DataCell key = (DataCell)view.getValueAt(i, 0);
            if (key.toString().matches(pattern)) {
                goToRow(startRow + i);
                return;
            }
        }
        int r = JOptionPane.showConfirmDialog(this, 
                "<html>No occurences!<br><br>Continue from top?</html>", 
                "Reached end", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            findRow(pattern, 0);
        }
    }

    /**
     * Scrolls to row number <code>rowNumber</code>. This method is invoked
     * from the navigation menu. If there is no such line number it will 
     * display an error message.
     * 
     * @param rowNumber the row to scroll to 
     */
    public void goToRow(final int rowNumber) {
        TableContentView cView = getContentTable();
        try {
            cView.getValueAt(rowNumber, 0);
        } catch (IndexOutOfBoundsException ioe) {
            if (cView.getColumnCount() != 0) {
                JOptionPane.showMessageDialog(cView, "No such Row: " 
                        + (rowNumber + 1), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        Rectangle rec = cView.getCellRect(rowNumber, 0, false);
        cView.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
        cView.scrollRectToVisible(rec);
    }

    /**
     * Opens the popup menu on the row header. It allows to trigger hilite
     * events.
     * 
     * @param p location where to open the popup
     */
    protected void showPopup(final Point p) {
        if (m_popup == null) {
            m_popup = new JPopupMenu();
            for (JMenuItem item : createHighlightMenuItems()) {
                m_popup.add(item);
            }
        }
        m_popup.show(this, p.x, p.y);
    }
    
    /**
     * Create the navigation menu for this table view.
     *  
     * @return a new <code>JMenu</code> with navigation controllers.
     */
    public JMenu createNavigationMenu() {
        final JMenu result = new JMenu("Navigation");
        result.setMnemonic('N');
        JMenuItem item = new JMenuItem("Go to Row...");
        item.setMnemonic('G');
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                String rowString = JOptionPane.showInputDialog(
                        TableView.this, "Enter row number:", "Go to Row", 
                        JOptionPane.QUESTION_MESSAGE);
                if (rowString == null) { // cancelled
                     return;
                }
                try { 
                    int row = Integer.parseInt(rowString);
                    goToRow(row - 1);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(TableView.this, 
                            "Can't parse " + rowString, "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        item.addPropertyChangeListener(new EnableListener(this, true, false));
        item.setEnabled(hasData());
        result.add(item);
//        item = new JMenuItem("Search Row Key...");
//        item.setMnemonic('S');
//        item.addActionListener(new ActionListener() {
//            public void actionPerformed(final ActionEvent e) {
//                String in = JOptionPane.showInputDialog(
//                        m_tableView, "Search Key ( \"*\" = any string, " 
//                        + "\"?\" = any character): ", "Search for Row Key", 
//                        JOptionPane.QUESTION_MESSAGE);
//                if (in == null) { // cancelled
//                     return;
//                }
//                String pattern = in.replaceAll("\\*", ".*");
//                pattern = pattern.replaceAll("\\?", ".");
//                findRow(pattern, 0);
//            }
//        });
//        result.add(item);
        return result;
    } // createNavigationMenu()
    
    /**
     * Get a new menu to control highlighting for this view.
     * 
     * @return a new JMenu with highlighting buttons
     */
    public JMenu createHighlightMenu() {
        final JMenu result = new JMenu("Highlight");
        result.setMnemonic('H');
        for (JMenuItem item : createHighlightMenuItems()) {
            result.add(item);
        }
        return result;
    } // createHighlightMenu()
    
    /**
     * Helper function to create new JMenuItems that are in the hilite menu.
     * 
     * @return all those items in an array
     */
    Collection<JMenuItem> createHighlightMenuItems() {
        ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
        JMenuItem hsitem = new JMenuItem("Highlight Selected");
        hsitem.setMnemonic('S');
        hsitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                hiliteSelected();
            }
        });
        hsitem.addPropertyChangeListener(new EnableListener(this, true, true));
        hsitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(hsitem);
        
        JMenuItem usitem = new JMenuItem("Unhighlight Selected");
        usitem.setMnemonic('U');
        usitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                unHiliteSelected();
            }
        });
        usitem.addPropertyChangeListener(new EnableListener(this, true, true));
        usitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(usitem);
        
        JMenuItem chitem = new JMenuItem("Clear Highlight");
        chitem.setMnemonic('C');
        chitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                resetHilite();
            }
        });
        chitem.addPropertyChangeListener(new EnableListener(this, true, true));
        chitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(chitem);
        
        JMenuItem shoitem = new JCheckBoxMenuItem("Show Highlighted Only");
        shoitem.setMnemonic('O');
        shoitem.addPropertyChangeListener(
                "ancestor", new PropertyChangeListener() {
            public void propertyChange(final PropertyChangeEvent evt) {
                JCheckBoxMenuItem source = (JCheckBoxMenuItem)evt.getSource();
                source.setSelected(showsHighlightedOnly());
            }
        });
        shoitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                boolean i = ((JCheckBoxMenuItem)e.getSource()).isSelected();
                showHighlightedOnly(i);
            }
        });
        shoitem.addPropertyChangeListener(new EnableListener(this, true, true));
        shoitem.setEnabled(hasData() && hasHiLiteHandler());
        result.add(shoitem);
        
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
                    if (in == null) { // cancelled
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
                    if (in == null) { // cancelled
                        return;
                    }
                    try {
                        int newWidth = Integer.parseInt(in);
                        if (newWidth <= 0) { // disallow negative values.
                            JOptionPane.showMessageDialog(
                                    TableView.this, "No negative values allowed",
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
    
    /** PropertyChangeListener that will disable/enable the menu items. */
    private static class EnableListener implements PropertyChangeListener {
        private final boolean m_watchData;
        private final boolean m_watchHilite;
        private final TableView m_view;
        
        /**
         * Constructor. Will respect the hasData(), hasHiliteHandler() flag
         * according to the arguments.
         * 
         */
        public EnableListener(final TableView view,
                final boolean watchData, final boolean watchHilite) {
            m_view = view;
            m_watchData = watchData;
            m_watchHilite = watchHilite;
        }
        
        /**
         * @see PropertyChangeListener#propertyChange(PropertyChangeEvent)
         */
        public void propertyChange(final PropertyChangeEvent evt) {
            JComponent source = (JComponent)evt.getSource();
            boolean data = !m_watchData || m_view.hasData();
            boolean hilite = !m_watchHilite || m_view.hasHiLiteHandler();
            source.setEnabled(data && hilite);
        }
    }
}   // TableView
