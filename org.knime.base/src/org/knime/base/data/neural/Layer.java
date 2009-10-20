/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
