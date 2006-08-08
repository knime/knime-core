/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.03.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeView;


/**
 * View to display basisfunction rule models.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class BasisFunctionLearnerNodeView extends NodeView {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BasisFunctionLearnerNodeView.class);

    /** Here the model is displayed. */
    private final JEditorPane m_content;

    /**
     * Create an empty bf view.
     * 
     * @param model the bf model
     */
    public BasisFunctionLearnerNodeView(
            final BasisFunctionLearnerNodeModel model) {
        super(model);
        m_content = new JEditorPane("text/html", "");
        m_content.setEditable(false);
        JScrollPane scroller = new JScrollPane(m_content);
        setComponent(scroller);
    }

    /**
     * Called when the model changed.
     * 
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        BasisFunctionLearnerNodeModel model = 
            (BasisFunctionLearnerNodeModel)super.getNodeModel();
        ModelContentRO pp = model.getModelInfo();
        if (pp == null) {
            m_content.setText("");
        } else {
            try {
                StringBuffer buf = new StringBuffer();
                buf.append("<html>\n");
                buf.append("<body>\n");
                buf.append("<h2>Learner Statistics</h2>");
                buf.append("<ul>");
                ModelContentRO statisticsContent = 
                    pp.getModelContent("learner_info");
                for (String key : statisticsContent.keySet()) {
                     buf.append("<li>");
                     buf.append(key + statisticsContent.getString(key) + "\n");
                     buf.append("</li>");
                }
                buf.append("<ul>");
                ModelContentRO classContent = pp.getModelContent("class_info");
                for (String key : classContent.keySet()) {
                    buf.append("<li>");
                    buf.append(key + classContent.getString(key) + "\n");
                    buf.append("</li>");
                }
                buf.append("</ul>");
                buf.append("</ul>");
                buf.append("<h2>Model Spec</h2>");
                buf.append("<ul>");
                ModelContentRO specContent = pp.getModelContent("column_info");
                for (String key : specContent.keySet()) {
                    buf.append("<li>");
                    buf.append(key + specContent.getString(key) + "\n");
                    buf.append("</li>");
                }
                buf.append("<ul>");
                buf.append("</body>\n");
                buf.append("</html>\n");
                m_content.setText(buf.toString());
            } catch (InvalidSettingsException ise) {
                LOGGER.coding("Basisfunction model info wrong", ise);
                m_content.setText("");
            }
        }
    }

    /**
     * @see org.knime.core.node.NodeView#onClose()
     */
    @Override
    protected void onClose() {

    }

    /**
     * @see org.knime.core.node.NodeView#onOpen()
     */
    @Override
    protected void onOpen() {

    }
}
