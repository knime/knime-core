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
package org.knime.base.node.mine.regression.pmmlgreg;

import java.math.BigInteger;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.CovariateListDocument.CovariateList;
import org.dmg.pmml.FactorListDocument.FactorList;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel.ModelType;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.PCellDocument.PCell;
import org.dmg.pmml.PCovCellDocument.PCovCell;
import org.dmg.pmml.PCovMatrixDocument.PCovMatrix;
import org.dmg.pmml.PMMLDocument;
import org.dmg.pmml.PPCellDocument.PPCell;
import org.dmg.pmml.PPMatrixDocument.PPMatrix;
import org.dmg.pmml.ParamMatrixDocument.ParamMatrix;
import org.dmg.pmml.ParameterDocument.Parameter;
import org.dmg.pmml.ParameterListDocument.ParameterList;
import org.dmg.pmml.PredictorDocument.Predictor;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * A general regression model translator class between KNIME and PMML.
 *
 * @author Dominik Morent, KNIME.com AG, Zurich, Switzerland
 *
 */
public class PMMLGeneralRegressionTranslator implements PMMLTranslator {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(PMMLGeneralRegressionTranslator.class);

    private final PMMLGeneralRegressionContent m_content;

    private DerivedFieldMapper m_nameMapper;

    /**
     * Creates a new PMML content translator for general regression models.
     * For usage with the {@link #initializeFrom(PMMLDocument)} method.
     */
    public PMMLGeneralRegressionTranslator() {
       this(new PMMLGeneralRegressionContent());
    }

