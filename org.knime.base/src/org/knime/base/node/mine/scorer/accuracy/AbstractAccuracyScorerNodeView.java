/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *
 * History
 *   May 30, 2016 (oole): created
 */
package org.knime.base.node.mine.scorer.accuracy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.knime.core.data.RowKey;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * This view displays the scoring results. It needs to be hooked up with a
 * scoring model.
 * 
 * It extends the {@link NodeView} and implements the {@link HiLiteListener} for accuracy scorers. 
 * The provided hiliting functionality is optional.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Ole Ostergaard
 * @param <M> the extended {@link NodeModel} for the accuracy scorer
 * @since 3.2
 */
public abstract class AbstractAccuracyScorerNodeView<M extends NodeModel> extends NodeView<M> implements HiLiteListener {

    /*
     * Components displaying the scorer table, number of correct/wrong
     * classified patterns, and the error percentage number.
     */
    private final JTable m_tableView;

    private JLabel m_correct;

    private JLabel m_wrong;

    private JLabel m_error;

    private JLabel m_accuracy;

    private final JLabel m_cohenKappa;

    private boolean[][] m_cellHilited;

    private boolean m_hiLite;

    /**
     * Creates a new ScorerNodeView displaying the table with the score.
     *
     * The view consists of the table with the example data and the appropriate
     * scoring in the upper part and the summary of correct and wrong classified
     * examples in the lower part.
     *
     * @param nodeModel
     *            the underlying <code>NodeModel</code>
     */
    public AbstractAccuracyScorerNodeView(final M nodeModel) {
        this(nodeModel, true);
    }
    /**
     * Creates a new ScorerNodeView displaying the table with the score.
     *
     * The view consists of the table with the example data and the appropriate
     * scoring in the upper part and the summary of correct and wrong classified
     * examples in the lower part.
     *
     * @param nodeModel
     *            the underlying <code>NodeModel</code>
     * @param hiLite whether hiliting is enabled
     */
    public AbstractAccuracyScorerNodeView(final M nodeModel, final boolean hiLite) {
        super(nodeModel);
        m_hiLite = hiLite;
        if (hiLite == true) {
            getJMenuBar().add(createHiLiteMenu());
        }

        m_tableView = new JTable();
        m_tableView.setRowSelectionAllowed(false);
        m_tableView.setCellSelectionEnabled(true);
        m_tableView.setDefaultRenderer(Object.class,
                new AttributiveCellRenderer());
        m_tableView.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JScrollPane scrollPane = new JScrollPane(m_tableView);

        JPanel summary = new JPanel(new GridLayout(3, 2));

        JPanel labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel("Correct classified:"));
        m_correct = new JLabel("n/a");
        labelPanel.add(m_correct);
        summary.add(labelPanel);

        labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel("Wrong classified:"));
        m_wrong = new JLabel("n/a");
        labelPanel.add(m_wrong);
        summary.add(labelPanel);

        labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel("Accuracy:"));
        m_accuracy = new JLabel("n/a");
        labelPanel.add(m_accuracy);
        labelPanel.add(new JLabel("%"));
        summary.add(labelPanel);

        labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel("Error:"));
        m_error = new JLabel("n/a");
        labelPanel.add(m_error);
        labelPanel.add(new JLabel("%"));
        summary.add(labelPanel);

        labelPanel = new JPanel(new FlowLayout());
        labelPanel.add(new JLabel("Cohen's kappa (\u03BA)"));
        m_cohenKappa = new JLabel("n/a");
        labelPanel.add(m_cohenKappa);
        summary.add(labelPanel);


        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(scrollPane, BorderLayout.CENTER);
        outerPanel.add(summary, BorderLayout.SOUTH);

        setComponent(outerPanel);
    }

    /**
     * Sets the all labels and arbitrary tooltips for the view.
     *
     * @param correctCount The String for the correct {@link JLabel}
     * @param wrongCount The String for the correct {@link JLabel}
     * @param error The String for the error {@link JLabel}
     * @param errorTooltip The String for the error {@link JLabel} tooltip, can be {@code null}
     * @param accuracy the String for the accuracy {@link JLabel}
     * @param accuracyTooltip the String for the accuracy {@link JLabel} tooltip, can be {@code null}
     * @param kappa the String for the kappa {@link JLabel}
     * @param kappaTooltip the String for the kappa {@link JLabel} tooltip can be {@code null}
     */
    protected void setLabels(final String correctCount, final String wrongCount, final String error,
        final String errorTooltip, final String accuracy, final String accuracyTooltip,
        final String kappa, final String kappaTooltip) {
        m_correct.setText(correctCount);
        m_wrong.setText(wrongCount);
        m_error.setText(error);
        m_error.setToolTipText(errorTooltip);
        m_accuracy.setText(accuracy);
        m_accuracy.setToolTipText(accuracyTooltip);
        m_cohenKappa.setText(kappa);
        m_cohenKappa.setToolTipText(kappaTooltip);

    }
    /**
     * Sets the all labels and for the accuracy, error and cohen's kappa it also sets deviated tooltips for the view.
     *
     * @param correctCount the count of correct predictions
     * @param wrongCount the count of wrong predictions
     * @param error the error
     * @param accuracy the accuracy
     * @param kappa cohen's kappa
     */
    protected void setLabels(final int correctCount, final int wrongCount, final double error, final double accuracy, final double kappa) {
        NumberFormat nf = NumberFormat.getInstance();
        setLabels(nf.format(correctCount), nf.format(wrongCount), nf.format(error), "Error: " + error + " %",
            nf.format(accuracy), "Accuracy: " + accuracy + " %", nf.format(kappa), "Cohen's \u03BA: " + kappa);

    }

    /**
     * Sets the {@link TableModel} for the {@link JTable}.
     *
     * @param tableModel the model for the table view should not be {@code null}
     */
    protected void setTableViewModel(final TableModel tableModel) {
        m_tableView.setModel(tableModel);
    }

    /**
     * @param cellHilited the table holding the hilited cells
     */
    protected void setCellHilited(final boolean[][] cellHilited) {
        m_cellHilited = cellHilited;
    }

     /**
     * Get a new menu to control hiliting for this view.
     *
     * @return a new JMenu with hiliting buttons
     */
    private JMenu createHiLiteMenu() {
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
        ArrayList<JMenuItem> result = new ArrayList<>();
        JMenuItem hsitem = new JMenuItem("Hilite Selected");
        hsitem.setMnemonic('S');
        hsitem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                hiliteSelected();
            }
        });
        result.add(hsitem);

        JMenuItem usitem = new JMenuItem("Unhilite Selected");
        usitem.setMnemonic('U');
        usitem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                unHiliteSelected();
            }
        });
        result.add(usitem);

        JMenuItem chitem = new JMenuItem("Clear Hilite");
        chitem.setMnemonic('C');
        chitem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                resetHilite();
            }
        });
        result.add(chitem);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        if (m_hiLite == true) {
            // remove hilitelistener
            getNodeModel().getInHiLiteHandler(0).removeHiLiteListener(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        if (m_hiLite == true) {
            // register the hilite handler
            getNodeModel().getInHiLiteHandler(0).addHiLiteListener(this);
        }
    }

    private Point[] getSelectedCells() {
        List<Point> cellVector = new ArrayList<>();

        int matrixLength = m_cellHilited.length;
        for (int i = 0; i < matrixLength; i++) {
            for (int j = 1; j <= matrixLength; j++) {
                if (m_tableView.isCellSelected(i, j)) {
                    cellVector.add(new Point(i, j - 1));
                }
            }
        }

        return cellVector.toArray(new Point[cellVector.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        ScorerViewData viewData = getScorerViewData();
        if (viewData == null) {
            // model is not executed yet, or was reset.
            // m_precision.setText(" n/a ");
            String na = " n/a ";
            setLabels(na, na, na, null, na, null, na, null);
            setTableViewModel(new DefaultTableModel());

            if (m_hiLite) {
                setCellHilited(new boolean[0][0]);
            }

            return;
        }

        String rowHeaderDescription = viewData.getFirstCompareColumn();
        String columnHeaderDescription = viewData.getSecondCompareColumn();

        if (m_hiLite) {
            // init the boolean array determining which cell is selected
            setCellHilited(new boolean[viewData.getScorerCount().length][viewData.getScorerCount().length]);
            updateHilitedCells();
        }

        ConfusionTableModel dataModel = new ConfusionTableModel(viewData.getScorerCount(),
            viewData.getTargetValues(), rowHeaderDescription, columnHeaderDescription);

        setTableViewModel(dataModel);

        double error = 100.0 * viewData.getError();
        double accuracy = 100.0 * viewData.getAccuracy();
        double cohenKappa = viewData.getCohenKappa();
        setLabels(viewData.getCorrectCount(), viewData.getFalseCount(),
            error , accuracy, cohenKappa);

    }

    /**
     * @return returns the {@link NodeModel}'s viewData, may return null.
     */
    abstract protected ScorerViewData getScorerViewData();

    private void hiliteSelected() {
        ScorerViewData viewData = getScorerViewData();
        if (viewData == null) {
            return;
        }

        Point[] selectedCells = getSelectedCells();

        // color the selected cells
        for (Point cell : selectedCells) {

            m_cellHilited[cell.x][cell.y] = true;
        }

        // get the row keys from the model and put them into the hilite handler
        if (getNodeModel().getInHiLiteHandler(0) != null) {
            getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(viewData.getSelectedSet(selectedCells));
        }

        // repaint the table
        m_tableView.repaint();
    }

    private void unHiliteSelected() {
        ScorerViewData viewData = getScorerViewData();
        if (viewData == null) {
            return;
        }

        Point[] selectedCells = getSelectedCells();

        // color the selected cells
        for (Point cell : selectedCells) {

            m_cellHilited[cell.x][cell.y] = false;
        }

        // get the row keys from the model and put them into the hilite handler
        if (getNodeModel().getInHiLiteHandler(0) != null) {
            getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(viewData.getSelectedSet(selectedCells));
        }

        m_tableView.repaint();
    }

    private void resetHilite() {

        // clear hilite background color
        clearHiliteBackgroundColor();

        // reset the hilite handler
        if (getNodeModel().getInHiLiteHandler(0) != null) {
            getNodeModel().getInHiLiteHandler(0).fireClearHiLiteEvent();
        }

    }

    private void clearHiliteBackgroundColor() {
        // reset the hilite marking
        for (int i = 0; i < m_cellHilited.length; i++) {
            for (int j = 0; j < m_cellHilited[i].length; j++) {
                m_cellHilited[i][j] = false;
            }
        }

        m_tableView.repaint();
    }

    private class AttributiveCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        /**
         * Creates a cell renderer for the hilite scorer view.
         *
         */
        public AttributiveCellRenderer() {
            setOpaque(true);
        }

        /**{@inheritDoc} */
        @Override
        public Component getTableCellRendererComponent(final JTable table,
                final Object value, final boolean isSelected,
                final boolean hasFocus, final int row, final int column) {

            // default background is white
            this.setBackground(Color.white);

            // first let the parent render this cell
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, row, column);

            // the first column is always grey and the text is black
            if (column == 0) {
                this.setBackground(Color.lightGray);
                this.setForeground(Color.black);
            }

            // if this cell is hilited, background color is yellow
            // the first column is never hilited
            if (column > 0) {
                if (m_cellHilited != null && m_cellHilited[row][column - 1]) {

                    if (isSelected) {
                        setBackground(ColorAttr.SELECTED_HILITE);
                        setForeground(Color.black);
                    } else {
                        setBackground(ColorAttr.HILITE);
                    }
                } else {
                    // not hilited
                    if (isSelected) {
                        setBackground(ColorAttr.SELECTED);
                    }
                }
            }

            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hiLite(final KeyEvent event) {

        updateHilitedCells();

        m_tableView.repaint();
    }

    /**
     *  Update the hilited cells
     */
    protected void updateHilitedCells() {
        // fix: don't update hilite when the model is not executed
        ScorerViewData viewData = getScorerViewData();
        if (viewData == null) {
            return;
        }

        if (getNodeModel().getInHiLiteHandler(0) != null) {
            Set<RowKey> hilitedKeys = getNodeModel().getInHiLiteHandler(0)
                    .getHiLitKeys();

            Point[] completeHilitedCells = viewData.getCompleteHilitedCells(hilitedKeys);

            // hilite all cells given by the points
            for (Point cell : completeHilitedCells) {
                m_cellHilited[cell.x][cell.y] = true;
            }
        }
    }

    /**
     * Checks for all hilit cells the model. If a key noted as unhilit in the
     * event occurs in a hilit cell the cell is unhilit (principle of
     * correctness!!)
     *
     * {@inheritDoc}
     */
    @Override
    public void unHiLite(final KeyEvent event) {
        ScorerViewData viewData = getScorerViewData();
        if (viewData == null) {
            return;
        }

        for (int i = 0; i < m_cellHilited.length; i++) {
            for (int j = 0; j < m_cellHilited[i].length; j++) {
                if (m_cellHilited[i][j] && viewData.containsConfusionMatrixKeys(i, j, event.keys())) {
                    m_cellHilited[i][j] = false;
                }
            }
        }

        m_tableView.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiLiteAll(final KeyEvent event) {
        clearHiliteBackgroundColor();
    }
}
