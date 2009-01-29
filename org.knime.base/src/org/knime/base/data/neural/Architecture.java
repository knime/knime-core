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
 * This class represents the general architecture of a neural network and
 * specifies how much layers, neurons constitute the neural network.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class Architecture {
    /*
     * The number of input neurons in the nn.
     */
    private int m_nrInputNeurons;

    /*
     * The number of output neurons in the nn.
     */
    private int m_nrOutputNeurons;

    /*
     * The number of hidden layers in the nn.
     */
    private int m_nrHiddenLayers;

    /*
     * The number of hidden neurons in the nn.
     */
    private int m_nrHiddenNeurons;

    /**
     * Constructs a new architecture.
     * 
     * @param nrInputNeurons number of input neurons
     * @param nrHiddenLayers number of hidden layers
     * @param nrHiddenNeurons number of hidden neurons
     * @param nrOutputNeurons number of output neurons
     */
    public Architecture(final int nrInputNeurons, final int nrHiddenLayers,
            final int nrHiddenNeurons, final int nrOutputNeurons) {
        m_nrHiddenLayers = nrHiddenLayers;
        m_nrHiddenNeurons = nrHiddenNeurons;
        m_nrInputNeurons = nrInputNeurons;
        m_nrOutputNeurons = nrOutputNeurons;
    }

    /**
     * Constructs a new empty architecture.
     */
    public Architecture() {
        this(0, 0, 0, 0);
    }

    /**
     * Returns the number of input neurons.
     * 
     * @return number of input neurons
     */
    public int getNrInputNeurons() {
        return m_nrInputNeurons;
    }

    /**
     * Sets the number of input neurons.
     * 
     * @param nrInputNeurons number of input neurons
     */
    public void setNrInputNeurons(final int nrInputNeurons) {
        if (nrInputNeurons < 1) {
            throw new IllegalArgumentException(
                    "Number of InputNeurons must be greater than zero");
        }
        m_nrInputNeurons = nrInputNeurons;
    }

    /**
     * @return number of hidden layers
     */
    public int getNrHiddenLayers() {
        return m_nrHiddenLayers;
    }

    /**
     * Sets the number of hidden layers.
     * 
     * @param nrHiddenLayers number of hidden layers
     */
    public void setNrHiddenLayers(final int nrHiddenLayers) {
        if (nrHiddenLayers >= 0) {
            m_nrHiddenLayers = nrHiddenLayers;
        } else {
            throw new IllegalArgumentException(
                    "Number of HiddenLayers must " 
                    + "be greater than or equal to zero.");
        }
    }

    /**
     * Returns the overall number of hidden neurons in the architecture.
     * 
     * @return number of hidden neurons
     */
    public int getNrHiddenNeurons() {
        return m_nrHiddenNeurons;
    }

    /**
     * Sets the overall number of hidden neurons.
     * 
     * @param nrHiddenNeurons number of hidden neurons
     */
    public void setNrHiddenNeurons(final int nrHiddenNeurons) {
        if (nrHiddenNeurons < 1) {
            throw new IllegalArgumentException(
                    "Number of HiddenNeurons must be greater than zero");
        }
        m_nrHiddenNeurons = nrHiddenNeurons;
    }

    /**
     * Returns the number of output neurons.
     * 
     * @return number of output neurons
     */
    public int getNrOutputNeurons() {
        return m_nrOutputNeurons;
    }

    /**
     * Sets the number of output neurons.
     * 
     * @param nrOutputNeurons number of output neurons
     */
    public void setNrOutputNeurons(final int nrOutputNeurons) {
        if (nrOutputNeurons < 1) {
            throw new IllegalArgumentException(
                    "Number of OutputNeurons must be greater than zero");
        }
        m_nrOutputNeurons = nrOutputNeurons;
    }
}
