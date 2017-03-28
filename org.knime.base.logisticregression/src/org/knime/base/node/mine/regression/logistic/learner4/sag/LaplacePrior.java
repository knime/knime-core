/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   24.03.2017 (Adrian): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.sag;

/**
 *
 * @author Adrian Nembach, KNIME.com
 */
final class LaplacePrior implements RegularizationPrior {

    static final double SQRT_TWO = Math.sqrt(2.0);
    private final double m_factor;
    private final int m_nRows;
    private final boolean m_clip;


    public LaplacePrior(final double variance, final int nRows, final boolean clip) {
        assert variance > 0;
        m_factor = SQRT_TWO / (Math.sqrt(variance) * nRows);
        m_nRows = nRows;
        m_clip = clip;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(final WeightVector<?> beta, final double stepSize) {
        if (m_clip) {
            beta.update((val, c, i) -> clip(val, stepSize), false);
        } else {
            beta.update((val, c, i) -> val - stepSize * m_factor * Math.signum(val), false);
        }
    }

    private double clip(final double val, final double stepSize) {
        double v = val - stepSize * m_factor * Math.signum(val);
        if (val < 0) {
            return v < 0.0 ? v : 0.0;
        } else {
            return v > 0.0 ? v : 0.0;
        }
    }

}