    /**
     * Creates a new PMML content translator for general regression models.
     * For usage with the {@link #exportTo(PMMLDocument, PMMLPortObjectSpec)}
     * method.
     *
     * @param content the regression content for the model
     */
    public PMMLGeneralRegressionTranslator(
            final PMMLGeneralRegressionContent content) {
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        GeneralRegressionModel[] models
                = pmmlDoc.getPMML().getGeneralRegressionModelArray();
        if (models.length == 0) {
            throw new IllegalArgumentException("No general regression model"
                + " provided.");
        } else if (models.length > 1) {
            LOGGER.warn("Multiple general regression models found. "
                + "Only the first model is considered.");
        }
        GeneralRegressionModel reg = models[0];

        // read the content type
       PMMLGeneralRegressionContent.ModelType modelType =
            getKNIMERegModelType(reg.getModelType());
       if (!PMMLGeneralRegressionContent.ModelType.multinomialLogistic.equals(
               modelType)) {
           throw new IllegalArgumentException("The ModelType \""
                   + modelType + "\" is currently not supported.");
       }
       m_content.setModelType(modelType);

       // read the function name
       FunctionName functionName = getKNIMEFunctionName(reg.getFunctionName());
       if (!FunctionName.classification.equals(functionName)) {
           throw new IllegalArgumentException("The FunctionName \""
                   + functionName + "\" is currently not supported.");
       }
       m_content.setFunctionName(functionName);

       m_content.setAlgorithmName(reg.getAlgorithmName());
       m_content.setModelName(reg.getModelName());
       if (reg.getCumulativeLink() != null) {
           throw new IllegalArgumentException("The attribute \"cumulativeLink\""
                   + " is currently not supported.");
       }

       // read the parameter list
       ParameterList pmmlParamList = reg.getParameterList();
       if (pmmlParamList != null && pmmlParamList.sizeOfParameterArray() > 0) {
           Parameter[] pmmlParam = pmmlParamList.getParameterArray();
           PMMLParameter[] paramList = new PMMLParameter[pmmlParam.length];
           for (int i = 0; i < pmmlParam.length; i++) {
               String name = m_nameMapper.getColumnName(pmmlParam[i].getName());
               String label = pmmlParam[i].getLabel();
               if (label == null) {
                   paramList[i] = new PMMLParameter(name);
               } else {
                   paramList[i] = new PMMLParameter(name, label);
               }
           }
           m_content.setParameterList(paramList);
       } else {
           m_content.setParameterList(new PMMLParameter[0]);
       }

       // read the factor list
       FactorList pmmlFactorList = reg.getFactorList();
       if (pmmlFactorList != null
               && pmmlFactorList.sizeOfPredictorArray() > 0) {
           Predictor[] pmmlPredictor = pmmlFactorList.getPredictorArray();
           PMMLPredictor[] predictor = new PMMLPredictor[pmmlPredictor.length];
           for (int i = 0; i < pmmlPredictor.length; i++) {
               predictor[i] = new PMMLPredictor(m_nameMapper.getColumnName(
                       pmmlPredictor[i].getName()));
           }
           m_content.setFactorList(predictor);
       } else {
           m_content.setFactorList(new PMMLPredictor[0]);
       }

       // read covariate list
       CovariateList covariateList = reg.getCovariateList();
       if (covariateList != null
               && covariateList.sizeOfPredictorArray() > 0) {
           Predictor[] pmmlPredictor = covariateList.getPredictorArray();
           PMMLPredictor[] predictor = new PMMLPredictor[pmmlPredictor.length];
           for (int i = 0; i < pmmlPredictor.length; i++) {
               predictor[i] = new PMMLPredictor(m_nameMapper.getColumnName(
                       pmmlPredictor[i].getName()));
           }
           m_content.setCovariateList(predictor);
       } else {
           m_content.setCovariateList(new PMMLPredictor[0]);
       }

       // read PPMatrix
       PPMatrix ppMatrix = reg.getPPMatrix();
       if (ppMatrix != null && ppMatrix.sizeOfPPCellArray() > 0) {
           PPCell[] pmmlCellArray = ppMatrix.getPPCellArray();
           PMMLPPCell[] cells = new PMMLPPCell[pmmlCellArray.length];
           for (int i = 0; i < pmmlCellArray.length; i++) {
               PPCell ppCell = pmmlCellArray[i];
               cells[i] = new PMMLPPCell(ppCell.getValue(),
                       m_nameMapper.getColumnName(ppCell.getPredictorName()),
                       ppCell.getParameterName(),
                       ppCell.getTargetCategory());
           }
           m_content.setPPMatrix(cells);
       } else {
           m_content.setPPMatrix(new PMMLPPCell[0]);
       }

       // read CovMatrix
        PCovMatrix pCovMatrix = reg.getPCovMatrix();
        if (pCovMatrix != null && pCovMatrix.sizeOfPCovCellArray() > 0) {
            PCovCell[] pCovCellArray = pCovMatrix.getPCovCellArray();
            PMMLPCovCell[] covCells = new PMMLPCovCell[pCovCellArray.length];
            for (int i = 0; i < pCovCellArray.length; i++) {
                PCovCell c = pCovCellArray[i];
                covCells[i] =
                        new PMMLPCovCell(c.getPRow(), c.getPCol(), c.getTRow(),
                                c.getTCol(), c.getValue(),
                                c.getTargetCategory());
            }
            m_content.setPCovMatrix(covCells);
        } else {
            m_content.setPCovMatrix(new PMMLPCovCell[0]);
        }

       // read ParamMatrix
       ParamMatrix paramMatrix = reg.getParamMatrix();
       if (paramMatrix != null && paramMatrix.sizeOfPCellArray() > 0) {
           PCell[] pCellArray = paramMatrix.getPCellArray();
           PMMLPCell[] cells = new PMMLPCell[pCellArray.length];
           for (int i = 0; i < pCellArray.length; i++) {
               PCell p = pCellArray[i];
               double beta = p.getBeta();
               BigInteger df = p.getDf();
               if (df != null) {
                   cells[i] = new PMMLPCell(p.getParameterName(), beta,
                           df.intValue(), p.getTargetCategory());
               } else {
                   cells[i] = new PMMLPCell(p.getParameterName(), beta,
                           p.getTargetCategory());
               }
           }
           m_content.setParamMatrix(cells);
       } else {
           m_content.setParamMatrix(new PMMLPCell[0]);
       }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc,
            final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);

