/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.xml.sax.SAXException;

/**
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class PMMLRegressionPortObject extends PMMLPortObject {
    
    public static final PortType TYPE = 
        new PortType(PMMLRegressionPortObject.class);
    
    /** */
    public PMMLRegressionPortObject() {
    }
    
    private RegressionTable m_regressionTable;
    private String m_modelName;
    
    /**
     * 
     */
    public PMMLRegressionPortObject(final PMMLPortObjectSpec spec,
            final PMMLRegressionContentHandler p) {
        super(spec);
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
        new PMMLRegressionContentHandler(
                this).writePMMLRegressionModel(handler);
    }
    
    /** {@inheritDoc} */
    @Override
    public void loadFrom(final PMMLPortObjectSpec spec, 
            final InputStream stream) 
        throws ParserConfigurationException, SAXException, IOException {
        PMMLRegressionContentHandler hdl = 
            new PMMLRegressionContentHandler(spec);
        super.addPMMLContentHandler("RegressionModel", hdl);
        super.loadFrom(spec, stream);
        try {
            hdl.checkValidity();
        } catch (IllegalStateException e) {
            throw new SAXException(
                    "Incomplete regression model: " + e.getMessage());
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
        case 0: b.append("Constant "); break;
        case 1: b.append("Linear "); break;
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
    
    public static final class NumericPredictor {
        private final String m_name;
        private final int m_exponent;
        private final double m_value;
        public NumericPredictor(final String name, 
                final int exponent, final double value) {
            m_name = name;
            m_exponent = exponent;
            m_value = value;
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
        public double getValue() {
            return m_value;
        }
        
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return m_name + " (val = " + DoubleFormat.formatDouble(m_value)
                + ", exponent = " + m_exponent + ")";
        }
    }
    
    public static final class RegressionTable {
        private final double m_intercept;
        private final List<NumericPredictor> m_variables;
        
        /**
         * 
         */
        public RegressionTable(final double intercept, 
                final NumericPredictor[] variables) {
            m_intercept = intercept;
            m_variables = Collections.unmodifiableList(
                    Arrays.asList(variables));
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
