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
 * An abstract class defining a layer in a Neural Network.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public abstract class Layer {
    /**
     * Perceptrons for this layer.
     */
    private Perceptron[] m_perceptrons;

    /**
     * Constructs a layer with given perceptrons.
     * 
     * @param perceptrons perceptrons for this layer
     */
    public Layer(final Perceptron[] perceptrons) {
        if (perceptrons != null && perceptrons.length < 1) {
            throw new IllegalArgumentException(
                    "Cannot create layer - no perceptrons.");
        }
        m_perceptrons = perceptrons;
    }

    /**
     * Used to construct an empty layer which cannot be used unless perceptrons
     * are set.
     */
    public Layer() {
        this(null);
    }

    /**
     * Returns all perceptrons in the layer.
     * 
     * @return perceptrons in layer
     */
    public Perceptron[] getPerceptrons() {
        return m_perceptrons;
    }

    /**
     * Returns perceptron at a given position.
     * 
     * @param i position
     * @return perceptron at position <code>i</code>
     */
    public Perceptron getPerceptron(final int i) {
        if (i < 0 || i >= m_perceptrons.length) {
            throw new IllegalArgumentException("No such perceptron: " + i);
        } else {
            return m_perceptrons[i];
        }
    }

    /**
     * Sets all perceptrons.
     * 
     * @param perceptrons new perceptrons for the layer
     */
    public void setPerceptrons(final Perceptron[] perceptrons) {
        if (m_perceptrons != null 
                && m_perceptrons.length != perceptrons.length) {
            throw new IllegalArgumentException(
                    "Cannot set Perceptrons, inappropriate array length");
        } else {
            m_perceptrons = perceptrons;
        }
    }

    /**
     * Sets perceptron at a given position.
     * 
     * @param i position
     * @param perceptron new perceptron for position <code>i</code>
     */
    public void setPerceptron(final int i, final Perceptron perceptron) {
        if (i < 0 || i >= m_perceptrons.length) {
            throw new IllegalArgumentException("No such perceptron: " + i);
        } else {
            m_perceptrons[i] = perceptron;
        }
    }
}