        GeneralRegressionModel reg =
            pmmlDoc.getPMML().addNewGeneralRegressionModel();
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, reg);
        reg.setModelType(
                getPMMLRegModelType(m_content.getModelType()));
        reg.setFunctionName(
                getPMMLMiningFunction(m_content.getFunctionName()));
        String algorithmName = m_content.getAlgorithmName();
        if (algorithmName != null && !algorithmName.isEmpty()) {
            reg.setAlgorithmName(algorithmName);
        }
        String modelName = m_content.getModelName();
        if (modelName != null && !modelName.isEmpty()) {
            reg.setModelName(modelName);
        }

        // add parameter list
        ParameterList paramList = reg.addNewParameterList();
        for (PMMLParameter p : m_content.getParameterList()) {
            Parameter param = paramList.addNewParameter();
            param.setName(p.getName());
            String label = p.getLabel();
            if (label != null) {
                param.setLabel(m_nameMapper.getDerivedFieldName(label));
            }
        }

        // add factor list
        FactorList factorList = reg.addNewFactorList();
        for (PMMLPredictor p : m_content.getFactorList()) {
           Predictor predictor = factorList.addNewPredictor();
           predictor.setName(m_nameMapper.getDerivedFieldName(p.getName()));
        }

        // add covariate list
        CovariateList covariateList = reg.addNewCovariateList();
        for (PMMLPredictor p : m_content.getCovariateList()) {
            Predictor predictor = covariateList.addNewPredictor();
            predictor.setName(m_nameMapper.getDerivedFieldName(p.getName()));
        }

        // add PPMatrix
        PPMatrix ppMatrix = reg.addNewPPMatrix();
        for (PMMLPPCell p : m_content.getPPMatrix()) {
            PPCell cell = ppMatrix.addNewPPCell();
            cell.setValue(p.getValue());
            cell.setPredictorName(m_nameMapper.getDerivedFieldName(
                    p.getPredictorName()));
            cell.setParameterName(p.getParameterName());
            String targetCategory = p.getTargetCategory();
            if (targetCategory != null && !targetCategory.isEmpty()) {
                cell.setTargetCategory(targetCategory);
            }
        }

        // add CovMatrix
        if (m_content.getPCovMatrix().length > 0) {
            PCovMatrix pCovMatrix = reg.addNewPCovMatrix();
            for (PMMLPCovCell p : m_content.getPCovMatrix()) {
                PCovCell covCell = pCovMatrix.addNewPCovCell();
                covCell.setPRow(p.getPRow());
                covCell.setPCol(p.getPCol());
                String tCol = p.getTCol();
                String tRow = p.getTRow();
                if (tRow != null || tCol != null) {
                    covCell.setTRow(tRow);
                    covCell.setTCol(tCol);
                }
                covCell.setValue(p.getValue());
                String targetCategory = p.getTargetCategory();
                if (targetCategory != null && !targetCategory.isEmpty()) {
                    covCell.setTargetCategory(targetCategory);
                }
            }
        }

        // add ParamMatrix
        ParamMatrix paramMatrix = reg.addNewParamMatrix();
        for (PMMLPCell p : m_content.getParamMatrix()) {
            PCell pCell = paramMatrix.addNewPCell();
            String targetCategory = p.getTargetCategory();
            if (targetCategory != null) {
                pCell.setTargetCategory(targetCategory);
            }
            pCell.setParameterName(p.getParameterName());
            pCell.setBeta(p.getBeta());
            Integer df = p.getDf();
            if (df != null) {
                pCell.setDf(BigInteger.valueOf(df));
            }
        }
        return GeneralRegressionModel.type;
    }



    private ModelType.Enum getPMMLRegModelType(
            final PMMLGeneralRegressionContent.ModelType type) {
        switch (type) {
        case generalLinear:
            return ModelType.GENERAL_LINEAR;
        case multinomialLogistic:
            return ModelType.MULTINOMIAL_LOGISTIC;
        case ordinalMultinomial:
            return ModelType.ORDINAL_MULTINOMIAL;
        case regression:
            return ModelType.REGRESSION;
        default:
            return null;
        }
    }

    private PMMLGeneralRegressionContent.ModelType getKNIMERegModelType(
            final ModelType.Enum modelType) {
        PMMLGeneralRegressionContent.ModelType type = null;
        if (ModelType.GENERAL_LINEAR.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.generalLinear;
        } else if (ModelType.MULTINOMIAL_LOGISTIC.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.multinomialLogistic;
        } else if (ModelType.ORDINAL_MULTINOMIAL.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.ordinalMultinomial;
        } else if (ModelType.REGRESSION.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.regression;
        } else if (ModelType.GENERALIZED_LINEAR.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.generalizedLinear;
        } else if (ModelType.COX_REGRESSION.equals(modelType)) {
            type = PMMLGeneralRegressionContent.ModelType.CoxRegression;
        }
        return type;
    }

    private MININGFUNCTION.Enum getPMMLMiningFunction(
            final PMMLGeneralRegressionContent.FunctionName function) {
        switch (function) {
        case classification:
            return MININGFUNCTION.CLASSIFICATION;
        case regression:
            return MININGFUNCTION.REGRESSION;
        default:
            throw new IllegalArgumentException("Only classification or "
                    + "regression are allowed as mining function for general "
                    + "regression");
        }
    }

    private PMMLGeneralRegressionContent.FunctionName getKNIMEFunctionName(
            final MININGFUNCTION.Enum mf) {
        PMMLGeneralRegressionContent.FunctionName function = null;
        if (MININGFUNCTION.CLASSIFICATION.equals(mf)) {
            function = PMMLGeneralRegressionContent.FunctionName.classification;
        } else if (MININGFUNCTION.REGRESSION.equals(mf)) {
                function = PMMLGeneralRegressionContent.FunctionName.regression;
        } else {
            throw new IllegalArgumentException("Only classification or "
                    + "regression are allowed as mining function for general "
                    + "regression");
        }
        return function;
    }

    /**
     * @return the content
     */
    public PMMLGeneralRegressionContent getContent() {
        return m_content;
    }
}
