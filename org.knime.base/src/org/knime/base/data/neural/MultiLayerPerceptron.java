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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * Representation of a MultiLayer Perceptron, a neural net with one or more
 * hidden layers.
 *
 * @author Nicolas Cebron, University of Konstanz
 */
public class MultiLayerPerceptron {
    /**
     * Key to store the input value of a neuron in the ModelContent.
     */
    public static final String INPUT_KEY = "input";

    /**
     * Key to store the weights of a neuron in the ModelContent.
     */
    public static final String WEIGHT_KEY = "weights";

    /**
     * Key to store the threshold value of a neuron in the ModelContent.
     */
    public static final String THRESHOLD_KEY = "threshold";

    /**
     * Key to store the class value of a neuron in the ModelContent.
     */
    public static final String CLASSVALUE_KEY = "classval";

    /**
     * Key to store the mode of the MLP in the ModelContent.
     */
    public static final String MODE_KEY = "mode";

    /**
     * Key to store all layers of the MLP in the ModelContent.
     */
    public static final String ALLLAYERS_KEY = "allLayers";

    /**
     * Indicates whether the MLP does regression with one output neuron.
     */
    public static final int REGRESSION_MODE = 1;

    /**
     * Indicates whether the MLP does classification with multiple output
     * neurons, one neuron for each class.
     */
    public static final int CLASSIFICATION_MODE = 2;

    /*
     * The actual mode.
     */
    private int m_mode = REGRESSION_MODE;

    /*
     * Layers in the net.
     */
    private Layer[] m_layers;

    /**
     * Architecture of this net.
     */
    private Architecture m_architecture;

    /*
     * Maps the values from the classes to the output neurons.
     */
    private HashMap<DataCell, Integer> m_classmap;

    /*
     * Maps the values from the classes to the input neurons.
     */
    private HashMap<String, Integer> m_inputmap;

    /**
     * Constructs a net with the given layers.
     *
     * @param layers layers for the new net
     */
    public MultiLayerPerceptron(final Layer[] layers) {
        if (layers.length < 1) {
            throw new IllegalArgumentException("MLP needs at least one layer");
        }
        m_layers = layers;
    }

    /**
     * Constructs a net with a given architecture.
     *
     * @param a architecture for the new net
     */
    public MultiLayerPerceptron(final Architecture a) {
        m_architecture = a;
        m_layers = new Layer[m_architecture.getNrHiddenLayers() + 2];
        m_layers[0] = new InputLayer(m_architecture.getNrInputNeurons());
        for (int i = 1; i < m_layers.length; i++) {
            m_layers[i] = new HiddenLayer(m_layers[i - 1],
                    (i == m_layers.length - 1) ? m_architecture
                            .getNrOutputNeurons() : m_architecture
                            .getNrHiddenNeurons());
        }
    }

    /**
     * Constructor for an empty Neural Net.
     */
    public MultiLayerPerceptron() {
        // empty.
    }

    /**
     * Allows for setting the class mapping from output neurons to class values.
     *
     * @param map a HashMap containing the mapping.
     */
    public void setClassMapping(final HashMap<DataCell, Integer> map) {
        m_classmap = map;
        Layer outputlayer = m_layers[m_layers.length - 1];
        Set<DataCell> keyset = m_classmap.keySet();
        Iterator<DataCell> it = keyset.iterator();
        while (it.hasNext()) {
            DataCell dc = it.next();
            int pos = m_classmap.get(dc);
            outputlayer.getPerceptron(pos).setClassValue(dc.toString());
        }
    }

    /**
     * Allows for setting the input mapping from input neurons to class values.
     *
     * @param map a HashMap containing the mapping
     */
    public void setInputMapping(final HashMap<String, Integer> map) {
        m_inputmap = map;
        Layer inputlayer = m_layers[0];
        Set<String> keyset = m_inputmap.keySet();
        for (String key : keyset) {
            inputlayer.getPerceptron(m_inputmap.get(key)).setClassValue(key);
        }
     }

    /**
     * Allows to get the class mapping from output neurons to class values.
     *
     * @return a HashMap containing the mapping.
     */
    public HashMap<DataCell, Integer> getClassMapping() {
        return m_classmap;
    }

    /**
     * Allows to get the input mapping from input neurons to columns.
     *
     * @return a HashMap containing the mapping
     */
    public HashMap<String, Integer> getInputMapping() {
        return m_inputmap;
    }

    /**
     * @return the layers of the net.
     */
    public Layer[] getLayers() {
        return m_layers;
    }

