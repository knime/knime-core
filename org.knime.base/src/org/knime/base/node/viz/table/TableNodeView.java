/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.viz.table;

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

import org.knime.base.node.io.csvwriter.CSVFilesHistoryPanel;
import org.knime.base.node.io.csvwriter.CSVWriter;
import org.knime.base.node.io.csvwriter.FileWriterSettings;
import org.knime.base.node.preproc.filter.row.RowFilterTable;
import org.knime.base.node.preproc.filter.row.rowfilter.EndOfTableException;
import org.knime.base.node.preproc.filter.row.rowfilter.IncludeFromNowOn;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeProgressMonitorView;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.tableview.TableContentModel;
import org.knime.core.node.tableview.TableContentModel.TableContentFilter;
import org.knime.core.node.tableview.TableContentView;
import org.knime.core.node.tableview.TableView;

/**
 * Table view on a {@link org.knime.core.data.DataTable}. It simply uses a
 * {@link javax.swing.JTable} to display the data table. If the node has not
 * been executed or is reset, the view will print "&lt;no data&gt;". The view
 * adds also a menu entry to the menu bar where the user can synchronize the
 * selection with a global hilite handler.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class TableNodeView extends NodeView {

    /** The Component displaying the table. */
    private final TableView m_tableView;

    /**
     * Starts a new <code>TableNodeView</code> displaying "&lt;no data&gt;".
     * The content comes up when the super class {@link NodeView} calls the
     * {@link #modelChanged()} method.
     *
     * @param nodeModel the underlying model
     */
    public TableNodeView(final TableNodeModel nodeModel) {
        super(nodeModel);
        // get data model, init view
        TableContentModel cntModel = nodeModel.getContentModel();
        assert (cntModel != null);
        m_tableView = new TableView(cntModel);
        m_tableView.setWrapColumnHeader(CorePlugin.getInstance().isWrapColumnHeaderInTableViews());
        m_tableView.setPreferredSizeDataDependent(true);
        cntModel.addTableModelListener(new TableModelListener() {
            @Override
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
     * (keeping the cache and so on) needs to have a {@link DataTable} to show.
     * This method returns <code>true</code> when the node was executed and
     * <code>false</code> otherwise.
     *
     * @return <code>true</code> if there is data to display
     */
    public boolean hasData() {
        return m_tableView.hasData();
    }

    /**
     * Checks is property handler is set.
     *
     * @return <code>true</code> if property handler set
     * @see TableContentView#hasHiLiteHandler()
     */
    public boolean hasHiLiteHandler() {
        return m_tableView.hasHiLiteHandler();
    }

    /**
     * Sets a new handler for this view.
     *
     * @param hiLiteHdl the new handler to set, may be <code>null</code> to
     *            disable any brushing
     */
    public void setHiLiteHandler(final HiLiteHandler hiLiteHdl) {
        m_tableView.setHiLiteHandler(hiLiteHdl);
    }

    /**
     * Shall row header encode the color information in an icon.
     *
     * @param isShowColor <code>true</code> for show icon (and thus the
     *            color), <code>false</code> ignore colors
     * @see org.knime.core.node.tableview.TableRowHeaderView
     *      #setShowColorInfo(boolean)
     */
    public void setShowColorInfo(final boolean isShowColor) {
        m_tableView.getHeaderTable().setShowColorInfo(isShowColor);
    } // setShowColorInfo(boolean)

    /**
     * Is the color info shown.
     *
     * @return <code>true</code> Icon with the color is present
     */
    public boolean isShowColorInfo() {
        return m_tableView.getHeaderTable().isShowColorInfo();
    } // isShowColorInfo()

    /**
     * Get row height from table.
     *
     * @return current row height
     * @see javax.swing.JTable#getRowHeight()
     */
    public int getRowHeight() {
        return m_tableView.getRowHeight();
    }

    /**
     * Set a new row height in the table.
     *
     * @param newHeight the new height
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
        StringBuffer title = new StringBuffer();
        if (model.hasData()) {
            String tableName = model.getTableName();
            if (!tableName.equals("default")) {
                title.append(" \"");
                title.append(tableName);
                title.append("\"");
            }
            title.append(" (");
            int rowCount = model.getRowCount();
            boolean isFinal = model.isRowCountFinal();
            title.append(rowCount);
            title.append(isFinal ? " x " : "+ x ");
            title.append(model.getColumnCount());
            title.append(")");
        } else {
            title.append(" <no data>");
        }
        super.setViewTitleSuffix(title.toString());
    }

    /**
     * Called from the super class when a property of the node has been changed.
     *
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        if (isOpen()) {
            countRowsInBackground();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // unregister from hilite handler
        m_tableView.cancelRowCountingInBackground();
    }

    /**
     * Does nothing since view is in sync anyway.
     *
     * @see org.knime.core.node.NodeView#onOpen()
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
                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {
                        ((JMenuItem)evt.getSource()).setEnabled(hasData());
                    }
                });
        final CSVFilesHistoryPanel hist = new CSVFilesHistoryPanel();
        item.addActionListener(new ActionListener() {
            @Override
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
     * @param file the file to write to
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
            @Override
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
         * @param file the file to write to
         * @param exec the execution monitor
         */
        public CSVWriterThread(final File file, final ExecutionMonitor exec) {
            m_file = file;
            m_exec = exec;
        }

        @Override
        public void run() {
            TableContentModel model = m_tableView.getContentModel();
            TableContentFilter filter = model.getTableContentFilter();
            DataTable table = model.getDataTable();
            HiLiteHandler hdl = model.getHiLiteHandler();
            Object mutex = filter.performsFiltering() ? hdl : new Object();
            // if hilighted rows are written only, we need to sync with
            // the handler (prevent others to (un-)hilight rows in the meantime)
            synchronized (mutex) {
                if (filter.performsFiltering()) {
                    DataTable hilightOnlyTable = new RowFilterTable(table,
                            new RowHiliteFilter(filter, hdl));
                    table = hilightOnlyTable;
                }
                try {
                    FileWriterSettings settings = new FileWriterSettings();
                    settings.setWriteColumnHeader(true);
                    settings.setWriteRowID(true);
                    settings.setColSeparator(",");
                    settings.setSeparatorReplacement("");
                    settings.setReplaceSeparatorInStrings(true);
                    settings.setMissValuePattern("");
                    CSVWriter writer = new CSVWriter(new FileWriter(m_file),
                            settings);
                    String message;
                    try {
                        writer.write(table, m_exec);
                        writer.close();
                        message = "Done.";
                    } catch (CanceledExecutionException ce) {
                        writer.close();
                        m_file.delete();
                        message = "Canceled.";
                    }
                    JOptionPane.showMessageDialog(m_tableView,
                            message, "Write CSV",
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ioe) {
                    JOptionPane.showMessageDialog(m_tableView,
                            ioe.getMessage(), "Write error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Row filter that filters non-hilited rows - it's the most convenient way
     * to write only the hilited rows.
     *
     * @author Bernd Wiswedel, University of Konstanz
     */
    private static final class RowHiliteFilter extends RowFilter {

        private final HiLiteHandler m_handler;
        private final TableContentFilter m_filter;

        /**
         * Creates new instance given a hilite handler.
         * @param filter table content filter
         * @param handler the handler to get the hilite info from
         */
        public RowHiliteFilter(final TableContentFilter filter,
                final HiLiteHandler handler) {
            m_handler = handler;
            m_filter = filter;
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
            return m_filter.matches(m_handler.isHiLit(row.getKey()));
        }
    }
}
