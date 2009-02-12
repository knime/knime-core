/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.09.2008 (cebron): created
 */
package org.knime.base.node.mine.neural.mlp;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import org.knime.base.data.neural.Architecture;
import org.knime.base.data.neural.HiddenLayer;
import org.knime.base.data.neural.InputLayer;
import org.knime.base.data.neural.InputPerceptron;
import org.knime.base.data.neural.Layer;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.Perceptron;
import org.knime.base.data.neural.SigmoidPerceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * PMMLHandler to read in a PMML file and create a KNIME
 * {@link MultiLayerPerceptron}.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLNeuralNetworkHandler extends PMMLContentHandler {

    private Stack<String> m_elementStack = new Stack<String>();

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

}
