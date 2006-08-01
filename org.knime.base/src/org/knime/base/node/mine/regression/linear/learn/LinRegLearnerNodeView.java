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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.NodeView;

/**
 * View on the linear regression learner node. It only has a text pane where
 * some statistics are displayed.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLearnerNodeView extends NodeView {
    /** The text pane that holds the information. */
    private final JEditorPane m_pane;

    /**
     * Constructs new view, inits members.
     * 
     * @param model the model to look at
     */
    public LinRegLearnerNodeView(final LinRegLearnerNodeModel model) {
        super(model);
        m_pane = new JEditorPane("text/html", "");
        m_pane.setEditable(false);
        JScrollPane scroller = new JScrollPane(m_pane);
        setComponent(scroller);
    }

    /**
     * Does cast.
     * 
     * @see NodeView#getNodeModel()
     */
    @Override
    protected LinRegLearnerNodeModel getNodeModel() {
        return (LinRegLearnerNodeModel)super.getNodeModel();
    }

    /**
     * @see org.knime.core.node.NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        LinRegLearnerNodeModel model = getNodeModel();
        m_pane.setText("");
        if (!model.isDataAvailable()) {
            return;
        }
        Map<String, Double> parameter = model.getParametersMap();
        int nrRows = model.getNrRows();
        int nrSkipped = model.getNrRowsSkipped();
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<body>\n");
        buffer.append("<h1>Statistics on Linear Regression</h1>");
        buffer.append("<hr>\n");
        if (parameter == null || parameter.isEmpty()) {
            buffer.append("No parameters available.\n");
        } else {
            buffer.append("<table>\n");
            buffer.append("<caption align=\"left\">Parameters</caption>");
            buffer.append("<tr>");
            buffer.append("<th>Column</th>");
            buffer.append("<th>Value</th>");
            buffer.append("</tr>");
            boolean isFirst = true;
            for (Map.Entry<String, Double> entry : parameter.entrySet()) {
                buffer.append("<tr>\n");
                buffer.append("<td>\n");
                String key;
                if (isFirst) {
                    isFirst = false;
                    key = "offset";
                } else {
                    key = entry.getKey();
                }
                buffer.append(key);
                buffer.append("\n</td>\n");
                buffer.append("<td>\n");
                String format = DoubleFormat.formatDouble(entry.getValue()
                        .doubleValue());
                buffer.append(format);
                buffer.append("\n</td>\n");
                buffer.append("</tr>\n");
            }
            buffer.append("</table>\n");
        }
        buffer.append("<hr>\n");
        buffer.append("<table>\n");
        buffer.append("<caption align=\"left\">Statistics</caption>\n");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Total Row Count\n");
        buffer.append("</td>\n");
        buffer.append("<td>\n");
        buffer.append(nrRows);
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Rows Processed\n");
        buffer.append("</td>\n");
        buffer.append("<td align=\"right\">\n");
        buffer.append(nrRows - nrSkipped);
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("<tr>\n");
        buffer.append("<td>\n");
        buffer.append("Rows Skipped\n");
        buffer.append("</td>\n");
        buffer.append("<td align=\"right\">\n");
        buffer.append(nrSkipped);
        buffer.append("\n</td>\n");
        buffer.append("</tr>\n");
        buffer.append("</table>\n");
        if (model.isCalcError()) {
            double error = model.getError();
            buffer.append("<hr>\n");
            buffer.append("<table>\n");
            buffer.append("<caption align=\"left\">Error</caption>\n");
            buffer.append("<tr>\n");
            buffer.append("<td>\n");
            buffer.append("Total Squared Error\n");
            buffer.append("</td>\n");
            buffer.append("<td>\n");
            buffer.append(DoubleFormat.formatDouble(error));
            buffer.append("\n</td>\n");
            buffer.append("</tr>\n");
            buffer.append("<tr>\n");
            buffer.append("<td>\n");
            buffer.append("Squared Error per Row\n");
            buffer.append("</td>\n");
            buffer.append("<td>\n");
            buffer.append(DoubleFormat.formatDouble(error
                    / (nrRows - nrSkipped)));
            buffer.append("\n</td>\n");
            buffer.append("</tr>\n");
            buffer.append("</table>\n");

        }
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        m_pane.setText(buffer.toString());
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
