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
 * This class represents a hidden layer in a MultiLayerPerceptron.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public class HiddenLayer extends Layer {
    /*
     * The predecessor layer.
     */
    private Layer m_predLayer;

    /**
     * Constructs a layer with given predecessor layer and given number of
     * hidden neurons.
     * 
     * @param predLayer predecessor layer
     * @param nrHiddenNeurons number of hidden neurons
     */
    public HiddenLayer(final Layer predLayer, final int nrHiddenNeurons) {
        if (nrHiddenNeurons < 1) {
            throw new IllegalArgumentException(
                    "Layer must contain at least one neuron");
        }
        m_predLayer = predLayer;
        Perceptron[] perceptrons = new Perceptron[nrHiddenNeurons];
        for (int i = 0; i < perceptrons.length; i++) {
            perceptrons[i] = new SigmoidPerceptron(predLayer.getPerceptrons());
        }
        setPerceptrons(perceptrons);
    }

    /**
     * Constructs a hidden layer with the given predecessor layer and the given
     * neurons.
     * 
     * @param predLayer predecessor layer
     * @param neurons neurons in the hidden layer
     */
    public HiddenLayer(final Layer predLayer, final Perceptron[] neurons) {
        m_predLayer = predLayer;
        setPerceptrons(neurons);
    }

    /**
     * Returns the predecessor layer for the current layer.
     * 
     * @return predecessor layer
     */
    public Layer getPredLayer() {
        return m_predLayer;
    }

    /**
     * Sets the predecessor layer.
     * 
     * @param predLayer predecessor layer to set
     */
    public void setPredLayer(final Layer predLayer) {
        m_predLayer = predLayer;
    }
}
