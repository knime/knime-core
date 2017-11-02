/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.regression.polynomial.learner2;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;

import org.apache.commons.lang.StringEscapeUtils;
import org.knime.base.node.util.DoubleFormat;

/**
 * This is the view that shows the coefficients in a table and the squared
 * error per row in a line below the table.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.10
 */
@SuppressWarnings("serial")
public class CoefficientTable extends JPanel {
    private PolyRegViewData m_viewData;
    /** The text pane that holds the information. */
    private final JEditorPane m_pane;

    /**
     * Creates a new coefficient table.
     *
     * @param nodeModel the node model
     */
    public CoefficientTable(final PolyRegLearnerNodeModel nodeModel) {
        super(new BorderLayout());
        m_pane = new JEditorPane("text/html", "");
        m_pane.setEditable(false);
        add(m_pane);
        m_viewData = nodeModel.getViewData();
    }

    /**
     * Updates the table.
     */
    public void update() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<html>\n");
        buffer.append("<head>\n");
        buffer.append("<style type=\"text/css\">\n");
        buffer.append("body {color:#333333;}");
        buffer.append("table {width: 100%;margin: 7px 0 7px 0;}");
        buffer.append("th {font-weight: bold;background-color: #aaccff;}");
        buffer.append("td,th {padding: 4px 5px; }");
        buffer.append(".numeric {text-align: right}");
        buffer.append(".odd {background-color:#ddeeff;}");
        buffer.append(".even {background-color:#ffffff;}");
        buffer.append("</style>\n");
        buffer.append("</head>\n");
        buffer.append("<body>\n");
        buffer.append("<h2>Statistics on Polynomial Regression</h2>");

        if (m_viewData.betas.length != 0) {
            //LinearRegressionContent content = model.getRegressionContent();
            List<String> parameters = Arrays.asList(m_viewData.columnNames);
            buffer.append("<table>\n");
            buffer.append("<tr>");
            buffer.append("<th>Variable</th>");
            buffer.append("<th>Coeff.</th>");
            buffer.append("<th>Std. Err.</th>");
            buffer.append("<th>t-value</th>");
            buffer.append("<th>P&gt;|t|</th>");
            buffer.append("</tr>");
            double[] coefficients = m_viewData.betas;
            double[] stdErrs = m_viewData.m_stdErrs;
            double[] tValues = m_viewData.m_tValues;
            double[] pValues = m_viewData.m_pValues;

            boolean odd = true;
            for (int d = 1; d <= m_viewData.degree; ++d) {
                for (int c = 0; c < parameters.size(); ++c) {
                    String parameter = parameters.get(c);
                    if (odd) {
                        buffer.append("<tr class=\"odd\">\n");
                    } else {
                        buffer.append("<tr class=\"even\">\n");
                    }
                    odd = !odd;

                    buffer.append("<td>");
                    buffer.append(StringEscapeUtils.escapeHtml(parameter) + (d > 1 ? "^" + d : ""));
                    buffer.append("</td>\n<td class=\"numeric\">");
                    String coeff = DoubleFormat.formatDouble(coefficients[(d - 1) + m_viewData.degree * c + 1]);
                    buffer.append(coeff);
                    buffer.append("</td>\n<td class=\"numeric\">");
                    int index = (d-1) * parameters.size() + c;
                    String stdErr = DoubleFormat.formatDouble(stdErrs[index]);
                    buffer.append(stdErr);
                    buffer.append("</td>\n<td class=\"numeric\">");
                    String zScore = DoubleFormat.formatDouble(tValues[index]);
                    buffer.append(zScore);
                    buffer.append("</td>\n<td class=\"numeric\">");
                    String pValue = DoubleFormat.formatDouble(pValues[index]);
                    buffer.append(pValue);
                    buffer.append("</td>\n");
                    buffer.append("</tr>\n");
                }
            }
                if (odd) {
                    buffer.append("<tr class=\"odd\">\n");
                } else {
                    buffer.append("<tr class=\"even\">\n");
                }

                buffer.append("<td>Intercept</td>\n<td class=\"numeric\">");
                String intercept = DoubleFormat.formatDouble(coefficients[0]);
                buffer.append(intercept);
                buffer.append("</td>\n<td class=\"numeric\">");
                String stdErr = DoubleFormat.formatDouble(stdErrs[stdErrs.length - 1]);
                buffer.append(stdErr);
                buffer.append("</td>\n<td class=\"numeric\">");
                String tValue = DoubleFormat.formatDouble(tValues[tValues.length - 1]);
                buffer.append(tValue);
                buffer.append("</td>\n<td class=\"numeric\">");
                String pValue = DoubleFormat.formatDouble(pValues[pValues.length - 1]);
                buffer.append(pValue);
                buffer.append("</td>\n");
                buffer.append("</tr>\n");
                buffer.append("</tr>\n");
            buffer.append("</table>\n");
            buffer.append("Multiple R-Squared: ");
            String rSquared = DoubleFormat.formatDouble(m_viewData.squaredError);
            buffer.append(rSquared);
            buffer.append("<br/>");
            buffer.append("Adjusted R-Squared: ");
            String adjustedRSquared = DoubleFormat.formatDouble(m_viewData.m_adjustedR2);
            buffer.append(adjustedRSquared);
            buffer.append("<br/>");
        } else {
            buffer.append("No parameters available.\n");
        }

        buffer.append("</body>\n");
        buffer.append("</html>\n");
        m_pane.setText(buffer.toString());
        m_pane.revalidate();
    }
}
