/* 
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   02.03.2006 (gabriel): created
 */
package org.knime.base.node.mine.bfn;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeView;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;


/**
 * View to display basisfunction rule models.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * @param <T> the type of <code>BasisFunctionLearnerNodeModel</code>
 * 
 */
public class BasisFunctionLearnerNodeView
    <T extends BasisFunctionLearnerNodeModel> 
        extends NodeView<T> {
    
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(BasisFunctionLearnerNodeView.class);

    /** Here the model is displayed. */
    private final JEditorPane m_content;

    /**
     * Create an empty bf view.
     * 
     * @param model the bf model
     */
    public BasisFunctionLearnerNodeView(final T model) {
        super(model);
        m_content = new JEditorPane("text/html", "");
        m_content.setEditable(false);
        JScrollPane scroller = new JScrollPane(m_content);
        setComponent(scroller);
    }

    /**
     * Called when the model changed.
     * 
     * {@inheritDoc}
     */
    @Override
    public void modelChanged() {
        BasisFunctionLearnerNodeModel model = getNodeModel();
        ModelContentRO pp = model.getModelInfo();
        if (pp == null) {
            m_content.setText("");
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append("<html>\n");
            buf.append("<body>\n");
            buf.append("<h2>Learner Statistics</h2>");
            getNextValue(pp, buf);
            buf.append("</body>\n");
            buf.append("</html>\n");
            m_content.setText(buf.toString());
        }
    }
    
    private void getNextValue(final ModelContentRO pp, 
            final StringBuilder buf) {
        for (String key : pp.keySet()) {
            String value = pp.getString(key, null);
            if (value == null) {
                try {
                    ModelContentRO nextCont = pp.getModelContent(key);
                    buf.append("<ul>");
                    getNextValue(nextCont, buf);
                    buf.append("</ul>");
                } catch (InvalidSettingsException ise) {
                    LOGGER.coding("Could not find model content for key: " 
                            + key, ise);
                }
            } else {
                buf.append("<li>" + key + " " + value + "\n</li>");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen() {

    }
}
