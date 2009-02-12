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
 * Abstract class defining the behaviour of a perceptron in a neural network.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public abstract class Perceptron {
    /*
     * Threshold
     */
    private double m_threshold;

    /*
     * Weights of the perceptron
     */
    private double[] m_weights;

    /*
     * Optional class value of the perceptron.
     */
    private String m_classval;

    /**
     * Construct a perceptron with given number of weights. Weights and
     * threshold are randomly initialized.
     *
     * @param nrInputs number of inputs for the new perceptron
     */
    public Perceptron(final int nrInputs) {
        if (nrInputs < 1) {
            throw new IllegalArgumentException(
                    "Cannot create Perceptron without weights");
        }
        m_weights = new double[nrInputs];
        for (int i = 0; i < m_weights.length; i++) {
            m_weights[i] = random();
        }
        setThreshold(random());
    }

    /**
     * Construct an empty perceptron (cannot be used properly unless the weights
     * are set).
     */
    public Perceptron() {
        // empty.
    }

    /**
     * Construct a perceptron with given weights.
     *
     * @param weights weights for the new perceptron
     */
    public Perceptron(final double[] weights) {
        if (weights.length < 1) {
            throw new IllegalArgumentException(
                    "Cannot create Perceptron without weights");
        }
        m_weights = weights;
        setThreshold(random());
    }

    /**
     * @return weights of the perceptron
     */
    public double[] getWeights() {
        return m_weights;
    }

    /**
     * Get weight at given position.
     *
     * @param i position
     * @return weight at position <code>i</code>
     */
    public double getWeight(final int i) {
        return m_weights[i];
    }

    /**
     * Set all new weights.
     *
     * @param weights new weights to set
     */
    public void setWeights(final double[] weights) {
        if (this.m_weights != null && this.m_weights.length != weights.length) {
            throw new IllegalArgumentException("Wrong array length.");
        } else {
            this.m_weights = weights;
        }
    }

    /**
     * Sets weight at given position.
     *
     * @param i position
     * @param weight new value for weight at position <code>i</code>
     */
    public void setWeight(final int i, final double weight) {
        if (i < 0 || i >= m_weights.length) {
            throw new IllegalArgumentException("No such weight: " + i);
        } else {
            this.m_weights[i] = weight;
        }
    }

    /**
     * Activation function. Implement it to create the desired behaviour for the
     * perceptron.
     *
     * @param in input value
     * @return output value for activation
     */
    public abstract double activationFunction(double in);

    /**
     * Evaluates a given input for the perceptron.
     *
     * @param in input to evaluate
     * @return activation function applied to the weighted sum of the inputs
     */
    public double evaluate(final double[] in) {
        if (in.length != m_weights.length) {
            throw new IllegalArgumentException("Wrong array length.");
        }
        double v = scalarProduct(m_weights, in);
        return activationFunction(v);
    }

    /**
     * Used to get the output from all input neurons.
     *
     * @return activation function applied to the weighted sum of all inputs,
     *         which are taken from the input neurons
     */
    public abstract double output();

    /**
     * Returns the threshold.
     *
     * @return threshold of the neutron
     */
    public double getThreshold() {
        return m_threshold;
    }

    /**
     * Sets the threshold.
     *
     * @param threshold The threshold to set
     */
    public void setThreshold(final double threshold) {
        m_threshold = threshold;
    }

    /**
     * Allows to set a class value for a perceptron (optional).
     *
     * @param classval the class value to set.
     */
    public void setClassValue(final String classval) {
        m_classval = classval;
    }

    /**
     * @return class value of the perceptron if it exists.
     */
    public String getClassValue() {
        return m_classval;
    }

    /**
     * Returns a randomly chosen double between -1 and 1.
     *
     * @return random double number between -1 and 1
     */
    public static double random() {
        double random = Math.random();
        random = (random - 0.5) * 2;
        return random;
    }

    /**
     * Computes the scalar Product of two vectors, represented as double arrays.
     *
     * @param x first array
     * @param y second array
     * @return scalar product of <code>x</code> and <code>y</code>
     */
    public static double scalarProduct(final double[] x, final double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Vector length not equal.");
        }
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i] * y[i];
        }
        return sum;
    }
}
