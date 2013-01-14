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
