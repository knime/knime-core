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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Mar 30, 2006 (wiswedel): created
 */
package org.knime.base.node.mine.regression.linear2.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.base.node.mine.regression.linear2.learner.LinReg2LearnerNodeModel;
import org.knime.base.node.mine.regression.linear2.learner.LinearRegressionContent;
import org.knime.base.node.viz.plotter.node.DefaultVisualizationNodeView;

/**
 * 2D plot showing the linear regression line. The plot allows to choose one
 * input column as x-coordinate and has the y-coordinate fixed to the response
 * variable.
 *
 * @author Heiko Hofer
 */
public class LinReg2LineNodeView extends DefaultVisualizationNodeView<LinReg2LearnerNodeModel> {

    private final LinReg2LinePlotter m_plotter;
    private Component m_scatterPlot;
    private JPanel m_message;

    /**
     * Create new view with a scatter plot an the regression line.
     *
     * @param nodeModel the model to look at
     */
    public LinReg2LineNodeView(final LinReg2LearnerNodeModel nodeModel) {
        this(nodeModel, new LinReg2LinePlotter(
                new LinReg2LineDrawingPane(),
                new LinReg2LinePlotterProperties()));
    }

    private LinReg2LineNodeView(final LinReg2LearnerNodeModel nodeModel,
            final LinReg2LinePlotter plotter) {
        super(nodeModel, plotter);
        m_plotter = plotter;
        m_scatterPlot = getComponent();
        GridBagConstraints c = new GridBagConstraints();
        m_message = new JPanel(new GridBagLayout());
        m_message.add(new JLabel(
            "This view is only available for regression models with numerical independent variables."), c);
        m_message.setPreferredSize(new Dimension(
            m_scatterPlot.getPreferredSize().width, m_scatterPlot.getPreferredSize().height));
    }

    /** {@inheritDoc} */
    @Override
    protected void modelChanged() {
        super.modelChanged();
        m_plotter.updatePaintModel();
        LinearRegressionContent content = getNodeModel().getLinRegContent();
        if (content != null && content.getCovariates().size() > 0) {
            if (getComponent() != m_scatterPlot) {
                setComponent(m_scatterPlot);
            }
        } else {
            if (getComponent() != m_message) {
                setComponent(m_message);
            }
        }
    }

}
