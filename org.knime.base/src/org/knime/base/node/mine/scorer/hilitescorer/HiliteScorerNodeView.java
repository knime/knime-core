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
 */
package org.knime.base.node.mine.scorer.hilitescorer;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.knime.core.data.DataTable;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.node.NodeView;
import org.knime.core.node.property.hilite.HiLiteListener;
import org.knime.core.node.property.hilite.KeyEvent;

/**
 * This view displays the scoring results. It needs to be hooked up with a
 * scoring model.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
final class HiliteScorerNodeView extends NodeView implements HiLiteListener {
    /*
     * Components displaying the scorer table, number of correct/wrong
     * classified patterns, and the error percentage number.
     */
    private JTable m_tableView;

    private JScrollPane m_scrollPane;

    private JLabel m_correct;

    private JLabel m_wrong;

    private JLabel m_error;

    private boolean[][] m_cellHilited;

    /**
     * Creates a new ScorerNodeView displaying the table with the score.
     * 
     * The view consists of the table with the example data and the appropriate
     * scoring in the upper part and the summary of correct and wrong classified
     * examples in the lower part.
     * 
     * @param nodeModel the underlying <code>NodeModel</code>
     */
    public HiliteScorerNodeView(final HiliteScorerNodeModel nodeModel) {
        super(nodeModel);

        getJMenuBar().add(createHiLiteMenu());

        m_tableView = new JTable();
        m_tableView.setRowSelectionAllowed(false);
        m_tableView.setCellSelectionEnabled(true);
        m_tableView.setDefaultRenderer(Object.class,
                new AttributiveCellRenderer());
        m_scrollPane = new JScrollPane(m_tableView);

        JPanel summary1 = new JPanel();
        summary1.setLayout(new FlowLayout());
        summary1.add(new JLabel("Correct classified:"));
        m_correct = new JLabel("n/a");
        summary1.add(m_correct);

        JPanel summary2 = new JPanel();
        summary2.setLayout(new FlowLayout());
        summary2.add(new JLabel("Wrong classified:"));
        m_wrong = new JLabel("n/a");
        summary2.add(m_wrong);

        JPanel summary3 = new JPanel();
        summary3.setLayout(new FlowLayout());
        summary3.add(new JLabel("Error:"));
        m_error = new JLabel("n/a");
        summary3.add(m_error);
        summary3.add(new JLabel("%"));

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.add(m_scrollPane);
        outerPanel.add(summary1);
        outerPanel.add(summary2);
        outerPanel.add(summary3);
        setComponent(outerPanel);

    }

    /**
     * Call this function to tell the view that the model has changed.
     * 
     * @see NodeView#modelChanged()
     */
    @Override
    public void modelChanged() {
        HiliteScorerNodeModel model = (HiliteScorerNodeModel)getNodeModel();

        /*
         * get the new scorer table and compute the numbers we display
         */
        DataTable scoreTable = model.getScorerTable();
        if (scoreTable == null) {
            // model is not executed yet, or was reset.
            m_tableView = null;
            m_correct.setText(" n/a ");
            m_wrong.setText(" n/a ");
            m_error.setText(" n/a ");
            getNodeModel().getInHiLiteHandler(0).removeHiLiteListener(this);
            return;
        }

        // now set the values in the components to get them displayed
        int[][] scoreCount = ((HiliteScorerNodeModel)getNodeModel())
                .getScorerCount();
        String[] headerNames = ((HiliteScorerNodeModel)getNodeModel())
                .getValues();
        TableModel dataModel = new ConfustionTableModel(scoreCount, headerNames);

        if (m_tableView == null) {
            m_tableView = new JTable();
        }
        m_tableView.setModel(dataModel);

        m_correct.setText(String.valueOf(model.getCorrectCount()));
        m_wrong.setText(String.valueOf(model.getFalseCount()));
        m_error.setText(String.valueOf(model.getError()));

        // init the boolean array determining which cell is selected
        m_cellHilited = new boolean[scoreCount.length][scoreCount.length];

        // register the hilite handler
        getNodeModel().getInHiLiteHandler(0).addHiLiteListener(this);
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {
        // release the table, so it can be killed.
        m_tableView = null;

        // remove hilitelistener
        getNodeModel().getInHiLiteHandler(0).removeHiLiteListener(this);
    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
        // do nothing here
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
        ArrayList<JMenuItem> result = new ArrayList<JMenuItem>();
        JMenuItem hsitem = new JMenuItem("Hilite Selected");
        hsitem.setMnemonic('S');
        hsitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                hiliteSelected();
            }
        });
        // hsitem.setEnabled(hasData());
        result.add(hsitem);

        JMenuItem usitem = new JMenuItem("Unhilite Selected");
        usitem.setMnemonic('U');
        usitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                unHiliteSelected();
            }
        });
        // usitem.setEnabled(hasData());
        result.add(usitem);

        JMenuItem chitem = new JMenuItem("Clear Hilite");
        chitem.setMnemonic('C');
        chitem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                resetHilite();
            }
        });
        // chitem.setEnabled(hasData());
        result.add(chitem);

        return result;
    }

    private Point[] getSelectedCells() {
        Vector<Point> cellVector = new Vector<Point>();

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

    private void hiliteSelected() {

        Point[] selectedCells = getSelectedCells();

        // color the selected cells
        for (Point cell : selectedCells) {

            m_cellHilited[cell.x][cell.y] = true;
        }

        // get the row keys from the model and put them into the hilite handler
        if (getNodeModel().getInHiLiteHandler(0) != null) {
            getNodeModel().getInHiLiteHandler(0).fireHiLiteEvent(
                    ((HiliteScorerNodeModel)getNodeModel())
                            .getSelectedSet(selectedCells));
        }

        // repaint the table
        m_tableView.repaint();
    }

    private void unHiliteSelected() {

        Point[] selectedCells = getSelectedCells();

        // color the selected cells
        for (Point cell : selectedCells) {

            m_cellHilited[cell.x][cell.y] = false;
        }

        // get the row keys from the model and put them into the hilite handler
        if (getNodeModel().getInHiLiteHandler(0) != null) {
            getNodeModel().getInHiLiteHandler(0).fireUnHiLiteEvent(
                    ((HiliteScorerNodeModel)getNodeModel())
                            .getSelectedSet(selectedCells));
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

        /**
         * Creates a cell renderer for the hilite scorer view.
         * 
         */
        public AttributiveCellRenderer() {
            setOpaque(true);
        }

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
                if (m_cellHilited[row][column - 1]) {

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
     * @see org.knime.core.node.property.hilite.HiLiteListener
     *  #hiLite(org.knime.core.node.property.hilite.KeyEvent)
     */
    public void hiLite(final KeyEvent event) {

        Point[] completeHilitedCells = ((HiliteScorerNodeModel)getNodeModel())
                .getCompleteHilitedCells(event.keys());

        // hilite all cells given by the points
        for (Point cell : completeHilitedCells) {
            m_cellHilited[cell.x][cell.y] = true;
        }

        m_tableView.repaint();
    }

    /**
     * Checks for all hilited cells the model. If a key noted as unhilited in
     * the event occures in a hilited cell the cell is unhilted (principle of
     * correctness!!)
     * 
     * @see org.knime.core.node.property.hilite.HiLiteListener
     *  #unHiLite(org.knime.core.node.property.hilite.KeyEvent)
     */
    public void unHiLite(final KeyEvent event) {

        for (int i = 0; i < m_cellHilited.length; i++) {
            for (int j = 0; j < m_cellHilited[i].length; j++) {
                if (m_cellHilited[i][j]) {
                    if (((HiliteScorerNodeModel)getNodeModel())
                            .containsConfusionMatrixKeys(i, j, event.keys())) {
                        m_cellHilited[i][j] = false;
                    }
                }
            }
        }

        m_tableView.repaint();
    }

    /**
     * @see org.knime.core.node.property.hilite.HiLiteListener#unHiLiteAll()
     * 
     */
    public void unHiLiteAll() {
        clearHiliteBackgroundColor();
    }

}
