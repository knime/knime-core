/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.ApplyDocument.Apply;
import org.dmg.pmml.ConstantDocument.Constant;
import org.dmg.pmml.CovariateListDocument.CovariateList;
import org.dmg.pmml.DATATYPE;
import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.dmg.pmml.FactorListDocument.FactorList;
import org.dmg.pmml.FieldRefDocument.FieldRef;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel;
import org.dmg.pmml.GeneralRegressionModelDocument.GeneralRegressionModel.ModelType;
import org.dmg.pmml.LocalTransformationsDocument.LocalTransformations;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.OPTYPE;
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
import org.knime.base.node.mine.regression.pmmlgreg.VectorHandling.NameAndIndex;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLTranslator;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;

/**
 * A general regression model translator class between KNIME and PMML.
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class PMMLGeneralRegressionTranslator implements PMMLTranslator {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PMMLGeneralRegressionTranslator.class);

    private final PMMLGeneralRegressionContent m_content;

    private DerivedFieldMapper m_nameMapper;

    /**
     * Creates a new PMML content translator for general regression models. For usage with the
     * {@link #initializeFrom(PMMLDocument)} method.
     */
    public PMMLGeneralRegressionTranslator() {
        this(new PMMLGeneralRegressionContent());
    }

    /**
     * Creates a new PMML content translator for general regression models. For usage with the
     * {@link #exportTo(PMMLDocument, PMMLPortObjectSpec)} method.
     *
     * @param content the regression content for the model
     */
    public PMMLGeneralRegressionTranslator(final PMMLGeneralRegressionContent content) {
        m_content = content;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeFrom(final PMMLDocument pmmlDoc) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);
        List<GeneralRegressionModel> models = pmmlDoc.getPMML().getGeneralRegressionModelList();
        if (models.isEmpty()) {
            throw new IllegalArgumentException("No general regression model" + " provided.");
        } else if (models.size() > 1) {
            LOGGER.warn("Multiple general regression models found. " + "Only the first model is considered.");
        }
        GeneralRegressionModel reg = models.get(0);

        // read the content type
        PMMLGeneralRegressionContent.ModelType modelType = getKNIMERegModelType(reg.getModelType());
        m_content.setModelType(modelType);

        // read the function name
        FunctionName functionName = getKNIMEFunctionName(reg.getFunctionName());
        m_content.setFunctionName(functionName);

        m_content.setAlgorithmName(reg.getAlgorithmName());
        m_content.setModelName(reg.getModelName());
        if (reg.getCumulativeLink() != null) {
            throw new IllegalArgumentException("The attribute \"cumulativeLink\"" + " is currently not supported.");
        }
        m_content.setTargetReferenceCategory(reg.getTargetReferenceCategory());
        if (reg.isSetOffsetValue()) {
            m_content.setOffsetValue(reg.getOffsetValue());
        }
        if (reg.getLocalTransformations() != null && reg.getLocalTransformations().getDerivedFieldList() != null) {
            updateVectorLengthsBasedOnDerivedFields(reg.getLocalTransformations().getDerivedFieldList());
        }
        //        final Stream<String> vectorLengthsAsJsonAsString = reg.getMiningSchema().getExtensionList().stream()
        //                .filter(e -> e.getExtender().equals(EXTENDER) && e.getName().equals(VECTOR_COLUMNS_WITH_LENGTH)).map(v -> v.getValue());
        //        vectorLengthsAsJsonAsString
        //            .forEachOrdered(jsonAsString -> m_content.updateVectorLengths(
        //                Json.createReader(new StringReader(jsonAsString)).readObject().entrySet().stream().collect(
        //                    Collectors.toMap(Entry::getKey, entry -> ((JsonNumber)entry.getValue()).intValueExact()))));
        // read the parameter list
        ParameterList pmmlParamList = reg.getParameterList();
        if (pmmlParamList != null && pmmlParamList.sizeOfParameterArray() > 0) {
            List<Parameter> pmmlParam = pmmlParamList.getParameterList();
            PMMLParameter[] paramList = new PMMLParameter[pmmlParam.size()];
            for (int i = 0; i < pmmlParam.size(); i++) {
                String name = m_nameMapper.getColumnName(pmmlParam.get(i).getName());
                String label = pmmlParam.get(i).getLabel();
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
        if (pmmlFactorList != null && pmmlFactorList.sizeOfPredictorArray() > 0) {
            List<Predictor> pmmlPredictor = pmmlFactorList.getPredictorList();
            PMMLPredictor[] predictor = new PMMLPredictor[pmmlPredictor.size()];
            for (int i = 0; i < pmmlPredictor.size(); i++) {
                predictor[i] = new PMMLPredictor(m_nameMapper.getColumnName(pmmlPredictor.get(i).getName()));
            }
            m_content.setFactorList(predictor);
        } else {
            m_content.setFactorList(new PMMLPredictor[0]);
        }

        // read covariate list
        CovariateList covariateList = reg.getCovariateList();
        if (covariateList != null && covariateList.sizeOfPredictorArray() > 0) {
            List<Predictor> pmmlPredictor = covariateList.getPredictorList();
            PMMLPredictor[] predictor = new PMMLPredictor[pmmlPredictor.size()];
            for (int i = 0; i < pmmlPredictor.size(); i++) {
                predictor[i] = new PMMLPredictor(m_nameMapper.getColumnName(pmmlPredictor.get(i).getName()));
            }
            m_content.setCovariateList(predictor);
        } else {
            m_content.setCovariateList(new PMMLPredictor[0]);
        }

        // read PPMatrix
        PPMatrix ppMatrix = reg.getPPMatrix();
        if (ppMatrix != null && ppMatrix.sizeOfPPCellArray() > 0) {
            List<PPCell> pmmlCellArray = ppMatrix.getPPCellList();
            PMMLPPCell[] cells = new PMMLPPCell[pmmlCellArray.size()];
            for (int i = 0; i < pmmlCellArray.size(); i++) {
                PPCell ppCell = pmmlCellArray.get(i);
                cells[i] = new PMMLPPCell(ppCell.getValue(), m_nameMapper.getColumnName(ppCell.getPredictorName()),
                    ppCell.getParameterName(), ppCell.getTargetCategory());
            }
            m_content.setPPMatrix(cells);
        } else {
            m_content.setPPMatrix(new PMMLPPCell[0]);
        }

        // read CovMatrix
        PCovMatrix pCovMatrix = reg.getPCovMatrix();
        if (pCovMatrix != null && pCovMatrix.sizeOfPCovCellArray() > 0) {
            List<PCovCell> pCovCellArray = pCovMatrix.getPCovCellList();
            PMMLPCovCell[] covCells = new PMMLPCovCell[pCovCellArray.size()];
            for (int i = 0; i < pCovCellArray.size(); i++) {
                PCovCell c = pCovCellArray.get(i);
                covCells[i] = new PMMLPCovCell(c.getPRow(), c.getPCol(), c.getTRow(), c.getTCol(), c.getValue(),
                    c.getTargetCategory());
            }
            m_content.setPCovMatrix(covCells);
        } else {
            m_content.setPCovMatrix(new PMMLPCovCell[0]);
        }

        // read ParamMatrix
        ParamMatrix paramMatrix = reg.getParamMatrix();
        if (paramMatrix != null && paramMatrix.sizeOfPCellArray() > 0) {
            List<PCell> pCellArray = paramMatrix.getPCellList();
            PMMLPCell[] cells = new PMMLPCell[pCellArray.size()];
            for (int i = 0; i < pCellArray.size(); i++) {
                PCell p = pCellArray.get(i);
                double beta = p.getBeta();
                BigInteger df = p.getDf();
                if (df != null) {
                    cells[i] = new PMMLPCell(p.getParameterName(), beta, df.intValue(), p.getTargetCategory());
                } else {
                    cells[i] = new PMMLPCell(p.getParameterName(), beta, p.getTargetCategory());
                }
            }
            m_content.setParamMatrix(cells);
        } else {
            m_content.setParamMatrix(new PMMLPCell[0]);
        }
    }

    /**
     * @param derivedFieldList
     */
    private void updateVectorLengthsBasedOnDerivedFields(final List<DerivedField> derivedFieldList) {
        final Map<String, Integer> lengths = new LinkedHashMap<>();
        for (final DerivedField df : derivedFieldList) {
            final String name = df.getName();
            Optional<NameAndIndex> vni = VectorHandling.parse(name);
            if (vni.isPresent()) {
                final String key = vni.get().getName();
                try {
                    String function = df.getApply().getFunction();
                    if (!"substring".equals(function)) {
                        continue;
                    }
                    final List<FieldRef> fieldRefList = df.getApply().getFieldRefList();
                    if (fieldRefList.isEmpty() || !key.equals(fieldRefList.get(0).getField())) {
                        LOGGER.debug("Field name is not related to the derived field name: " + fieldRefList + " <-> " + key);
                        continue;
                    }
                    if (2 != df.getApply().getConstantList().size()) {
                        LOGGER.debug("substring requires two parameters: " + df);
                        continue;
                    }
                    if (!DATATYPE.INTEGER.equals(df.getDataType())) {
                        LOGGER.debug("Array value should be integer: " + df);
                        continue;
                    }
                    if (!OPTYPE.CONTINUOUS.equals(df.getOptype())) {
                        LOGGER.debug("The optype should be continuous: " + df);
                        continue;
                    }
                    int index = vni.get().getIndex();
                    int old = Math.max(0, lengths.getOrDefault(key, Integer.valueOf(0)).intValue());
                    if (old <= index) {
                        lengths.put(key, index + 1);
                    }
                } catch (RuntimeException e) {
                    //Ignore
                    LOGGER.debug(df.toString(), e);
                }
            }
        }
        LOGGER.debug(lengths);
        m_content.updateVectorLengths(lengths);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SchemaType exportTo(final PMMLDocument pmmlDoc, final PMMLPortObjectSpec spec) {
        m_nameMapper = new DerivedFieldMapper(pmmlDoc);

        GeneralRegressionModel reg = pmmlDoc.getPMML().addNewGeneralRegressionModel();
        final JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
        if (!m_content.getVectorLengths().isEmpty()) {
            LocalTransformations localTransformations = reg.addNewLocalTransformations();
            for (final Entry<? extends String, ? extends Integer> entry : m_content.getVectorLengths().entrySet()) {
                DataColumnSpec columnSpec = spec.getDataTableSpec().getColumnSpec(entry.getKey());
                if (columnSpec != null) {
                    final DataType type = columnSpec.getType();
                    final DataColumnProperties props = columnSpec.getProperties();
                    final boolean bitVector =
                        type.isCompatible(BitVectorValue.class) || (type.isCompatible(StringValue.class)
                            && props.containsProperty("realType") && "BitVector".equals(props.getProperty("realType")));
                    final boolean byteVector = type.isCompatible(ByteVectorValue.class)
                        || (type.isCompatible(StringValue.class) && props.containsProperty("realType")
                            && "ByteVector".equals(props.getProperty("realType")));
                    final String lengthAsString;
                    final int width;
                    if (byteVector) {
                        lengthAsString = "3";
                        width = 4;
                    } else if (bitVector) {
                        lengthAsString = "1";
                        width = 1;
                    } else {
                        throw new UnsupportedOperationException(
                            "Not supported type: " + type + " for column: " + columnSpec);
                    }
                    for (int i = 0; i < entry.getValue().intValue(); ++i) {
                        final DerivedField derivedField = localTransformations.addNewDerivedField();
                        derivedField.setOptype(OPTYPE.CONTINUOUS);
                        derivedField.setDataType(DATATYPE.INTEGER);
                        derivedField.setName(entry.getKey() + "[" + i + "]");
                        Apply apply = derivedField.addNewApply();
                        apply.setFunction("substring");
                        apply.addNewFieldRef().setField(entry.getKey());
                        Constant from = apply.addNewConstant();
                        from.setDataType(DATATYPE.INTEGER);
                        from.setStringValue(bitVector ? Long.toString(entry.getValue().longValue() - i) : Long.toString(i * width + 1L));
                        Constant length = apply.addNewConstant();
                        length.setDataType(DATATYPE.INTEGER);
                        length.setStringValue(lengthAsString);

                    }
                }
                jsonBuilder.add(entry.getKey(), entry.getValue().intValue());
            }
        }
        //        PMMLPortObjectSpecCreator newSpecCreator = new PMMLPortObjectSpecCreator(spec);
        //        newSpecCreator.addPreprocColNames(m_content.getVectorLengths().entrySet().stream()
        //            .flatMap(
        //                e -> IntStream.iterate(0, o -> o + 1).limit(e.getValue()).mapToObj(i -> e.getKey() + "[" + i + "]"))
        //            .collect(Collectors.toList()));
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, reg);
        //        if (!m_content.getVectorLengths().isEmpty()) {
        //        Extension miningExtension = reg.getMiningSchema().addNewExtension();
        //        miningExtension.setExtender(EXTENDER);
        //        miningExtension.setName(VECTOR_COLUMNS_WITH_LENGTH);
        //        miningExtension.setValue(jsonBuilder.build().toString());
        //        }
        reg.setModelType(getPMMLRegModelType(m_content.getModelType()));
        reg.setFunctionName(getPMMLMiningFunction(m_content.getFunctionName()));
        String algorithmName = m_content.getAlgorithmName();
        if (algorithmName != null && !algorithmName.isEmpty()) {
            reg.setAlgorithmName(algorithmName);
        }
        String modelName = m_content.getModelName();
        if (modelName != null && !modelName.isEmpty()) {
            reg.setModelName(modelName);
        }
        String targetReferenceCategory = m_content.getTargetReferenceCategory();
        if (targetReferenceCategory != null && !targetReferenceCategory.isEmpty()) {
            reg.setTargetReferenceCategory(targetReferenceCategory);
        }
        if (m_content.getOffsetValue() != null) {
            reg.setOffsetValue(m_content.getOffsetValue());
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
            cell.setPredictorName(m_nameMapper.getDerivedFieldName(p.getPredictorName()));
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

    private ModelType.Enum getPMMLRegModelType(final PMMLGeneralRegressionContent.ModelType type) {
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

    private PMMLGeneralRegressionContent.ModelType getKNIMERegModelType(final ModelType.Enum modelType) {
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

    private MININGFUNCTION.Enum getPMMLMiningFunction(final PMMLGeneralRegressionContent.FunctionName function) {
        switch (function) {
            case classification:
                return MININGFUNCTION.CLASSIFICATION;
            case regression:
                return MININGFUNCTION.REGRESSION;
            default:
                throw new IllegalArgumentException("Only classification or "
                    + "regression are allowed as mining function for general " + "regression");
        }
    }

    private PMMLGeneralRegressionContent.FunctionName getKNIMEFunctionName(final MININGFUNCTION.Enum mf) {
        PMMLGeneralRegressionContent.FunctionName function = null;
        if (MININGFUNCTION.CLASSIFICATION.equals(mf)) {
            function = PMMLGeneralRegressionContent.FunctionName.classification;
        } else if (MININGFUNCTION.REGRESSION.equals(mf)) {
            function = PMMLGeneralRegressionContent.FunctionName.regression;
        } else {
            throw new IllegalArgumentException(
                "Only classification or " + "regression are allowed as mining function for general " + "regression");
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
