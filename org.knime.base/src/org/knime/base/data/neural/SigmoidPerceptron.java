/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   26.10.2005 (cebron): created
 */
package org.knime.base.data.neural;

/**
 * A hidden layer perceptron with a sigmoid activation function.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class SigmoidPerceptron extends Perceptron {

    /*
     * The predecessor perceptrons.
     */
    private Perceptron[] m_predecessors;

    /**
     * Constructs a perceptron with given weights and predecessors.
     * 
     * @param weights the weights for the perceptron
     * @param predecessors the predecessor perceptrons
     */
    public SigmoidPerceptron(final double[] weights,
            final Perceptron[] predecessors) {
        super(weights);
        if (weights.length != predecessors.length) {
            throw new IllegalArgumentException(
                    "Number of weights and predecessors must be equal");
        }
        m_predecessors = predecessors;
    }

    /**
     * Constructs a perceptron with given predecessors, weights initialized
     * randomly.
     * 
     * @param predecessors the predecessor perceptrons
     */
    public SigmoidPerceptron(final Perceptron[] predecessors) {
        super(predecessors.length);
        m_predecessors = predecessors;
    }

    /**
     * Constructs an empty perceptron.
     */
    public SigmoidPerceptron() {
        super();
    }

    /**
     * Returns the predecessors.
     * 
     * @return predecessors of the perceptron
     */
    public Perceptron[] getPredecessors() {
        return m_predecessors;
    }

    /**
     * Returns the predecessor at a given position.
     * 
     * @param i position
     * @return Predecessor at position <code>i</code>
     */
    public Perceptron getPredecessor(final int i) {
        return m_predecessors[i];
    }

    /**
     * Sets the predecessors.
     * 
     * @param predecessors new predecessors for the perceptron
     */
    public void setPredecessors(final Perceptron[] predecessors) {
        if (m_predecessors != null
                && m_predecessors.length != predecessors.length) {
            throw new IllegalArgumentException(
                    "Cannot set predecessors, inappropriate array length");
        } else {
            m_predecessors = predecessors;
        }
    }

    /**
     * Set predecessor at a given position.
     * 
     * @param i position
     * @param predecessor new predecessor for position <code>i</code>
     */
    public void setPredecessor(final int i, final Perceptron predecessor) {
        if (i < 0 || i >= m_predecessors.length) {
            throw new IllegalArgumentException(
                    "Predecessor cannot be set, wrong index " + i);
        } else {
            m_predecessors[i] = predecessor;
        }
    }

    /**
     * Returns the output.
     * 
     * @return output
     */
    @Override
    public double output() {
        double[] input = new double[m_predecessors.length];
        for (int i = 0; i < input.length; i++) {
            input[i] = m_predecessors[i].output();
        }
        return evaluate(input);
    }

    /**
     * Sigmoid activation function, computes the sigmoid value of its argument.
     * 
     * @param in argument to compute
     * @return 1/(1+e^<code>-in</code>)
     */
    @Override
    public double activationFunction(final double in) {
        double value = in;
        if (value < -37) {
            value = 0;
        } else if (value > 37) {
            value = 1;
        } else {
            value = 1 / (1 + Math.exp(-value));
        }
        return value;
    }
}
