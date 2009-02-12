/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * -------------------------------------------------------------------
 *
 */

package org.knime.base.node.mine.bayes.naivebayes.learner;

import org.knime.core.node.NodeView;

import org.knime.base.node.mine.bayes.naivebayes.datamodel.NaiveBayesModel;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;


/**
 * <code>NodeView</code> for the "Naive Bayes Learner" Node.
 *
 * @author Tobias Koetter
 */
public class NaiveBayesLearnerNodeView
extends NodeView<NaiveBayesLearnerNodeModel> {


    private NaiveBayesModel m_model;
    private final JEditorPane m_htmlPane;

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (
     * class: <code>NaiveBayesLearnerNodeModel</code>)
     */
    protected NaiveBayesLearnerNodeView(
            final NaiveBayesLearnerNodeModel nodeModel) {
        super(nodeModel);
        m_model = (nodeModel).getNaiveBayesModel();
        //The output as HTML
        m_htmlPane = new JEditorPane("text/html", "");
//        m_htmlPane.setText(m_model.getHTMLTable());
        m_htmlPane.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(m_htmlPane);
/*
        //The output as a JTABLE
        final String[] captions = m_model.getDataTableCaptions();
        final String[][] dataTable = m_model.getDataTable();
        JTable table = new JTable(dataTable, captions);
        JScrollPane scrollPane = new JScrollPane(table);
        table.setPreferredScrollableViewportSize(new Dimension(540, 400));
        */
        setComponent(scrollPane);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        final NaiveBayesLearnerNodeModel nodeModel = getNodeModel();
        m_model = nodeModel.getNaiveBayesModel();
        if (m_model != null) {
            m_htmlPane.setText(m_model.getHTMLView());
        } else {
            m_htmlPane.setText("No model available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        //nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
//        nothing to do
    }
}
