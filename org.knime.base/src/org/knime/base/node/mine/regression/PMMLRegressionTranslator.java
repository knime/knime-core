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
 */
package org.knime.base.node.mine.regression;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml40.MININGFUNCTION;
import org.dmg.pmml40.NumericPredictorDocument;
import org.dmg.pmml40.PMMLDocument;
import org.dmg.pmml40.RegressionModelDocument.RegressionModel;
import org.dmg.pmml40.RegressionTableDocument;
import org.knime.base.node.util.DoubleFormat;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * A regression model translator class between KNIME and PMML.
 *
 * @author wenlin, Zementis, May 2011
 * @author Dominik Morent, KNIME.com AG, Zurich, Switzerland
 *
 */
public class PMMLRegressionTranslator implements PMMLTranslator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLRegressionTranslator.class);

    private String m_targetField;
    private String m_modelName;
    private String m_algorithmName;
    private RegressionTable m_regressionTable;

    private DerivedFieldMapper m_nameMapper;

    /**
     * For usage with the {@link #initializeFrom(PMMLDocument)} method.
     */
    public PMMLRegressionTranslator() {
        super();
    }

    /**
     * For usage with the {@link #exportTo(PMMLDocument, PMMLPortObjectSpec)}
     * method.
     * @param modelName the name of the model
     * @param algorithmName the name of the algorithm
     * @param regressionTable the regression table
     * @param targetField the name of the target column
     */
    public PMMLRegressionTranslator(final String modelName,
            final String algorithmName,
            final RegressionTable regressionTable,
            final String targetField) {
        m_modelName = modelName;
        m_algorithmName = algorithmName;
        m_regressionTable = regressionTable;
        m_targetField = targetField;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        RegressionModel[] models = pmmlDoc.getPMML().getRegressionModelArray();
        if (models.length == 0) {
            throw new IllegalArgumentException("No regression model"
                + " provided.");
        } else if (models.length > 1) {
            LOGGER.warn("Multiple regression models found. "
                + "Only the first model is considered.");
        }
        RegressionModel regressionModel = models[0];

        if (MININGFUNCTION.REGRESSION != regressionModel.getFunctionName()) {
            LOGGER.error("Only regression is supported by KNIME.");
        }
        m_algorithmName = regressionModel.getAlgorithmName();
        m_modelName = regressionModel.getModelName();

        RegressionTableDocument.RegressionTable regressionTable
                = regressionModel.getRegressionTableArray(0);

        List<NumericPredictor> knimePredictors =
            new ArrayList<NumericPredictor>();
        for (NumericPredictorDocument.NumericPredictor pmmlPredictor
                : regressionTable.getNumericPredictorArray()) {
            NumericPredictor knp =
                new NumericPredictor(
                        m_nameMapper.getColumnName(pmmlPredictor.getName()),
                        pmmlPredictor.getExponent().intValue(),
                        pmmlPredictor.getCoefficient());
            knimePredictors.add(knp);
        }

        m_regressionTable = new RegressionTable(regressionTable.getIntercept(),
                knimePredictors.toArray(new NumericPredictor[0]));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);

        RegressionModel regressionModel =
            pmmlDoc.getPMML().addNewRegressionModel();

        regressionModel.setFunctionName(MININGFUNCTION.REGRESSION);
        if (m_algorithmName != null && !m_algorithmName.isEmpty()) {
            regressionModel.setAlgorithmName(m_algorithmName);
        }
        regressionModel.setModelName(m_modelName);
        regressionModel.setTargetFieldName(m_targetField);

        PMMLMiningSchemaTranslator.writeMiningSchema(spec, regressionModel);

        RegressionTableDocument.RegressionTable regressionTable =
            regressionModel.addNewRegressionTable();
        regressionTable.setIntercept(m_regressionTable.getIntercept());

        for (NumericPredictor p : m_regressionTable.getVariables()) {
            NumericPredictorDocument.NumericPredictor np =
                regressionTable.addNewNumericPredictor();
            np.setName(m_nameMapper.getDerivedFieldName(p.getName()));
            if (p.getExponent() != 1) {
                np.setExponent(BigInteger.valueOf(p.getExponent()));
            }
            np.setCoefficient(p.getCoefficient());
        }

        return RegressionModel.type;
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
         * @param name
         *            the predictor's name (usually the column name)
         * @param exponent
         *            the exponent
         * @param coefficient
         *            the coefficient
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
                + DoubleFormat.formatDouble(m_coefficient) + ", exponent = "
                + m_exponent + ")";
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
         * @param intercept
         *            the constant intercept of the regression formula
         * @param variables
         *            the regression variables
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
     * @return the modelName
     */
    public String getModelName() {
        return m_modelName;
    }

    /**
     * @return the algorithmName
     */
    public String getAlgorithmName() {
        return m_algorithmName;
    }

    /**
     * @return the regressionTable
     */
    public RegressionTable getRegressionTable() {
        return m_regressionTable;
    }

}
