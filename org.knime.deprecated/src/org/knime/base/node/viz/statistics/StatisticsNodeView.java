/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 * History
 *   18.04.2005 (cebron): created
 */
package org.knime.base.node.viz.statistics;

import java.text.NumberFormat;
import java.util.Locale;

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
    
    /* Used to get a string representation of the double value. */
    private final NumberFormat m_format;
    
    /** disable grouping in renderer */
    static {
        NumberFormat.getNumberInstance(Locale.US).setGroupingUsed(false);
    }

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
        m_format = NumberFormat.getNumberInstance(Locale.US);
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
                if (Double.isNaN(min)) {
                    buffer.append("<td> - </td>");
                } else {
                    buffer.append("<td>" + m_format.format(min) + "</td>");
                }
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Maximum </td>");
        if (myModel.getMax() != null) {
            double[] maxs = myModel.getMax();
            for (double max : maxs) {
                if (Double.isNaN(max)) {
                    buffer.append("<td> - </td>");
                } else {
                    buffer.append("<td>" + m_format.format(max) + "</td>");
                }
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Mean </td>");
        if (myModel.getMean() != null) {
            double[] means = myModel.getMean();

            for (double mean : means) {
                if (Double.isNaN(mean)) {
                    buffer.append("<td> - </td>");
                } else {
                    buffer.append("<td>" + m_format.format(mean) + "</td>");
                }
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Standard deviation </td>");
        if (myModel.getStddev() != null) {
            double[] stddevs = myModel.getStddev();
            for (double stddev : stddevs) {
                if (Double.isNaN(stddev)) {
                    buffer.append("<td> - </td>");
                } else {
                    buffer.append("<td>" + m_format.format(stddev) + "</td>");
                }
            }
        }
        buffer.append("</tr>");

        buffer.append("<tr>");
        buffer.append("<td> Variance </td>");
        if (myModel.getVariance() != null) {
            double[] variances = myModel.getVariance();
            for (double variance : variances) {
                if (Double.isNaN(variance)) {
                    buffer.append("<td> - </td>");
                } else {
                    buffer.append("<td>" + m_format.format(variance) + "</td>");
                }
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
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
    }
}
