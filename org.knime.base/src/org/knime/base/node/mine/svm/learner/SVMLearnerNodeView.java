/*
 * ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   07.10.2007 (cebron): created
 */
package org.knime.base.node.mine.svm.learner;

import java.awt.Dimension;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeModel;

/**
 * The SVM view provides information about all SVM'S trained for
 * each class with their corresponding support vectors.
 *
 * @author cebron, University of Konstanz
 */
public class SVMLearnerNodeView extends GenericNodeView<SVMLearnerNodeModel> {

    /*
     * Output is printed in a JEditorPane
     */
    private JEditorPane m_output;

    /**
     * Constructor.
     * @param model the underlying {@link NodeModel}.
     */
    public SVMLearnerNodeView(final SVMLearnerNodeModel model) {
        super(model);
        m_output = new JEditorPane("text/html", "");
        m_output.setEditable(false);
        m_output.setPreferredSize(new Dimension(800, 600));
        JScrollPane scroller = new JScrollPane(m_output);
        setComponent(scroller);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        String text = (getNodeModel()).getSVMInfos();
        m_output.setText(text);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO Auto-generated method stub

    }

}
