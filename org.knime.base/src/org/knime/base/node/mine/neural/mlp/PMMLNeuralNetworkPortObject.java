/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 *   15.08.2008 (cebron): created
 */
package org.knime.base.node.mine.neural.mlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.data.neural.Layer;
import org.knime.base.data.neural.MultiLayerPerceptron;
import org.knime.base.data.neural.Perceptron;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A {@link PMMLPortObject} responsible for transforming a MultiLayer Perceptron
 * (MLP) into a PMML file.
 *
 * @author cebron, University of Konstanz
 */
public class PMMLNeuralNetworkPortObject extends PMMLPortObject {

    /* The MultiLayerPerceptron */
    private MultiLayerPerceptron m_mlp;

    /**
     * PMML Neural Network port type.
     */
    public static final PortType TYPE =
            new PortType(PMMLNeuralNetworkPortObject.class);

    /**
     * Empty constructor.
     */
    public PMMLNeuralNetworkPortObject() {
        // I'm empty.
    }

    /**
     * Constructor.
     *
     * @param spec the {@link PMMLPortObjectSpec} of the training table.
     * @param mlp the {@link MultiLayerPerceptron} from KNIME to be
     * written.
     */
    public PMMLNeuralNetworkPortObject(final PMMLPortObjectSpec spec,
            final MultiLayerPerceptron mlp) {
        super(spec);
        m_mlp = mlp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {
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
                + m_mlp.getNrLayers());

        handler.startElement(null, null, "NeuralNetwork", atts);
        PMMLPortObjectSpec.writeMiningSchema(getSpec(), handler);
        addTargets(handler, m_mlp);
        // input layer
        addInputLayer(handler, m_mlp);
        // hidden layers
        for (int i = 1; i < m_mlp.getNrLayers(); i++) {
            addLayer(handler, m_mlp, i);
        }
        // and output layer
        addOutputLayer(handler, m_mlp);
        handler.endElement(null, null, "NeuralNetwork");
    }

    /**
     * Writes the PMML target attributes.
     *
     * @param handler to write to.
     * @param mlp the underlying {@link MultiLayerPerceptron}.
     * @throws SAXException if something goes wrong.
     */
    protected void addTargets(final TransformerHandler handler,
            final MultiLayerPerceptron mlp) throws SAXException {
        Set<DataColumnSpec> targetCols = getSpec().getTargetCols();
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
     * @throws SAXException if something goes wrong.
     */
    protected void addOutputLayer(final TransformerHandler handler,
            final MultiLayerPerceptron mlp) throws SAXException {
        int lastlayer = mlp.getNrLayers() - 1;
        String targetCol = getSpec().getTargetFields().iterator().next();
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

    /** {@inheritDoc} */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec, final InputStream in,
            final String version)
            throws ParserConfigurationException, SAXException, IOException {
        PMMLNeuralNetworkHandler hdl = new PMMLNeuralNetworkHandler();
        super.addPMMLContentHandler("MLP", hdl);
        super.loadFrom(spec, in, version);
        hdl = (PMMLNeuralNetworkHandler)super.getPMMLContentHandler("MLP");
        m_mlp = hdl.getMLP();
    }

    /**
     * @return the {@link MultiLayerPerceptron}.
     */
    public MultiLayerPerceptron getMLP() {
        return m_mlp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        String mode = (m_mlp.getMode()
                == MultiLayerPerceptron.CLASSIFICATION_MODE) ? "classification"
                        : "regression";
        StringBuffer buffer = new StringBuffer();
        buffer.append("PMML MLP Object.\n");
        buffer.append("Mode: " + mode + "\n");
        buffer.append("Number of Layers: " + m_mlp.getNrLayers());
        return buffer.toString();
    }

}
