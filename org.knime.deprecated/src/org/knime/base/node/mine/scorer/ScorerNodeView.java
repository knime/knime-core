/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
package org.knime.base.node.mine.scorer;

import java.awt.FlowLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTable;
import org.knime.core.node.NodeView;
import org.knime.core.node.tableview.TableView;


/**
 * This view displays the scoring results. It needs to be hooked up with a
 * scoring model.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
final class ScorerNodeView extends NodeView {
    /*
     * Components displaying the scorer table, number of correct/wrong
     * classified patterns, and the error percentage number.
     */
    private TableView m_tableView;

    private JLabel m_correct;

    private JLabel m_wrong;

    private JLabel m_error;

    /**
     * Creates a new ScorerNodeView displaying the table with the score.
     * 
     * The view consists of the table with the example data and the appropriate
     * scoring in the upper part and the summary of correct and wrong classified
     * examples in the lower part.
     * 
     * @param nodeModel the underlying <code>NodeModel</code>
     */
    public ScorerNodeView(final ScorerNodeModel nodeModel) {
        super(nodeModel);

        m_tableView = new TableView();
        m_tableView.setShowColorInfo(false);

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
        outerPanel.add(m_tableView);
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
        ScorerNodeModel model = (ScorerNodeModel)getNodeModel();
        /*
         * get the new scorer table and compute the numbers we display
         */
        DataTable scoreTable = model.getScorerTable();
        if (scoreTable == null) {
            // model is not executed yet, or was reset.
            m_tableView.setDataTable(null);
            m_correct.setText(" n/a ");
            m_wrong.setText(" n/a ");
            m_error.setText(" n/a ");
            return;
        }

        // now set the values in the components to get them displayed
        m_tableView.setDataTable(scoreTable);
        m_correct.setText(String.valueOf(model.getCorrectCount()));
        m_wrong.setText(String.valueOf(model.getFalseCount()));
        m_error.setText(String.valueOf(model.getError()));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // release the table, so it can be killed.
        m_tableView.setDataTable(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing here
    }
}
