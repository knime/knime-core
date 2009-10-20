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
 *   Feb 23, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear.learn;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.knime.base.node.mine.regression.linear.LinearRegressionContent;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeView;

/**
 * View on the linear regression learner node. It only has a text pane where
 * some statistics are displayed.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class LinRegLearnerNodeView 
    extends NodeView<LinRegLearnerNodeModel> {
    
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
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        LinRegLearnerNodeModel model = getNodeModel();
        m_pane.setText("");
        LinearRegressionContent params = model.getParams();
        int nrRows = model.getNrRows();
        int nrSkipped = model.getNrRowsSkipped();
        final StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<body>\n");
        buffer.append("<h1>Statistics on Linear Regression</h1>");
        buffer.append("<hr>\n");
        if (params == null) {
            buffer.append("No parameters available.\n");
        } else {
            DataTableSpec outSpec = params.getSpec();
            double[] multipliers = params.getMultipliers();
            buffer.append("<table>\n");
            buffer.append("<caption align=\"left\">Parameters</caption>");
            buffer.append("<tr>");
            buffer.append("<th>Column</th>");
            buffer.append("<th>Value</th>");
            buffer.append("</tr>");
            for (int i = 0; i < multipliers.length + 1; i++) {
                buffer.append("<tr>\n");
                buffer.append("<td>\n");
                String key;
                double value;
                if (i == 0) {
                    key = "offset";
                    value = params.getOffset();
                } else {
                    key = outSpec.getColumnSpec(i - 1).getName();
                    value = multipliers[i - 1];
                }
                buffer.append(key);
                buffer.append("\n</td>\n");
                buffer.append("<td>\n");
                String format = DoubleFormat.formatDouble(value);
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
        m_pane.revalidate();
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
