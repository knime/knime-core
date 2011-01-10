/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
