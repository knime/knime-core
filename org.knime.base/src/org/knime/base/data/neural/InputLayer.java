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
 * A special layer holding the input perceptrons.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class InputLayer extends Layer {
    /**
     * Constructs an input layer with given input perceptrons.
     *
     * @param inputs input neurons for the layer
     */
    public InputLayer(final Perceptron[] inputs) {
        setPerceptrons(inputs);
    }

    /**
     * Constructs an input layer with given number of input neurons, input set
     * to zero.
     *
     * @param nrInputs number of input neurons for the layer
     */
    public InputLayer(final int nrInputs) {
        super(new Perceptron[nrInputs]);
        for (int i = 0; i < this.getPerceptrons().length; i++) {
            setPerceptron(i, new InputPerceptron());
        }
    }
}
