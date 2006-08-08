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
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;


/**
 * The view shows the statistical information.
 * 
 * @author cebron, University of Konstanz
 */
public class StatisticsNodeView extends NodeView {

    /*
     * Output is printed in a JTExtArea
     */
    private JEditorPane m_output;

    /**
     * Constructs a <code>NodeView</code> consisting of statistical values.
     * 
     * @param model The underlying NodeModel
     */
    StatisticsNodeView(final NodeModel model) {
        super(model);

        m_output = new JEditorPane("text/html", "");
        m_output.setEditable(false);
        JScrollPane scroller = new JScrollPane(m_output);
        setComponent(scroller);
    }

    /**
     * If the model changes, the new statistical information from the original
     * table is added to the upper statistics table.
     * 
     * @see NodeView#modelChanged()
     */
    @Override
    protected void modelChanged() {
        StatisticsNodeModel myModel = (StatisticsNodeModel)getNodeModel();
        StringBuffer buffer = new StringBuffer();
        buffer.append("<html>\n");
        buffer.append("<body>\n");
        buffer.append("<p>&nbsp</p>");
        buffer.append("<table border = \"1\">");
        buffer.append("<th> Moment </th>");

        if (myModel.getColumnNames() != null) {
            String[] columnNames = myModel.getColumnNames();
            for (String colname : columnNames) {
                buffer.append("<th>" + colname + "</th>");
            }
        }

        buffer.append("<tr>");
        buffer.append("<td> Minimum </td>");
        if (myModel.getMin() != null) {
            double[] mins = myModel.getMin();
            for (double min : mins) {
                buffer.append("<td>" + min + "</td>");
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Maximum </td>");
        if (myModel.getMax() != null) {
            double[] maxs = myModel.getMax();
            for (double max : maxs) {
                buffer.append("<td>" + max + "</td>");
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Mean </td>");
        if (myModel.getMean() != null) {
            double[] means = myModel.getMean();

            for (double mean : means) {
                buffer.append("<td>" + mean + "</td>");
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Standard deviation </td>");
        if (myModel.getStddev() != null) {
            double[] stddevs = myModel.getStddev();
            for (double stddev : stddevs) {
                buffer.append("<td>" + stddev + "</td>");
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Variance </td>");
        if (myModel.getVariance() != null) {
            double[] variances = myModel.getVariance();
            for (double variance : variances) {
                buffer.append("<td>" + variance + "</td>");
            }
        }
        buffer.append("</tr>");

        buffer.append("</table>");
        buffer.append("<p>&nbsp</p>");
        buffer.append("</body>\n");
        buffer.append("</html>\n");
        buffer.append("");
        m_output.setText(buffer.toString());
    }

    /**
     * @see NodeView#onClose()
     */
    @Override
    protected void onClose() {
    }

    /**
     * @see NodeView#onOpen()
     */
    @Override
    protected void onOpen() {
    }
}
