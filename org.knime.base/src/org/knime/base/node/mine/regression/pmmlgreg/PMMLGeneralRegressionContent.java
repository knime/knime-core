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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   26.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;



/**
 *
 * @author Heiko Hofer
 */
public final class PMMLGeneralRegressionContent {
    /**
     * The Type of regression.
     * @author Heiko Hofer
     */
    public enum ModelType {
        /** Currently not supported. */
        regression,
        /** Currently not supported. */
        generalLinear,
        /** Multinomial Target (logistic regression). */
        multinomialLogistic,
        /** Currently not supported. */
        ordinalMultinomial,
        /** Currently not supported. */
        generalizedLinear,
        /** Currently not supported. */
        CoxRegression
    }

    /**
     * The function name.
     * @author Heiko Hofer
     */
    public enum FunctionName {
        /** Currently not supported. */
        regression,
        /** Main purpose is classification, e.g. used in logistic regression. */
        classification
    }

    /* required */
    private ModelType m_modelType;

    private String m_modelName;

    /* required */
    private FunctionName m_functionName;

    private String m_algorithmName;

    private PMMLParameter[] m_parameterList;

    private PMMLPredictor[] m_factorList;

    private PMMLPredictor[] m_covariateList;

    private PMMLPPCell[] m_ppMatrix;

    private PMMLPCovCell[] m_pCovMatrix;

    private PMMLPCell[] m_paramMatrix;

    /** Empty Contstuctor used when reading xml file. */
    PMMLGeneralRegressionContent() {
        // Fields are defined by setters.
    }

    /**
     * @param modelType the type of the regression model
     * @param modelName the name of the model
     * @param functionName either regression of classification
     * @param algorithmName the name of the algorithm
     * @param parameterList the list of parameters
     * @param factorList the list of factor names
     * @param covariateList the list of covariate names
     * @param ppMatrix Predictor-to-Parameter correlation matrix
     * @param pCovMatrix matrix of parameter estimate covariates
     * @param paramMatrix parameter matrix
     */
    public PMMLGeneralRegressionContent(
            final ModelType modelType, final String modelName,
            final FunctionName functionName, final String algorithmName,
            final PMMLParameter[] parameterList,
            final PMMLPredictor[] factorList,
            final PMMLPredictor[] covariateList, final PMMLPPCell[] ppMatrix,
            final PMMLPCovCell[] pCovMatrix, final PMMLPCell[] paramMatrix) {
        m_modelType = modelType;
        m_modelName = modelName;
        m_functionName = functionName;
        m_algorithmName = algorithmName;
        m_parameterList = parameterList;
        m_factorList = factorList;
        m_covariateList = covariateList;
        m_ppMatrix = ppMatrix;
        m_pCovMatrix = pCovMatrix;
        m_paramMatrix = paramMatrix;
    }


    /**
     * @return the modelType
     */
    public ModelType getModelType() {
        return m_modelType;
    }

    /**
     * @return the modelName
     */
    public String getModelName() {
        return m_modelName;
    }

    /**
     * @return the functionName
     */
    public FunctionName getFunctionName() {
        return m_functionName;
    }

    /**
     * @return the algorithmName
     */
    public String getAlgorithmName() {
        return m_algorithmName;
    }

    /**
     * @return the parameterList
     */
    public PMMLParameter[] getParameterList() {
        return m_parameterList;
    }


    /**
     * @return the factorList
     */
    public PMMLPredictor[] getFactorList() {
        return m_factorList;
    }

    /**
     * @return the covariateList
     */
    public PMMLPredictor[] getCovariateList() {
        return m_covariateList;
    }

    /**
     * @return the ppMatrix
     */
    public PMMLPPCell[] getPPMatrix() {
        return m_ppMatrix;
    }

    /**
     * @return the pCovMatrix
     */
    public PMMLPCovCell[] getPCovMatrix() {
        return m_pCovMatrix;
    }

    /**
     * @return the paramMatrix
     */
    public PMMLPCell[] getParamMatrix() {
        return m_paramMatrix;
    }

    /**
     * @param ppMatrix the ppMatrix to set
     */
    void setPPMatrix(final PMMLPPCell[] ppMatrix) {
        m_ppMatrix = ppMatrix;
    }

    /**
     * @param modelType the modelType to set
     */
    void setModelType(final ModelType modelType) {
        m_modelType = modelType;
    }

    /**
     * @param modelName the modelName to set
     */
    void setModelName(final String modelName) {
        m_modelName = modelName;
    }

    /**
     * @param functionName the functionName to set
     */
    void setFunctionName(final FunctionName functionName) {
        m_functionName = functionName;
    }

    /**
     * @param algorithmName the algorithmName to set
     */
    void setAlgorithmName(final String algorithmName) {
        m_algorithmName = algorithmName;
    }

    /**
     * @param parameterList the parameterList to set
     */
    void setParameterList(final PMMLParameter[] parameterList) {
        m_parameterList = parameterList;
    }

    /**
     * @param factorList the factorList to set
     */
    void setFactorList(final PMMLPredictor[] factorList) {
        m_factorList = factorList;
    }

    /**
     * @param covariateList the covariateList to set
     */
    void setCovariateList(final PMMLPredictor[] covariateList) {
        m_covariateList = covariateList;
    }

    /**
     * @param covMatrix the pCovMatrix to set
     */
    void setPCovMatrix(final PMMLPCovCell[] covMatrix) {
        m_pCovMatrix = covMatrix;
    }

    /**
     * @param paramMatrix the paramMatrix to set
     */
    void setParamMatrix(final PMMLPCell[] paramMatrix) {
        m_paramMatrix = paramMatrix;
    }
}
