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
        new PMMLRegressionContentHandler(this, getWriteVersion())
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getWriteVersion() {
        return PMML_V3_2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeLocalTransformations(final TransformerHandler handler)
            throws SAXException {
        super.writeLocalTransformations(handler);
    }

}
