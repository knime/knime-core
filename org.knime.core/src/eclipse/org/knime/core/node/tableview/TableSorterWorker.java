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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 31, 2011 (wiswedel): created
 */
package org.knime.core.node.tableview;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.sort.DataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DefaultNodeProgressMonitor;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.util.ViewUtils;
import org.knime.core.node.workflow.NodeProgress;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;

/**
 * SwingWorker that is used to sort the table content on mouse click in header.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
final class TableSorterWorker extends SwingWorker<DataTable, NodeProgress> {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(TableSorterWorker.class);

    /** Table to sort. */
    private final DataTable m_inputTable;

    /** Content model (for callback). */
    private final TableContentModel m_cntModel;

    /** Panel to block while the sorting is ongoing (progress dialog is
     * modal to this component). */
    private final JComponent m_parentComponent;

    /** Progress dialog. */
    private final TableSorterProgressBar m_progBar;

    /** The sort order, set into the table content after the sorting
     * has finished. */
    private final TableSortOrder m_sortOrder;

    /** Progress monitor that is passed to the sort routines. A listener on
     * this monitor updates the progress bar. */
    private DefaultNodeProgressMonitor m_nodeProgressMonitor;


    /** Initialize new sorter, does not start sorting yet.
     * @param table Table to sort.
     * @param order The sort order
     * @param parentComponent The parent component to block using a modal
     *        progress bar
     * @param cntModel The callback model. */
    TableSorterWorker(final DataTable table, final TableSortOrder order,
            final JComponent parentComponent,
            final TableContentModel cntModel) {
        m_inputTable = table;
        m_parentComponent = parentComponent;
        m_cntModel = cntModel;
        m_sortOrder = order;
        m_progBar = new TableSorterProgressBar();
    }

    /** Starts the sorting by calling {@link #execute()} and popping up the
     * progress bar.
     */
    final void executeAndShowProgress() {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                execute();
                m_progBar.setVisible(true);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected DataTable doInBackground() throws Exception {
        int rowCount; // passed to table sorter for progress
        if (m_inputTable instanceof BufferedDataTable) {
            rowCount = ((BufferedDataTable)m_inputTable).getRowCount();
        } else if (m_inputTable instanceof ContainerTable) {
            rowCount = ((ContainerTable)m_inputTable).getRowCount();
        } else {
            rowCount = -1; // unknown, no progress
        }
        publish(new NodeProgress(0.0, "Starting table sort..."));
        Collection<String> sortColNames = new ArrayList<String>(2);
        DataTableSpec spec = m_inputTable.getDataTableSpec();
        for (int i : m_sortOrder.getSortColumnIndices()) {
            String name;
            if (i < 0) { // row id
                name = DataTableSorter.ROWKEY_SORT_SPEC.getName();
            } else {
                name = spec.getColumnSpec(i).getName();
            }
            sortColNames.add(name);
        }
        long start = System.currentTimeMillis();
        LOGGER.debug("Starting interactive table sorting on column(s) "
                + sortColNames);
        boolean[] sortOrders = m_sortOrder.getSortColumnOrder();
        // it DOES NOT respect blobs -- they will be copied (expensive)
        DataTableSorter sorter =
                new DataTableSorter(m_inputTable, rowCount, sortColNames,
                        sortOrders, false);
        NodeProgressListener progLis = new NodeProgressListener() {
            @Override
            public void progressChanged(final NodeProgressEvent pe) {
                publish(pe.getNodeProgress());
            }
        };
        m_nodeProgressMonitor = new DefaultNodeProgressMonitor();
        ExecutionMonitor exec = new ExecutionMonitor(m_nodeProgressMonitor);
        m_nodeProgressMonitor.addProgressListener(progLis);
        try {
            DataTable result = sorter.sort(exec);
            long elapsedMS = System.currentTimeMillis() - start;
            String time = StringFormat.formatElapsedTime(elapsedMS);
            LOGGER.debug("Interactive table sorting finished (" + time + ")");
            return result;
        } finally {
            m_nodeProgressMonitor.removeProgressListener(progLis);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void process(final List<NodeProgress> chunks) {
        // only display the latest progress update
        if (chunks.size() > 0) {
            NodeProgress nodeProgress = chunks.get(chunks.size() - 1);
            if (nodeProgress.hasProgress()) {
                m_progBar.setProgress(nodeProgress.getProgress());
            }
            if (nodeProgress.hasMessage()) {
                m_progBar.setMessage(nodeProgress.getMessage());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void done() {
        m_progBar.dispose();
        if (isCancelled()) {
            return;
        }
        DataTable sortedTable;
        try {
            sortedTable = get();
        } catch (InterruptedException e) {
            LOGGER.debug("Interactive table sorting cancelled");
            return;
            // cancelled, ignore
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CanceledExecutionException) {
                LOGGER.debug("Interactive table sorting cancelled");
                return;
            }
            if (cause == null) {
                cause = e;
            }
            String error = "Error sorting table: " + cause.getMessage();
            LOGGER.error(error, e);
            JOptionPane.showMessageDialog(m_parentComponent.getRootPane(), error
                    + "\nSee log files for details", "Sort error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        m_cntModel.setDataTableOnSort(sortedTable, m_sortOrder);
    }

    /** Custom progress bar dialog. Ideally we would have used a Swing
     * ProgressMonitor but that one isn't modal. */
    @SuppressWarnings("serial")
    private final class TableSorterProgressBar extends JDialog {

        private final JLabel m_progLabel;

        private final JProgressBar m_progBarInDialog;

        TableSorterProgressBar() {
            super(SwingUtilities.windowForComponent(
                    m_parentComponent), ModalityType.DOCUMENT_MODAL);
            setTitle("Sorting table...");
            setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            JPanel labelPanel = new JPanel(new GridLayout(0, 1));
            char[] empty = new char[80];
            Arrays.fill(empty, ' ');
            labelPanel.add(new JLabel(new String(empty)));
            m_progLabel = new JLabel("Sorting table...");
            labelPanel.add(m_progLabel);
            labelPanel.add(new JLabel(new String(empty)));
            m_progBarInDialog = new JProgressBar(0, 100);
            m_progBarInDialog.setStringPainted(true);
            Container contentPane = getContentPane();
            contentPane.setLayout(new BorderLayout());
            contentPane.add(labelPanel, BorderLayout.NORTH);
            contentPane.add(ViewUtils.getInFlowLayout(m_progBarInDialog),
                    BorderLayout.CENTER);
            JButton button = new JButton("Cancel");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    onSortCancel();
                }

            });
            contentPane.add(ViewUtils.getInFlowLayout(button),
                    BorderLayout.SOUTH);
            button.addKeyListener(new KeyAdapter() {

                /** {@inheritDoc} */
                @Override
                public void keyPressed(final KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        onSortCancel();
                    }
                }
            });
            button.requestFocus();
            pack();
            setLocationRelativeTo(m_parentComponent.getRootPane());
        }

        void setProgress(final double value) {
            m_progBarInDialog.setValue((int)(100 * value));
        }

        void setMessage(final String message) {
            m_progLabel.setText(" " + message);
            m_progLabel.setToolTipText(message);
        }

        private void onSortCancel() {
            if (m_nodeProgressMonitor != null) {
                m_nodeProgressMonitor.setExecuteCanceled();
            }
        }
    }

}
