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
 * ---------------------------------------------------------------------
 *
 * History
 *   16.05.2011 (morent): created
 */
package org.knime.base.node.mine.neural.mlp;

import static org.knime.core.node.port.pmml.PMMLPortObject.CDATA;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.ACTIVATIONFUNCTION;
import org.dmg.pmml.ConDocument.Con;
import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.FieldRefDocument.FieldRef;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.NNNORMALIZATIONMETHOD;
import org.dmg.pmml.NeuralInputDocument.NeuralInput;
import org.dmg.pmml.NeuralInputsDocument.NeuralInputs;
import org.dmg.pmml.NeuralLayerDocument.NeuralLayer;
import org.dmg.pmml.NeuralNetworkDocument.NeuralNetwork;
import org.dmg.pmml.NeuralOutputDocument.NeuralOutput;
import org.dmg.pmml.NeuralOutputsDocument.NeuralOutputs;
import org.dmg.pmml.NeuronDocument.Neuron;
import org.dmg.pmml.NormDiscreteDocument.NormDiscrete;
import org.dmg.pmml.OPTYPE;
import org.dmg.pmml.PMMLDocument;
import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.HiddenLayer;
import org.knime.base.data.neural.InputLayer;
import org.knime.base.data.neural.InputPerceptron;
import org.knime.base.data.neural.Layer;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.Perceptron;
import org.knime.base.data.neural.SigmoidPerceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A NeuralNetwork translator class between KNIME and PMML.
 *
 * @author Dominik Morent, KNIME.com AG, Zurich, Switzerland
 * @author wenlin, Zementis, May 2011
 *
 */
