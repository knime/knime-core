/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * 2006-06-08 (tm): reviewed 
 */
package org.knime.core.node.tableview;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.knime.core.data.DataTable;
import org.knime.core.data.def.DefaultTable;
import org.knime.core.node.property.hilite.HiLiteHandler;


/** 
 * Frame for a {@link org.knime.core.node.tableview.TableContentView}. It
 * simply puts a table view into a frame, adds a menu bar (by now with one item
 * "Close") and displays it.
 * <br />
 * <b>Note:</b>This class is obsolete as a table view pops up in an specific
 * frame in the flow. But this view is currently used inside the port to show
 * its data table. Might be extended.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 * @see org.knime.core.node.tableview.TableContentView
 * @see org.knime.core.node.tableview.TableContentModel
 */
public class TableViewFrame extends JFrame {
    private static final long serialVersionUID = -931131149550199849L;
    /** Scroll pane to be displayed. Holds the table in its view port */
    private final TableView m_scroller;
    
    /**
     * Opens new frame and displays the <code>TableContentView</code> in a 
     * scroll pane.
     * 
     * @param view the table content view to display.
     * @throws NullPointerException if <code>view</code> is <code>null</code>.
     */
    public TableViewFrame(final TableContentView view) {
        if (view == null) {
            throw new NullPointerException("View must not be null.");
        }
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem item = new JMenuItem("Close");
        // emulate same handling as done by JFrame
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                int defaultCloseOperation = getDefaultCloseOperation();
                switch (defaultCloseOperation) {
                    case WindowConstants.DO_NOTHING_ON_CLOSE: 
                        return;
                    case WindowConstants.HIDE_ON_CLOSE: 
                        TableViewFrame.this.setVisible(false);
                        return;
                    case WindowConstants.DISPOSE_ON_CLOSE:
                        TableViewFrame.this.setVisible(false);
                        TableViewFrame.this.dispose();
                        return;
                    case WindowConstants.EXIT_ON_CLOSE:
                        TableViewFrame.this.dispose();
                        System.exit(0);
                    default:
                        // do nothing
                }
            }
        });
        menu.add(item);
        menuBar.add(menu);
        setJMenuBar(menuBar);
        m_scroller = new TableView(view);
        
        // TableModelListener that prints the row and column count in the 
        // frame's title bar. It is updated as new rows are inserted (user
        // scrolls down)
        view.getContentModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(final TableModelEvent e) {
                // fired when new rows have been seen (refer to description
                // of caching strategy of the model)
                if (e.getType() == TableModelEvent.INSERT) {
                    updateTitle();
                }
            }
        });
        
        getJMenuBar().add(m_scroller.createHiLiteMenu());
        getJMenuBar().add(m_scroller.createNavigationMenu());
        getJMenuBar().add(m_scroller.createViewMenu());
        
        getContentPane().add(m_scroller);
        updateTitle();
        pack();
        setVisible(true);
        
    } // TableViewFrame(TableContentView)
    
    /** 
     * Creates a new frame by initializing a new {@link TableContentView}
     * and displays it.
     * 
     * @param table the table to display
     * @see TableContentView#TableContentView(DataTable)
     * @throws NullPointerException if <code>table</code> is <code>null</code>. 
     */ 
    public TableViewFrame(final DataTable table) {
        this(new TableContentView(table));
    }
    
    /** 
     * Get reference to underlying <code>TableView</code>.
     * 
     * @return the table view displayed.
     */
    public TableView getTableView() {
        return m_scroller;
    }
    
    /**
     * Delegating method to internal table view.
     * 
     * @return if data is available
     */
    public boolean hasData() {
        return m_scroller.hasData();
    }
    
    /**
     * Delegating method to internal table view.
     * 
     * @return if hilite handler has been set
     */
    public boolean hasHiLiteHandler() {
        return m_scroller.hasHiLiteHandler();
    }
    
    /**
     * Delegating method to internal table view.
     * 
     * @param data New data to be displayed
     */
    public void setDataTable(final DataTable data) {
        m_scroller.setDataTable(data);
    }

    /**
     * Delegating method to internal table view.
     * 
     * @param hiLiteHdl A new hilite handler.
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        m_scroller.setHiLiteHandler(hiLiteHdl);
    }

    /** 
     * Updates the title of the frame. It prints: 
     * "Table View (#rows[+] x #cols)". It is invoked each time new rows are
     * inserted (user scrolls down).
     */
    protected void updateTitle() {
        final TableContentView view = m_scroller.getContentTable();
        int rowCount = view.getRowCount();
        boolean isFinal = view.isRowCountFinal();
        StringBuilder title = new StringBuilder("Table View (");
        title.append(isFinal ? rowCount : rowCount - 1);
        title.append(isFinal ? " x " : "+ x ");
        title.append(view.getColumnCount() + ")");
        setTitle(title.toString());
    }
    

    /** Main method for testing purposes.
     * @param args Ignored.
     */
    public static void main(final String[] args) {
        Object[][] os = new Object[][] {
                new Object[] {new Integer(1), new Double(1.1), "eins"},
                new Object[] {new Integer(2), new Double(2.1), "zwei"},
                new Object[] {new Integer(3), new Double(3.1), "drei"}
        };
        DefaultTable table = new DefaultTable(os);
        new TableViewFrame(table);
    }
}   // TableViewFrame
