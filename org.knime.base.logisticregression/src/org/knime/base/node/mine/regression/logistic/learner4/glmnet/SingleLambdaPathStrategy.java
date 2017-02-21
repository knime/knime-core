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
 *   14.02.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.glmnet;

/**
 * Performs coordinate descent for a single lambda.
 * The getNextLambda() method can be invoked only once and afterwards throws an IllegalStateException.
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class SingleLambdaPathStrategy implements PathStrategy {

    private final double m_lambda;
    private boolean m_fired = false;

    public SingleLambdaPathStrategy(final double lambda) {
        if (lambda < 0.0 || lambda > 1.0) {
            throw new IllegalArgumentException("lambda must be in the range [0,1] but was " + lambda + ".");
        }
        m_lambda = lambda;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean pathComplete() {
        return m_fired;
    }

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if it is invoked more than once
     */
    @Override
    public double getNextLambda() {
        if (m_fired) {
            throw new IllegalStateException("The lambda of this SingleLambdaPathStrategy has already been returned.");
        }
        m_fired = true;
        return m_lambda;
    }

}