    /**
     * Returns layer at a given position.
     *
     * @param i position
     * @return layer at position <code>i</code>
     */
    public Layer getLayer(final int i) {
        if (i < 0 || i >= m_layers.length) {
            throw new IllegalArgumentException("No such layer: " + i);
        } else {
            return m_layers[i];
        }
    }

    /**
     * @return number of Layers in MLP.
     */
    public int getNrLayers() {
        return m_layers.length;
    }

    /**
     * Sets all layers.
     *
     * @param layers new layers for the net
     */
    public void setLayers(final Layer[] layers) {
        if (m_layers != null && m_layers.length != layers.length) {
            throw new IllegalArgumentException(
                    "Cannot set layers, inappropriate array length");
        } else {
            m_layers = layers;
        }
    }

    /**
     * Sets the layer at a given position.
     *
     * @param i position
     * @param layer new layer for position <code>i</code>
     */
    public void setLayer(final int i, final Layer layer) {
        if (i < 0 || i >= m_layers.length) {
            throw new IllegalArgumentException("No such layer: " + i);
        } else {
            m_layers[i] = layer;
        }
    }

    /**
     * Evaluates input and returns output of output neurons.
     *
     * @param in input for the mlp
     * @return output of the output neurons after having processed a forward
     *         wave through the net
     */
    public double[] output(final double[] in) {
        if (in.length != getLayers()[0].getPerceptrons().length) {
            throw new IllegalArgumentException("Number of inputs must be "
                    + getLayers()[0].getPerceptrons().length);
        }
        // output for input layer
        // Initialize output
        Architecture a = getArchitecture();
        double[][] output = new double[m_layers.length][];
        output[0] = new double[a.getNrInputNeurons()];
        for (int i = 1; i < m_layers.length - 1; i++) {
            output[i] = new double[a.getNrHiddenNeurons()];
        }
        output[m_layers.length - 1] = new double[a.getNrOutputNeurons()];
        double[] sample = in;
        double sum;

        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output[i].length; j++) {
                if (i == 0) { // input neuron
                    output[i][j] = sample[j];
                } else { // non-input neuron
                    Perceptron p = this.getLayer(i).getPerceptron(j);
                    sum = 0.0;
                    for (int k = 0; k < output[i - 1].length; k++) {
                        sum += output[i - 1][k] * p.getWeight(k);
                    }
                    output[i][j] = p.activationFunction(sum - p.getThreshold());
                }
            }
        }
        return output[output.length - 1];
    }

    /**
     * Computes the output for given input.
     *
     * @param in the input values
     * @return output values of MLP.
     */
    public double[] output(final Double[] in) {
        double[] temp = new double[in.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = in[i];
        }
        return output(temp);
    }

    /**
     * Finds the winner and returns its class output value.
     *
     * @param in input for the mlp
     * @return class value
     */
    public String getClassOutput(final double[] in) {
        double[] output = output(in);
        int winnerindex = -1;
        double temp = Double.MIN_VALUE;
        for (int i = 0; i < output.length; i++) {
            if (output[i] > temp) {
                winnerindex = i;
                temp = output[i];
            }
        }
        // now winner found?
        if (winnerindex == -1) {
            return "NoWinner";
        }
        return m_layers[m_layers.length - 1].getPerceptron(winnerindex)
                .getClassValue();
    }

    /**
     * @return Architecture of the net
     */
    public Architecture getArchitecture() {
        return m_architecture;
    }

    /**
     * Sets the architecture.
     *
     * @param architecture Architecture for the net
     */
    public void setArchitecture(final Architecture architecture) {
        m_architecture = architecture;
    }

    /**
     * Stores this MLP model to config.
     *
     * @param predParams ModelContent to write into.
     */
    public void savePredictorParams(final ModelContentWO predParams) {
        predParams.addInt(MODE_KEY, m_mode);
        ModelContentWO layers = predParams.addModelContent(ALLLAYERS_KEY);
        for (int l = 0; l < m_layers.length; l++) {
            Layer mylayer = m_layers[l];
            Perceptron[] neurons = mylayer.getPerceptrons();
            ModelContentWO layerconf = layers.addModelContent("" + l);
            if (l == 0) {
                // Input Layer must be handled seperately.
                for (int n = 0; n < neurons.length; n++) {
                    ModelContentWO neuronsconf = layerconf.addModelContent(""
                            + n);
                    neuronsconf.addDouble(INPUT_KEY,
                            ((InputPerceptron)neurons[n]).getInput());
                    neuronsconf.addString(CLASSVALUE_KEY, neurons[n]
                            .getClassValue());
                }
            } else {

                for (int n = 0; n < neurons.length; n++) {
                    ModelContentWO neuronsconf = layerconf.addModelContent(""
                            + n);

                    neuronsconf.addDoubleArray(WEIGHT_KEY, neurons[n]
                            .getWeights());
                    neuronsconf.addDouble(THRESHOLD_KEY, neurons[n]
                            .getThreshold());
                    if (neurons[n].getClassValue() != null) {
                        neuronsconf.addString(CLASSVALUE_KEY, neurons[n]
                                .getClassValue());
                    }

                }
            }
        }
    }

    /**
     * @return the mode of the MLP
     * @see #CLASSIFICATION_MODE
     * @see #REGRESSION_MODE
     */
    public int getMode() {
        return m_mode;
    }

    /**
     * Sets the mode of the MLP. This can either be CLASSIFICATION_MODE or
     * REGRESSION_MODE, other values are ignored.
     *
     * @param mode the mode of the MLP
     * @see #CLASSIFICATION_MODE
     * @see #REGRESSION_MODE
     */
    public void setMode(final int mode) {
        if (mode == CLASSIFICATION_MODE || mode == REGRESSION_MODE) {
            m_mode = mode;
        }
    }

    /**
     *
     * @param predParams the ConfigObject containing the model of the mlp
     * @return a new MultiLayerPerceptron based on the config
     * @throws InvalidSettingsException if settings are incorrect
     */
    public static MultiLayerPerceptron loadPredictorParams(
            final ModelContentRO predParams) throws InvalidSettingsException {
        MultiLayerPerceptron mlp;
        Layer predecessorLayer = null;
        Layer actLayer;
        ModelContentRO alllayers = predParams.getModelContent(ALLLAYERS_KEY);
        Layer[] allLayers = new Layer[alllayers.keySet().size()];
        HashMap<DataCell, Integer> myclassmap =
            new HashMap<DataCell, Integer>();
        HashMap<String, Integer> myinputmap = new HashMap<String, Integer>();
        int l = 0;
        for (String layerKey : alllayers.keySet()) {
            ModelContentRO neuronsconf = alllayers.getModelContent(layerKey);
            if (l == 0) {
                // Input Layer
                InputPerceptron[] inputs = new InputPerceptron[neuronsconf
                        .keySet().size()];
                int n = 0;
                for (String neuron : neuronsconf.keySet()) {
                    ModelContentRO inpneurconf = neuronsconf
                            .getModelContent(neuron);

                    inputs[n] = new InputPerceptron(inpneurconf
                            .getDouble(INPUT_KEY));

                    if (inpneurconf.containsKey(CLASSVALUE_KEY)) {
                        inputs[n].setClassValue(inpneurconf
                                .getString(CLASSVALUE_KEY));
                        myinputmap
                                .put(inpneurconf.getString(CLASSVALUE_KEY), n);
                    }
                    n++;
                }
                actLayer = new InputLayer(inputs);
                allLayers[l] = actLayer;
                predecessorLayer = actLayer;
            } else {
                Perceptron[] neuronodes = new Perceptron[neuronsconf.keySet()
                        .size()];
                int n = 0;
                for (String neuron : neuronsconf.keySet()) {
                    ModelContentRO neurconf = neuronsconf
                            .getModelContent(neuron);

                    // TODO: save neuron type in config, create new neuron
                    // accordingly?
                    neuronodes[n] = new SigmoidPerceptron(neurconf
                            .getDoubleArray(WEIGHT_KEY), predecessorLayer
                            .getPerceptrons());
                    neuronodes[n].setThreshold(neurconf
                            .getDouble(THRESHOLD_KEY));
                    if (neurconf.containsKey(CLASSVALUE_KEY)) {
                        neuronodes[n].setClassValue(neurconf
                                .getString(CLASSVALUE_KEY));
                        myclassmap.put(new StringCell(neurconf
                                .getString(CLASSVALUE_KEY)), n);
                    }
                    n++;
                }
                actLayer = new HiddenLayer(predecessorLayer, neuronodes);
                allLayers[l] = actLayer;
                predecessorLayer = actLayer;
            }
            l++;
        }
        int mode = predParams.getInt(MODE_KEY);
        mlp = new MultiLayerPerceptron(allLayers);
        Architecture myarch = new Architecture(
                allLayers[0].getPerceptrons().length, allLayers.length - 1,
                allLayers[allLayers.length - 2].getPerceptrons().length,
                allLayers[allLayers.length - 1].getPerceptrons().length);
        mlp.setArchitecture(myarch);
        mlp.setClassMapping(myclassmap);
        mlp.setInputMapping(myinputmap);
        mlp.setMode(mode);
        return mlp;
    }
}
