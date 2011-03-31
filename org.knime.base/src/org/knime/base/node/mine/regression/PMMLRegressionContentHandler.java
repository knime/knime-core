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
 *   Aug 22, 2008 (wiswedel): created
 */
package org.knime.base.node.mine.regression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.transform.sax.TransformerHandler;


import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLContentHandler;
import org.knime.core.node.port.pmml.PMMLPortObject;
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
    // TODO No longer needed?
    //private String m_writeVersion;



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
     * @param writeVersion The PMML version to write
     */
    public PMMLRegressionContentHandler(String modelName,
//    		final String writeVersion,
    		RegressionTable regressionTable,
    		PMMLPortObjectSpec spec) {
        m_modelName = modelName;
//        m_writeVersion = writeVersion;
        m_regressionTable = regressionTable;
        m_spec = spec;
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
        List<String> regressorCols = m_spec.getLearningFields();
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

    protected  void addModelPMMLContent(final TransformerHandler handler,
            final PMMLPortObjectSpec spec) throws SAXException {
          AttributesImpl a = new AttributesImpl();
        a.addAttribute("", "", "functionName", "CDATA", "regression");
        if (m_algorithmName != null && m_algorithmName.length() > 0) {
            a.addAttribute("", "", "algorithmName", "CDATA", m_algorithmName);
        }
        a.addAttribute("", "", "targetFieldName", "CDATA", getTargetField());
        handler.startElement("", "", "RegressionModel", a);
        PMMLPortObjectSpec.writeMiningSchema(m_spec, handler);
        //TODO implement the local tranformations
//        m_port.writeLocalTransformations(h);
        addRegressionTable(handler);
        handler.endElement("", "", "RegressionModel");
    }

    private  void addRegressionTable(final TransformerHandler h)
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
        // ignore character data
    }

    /** {@inheritDoc} */
    @Override
    public void endDocument() throws SAXException {
        // nothing to do here
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
            // ignore character data
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
            // nothing to do here
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
         // nothing to do here
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
            // ignore character data
        }

        /** {@inheritDoc} */
        @Override
        public void endDocument() throws SAXException {
            // nothing to do here
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
     * This class represents a single numeric predictor with its name (usually
     * the column name it is responsible for), the exponent and the coefficient.
     *
     * @author Bernd Wiswedel, University of Konstanz
     */
    public static final class NumericPredictor {
        private final String m_name;

        private final int m_exponent;

        private final double m_coefficient;

        /**
         * Creates a new numeric predictor.
         *
         * @param name the predictor's name (usually the column name)
         * @param exponent the exponent
         * @param coefficient the coefficient
         */
        public NumericPredictor(final String name, final int exponent,
                final double coefficient) {
            m_name = name;
            m_exponent = exponent;
            m_coefficient = coefficient;
        }

        /** @return the name */
        public String getName() {
            return m_name;
        }

        /** @return the exponent */
        public int getExponent() {
            return m_exponent;
        }

        /** @return the value */
        public double getCoefficient() {
            return m_coefficient;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_name + " (coefficient = "
                    + DoubleFormat.formatDouble(m_coefficient)
                    + ", exponent = " + m_exponent + ")";
        }
    }

    /**
     * This table wraps a polynomial regression formula for use inside a PMML
     * model.
     *
     * @author Bernd Wiswedel, University of Konstanz
     */
    public static final class RegressionTable {
        private final double m_intercept;

        private final List<NumericPredictor> m_variables;

        /**
         * Creates a new regression table.
         *
         * @param intercept the constant intercept of the regression formula
         * @param variables the regression variables
         */
        public RegressionTable(final double intercept,
                final NumericPredictor[] variables) {
            m_intercept = intercept;
            m_variables =
                    Collections.unmodifiableList(Arrays.asList(variables));
        }

        /** @return the intercept */
        public double getIntercept() {
            return m_intercept;
        }

        /** @return the variables */
        public List<NumericPredictor> getVariables() {
            return m_variables;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "RegressionTable: " + m_variables.size() + " variables";
        }
    }
}
