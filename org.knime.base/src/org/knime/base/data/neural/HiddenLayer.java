/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
