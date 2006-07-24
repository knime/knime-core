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
 */
package de.unikn.knime.base.node.view.table;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import de.unikn.knime.base.node.filter.row.RowFilterTable;
import de.unikn.knime.base.node.filter.row.rowfilter.EndOfTableException;
import de.unikn.knime.base.node.filter.row.rowfilter.IncludeFromNowOn;
import de.unikn.knime.base.node.filter.row.rowfilter.RowFilter;
import de.unikn.knime.base.node.io.csvwriter.CSVFilesHistoryPanel;
import de.unikn.knime.base.node.io.csvwriter.CSVWriter;
import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.CanceledExecutionException;
import de.unikn.knime.core.node.DefaultNodeProgressMonitor;
import de.unikn.knime.core.node.ExecutionMonitor;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeProgressMonitorView;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;
import de.unikn.knime.core.node.NodeView;
import de.unikn.knime.core.node.property.hilite.HiLiteHandler;
import de.unikn.knime.core.node.tableview.TableContentModel;
import de.unikn.knime.core.node.tableview.TableContentView;
import de.unikn.knime.core.node.tableview.TableView;

/**
 * Table View on a <code>DataTable</code>. It simply uses a
 * <code>JTable</code> to display a <code>DataTable</code>. If the node has
 * not been executed or is reset, the view will print "&lt;no data&gt;". The
 * view adds also a menu entry to the menu bar where the user can synchronize
 * the selection with a global hilite handler.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableNodeView extends NodeView {

    /** The Component displaying the table. */
    private final TableView m_tableView;

    /**
     * Starts a new <code>TableNodeView</code> displaying "&lt;no data&gt;".
     * The content comes up when the super class <code>NodeView</code> calls
     * the <code>modelChanged(Object)</code> method.
     * 
     * @param nodeModel The underlying model.
     */
    public TableNodeView(final TableNodeModel nodeModel) {
        super(nodeModel);
        // get data model, init view
        TableContentModel cntModel = nodeModel.getContentModel();
        assert (cntModel != null);
        m_tableView = new TableView(cntModel);
        cntModel.addTableModelListener(new TableModelListener() {
            public void tableChanged(final TableModelEvent e) {
               // fired when new rows have been seen (refer to description 
               // of caching strategy of the model)
               updateTitle();
            }
        });
        getJMenuBar().add(m_tableView.createHiLiteMenu());
        getJMenuBar().add(m_tableView.createNavigationMenu());
        getJMenuBar().add(m_tableView.createViewMenu());
        getJMenuBar().add(createWriteCSVMenu());
        setHiLiteHandler(getNodeModel().getInHiLiteHandler(
                TableNodeModel.INPORT));
        setComponent(m_tableView);
    } // TableNodeView(TableNodeModel)

    /**
     * Checks if there is data to display. That is: The model's content model
     * (keeping the cache and so on) needs to have a <code>DataTable</code> to
     * show. This method returns <code>true</code> when the node was executed
     * and <code>false</code> otherwise.
     * 
     * @return <code>true</code> if there is data to display.
     */
    public boolean hasData() {
        return m_tableView.hasData();
    }

    /**
     * Checks is property handler is set.
     * 
     * @return <code>true</code> If property handler set.
     * @see TableContentView#hasHiLiteHandler()
     */
    public boolean hasHiLiteHandler() {
        return m_tableView.hasHiLiteHandler();
    }

    /**
     * Sets a new handler for this view.
     * 
     * @param hiLiteHdl New handler to set, may be <code>null</code> to
     *            disable any brushing.
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        m_tableView.setHiLiteHandler(hiLiteHdl);
    }

    /**
     * Control behaviour to show only hilited rows.
     * 
     * @param showOnlyHilit <code>true</code> Filter and display only rows
     *            whose hiLite status is set.
     * @see TableContentModel#showHiLitedOnly(boolean)
     */
    public final void showHiLitedOnly(final boolean showOnlyHilit) {
        m_tableView.showHiLitedOnly(showOnlyHilit);
    }

    /**
     * Get status of filtering for hilited rows.
     * 
     * @return <code>true</code> only hilited rows are shown,
     *         <code>false</code> all rows are shown.
     * @see TableContentModel#showsHiLitedOnly()
     */
    public boolean showsHiLitedOnly() {
        return m_tableView.showsHiLitedOnly();
    }

    /**
     * Shall row header encode the color information in an icon.
     * 
     * @param isShowColor <code>true</code> for show icon (and thus the
     *            color), <code>false</code> ignore colors.
     * @see de.unikn.knime.core.node.tableview.TableRowHeaderView
     *      #setShowColorInfo(boolean)
     */
    public void setShowColorInfo(final boolean isShowColor) {
        m_tableView.getHeaderTable().setShowColorInfo(isShowColor);
    } // setShowColorInfo(boolean)

    /**
     * Is the color info shown.
     * 
     * @return <code>true</code> Icon with the color is present.
     */
    public boolean isShowColorInfo() {
        return m_tableView.getHeaderTable().isShowColorInfo();
    } // isShowColorInfo()

    /**
     * Get row height from table.
     * 
     * @return Current row height
     * @see javax.swing.JTable#getRowHeight()
     */
    public int getRowHeight() {
        return m_tableView.getRowHeight();
    }

    /**
     * Set a new row height in the table.
     * 
     * @param newHeight The new height.
     * @see javax.swing.JTable#setRowHeight(int)
     */
    public void setRowHeight(final int newHeight) {
        m_tableView.setRowHeight(newHeight);
    }

    /**
     * Hilites selected rows in the hilite handler.
     * 
     * @see TableView#hiliteSelected()
     */
    public void hiliteSelected() {
        m_tableView.hiliteSelected();
    }

    /**
     * Unhilites selected rows in the hilite handler.
     * 
     * @see TableView#unHiliteSelected()
     */
    public void unHiliteSelected() {
        m_tableView.unHiliteSelected();
    }

    /**
     * Resets hiliting in the hilite handler.
     * 
     * @see TableView#resetHilite()
     */
    public void resetHilite() {
        m_tableView.resetHilite();
    }

    /**
     * Updates the title of the frame. It prints: "Table (#rows[+] x #cols)". It
     * is invoked each time new rows are inserted (user scrolls down).
     */
    protected void updateTitle() {
        final TableContentView view = m_tableView.getContentTable();
        TableContentModel model = view.getContentModel();
        StringBuffer title = new StringBuffer(getViewName());
        if (model.hasData()) {
            String tableName = model.getTableName();
            title.append(" \"");
            title.append(tableName);
            title.append("\" (");
            int rowCount = model.getRowCount();
            boolean isFinal = model.isRowCountFinal();
            title.append(rowCount);
            title.append(isFinal ? " x " : "+ x ");
            title.append(model.getColumnCount());
            title.append(")");
        } else {
            title.append(" <no data>");
        }
        super.setViewTitle(title.toString());
    } // updateTitle()

    /**
     * Called from the super class when a property of the node has been changed.
     * 
     * @see de.unikn.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        if (isOpen()) {
            countRowsInBackground();
        }
    }

    /**
     * @see de.unikn.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // unregister from hilite handler
        m_tableView.cancelRowCountingInBackground();
    }

    /**
     * Does nothing since view is in sync anyway.
     * 
     * @see de.unikn.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        countRowsInBackground();
        updateTitle();
    }

    /**
     * Delegates to the table view that it should start a row counter thread.
     * Multiple invocations of this method don't harm.
     */
    private void countRowsInBackground() {
        if (hasData()) {
            m_tableView.countRowsInBackground();
        }
    }

    /* A JMenu that has one entry "Write to CSV file". */
    private JMenu createWriteCSVMenu() {
        JMenu menu = new JMenu("Output");
        JMenuItem item = new JMenuItem("Write CSV");
        item.addPropertyChangeListener("ancestor",
                new PropertyChangeListener() {
                    public void propertyChange(final PropertyChangeEvent evt) {
                        ((JMenuItem)evt.getSource()).setEnabled(hasData());
                    }
                });
        final CSVFilesHistoryPanel hist = new CSVFilesHistoryPanel();
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                int i = JOptionPane.showConfirmDialog(m_tableView, hist,
                        "Choose File", JOptionPane.OK_CANCEL_OPTION);
                if (i == JOptionPane.OK_OPTION) {
                    String sfile = hist.getSelectedFile();
                    File file = CSVFilesHistoryPanel.getFile(sfile);
                    writeToCSV(file);
                }
            }
        });
        menu.add(item);
        return menu;
    }

    /**
     * Called by the JMenu item "Write to CVS", it write the table as shown in
     * table view to a CSV file.
     * 
     * @param file The file to write to.
     */
    private void writeToCSV(final File file) {
        // CSV Writer supports ExecutionMonitor. Some table may be big.
        DefaultNodeProgressMonitor progMon = new DefaultNodeProgressMonitor();
        ExecutionMonitor e = new ExecutionMonitor(progMon);
        // Frame of m_tableView (if any)
        Frame f = (Frame)SwingUtilities.getAncestorOfClass(Frame.class,
                m_tableView);
        final NodeProgressMonitorView progView = new NodeProgressMonitorView(f,
                progMon);
        // CSV Writer does not support 1-100 progress (unknown row count)
        progView.setShowProgress(false);
        // Writing is done in a thread (allows repainting of GUI)
        final CSVWriterThread t = new CSVWriterThread(file, e);
        t.start();
        // A thread that waits for t to finish and then disposes the prog view
        new Thread(new Runnable() {
            public void run() {
                try {
                    t.join();
                } catch (InterruptedException ie) {
                    // do nothing. Only dispose the view
                } finally {
                    progView.dispose();
                }
            }
        }).start();
        progView.pack();
        progView.setLocationRelativeTo(m_tableView);
        progView.setVisible(true);
    }

    /** Thread that write the current table to a file. */
    private final class CSVWriterThread extends Thread {

        private final File m_file;

        private final ExecutionMonitor m_exec;

        /**
         * Creates instance.
         * 
         * @param file The file to write to
         * @param exec The execution monitor.
         */
        public CSVWriterThread(final File file, final ExecutionMonitor exec) {
            m_file = file;
            m_exec = exec;
        }

        @Override
        public void run() {
            DataTable table = m_tableView.getContentModel().getDataTable();
            boolean writeHilightedOnly = m_tableView.getContentModel()
                    .showsHiLitedOnly();
            HiLiteHandler hdl = m_tableView.getContentModel()
                    .getHiLiteHandler();
            Object mutex = writeHilightedOnly ? hdl : new Object();
            // if hilighted rows are written only, we need to sync with
            // the handler (prevent others to (un-)hilight rows in the meantime)
            synchronized (mutex) {
                if (writeHilightedOnly) {
                    DataTable hilightOnlyTable = new RowFilterTable(table,
                            new HilightOnlyRowFilter(hdl));
                    table = hilightOnlyTable;
                }
                try {
                    CSVWriter writer = new CSVWriter(new FileWriter(m_file));
                    writer.setWriteColHeader(true);
                    writer.setWriteRowHeader(true);
                    writer.setSepChar(';', true);
                    writer.setMissing("");
                    try {
                        writer.write(table, m_exec);
                        writer.close();
                    } catch (CanceledExecutionException ce) {
                        writer.close();
                        m_file.delete();
                    }
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(m_tableView,
                            ioe.getMessage(), "Write error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * RowFilter that filters non-hilited rows - it's the most convenient way to
     * write only the hilited rows.
     * 
     * @author wiswedel, University of Konstanz
     */
    private static final class HilightOnlyRowFilter extends RowFilter {

        private final HiLiteHandler m_handler;

        /**
         * Creates new instance given a hilight handler.
         * 
         * @param handler The handler to get the hilite info from.
         */
        public HilightOnlyRowFilter(final HiLiteHandler handler) {
            m_handler = handler;
        }

        @Override
        public DataTableSpec configure(final DataTableSpec inSpec)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public void loadSettingsFrom(final NodeSettingsRO cfg)
                throws InvalidSettingsException {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        protected void saveSettings(final NodeSettingsWO cfg) {
            throw new IllegalStateException("Not intended for permanent usage");
        }

        @Override
        public boolean matches(final DataRow row, final int rowIndex)
                throws EndOfTableException, IncludeFromNowOn {
            return m_handler.isHiLit(row.getKey().getId());
        }
    }
} // TableNodeView
