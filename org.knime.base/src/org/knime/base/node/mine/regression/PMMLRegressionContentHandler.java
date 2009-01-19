/* ------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Aug 22, 2008 (wiswedel): created
 */
package org.knime.base.node.mine.regression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.mine.regression.PMMLRegressionPortObject.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionPortObject.RegressionTable;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PMMLRegressionContentHandler extends PMMLContentHandler {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PMMLRegressionContentHandler.class);

    private Stack<PMMLContentHandler> m_contentHandlerStack =
        new Stack<PMMLContentHandler>();
    private RegressionTable m_regressionTable;
    private PMMLPortObjectSpec m_spec;
    private String m_modelName;
    private String m_algorithmName;

    /**
     * Creates a new PMML content handler for regression models.
     *
     * @param spec the spec for the regression model
     */
    public PMMLRegressionContentHandler(final PMMLPortObjectSpec spec) {
        if (spec == null) {
            throw new NullPointerException("Arg must not be null");
        }
        m_spec = spec;
    }

    /**
     * Creates a new PMML content handler for regression models based on an
     * existing port object.
     *
     * @param po a PMML regression port object
     */
    public PMMLRegressionContentHandler(final PMMLRegressionPortObject po) {
        m_modelName = po.getModelName();
        m_regressionTable = po.getRegressionTable();
        m_spec = po.getSpec();
    }

    /**
     * Checks if the internal regression has been set and if all predictors
     * are assigned. If not an {@link IllegalStateException} is thrown.
     *
     * @throws IllegalStateException if required information is missing
     */
    public void checkValidity() {
        if (m_regressionTable == null) {
            throw new IllegalStateException("No regression table set");
        }
        for (NumericPredictor p : m_regressionTable.getVariables()) {
            if (p == null) {
                throw new IllegalStateException("NumericPredictor is null");
            }
        }
    }

    /**
     * Returns the model's name.
     *
     * @return the model's name
     */
    public final String getModelName() {
        return m_modelName;
    }

    /**
     * @param modelName the modelName to set
     */
    public final void setModelName(final String modelName) {
        m_modelName = modelName;
    }

    /**
     * @return the regressionTable
     */
    public final RegressionTable getRegressionTable() {
        return m_regressionTable;
    }

    /** Get the response column name as said in the spec or "Response" if none
     * is set.
     * @return target field name.
     */
    public String getTargetField() {
        for (String s : m_spec.getTargetFields()) {
            return s;
        }
        return "Response";
    }

    /**
     * Checks if the given target field name exists in this model.
     *
     * @param targetFieldName a target field name
     */
    public void checkTargetField(final String targetFieldName) {
        if (!getTargetField().equals(targetFieldName)) {
            LOGGER.warn("Non matching target field name or no target "
                    + "field in mining schema");
        }
    }

    /**
     * @param regressionTable the regressionTable to set
     */
    public final void setRegressionTable(
            final RegressionTable regressionTable) {
        m_regressionTable = regressionTable;
        List<NumericPredictor> preds = m_regressionTable.getVariables();
        Set<String> regressorCols = m_spec.getLearningFields();
        for (NumericPredictor p : preds) {
            if (!regressorCols.contains(p.getName())) {
                LOGGER.warn("Regression column not found in spec");
            }
        }
        for (NumericPredictor p : preds) {
            if (!regressorCols.contains(p.getName())) {
                LOGGER.warn("Non matching variables in Spec and "
                        + "RegressionTable");
                return;
            }
        }
    }

    /**
     * @return the algorithmName
     */
    public final String getAlgorithmName() {
        return m_algorithmName;
    }

    /**
     * @param algorithmName the algorithmName to set
     */
    public final void setAlgorithmName(final String algorithmName) {
        m_algorithmName = algorithmName;
    }

    /**
     * Writes the PMML regression model to the given handler.
     *
     * @param h a transform handler
     * @throws SAXException if anything goes wrong while serializing the model
     */
    public void writePMMLRegressionModel(final TransformerHandler h)
        throws SAXException {
        AttributesImpl a = new AttributesImpl();
        a.addAttribute("", "", "functionName", "CDATA", "regression");
        if (m_algorithmName != null && m_algorithmName.length() > 0) {
            a.addAttribute("", "", "algorithmName", "CDATA", m_algorithmName);
        }
        a.addAttribute("", "", "targetFieldName", "CDATA", getTargetField());
        h.startElement("", "", "RegressionModel", a);
        PMMLPortObjectSpec.writeMiningSchema(m_spec, h);
        addRegressionTable(h);
        h.endElement("", "", "RegressionModel");
    }

    private void addRegressionTable(final TransformerHandler h)
    throws SAXException {
        AttributesImpl a = new AttributesImpl();
        String interceptS = Double.toString(m_regressionTable.getIntercept());
        a.addAttribute("", "", "intercept", "CDATA", interceptS);
        h.startElement("", "", "RegressionTable", a);
        for (NumericPredictor p : m_regressionTable.getVariables()) {
            a = new AttributesImpl();
            a.addAttribute("", "", "name", "CDATA", p.getName());
            if (p.getExponent() != 1) { // "1" is default in schema
                String exponentS = Integer.toString(p.getExponent());
                a.addAttribute("", "", "exponent", "CDATA", exponentS);
            }
            String coefficientS = Double.toString(p.getCoefficient());
            a.addAttribute("", "", "coefficient", "CDATA", coefficientS);
            h.startElement("", "", "NumericPredictor", a);
            h.endElement("", "", "NumericPredictor");
        }
        h.endElement("", "", "RegressionTable");
    }

    /** {@inheritDoc} */
    @Override
    public void characters(final char[] ch, final int start, final int length)
            throws SAXException {
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {

    }

    /** {@inheritDoc} */
    @Override
    public void endElement(final String uri, final String localName,
            final String name) throws SAXException {
        if (!m_contentHandlerStack.isEmpty()) {
            m_contentHandlerStack.peek().endElement(
                    uri, localName, name);
        } else if ("RegressionModel".equals(name)) {
            assert m_contentHandlerStack.isEmpty();
        } else if ("MiningSchema".equals(name)) {
            assert false : "MiningSchema must be terminated elsewhere";
        } else if ("RegressionTable".equals(name)) {
            assert false : "RegressionTable must be terminated elsewhere";
        } else if ("Output".equals(name)
                || "ModelStats".equals(name)
                || "Targets".equals(name)
                || "LocalTransformations".equals(name)
                || "ModelVerification".equals(name)
                || "Extension".equals(name)) {
            // ignored
        }
    }

    /** {@inheritDoc} */
    @Override
    public void startElement(final String uri, final String localName,
            final String name, final Attributes atts) throws SAXException {
        if (!m_contentHandlerStack.isEmpty()) {
            m_contentHandlerStack.peek().startElement(
                    uri, localName, name, atts);
        } else if ("RegressionModel".equals(name)) {
            assert m_contentHandlerStack.isEmpty();
            m_modelName = atts.getValue("modelName");
            String funcName = getValue(atts, "functionName", "RegressionModel");
            if ("regression".equals(funcName)) {
                // the very only one we support
            } else if ("classification".equals(funcName)) {
                throw new SAXException(
                        "classification currently not supported");
            } else {
                throw new SAXException("Unknown function name: " + funcName);
            }
            String targetFieldName = atts.getValue("targetFieldName");
            checkTargetField(targetFieldName);
            String normalizationMethod =
                getValue(atts, "normalizationMethod", "RegressionModel");
            if (!"none".equals(normalizationMethod)) {
                throw new SAXException("Normalization currently not "
                        + "supported: " + normalizationMethod);
            }
            m_algorithmName = atts.getValue("algorithmName");
        } else if ("MiningSchema".equals(name)) {
            m_contentHandlerStack.push(
                    new PMMLMiningSchemaHandler(m_contentHandlerStack));
        } else if ("RegressionTable".equals(name)) {
            m_contentHandlerStack.push(new PMMLRegressionTableHandler(
                    this, m_contentHandlerStack, atts));
        } else if ("Output".equals(name)
                || "ModelStats".equals(name)
                || "Targets".equals(name)
                || "ModelVerification".equals(name)
                || "Extension".equals(name)) {
            LOGGER.warn("Skipping unknown element " + name);
        } else if ("LocalTransformations".equals(name)) {
            throw new SAXException("LocalTransformation currently not "
                    + "supported.");
        }

    }

    private static final class PMMLMiningSchemaHandler
        extends PMMLContentHandler {

        private final Stack<PMMLContentHandler> m_contentHandlerStack;

        public PMMLMiningSchemaHandler(
                final Stack<PMMLContentHandler> contentHandlerStack) {
            m_contentHandlerStack = contentHandlerStack;
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String name) throws SAXException {
            if ("MiningSchema".equals(name)) {
                // this element ends
                m_contentHandlerStack.pop();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String name, final Attributes atts) throws SAXException {
        }
    } // class PMMLMiningSchemaHandler

    private static final class PMMLRegressionTableHandler
        extends PMMLContentHandler {

        private final Stack<PMMLContentHandler> m_contentHandlerStack;
        private final PMMLRegressionContentHandler m_parent;
        private final double m_intercept;
        private final ArrayList<NumericPredictor> m_numPredictorList;

        public PMMLRegressionTableHandler(
                final PMMLRegressionContentHandler parent,
                final Stack<PMMLContentHandler> contentHandlerStack,
                final Attributes atts) throws SAXException  {
            m_contentHandlerStack = contentHandlerStack;
            m_parent = parent;
            m_numPredictorList = new ArrayList<NumericPredictor>();
            String interceptS = getValue(atts, "intercept", "RegressionTable");
            try {
                m_intercept = Double.parseDouble(interceptS);
            } catch (NumberFormatException e) {
                throw new SAXException("Unable to parse intercept value "
                        + "in RegressionTable: " + interceptS, e);
            }
        }

        public void addNumericPredictor(final NumericPredictor np) {
            m_numPredictorList.add(np);
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {

        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {

        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String name) throws SAXException {
            if ("Extension".equals(name)) {
                // unknown sub element, ignore
            } else if ("RegressionTable".equals(name)) {
                // end of this element (and its life cycle)
                NumericPredictor[] preds = m_numPredictorList.toArray(
                        new NumericPredictor[m_numPredictorList.size()]);
                RegressionTable t = new RegressionTable(m_intercept, preds);
                m_parent.setRegressionTable(t);
                PMMLContentHandler contHdl = m_contentHandlerStack.pop();
                assert contHdl == this : "Invalid content handler stack";
            } else {
                throw new SAXException("Invalid end of sub element: " + name);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String name, final Attributes atts) throws SAXException {
            if ("Extension".equals(name)) {
                LOGGER.debug("Skipping unknown extension in RegressionTable: "
                        + name);
            } else if ("NumericPredictor".equals(name)) {
                PMMLContentHandler pch = new PMMLNumericPredictorContentHandler(
                        this, m_contentHandlerStack, atts);
                m_contentHandlerStack.push(pch);
            } else if ("CategoricalPredictor".equals(name)) {
                throw new SAXException(
                        "CategoricalPredictor currently not supported");
            } else if ("PredictorTerm".equals(name)) {
                throw new SAXException("PredictorTerm currently not supported");
            } else {
                throw new SAXException(
                        "Unknown xml element in RegressionTable: " + name);
            }

        }
    } // class PMMLRegressionTableContentHandler

    private static final class PMMLNumericPredictorContentHandler
        extends PMMLContentHandler {

        private final PMMLRegressionTableHandler m_parent;
        private final Stack<PMMLContentHandler> m_contentHandlerStack;
        private final NumericPredictor m_numPredictor;

        public PMMLNumericPredictorContentHandler(
                final PMMLRegressionTableHandler parent,
                final Stack<PMMLContentHandler> contentHandlerStack,
                final Attributes atts) throws SAXException {
            m_contentHandlerStack = contentHandlerStack;
            m_parent = parent;
            String name = getValue(atts, "name", "NumericPredictor");
            String exponentS = atts.getValue("exponent");
            if (exponentS == null) {
                exponentS = "1";
            }
            String valueS = getValue(atts, "coefficient", "NumericPredictor");
            int exponent;
            double value;
            try {
                exponent = Integer.parseInt(exponentS);
            } catch (NumberFormatException e) {
                throw new SAXException("Can't parse exponent in "
                        + "NumericPredictor: " + exponentS, e);
            }
            try {
                value = Double.parseDouble(valueS);
            } catch (NumberFormatException e) {
                throw new SAXException("Can't parse coefficient in "
                        + "NumericPredictor: " + valueS, e);
            }
            m_numPredictor = new NumericPredictor(name, exponent, value);
        }

        /** {@inheritDoc} */
        @Override
        public void characters(final char[] ch, final int start,
                final int length) throws SAXException {

        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
        }

        /** {@inheritDoc} */
        @Override
        public void endElement(final String uri, final String localName,
                final String name) throws SAXException {
            if ("Extension".equals(name)) {
                // sub element, ignore
            } else if ("NumericPredictor".equals(name)) {
                PMMLContentHandler me = m_contentHandlerStack.pop();
                assert me == this : "Invalid content handler stack";
                m_parent.addNumericPredictor(m_numPredictor);
            } else {
                throw new SAXException("Invalid element end: " + name);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void startElement(final String uri, final String localName,
                final String name, final Attributes atts) throws SAXException {
            if ("Extension".equals(name)) {
                LOGGER.debug("Skipping unknown extension in numeric predictor: "
                        + name);
            } else {
                throw new SAXException("NumericPredictor must not have "
                        + "child xml element: " + name);
            }
        }

    } // class NumericPredictorContentHandler


    private static final String getValue(final Attributes atts,
            final String attName, final String elementName)
        throws SAXException {
        String value = atts.getValue(attName);
        if (value == null || value.length() == 0) {
            throw new SAXException("No value for attribute \"" + attName
                    + "\" in xml element \"" + elementName + "\"");
        }
        return value;
    }
}