public class PMMLNeuralNetworkTranslator implements PMMLTranslator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLNeuralNetworkTranslator.class);

    private int m_mlpMethod;

    private int m_counter;

    private HashMap<String, Integer> m_inputmap;

    private HashMap<String, Integer> m_idPosMap;

    private HashMap<String, Integer> m_predidPosMap;

    private String m_curPercpetronID;

    private Layer m_predLayer;

    private Vector<Perceptron> m_curPerceptrons;

    private Perceptron[] m_predPerceptrons;

    private double[] m_weights;

    private final Vector<Layer> m_allLayers;

    private int m_curLayer;

    private HashMap<DataCell, Integer> m_classmap;

    private MultiLayerPerceptron m_mlp;

    private double m_curThreshold;

    private DerivedFieldMapper m_nameMapper;

    /**
     * Creates an initialized neural network handler that can be used to output
     * the neural network model.
     *
     * @param mlp
     *            the neural network
     */
    public PMMLNeuralNetworkTranslator(final MultiLayerPerceptron mlp) {
        this();
        m_mlp = mlp;
    }

    /**
     * Creates a new neural network translator. The initialization has to be
     * performed by registering the handler to a parser.
     */
    public PMMLNeuralNetworkTranslator() {
        super();
        m_allLayers = new Vector<Layer>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        NeuralNetwork[] models = pmmlDoc.getPMML().getNeuralNetworkArray();
        if (models.length == 0) {
            throw new IllegalArgumentException("No neural network model"
                + " provided.");
        } else if (models.length > 1) {
            LOGGER.warn("Multiple neural network models found. "
                + "Only the first model is considered.");
        }
        NeuralNetwork nnModel = models[0];

        // ------------------------------
        // initiate Neural Input
        initInputLayer(nnModel);

        // -------------------------------
        // initiate Hidden Layer
        initiateHiddenLayers(nnModel);

        // -------------------------------
        // initiate Final Layer
        initiateFinalLayer(nnModel);

        // --------------------------------
        // initiate Neural Outputs
        initiateNeuralOutputs(nnModel);

        // --------------------------------
        // initiate Neural Network properties

        ACTIVATIONFUNCTION.Enum actFunc = nnModel.getActivationFunction();
        NNNORMALIZATIONMETHOD.Enum normMethod =
            nnModel.getNormalizationMethod();
        if (ACTIVATIONFUNCTION.LOGISTIC != actFunc) {
            LOGGER.error("Only logistic activation function is "
                + "supported in KNIME MLP.");
        }
        if (NNNORMALIZATIONMETHOD.NONE != normMethod) {
            LOGGER.error("No normalization method is "
                + "supported in KNIME MLP.");
        }

        MININGFUNCTION.Enum functionName = nnModel.getFunctionName();
        if (MININGFUNCTION.CLASSIFICATION == functionName) {
            m_mlpMethod = MultiLayerPerceptron.CLASSIFICATION_MODE;
        } else if (MININGFUNCTION.REGRESSION == functionName) {
            m_mlpMethod = MultiLayerPerceptron.REGRESSION_MODE;
        }

        if (m_allLayers.size() < 3) {
            throw new IllegalArgumentException(
                  "Only neural networks with 3 Layers supported in KNIME MLP.");
        }

        Layer[] allLayers = new Layer[m_allLayers.size()];
        allLayers = m_allLayers.toArray(allLayers);
        m_mlp = new MultiLayerPerceptron(allLayers);
        Architecture myarch =
            new Architecture(allLayers[0].getPerceptrons().length,
                    allLayers.length - 2, allLayers[1].getPerceptrons().length,
                    allLayers[allLayers.length - 1].getPerceptrons().length);
        m_mlp.setArchitecture(myarch);
        m_mlp.setClassMapping(m_classmap);
        m_mlp.setInputMapping(m_inputmap);
        m_mlp.setMode(m_mlpMethod);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);

        NeuralNetwork nnModel = pmmlDoc.getPMML().addNewNeuralNetwork();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, nnModel);

        if (m_mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            nnModel.setFunctionName(MININGFUNCTION.CLASSIFICATION);
        } else if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
            nnModel.setFunctionName(MININGFUNCTION.REGRESSION);
        }

        nnModel.setAlgorithmName("RProp");
        nnModel.setActivationFunction(ACTIVATIONFUNCTION.LOGISTIC);
        nnModel.setNormalizationMethod(NNNORMALIZATIONMETHOD.NONE);
        nnModel.setWidth(0.0);
        nnModel.setNumberOfLayers(BigInteger.valueOf(m_mlp.getNrLayers() - 1));

        // add input layer
        addInputLayer(nnModel, m_mlp);

        // add hidden & final layers
        for (int i = 1; i < m_mlp.getNrLayers(); i++) {
            addLayer(nnModel, m_mlp, i);
        }

        // add output layer
        addOutputLayer(nnModel, m_mlp, spec);

        return NeuralNetwork.type;
    }

    /**
     * @param nnModel the PMML neural network model
     */
    private void initInputLayer(final NeuralNetwork nnModel) {
        NeuralInputs neuralInputs = nnModel.getNeuralInputs();
        m_idPosMap = new HashMap<String, Integer>();
        m_curPerceptrons = new Vector<Perceptron>();
        m_inputmap = new HashMap<String, Integer>();
        m_counter = 0;
        m_curLayer = 0;
        for (NeuralInput ni : neuralInputs.getNeuralInputArray()) {
            m_curPercpetronID = ni.getId();
            String fieldName =
                m_nameMapper.getColumnName(ni.getDerivedField().getFieldRef()
                        .getField());
            Perceptron p = new InputPerceptron();
            p.setClassValue(fieldName);
            m_inputmap.put(fieldName, m_counter);
            m_curPerceptrons.add(p);
            m_idPosMap.put(m_curPercpetronID, m_counter);
            m_counter++;
        }

        Perceptron[] curPerceptrons = new Perceptron[m_curPerceptrons.size()];
        curPerceptrons = m_curPerceptrons.toArray(curPerceptrons);
        m_predLayer = new InputLayer(curPerceptrons);
        m_allLayers.add(m_curLayer, new InputLayer(curPerceptrons));
        m_predPerceptrons = curPerceptrons;
        m_predidPosMap = new HashMap<String, Integer>(m_idPosMap);
    }

    /**
     * @param nnModel the PMML neural network model
     */
    private void initiateHiddenLayers(final NeuralNetwork nnModel) {
        for (int i = 0; i < nnModel.getNeuralLayerArray().length - 1; i++) {
            NeuralLayer hiddenLayer = nnModel.getNeuralLayerArray(i);
            m_counter = 0;
            m_idPosMap = new HashMap<String, Integer>();
            m_curLayer++;
            m_curPerceptrons = new Vector<Perceptron>();
            for (Neuron neuron : hiddenLayer.getNeuronArray()) {
                m_weights = new double[m_predPerceptrons.length];
                m_curPercpetronID = neuron.getId();
                m_curThreshold = -1 * neuron.getBias();
                for (Con con : neuron.getConArray()) {
                    String fromID = con.getFrom();
                    double weight = con.getWeight();
                    int pos = m_predidPosMap.get(fromID);
                    m_weights[pos] = weight;
                }
                Perceptron p =
                    new SigmoidPerceptron(m_weights, m_predPerceptrons);
                p.setThreshold(m_curThreshold);
                m_curPerceptrons.add(p);
                m_idPosMap.put(m_curPercpetronID, m_counter);
                m_counter++;
            }

            Perceptron[] curPerceptrons =
                new Perceptron[m_curPerceptrons.size()];
            curPerceptrons = m_curPerceptrons.toArray(curPerceptrons);
            m_allLayers.add(m_curLayer, new HiddenLayer(m_predLayer,
                    curPerceptrons));
            m_predLayer = m_allLayers.get(m_curLayer);
            m_predPerceptrons = curPerceptrons;
            m_predidPosMap = new HashMap<String, Integer>(m_idPosMap);
        }
    }

    /**
     * @param nnModel
     *            the PMML neural network model
     */
    private void initiateFinalLayer(final NeuralNetwork nnModel) {
        NeuralLayer hiddenLayer = nnModel.getNeuralLayerArray(
                nnModel.getNeuralLayerArray().length - 1);
        m_counter = 0;
        m_idPosMap = new HashMap<String, Integer>();
        m_curLayer++;
        m_curPerceptrons = new Vector<Perceptron>();
        for (Neuron neuron : hiddenLayer.getNeuronArray()) {
            m_weights = new double[m_predPerceptrons.length];
            m_curPercpetronID = neuron.getId();
            m_curThreshold = -1 * neuron.getBias();
            for (Con con : neuron.getConArray()) {
                String fromID = con.getFrom();
                double weight = con.getWeight();
                int pos = m_predidPosMap.get(fromID);
                m_weights[pos] = weight;
            }
            Perceptron p = new SigmoidPerceptron(m_weights, m_predPerceptrons);
            p.setThreshold(m_curThreshold);
            m_curPerceptrons.add(p);
            m_idPosMap.put(m_curPercpetronID, m_counter);
            m_counter++;
        }

        Perceptron[] curPerceptrons = new Perceptron[m_curPerceptrons.size()];
        curPerceptrons = m_curPerceptrons.toArray(curPerceptrons);
        m_allLayers.add(m_curLayer,
                new HiddenLayer(m_predLayer, curPerceptrons));
        m_predLayer = m_allLayers.get(m_curLayer);
        m_predPerceptrons = curPerceptrons;
        m_predidPosMap = new HashMap<String, Integer>(m_idPosMap);
    }

    /**
     * @param nnModel
     *            the PMML neural network model
     */
    private void initiateNeuralOutputs(final NeuralNetwork nnModel) {
        NeuralOutputs neuralOutputs = nnModel.getNeuralOutputs();
        m_classmap = new HashMap<DataCell, Integer>();
        for (NeuralOutput no : neuralOutputs.getNeuralOutputArray()) {
            m_curPercpetronID = no.getOutputNeuron();
            DerivedField df = no.getDerivedField();
            if (df.isSetNormDiscrete()) {
                String value = df.getNormDiscrete().getValue();
                int pos = m_idPosMap.get(m_curPercpetronID);
                m_classmap.put(new StringCell(value), pos);
            } else if (df.isSetFieldRef()) {
                int pos = m_idPosMap.get(m_curPercpetronID);
                m_classmap
                        .put(new StringCell(df.getFieldRef().getField()), pos);
            } else {
                LOGGER.error("The expression is not supported in KNIME MLP.");
            }
        }
    }

    /**
     * Writes the PMML input layer of the MLP.
     *
     * @param nnModel
     *            the Neural Network model.
     * @param mlp
     *            the underlying {@link MultiLayerPerceptron}.
     */
    protected void addInputLayer(final NeuralNetwork nnModel,
            final MultiLayerPerceptron mlp) {
        Layer inputlayer = mlp.getLayer(0);
        Perceptron[] inputperceptrons = inputlayer.getPerceptrons();
        HashMap<String, Integer> inputmap = mlp.getInputMapping();

        NeuralInputs neuralInputs = nnModel.addNewNeuralInputs();
        neuralInputs.setNumberOfInputs(BigInteger
                .valueOf(inputperceptrons.length));

        for (int i = 0; i < inputperceptrons.length; i++) {
            NeuralInput neuralInput = neuralInputs.addNewNeuralInput();
            neuralInput.setId(0 + "," + i);

            // search corresponding input column
            String colname = "";
            for (Entry<String, Integer> e : inputmap.entrySet()) {
                if (e.getValue().equals(i)) {
                    colname = e.getKey();
                }
            }

            DerivedField df = neuralInput.addNewDerivedField();
            df.setOptype(OPTYPE.CONTINUOUS);
            df.setDataType(DATATYPE.DOUBLE);

            FieldRef fieldRef = df.addNewFieldRef();
            fieldRef.setField(m_nameMapper.getDerivedFieldName(colname));
        }
    }

    /**
     * Writes a layer of the MLP.
     *
     * @param nnModel
     *            the NeuralNetwork model.
     * @param mlp
     *            the underlying {@link MultiLayerPerceptron}.
     * @param layer
     *            the number of the current layer.
     */
    protected void addLayer(final NeuralNetwork nnModel,
            final MultiLayerPerceptron mlp, final int layer) {
        Layer curLayer = mlp.getLayer(layer);
        Perceptron[] perceptrons = curLayer.getPerceptrons();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfNeurons", CDATA, ""
                + perceptrons.length);
        NeuralLayer neuralLayer = nnModel.addNewNeuralLayer();
        for (int i = 0; i < perceptrons.length; i++) {
            Neuron neuron = neuralLayer.addNewNeuron();
            neuron.setId(layer + "," + i);
            neuron.setBias(-1 * perceptrons[i].getThreshold());

            double[] weights = perceptrons[i].getWeights();
            int predLayerLength = weights.length;
            for (int j = 0; j < predLayerLength; j++) {
                Con con = neuron.addNewCon();
                con.setFrom((layer - 1) + "," + j);
                con.setWeight(weights[j]);
            }
        }
    }

    /**
     * Writes the PMML output layer of the MLP.
     *
     * @param nnModel
     *            the neural network model.
     * @param mlp
     *            the underlying {@link MultiLayerPerceptron}.
     * @param spec
     *            the port object spec
     */
    protected void addOutputLayer(final NeuralNetwork nnModel,
            final MultiLayerPerceptron mlp, final PMMLPortObjectSpec spec) {
        int lastlayer = mlp.getNrLayers() - 1;
        String targetCol = spec.getTargetFields().iterator().next();
        Layer outputlayer = mlp.getLayer(lastlayer);
        Perceptron[] outputperceptrons = outputlayer.getPerceptrons();
        HashMap<DataCell, Integer> outputmap = mlp.getClassMapping();

        NeuralOutputs neuralOuts = nnModel.addNewNeuralOutputs();
        neuralOuts.setNumberOfOutputs(BigInteger
                .valueOf(outputperceptrons.length));

        for (int i = 0; i < outputperceptrons.length; i++) {
            NeuralOutput neuralOutput = neuralOuts.addNewNeuralOutput();
            neuralOutput.setOutputNeuron(lastlayer + "," + i);

            // search corresponding output value
            String colname = "";
            for (Entry<DataCell, Integer> e : outputmap.entrySet()) {
                if (e.getValue().equals(i)) {
                    colname = ((StringValue) e.getKey()).getStringValue();
                }
            }

            DerivedField df = neuralOutput.addNewDerivedField();
            df.setOptype(OPTYPE.CATEGORICAL);
            df.setDataType(DATATYPE.STRING);

            if (mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
                df.setOptype(OPTYPE.CATEGORICAL);
                df.setDataType(DATATYPE.STRING);
            } else if (mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                df.setOptype(OPTYPE.CONTINUOUS);
                df.setDataType(DATATYPE.DOUBLE);
            }

            if (mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
                NormDiscrete normDiscrete = df.addNewNormDiscrete();
                normDiscrete.setField(targetCol);
                normDiscrete.setValue(colname);
            } else if (mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                FieldRef fieldRef = df.addNewFieldRef();
                fieldRef.setField(targetCol);
            }
        }
    }

    /**
     * @return the mlp
     */
    public MultiLayerPerceptron getMLP() {
        return m_mlp;
    }
}
