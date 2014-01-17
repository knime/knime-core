/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   23.10.2013 (gabor): created
 */
package org.knime.base.node.mine.scorer.numeric;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "NumericScorer" Node.
 * Computes the distance between the a numeric column's values and predicted values.
 *
 * @author Gabor Bakos
 */
class NumericScorerNodeView extends NodeView<NumericScorerNodeModel> {

    private JLabel m_rSquared;
    private JLabel m_meanAbsError;
    private JLabel m_meanSquaredError;
    private JLabel m_rootMeanSquaredError;
    private JLabel m_meanSignedDifference;

    /**
     * Creates a new view.
     *
     * @param nodeModel The model (class: {@link NumericScorerNodeModel})
     */
    protected NumericScorerNodeView(final NumericScorerNodeModel nodeModel) {
        super(nodeModel);
        JPanel summary = new JPanel(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(3, 3, 3, 5);
        c.anchor = GridBagConstraints.WEST;

        summary.add(new JLabel("R\u00b2: "), c);
        m_rSquared = new JLabel("n/a");
        c.gridx = 1;
        summary.add(m_rSquared, c);

        c.gridx = 0;
        c.gridy++;
        summary.add(new JLabel("Mean absolute error: "), c);
        m_meanAbsError = new JLabel("n/a");
        c.gridx = 1;
        summary.add(m_meanAbsError, c);

        c.gridx = 0;
        c.gridy++;
        summary.add(new JLabel("Mean squared error: "), c);
        m_meanSquaredError = new JLabel("n/a");
        c.gridx = 1;
        summary.add(m_meanSquaredError, c);

        c.gridx = 0;
        c.gridy++;
        summary.add(new JLabel("Root mean squared error: "), c);
        m_rootMeanSquaredError = new JLabel("n/a");
        c.gridx = 1;
        summary.add(m_rootMeanSquaredError, c);

        c.gridx = 0;
        c.gridy++;
        summary.add(new JLabel("Mean signed difference: "), c);
        m_meanSignedDifference = new JLabel("n/a");
        c.gridx = 1;
        summary.add(m_meanSignedDifference, c);
        setComponent(summary);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
        NumericScorerNodeModel model = getNodeModel();
        NumberFormat nf = NumberFormat.getNumberInstance();
        m_rSquared.setText(nf.format(model.getRSquare()));
        m_meanAbsError.setText(nf.format(model.getMeanAbsError()));
        m_meanSquaredError.setText(nf.format(model.getMeanSquaredError()));
        m_rootMeanSquaredError.setText(nf.format(model.getRmsd()));
        m_meanSignedDifference.setText(nf.format(model.getMeanSignedDifference()));
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

