/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 * Class representing an input perceptron.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class InputPerceptron extends Perceptron {
    /*
     * The input.
     */
    private double m_input;

    /**
     * Constructs a perceptron with given input.
     * 
     * @param input input for the perceptron
     */
    public InputPerceptron(final double input) {
        m_input = input;
    }

    /**
     * Constructs a perceptron with input 0.
     */
    public InputPerceptron() {
        this(0.0);
    }

    /**
     * Returns the current input of the perceptron.
     * 
     * @return current input
     */
    public double getInput() {
        return m_input;
    }

    /**
     * Sets the input to given value.
     * 
     * @param input new input
     */
    public void setInput(final double input) {
        m_input = input;
    }

    /**
     * Returns the current input of the perceptron.
     * 
     * @return current input
     */
    @Override
    public double output() {
        return m_input;
    }

    /**
     * Activation function, returns the input.
     * 
     * @param in input
     * @return <code>in</code>
     */
    @Override
    public double activationFunction(final double in) {
        return in;
    }
}
