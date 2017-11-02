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
 * -------------------------------------------------------------------
 *
 * History
 *   21.01.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import java.util.List;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.apache.commons.lang.StringEscapeUtils;
import org.knime.base.node.mine.regression.RegressionContent;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.NodeView;
import org.knime.core.util.Pair;

/**
 * View on the linear regression learner node. It only has a text pane where
 * some statistics are displayed.
 *
 * @author Heiko Hofer
 */
class LinReg2LearnerNodeView extends NodeView<LinReg2LearnerNodeModel> {

    /** The text pane that holds the information. */
    private final JEditorPane m_pane;

    /**
     * New instance.
     *
     * @param model the model to look at
     */
    public LinReg2LearnerNodeView(final LinReg2LearnerNodeModel model) {
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
        LinReg2LearnerNodeModel model = getNodeModel();

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
        buffer.append("<h2>Statistics on Linear Regression</h2>");

        if (model.isDataAvailable()) {
            RegressionContent content = model.getRegressionContent();
            List<String> parameters = content.getParameters();
            buffer.append("<table>\n");
            buffer.append("<tr>");
            buffer.append("<th>Variable</th>");
            buffer.append("<th>Coeff.</th>");
            buffer.append("<th>Std. Err.</th>");
            buffer.append("<th>t-value</th>");
            buffer.append("<th>P&gt;|t|</th>");
            buffer.append("</tr>");
            Map<Pair<String, Integer>, Double> coefficients = content.getCoefficients();
            Map<Pair<String, Integer>, Double> stdErrs = content.getStandardErrors();
            Map<Pair<String, Integer>, Double> tValues = content.getTValues();
            Map<Pair<String, Integer>, Double> pValues = content.getPValues();

            boolean odd = true;
            for (String parameter : parameters) {
                if (odd) {
                    buffer.append("<tr class=\"odd\">\n");
                } else {
                    buffer.append("<tr class=\"even\">\n");
                }
                odd = !odd;

                buffer.append("<td>");
                buffer.append(StringEscapeUtils.escapeHtml(parameter));
                buffer.append("</td>\n<td class=\"numeric\">");
                Pair<String, Integer> pair = Pair.create(parameter, 1);
                String coeff = DoubleFormat.formatDouble(coefficients.get(pair));
                buffer.append(coeff);
                buffer.append("</td>\n<td class=\"numeric\">");
                String stdErr = DoubleFormat.formatDouble(stdErrs.get(pair));
                buffer.append(stdErr);
                buffer.append("</td>\n<td class=\"numeric\">");
                String zScore = DoubleFormat.formatDouble(tValues.get(pair));
                buffer.append(zScore);
                buffer.append("</td>\n<td class=\"numeric\">");
                String pValue = DoubleFormat.formatDouble(pValues.get(pair));
                buffer.append(pValue);
                buffer.append("</td>\n");
                buffer.append("</tr>\n");
            }
            if (content.getIncludeConstant()) {
                if (odd) {
                    buffer.append("<tr class=\"odd\">\n");
                } else {
                    buffer.append("<tr class=\"even\">\n");
                }

                buffer.append("<td>");
                buffer.append("Intercept");
                buffer.append("</td>\n<td class=\"numeric\">");
                String intercept = DoubleFormat.formatDouble(content.getIntercept());
                buffer.append(intercept);
                buffer.append("</td>\n<td class=\"numeric\">");
                String stdErr = DoubleFormat.formatDouble(content.getInterceptStdErr());
                buffer.append(stdErr);
                buffer.append("</td>\n<td class=\"numeric\">");
                String tValue = DoubleFormat.formatDouble(content.getInterceptTValue());
                buffer.append(tValue);
                buffer.append("</td>\n<td class=\"numeric\">");
                String pValue = DoubleFormat.formatDouble(content.getInterceptPValue());
                buffer.append(pValue);
                buffer.append("</td>\n");
                buffer.append("</tr>\n");
                buffer.append("</tr>\n");
            }
            buffer.append("</table>\n");

            if (!content.getIncludeConstant()) {
                buffer.append("Offset Value: ");
                String offsetValue = DoubleFormat.formatDouble(content.getOffsetValue());
                buffer.append(offsetValue);
                buffer.append("<br/>");
            }
            buffer.append("Multiple R-Squared: ");
            String rSquared = DoubleFormat.formatDouble(content.getRSquared());
            buffer.append(rSquared);
            buffer.append("<br/>");
            buffer.append("Adjusted R-Squared: ");
            String adjustedRSquared = DoubleFormat.formatDouble(content.getAdjustedRSquared());
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // do nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // do nothing.
    }
}
