/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
