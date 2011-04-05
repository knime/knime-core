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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.09.2008 (cebron): created
 */
package org.knime.base.node.mine.neural.mlp;

import static org.knime.core.node.port.pmml.PMMLPortObject.CDATA;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.HiddenLayer;
import org.knime.base.data.neural.InputLayer;
import org.knime.base.data.neural.InputPerceptron;
import org.knime.base.data.neural.Layer;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.Perceptron;
import org.knime.base.data.neural.SigmoidPerceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * PMMLHandler to read in a PMML file and create a KNIME
 * {@link MultiLayerPerceptron}.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLNeuralNetworkHandler extends PMMLContentHandler {

    private final Stack<String> m_elementStack = new Stack<String>();

    private int m_mlpMethod;

    private int m_counter;

    private HashMap<String, Integer> m_inputmap;

    private HashMap<String, Integer> m_idPosMap;

    private HashMap<String, Integer> m_predidPosMap;

    private String m_curPercpetronID;

    private boolean m_input;

    private Layer m_predLayer;

    private Vector<Perceptron> m_curPerceptrons;

    private Perceptron[] m_predPerceptrons;

    private double[] m_weights;

    private Vector<Layer> m_allLayers;

    private int m_curLayer;

    private HashMap<DataCell, Integer> m_classmap;

    private MultiLayerPerceptron m_mlp;

    private double m_curThreshold;


    /**
     * Creates an initialized neural network handler that can be used to
     * output the neural network model by invoking
     * {@link #addPMMLModel(org.w3c.dom.DocumentFragment, PMMLPortObjectSpec)}.
     * @param mlp the neural network
     */
    public PMMLNeuralNetworkHandler(final MultiLayerPerceptron mlp) {
        this();
        m_mlp = mlp;
    }

    /**
     * Creates a new neural network handler. The initialization has to
     * be performed by registering the handler to a parser.
     */
    public PMMLNeuralNetworkHandler() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
        // does nothing
    }

    /**
     * @return {@link MultiLayerPerceptron} created by the PMML model.
     */
    public MultiLayerPerceptron getMLP() {
        return m_mlp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endDocument() throws SAXException {
        // does nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        m_elementStack.pop();
        if (name.equals("NeuralInputs")) {
            Perceptron[] curPerceptrons =
                    new Perceptron[m_curPerceptrons.size()];
            curPerceptrons = m_curPerceptrons.toArray(curPerceptrons);
            m_predLayer = new InputLayer(curPerceptrons);
            m_allLayers.add(m_curLayer, new InputLayer(curPerceptrons));
            m_predPerceptrons = curPerceptrons;
            m_predidPosMap = new HashMap<String, Integer>(m_idPosMap);
        } else if (name.equals("Neuron")) {
            Perceptron p = new SigmoidPerceptron(m_weights, m_predPerceptrons);
            p.setThreshold(m_curThreshold);
            m_curPerceptrons.add(p);
            m_idPosMap.put(m_curPercpetronID, m_counter);
            m_counter++;
        } else if (name.equals("NeuralLayer")) {
            Perceptron[] curPerceptrons =
                    new Perceptron[m_curPerceptrons.size()];
            curPerceptrons = m_curPerceptrons.toArray(curPerceptrons);
            m_allLayers.add(m_curLayer, new HiddenLayer(m_predLayer,
                    curPerceptrons));
            m_predLayer = m_allLayers.get(m_curLayer);
            m_predPerceptrons = curPerceptrons;
            m_predidPosMap = new HashMap<String, Integer>(m_idPosMap);
        } else if (name.equals("NeuralNetwork")) {
            if (m_allLayers.size() < 3) {
                throw new SAXException("Only neural networks with 3 Layers "
                        + "supported in KNIME MLP.");
            }
            Layer[] allLayers = new Layer[m_allLayers.size()];
            allLayers = m_allLayers.toArray(allLayers);
            m_mlp = new MultiLayerPerceptron(allLayers);
            Architecture myarch =
                    new Architecture(allLayers[0].getPerceptrons().length,
                            allLayers.length - 2,
                            allLayers[1].getPerceptrons().length,
                            allLayers[allLayers.length - 1]
                                    .getPerceptrons().length);
            m_mlp.setArchitecture(myarch);
            m_mlp.setClassMapping(m_classmap);
            m_mlp.setInputMapping(m_inputmap);
            m_mlp.setMode(m_mlpMethod);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        String peek = "";
        try {
            peek = m_elementStack.peek();
        } catch (EmptyStackException ese) {
            // ok, nothing on the stack.
        }
        if (name.equals("NeuralNetwork")) {
            String actFunc = atts.getValue("activationFunction");
            if (!actFunc.equals("logistic")) {
                throw new SAXException("Only logistic activation function is "
                        + "supported in KNIME MLP.");
            }
            String normMethod = atts.getValue("normalizationMethod");
            if (normMethod != null && !normMethod.equals("none")) {
                throw new SAXException("No normalization method is "
                        + "supported in KNIME MLP.");
            }
            m_allLayers = new Vector<Layer>();
            String method = atts.getValue("functionName");
            if (method.equals("classification")) {
                m_mlpMethod = MultiLayerPerceptron.CLASSIFICATION_MODE;
            } else if (method.equals("regression")) {
                m_mlpMethod = MultiLayerPerceptron.REGRESSION_MODE;
            }
        } else if (name.equals("NeuralInputs")) {
            m_idPosMap = new HashMap<String, Integer>();
            m_input = true;
            m_curPerceptrons = new Vector<Perceptron>();
            m_inputmap = new HashMap<String, Integer>();
            m_counter = 0;
            m_curLayer = 0;
         } else if (name.equals("NeuralInput")) {
            m_curPercpetronID = atts.getValue("id");
        } else if (name.equals("FieldRef")) {
            String fieldname = atts.getValue("field");
            if (m_input) {
                Perceptron p = new InputPerceptron();
                p.setClassValue(fieldname);
                m_inputmap.put(fieldname, m_counter);
                m_curPerceptrons.add(p);
                m_idPosMap.put(m_curPercpetronID, m_counter);
                m_counter++;
            } else {
                int pos = m_idPosMap.get(m_curPercpetronID);
                m_classmap.put(new StringCell(fieldname), pos);
            }
        } else if (name.equals("NeuralLayer")) {
            m_counter = 0;
            m_idPosMap = new HashMap<String, Integer>();
            m_curLayer++;
            m_curPerceptrons = new Vector<Perceptron>();
        } else if (name.equals("Neuron")) {
            m_weights = new double[m_predPerceptrons.length];
            m_curPercpetronID = atts.getValue("id");
            m_curThreshold = Double.parseDouble(atts.getValue("bias"));
        } else if (name.equals("Con")) {
            String fromID = atts.getValue("from");
            double weight = Double.parseDouble(atts.getValue("weight"));
            int pos = m_predidPosMap.get(fromID);
            m_weights[pos] = weight;
        } else if (name.equals("NeuralOutputs")) {
            m_input = false;
            m_classmap = new HashMap<DataCell, Integer>();
        } else if (name.equals("NeuralOutput")) {
            m_curPercpetronID = atts.getValue("outputNeuron");
        } else if (name.equals("NormDiscrete")) {
            String value = atts.getValue("value");
            int pos = m_idPosMap.get(m_curPercpetronID);
            m_classmap.put(new StringCell(value), pos);
        } else if (peek.equals("DerivedField")
                && (!name.equals("FieldRef") || name.equals("NormDiscrete"))) {
            throw new SAXException("Norm " + name + " is not "
                    + "supported in KNIME MLP.");
        }
        m_elementStack.push(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getSupportedVersions() {
        TreeSet<String> versions = new TreeSet<String>();
        versions.add(PMMLPortObject.PMML_V3_0);
        versions.add(PMMLPortObject.PMML_V3_1);
        versions.add(PMMLPortObject.PMML_V3_2);
        return versions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addPMMLModelContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec) throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "modelName", CDATA,
                "KNIME Neural Network");
        if (m_mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            atts.addAttribute(null, null, "functionName", CDATA,
                    "classification");
        } else if (m_mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
            atts.addAttribute(null, null, "functionName", CDATA, "regression");
        }
        atts.addAttribute(null, null, "algorithmName", CDATA, "RProp");
        atts.addAttribute(null, null, "activationFunction", CDATA, "logistic");
        atts.addAttribute(null, null, "normalizationMethod", CDATA, "none");
        atts.addAttribute(null, null, "width", CDATA, "" + 0);
        atts.addAttribute(null, null, "numberOfLayers", CDATA, ""
                // in PMML the input layer is not counted as a layer
                // in contrast to our MLP understanding
                + (m_mlp.getNrLayers() - 1));

        handler.startElement(null, null, "NeuralNetwork", atts);
        PMMLPortObjectSpec.writeMiningSchema(spec, handler);
        addTargets(handler, m_mlp, spec);
        // input layer
        addInputLayer(handler, m_mlp);
        // hidden layers
        for (int i = 1; i < m_mlp.getNrLayers(); i++) {
            addLayer(handler, m_mlp, i);
        }
        // and output layer
        addOutputLayer(handler, m_mlp, spec);
        handler.endElement(null, null, "NeuralNetwork");
    }



    /**
     * Writes the PMML target attributes.
     *
     * @param handler to write to.
     * @param mlp the underlying {@link MultiLayerPerceptron}.
     * @param spec the port object spec
     * @throws SAXException if something goes wrong.
     */
    protected void addTargets(final TransformerHandler handler,
            final MultiLayerPerceptron mlp, final PMMLPortObjectSpec spec)
            throws SAXException {
        List<DataColumnSpec> targetCols = spec.getTargetCols();
        assert (targetCols.size() == 1) : "Only one target column allowed";
        DataColumnSpec classcol = targetCols.iterator().next();
        // open targets schema
        handler.startElement(null, null, "Targets", null);
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "field", CDATA, classcol.getName());
        if (mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
            atts.addAttribute(null, null, "optype", CDATA, "categorical");
            handler.startElement(null, null, "Target", atts);
            atts = new AttributesImpl();
            for (DataCell target : classcol.getDomain().getValues()) {
                // add target values
                atts.addAttribute(null, null, "value", CDATA,
                        ((StringValue)target).getStringValue());
                handler.startElement(null, null, "TargetValue", atts);
                handler.endElement(null, null, "TargetValue");
            }
            // close targets schema
            handler.endElement(null, null, "Target");
        } else if (mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
            atts.addAttribute(null, null, "optype", CDATA, "continuous");
            handler.startElement(null, null, "Target", atts);
            handler.endElement(null, null, "Target");
        }
        handler.endElement(null, null, "Targets");
    }

    /**
     * Writes the PMML input layer of the MLP.
     *
     * @param handler to write to.
     * @param mlp the underlying {@link MultiLayerPerceptron}.
     * @throws SAXException if something goes wrong.
     */
    protected void addInputLayer(final TransformerHandler handler,
            final MultiLayerPerceptron mlp) throws SAXException {
        Layer inputlayer = mlp.getLayer(0);
        Perceptron[] inputperceptrons = inputlayer.getPerceptrons();
        HashMap<String, Integer> inputmap = mlp.getInputMapping();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfInputs", CDATA, ""
                + inputperceptrons.length);
        handler.startElement(null, null, "NeuralInputs", atts);
        for (int i = 0; i < inputperceptrons.length; i++) {
            atts = new AttributesImpl();
            // id = layer + , + neuron number
            atts.addAttribute(null, null, "id", CDATA, 0 + "," + i);
            handler.startElement(null, null, "NeuralInput", atts);
            // search corresponding input column
            String colname = "";
            for (Entry<String, Integer> e : inputmap.entrySet()) {
                if (e.getValue().equals(i)) {
                    colname = e.getKey();
                }
            }
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "optype", CDATA, "continuous");
            atts.addAttribute(null, null, "dataType", CDATA, "double");
            handler.startElement(null, null, "DerivedField", atts);
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "field", CDATA, colname);
            handler.startElement(null, null, "FieldRef", atts);
            handler.endElement(null, null, "FieldRef");
            handler.endElement(null, null, "DerivedField");
            handler.endElement(null, null, "NeuralInput");
        }
        handler.endElement(null, null, "NeuralInputs");
    }

    /**
     * Writes a layer of the MLP.
     *
     * @param handler to write to.
     * @param mlp the underlying {@link MultiLayerPerceptron}.
     * @param layer the number of the current layer.
     * @throws SAXException if something goes wrong.
     */
    protected void addLayer(final TransformerHandler handler,
            final MultiLayerPerceptron mlp, final int layer)
            throws SAXException {
        Layer curLayer = mlp.getLayer(layer);
        Perceptron[] perceptrons = curLayer.getPerceptrons();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfNeurons", CDATA, ""
                + perceptrons.length);
        handler.startElement(null, null, "NeuralLayer", atts);
        for (int i = 0; i < perceptrons.length; i++) {
            atts = new AttributesImpl();
            atts.addAttribute(null, null, "id", CDATA, layer + "," + i);
            atts.addAttribute(null, null, "bias", CDATA, ""
                    + perceptrons[i].getThreshold());
            handler.startElement(null, null, "Neuron", atts);
            double[] weights = perceptrons[i].getWeights();
            int predLayerLength = weights.length;
            for (int j = 0; j < predLayerLength; j++) {
                atts = new AttributesImpl();
                atts.addAttribute(null, null, "from", CDATA, (layer - 1) + ","
                        + j);
                atts.addAttribute(null, null, "weight", CDATA, "" + weights[j]);
                handler.startElement(null, null, "Con", atts);
                handler.endElement(null, null, "Con");
            }
            handler.endElement(null, null, "Neuron");
        }
        handler.endElement(null, null, "NeuralLayer");
    }

    /**
     * Writes the PMML output layer of the MLP.
     *
     * @param handler to write to.
     * @param mlp the underlying {@link MultiLayerPerceptron}.
     * @param spec the port object spec
     * @throws SAXException if something goes wrong.
     */
    protected void addOutputLayer(final TransformerHandler handler,
            final MultiLayerPerceptron mlp, final PMMLPortObjectSpec spec)
            throws SAXException {
        int lastlayer = mlp.getNrLayers() - 1;
        String targetCol = spec.getTargetFields().iterator().next();
        Layer outputlayer = mlp.getLayer(lastlayer);
        Perceptron[] outputperceptrons = outputlayer.getPerceptrons();
        HashMap<DataCell, Integer> outputmap = mlp.getClassMapping();
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, "numberOfOutputs", CDATA, ""
                + outputperceptrons.length);
        handler.startElement(null, null, "NeuralOutputs", atts);
        for (int i = 0; i < outputperceptrons.length; i++) {
            atts = new AttributesImpl();
            // id = layer + , + neuron number
            atts.addAttribute(null, null, "outputNeuron", CDATA, lastlayer
                    + "," + i);
            handler.startElement(null, null, "NeuralOutput", atts);
            // search corresponding output value
            String colname = "";
            for (Entry<DataCell, Integer> e : outputmap.entrySet()) {
                if (e.getValue().equals(i)) {
                    colname = ((StringValue)e.getKey()).getStringValue();
                }
            }
            atts = new AttributesImpl();
            if (mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
                atts.addAttribute(null, null, "optype", CDATA, "categorical");
                atts.addAttribute(null, null, "dataType", CDATA, "string");
            } else if (mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                atts.addAttribute(null, null, "optype", CDATA, "continuous");
                atts.addAttribute(null, null, "dataType", CDATA, "double");
            }
            handler.startElement(null, null, "DerivedField", atts);
            atts = new AttributesImpl();
            if (mlp.getMode() == MultiLayerPerceptron.CLASSIFICATION_MODE) {
                atts.addAttribute(null, null, "field", CDATA, targetCol);
                atts.addAttribute(null, null, "value", CDATA, colname);
                handler.startElement(null, null, "NormDiscrete", atts);
                handler.endElement(null, null, "NormDiscrete");
            } else if (mlp.getMode() == MultiLayerPerceptron.REGRESSION_MODE) {
                atts.addAttribute(null, null, "field", CDATA, targetCol);
                handler.startElement(null, null, "FieldRef", atts);
                handler.endElement(null, null, "FieldRef");
            }
            handler.endElement(null, null, "DerivedField");
            handler.endElement(null, null, "NeuralOutput");
        }
        handler.endElement(null, null, "NeuralOutputs");
    }


}
