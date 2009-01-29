/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.pmml.PMMLModelType;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;

/**
 * This class wraps a PMML regression model that can then be transferred from
 * one node to the other.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PMMLRegressionPortObject extends PMMLPortObject {
    /** The port object's type. */
    public static final PortType TYPE =
            new PortType(PMMLRegressionPortObject.class);

    /** */
    public PMMLRegressionPortObject() {
    }

    private RegressionTable m_regressionTable;

    private String m_modelName;

    /**
     * Creates a new PMML port object for polynomial regression.
     *
     * @param spec the objects spec
     * @param p the content handler that receives SAX parsing events upon
     * reading a PMML model
     */
    public PMMLRegressionPortObject(final PMMLPortObjectSpec spec,
            final PMMLRegressionContentHandler p) {
        super(spec, PMMLModelType.RegressionModel);
        try {
            p.checkValidity();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Content handler ist not "
                    + "fully setup: " + e.getMessage(), e);
        }
        m_regressionTable = p.getRegressionTable();
        m_modelName = p.getModelName();
    }

    /** {@inheritDoc} */
    @Override
    protected void writePMMLModel(final TransformerHandler handler)
            throws SAXException {
        new PMMLRegressionContentHandler(this)
                .writePMMLRegressionModel(handler);
    }

    /** {@inheritDoc} */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec,
            final InputStream stream, final String version)
            throws ParserConfigurationException, SAXException, IOException {
        PMMLRegressionContentHandler hdl =
                new PMMLRegressionContentHandler(spec);
        super.addPMMLContentHandler("RegressionModel", hdl);
        super.loadFrom(spec, stream, version);
        try {
            hdl.checkValidity();
        } catch (IllegalStateException e) {
            throw new SAXException("Incomplete regression model: "
                    + e.getMessage());
        }
        m_modelName = hdl.getModelName();
        m_regressionTable = hdl.getRegressionTable();
    }

    /**
     * @return the targetVariableName
     */
    public String getTargetVariableName() {
        for (String s : getSpec().getTargetFields()) {
            return s;
        }
        return "Response";
    }

    /**
     * @return the regressionTable
     */
    public RegressionTable getRegressionTable() {
        return m_regressionTable;
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        int highestPolynom = 0;
        for (NumericPredictor p : getRegressionTable().getVariables()) {
            highestPolynom = Math.max(highestPolynom, p.m_exponent);
        }
        StringBuilder b = new StringBuilder();
        switch (highestPolynom) {
            case 0:
                b.append("Constant ");
                break;
            case 1:
                b.append("Linear ");
                break;
            default:
                b.append("Polynomial (max degree ");
                b.append(highestPolynom);
                b.append(") ");
                break;
        }
        b.append(" Regression on \"");
        b.append(getTargetVariableName());
        b.append("\"");
        return b.toString();
    }

    /**
     * @return the name
     */
    public String getModelName() {
        return m_modelName;
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
