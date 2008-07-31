/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
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
