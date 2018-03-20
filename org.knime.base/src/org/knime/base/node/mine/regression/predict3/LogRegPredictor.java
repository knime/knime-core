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
 * ------------------------------------------------------------------------
 *
 * History
 *   19.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.regression.RegressionTrainingRow;
import org.knime.base.node.mine.regression.RegressionTrainingRow.MissingHandling;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.VectorHandling;
import org.knime.base.node.mine.regression.pmmlgreg.VectorHandling.NameAndIndex;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;


/**
 * A Predictor for a logistic regression model.
 * <p>Despite being public no official API.
 * @author Adrian Nembach, KNIME.com
 * @since 3.5
 */
public final class LogRegPredictor extends RegressionPredictorCellFactory {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LogRegPredictor.class);
    private static final double OVERFLOW_LIMIT = 700.0;
    private static final MissingHandling MISSING_HANDLING = new MissingHandling(true);
    private PMMLGeneralRegressionContent m_content;
    private PPMatrix m_ppMatrix;
    /** A mapping of the parameter name to its index of the
     * associated column in the input table or -1 if the parameter is not
     * associated with a column (e.g. intercept).
     */
    private Map<String, Integer> m_parameterI;
    private List<String> m_parameters;
    private List<String> m_predictors;
    private Map<? extends String, ? extends Integer> m_vectorLengths;
    private DataTableSpec m_trainingSpec;
    private Set<String> m_factors;
    private Map<String, List<DataCell>> m_values;
    private List<DataCell> m_targetCategories;

    private final int[] m_paramIdx2PredictorIdx;

    // matrix
    // Number of Rows: dim(x)
    // Number of Cols: numTargetCategories
    private RealMatrix m_beta = null;
    private boolean m_includeProbs;
    /** maps the indices of the values from m_targetCategories to the domain values of the target column. */
    private Map<Integer, Integer> m_targetCategoryIndex;
    /** the number of domain values of the target column. */
    private int m_targetDomainValuesCount;
    /** from label to column name */
    private Map<String, String> m_baseLabelToColName;

    /**
     * This constructor should be used when executing the node. Use it when
     * you want to compute output cells.
     *
     * @param content the general regression content
     * @param inSpec the spec of the data input port
     * @param portSpec the pmml port object spec
     * @param targetVariableName the name of the target variable
     * @param settings settings for the predictor node
     * @throws InvalidSettingsException when inSpec and regModel do not match
     */
    public LogRegPredictor(final PMMLGeneralRegressionContent content,
            final DataTableSpec inSpec,
            final PMMLPortObjectSpec  portSpec,
            final String targetVariableName,
            final RegressionPredictorSettings settings)
            throws InvalidSettingsException {
        super(portSpec, inSpec, settings);

        m_includeProbs = settings.getIncludeProbabilities();
        m_content = content;
        m_baseLabelToColName = Arrays.asList(m_content.getParameterList()).stream().collect(Collectors.toMap(PMMLParameter::getName, v->{
            final String param = v.getLabel();
            return param.matches("(.*?)\\[(\\d+)\\]") ?
                param.substring(0, param.lastIndexOf('['))
                : param;}));
        m_ppMatrix = new PPMatrix(m_content.getPPMatrix());
        m_parameters = new ArrayList<>();
        m_predictors = new ArrayList<>();
        m_vectorLengths = m_content.getVectorLengths();
        m_parameterI = new HashMap<>();
        for (PMMLParameter parameter : m_content.getParameterList()) {
            m_parameters.add(parameter.getName());
            String predictor = m_ppMatrix.getPredictor(parameter.getName());
            Optional<NameAndIndex> vni = VectorHandling.parse(parameter.getLabel());
            if (vni.isPresent() && m_vectorLengths.containsKey(vni.get().getName())) {
                m_parameterI.put(parameter.getName(),
                    inSpec.findColumnIndex(vni.get().getName()));
                m_predictors.add(parameter.getLabel());
            } else {
                m_predictors.add(predictor);
                m_parameterI.put(parameter.getName(),
                        inSpec.findColumnIndex(predictor));
            }
        }

        m_paramIdx2PredictorIdx = createParameter2PredictorIdxMap();

        m_trainingSpec = portSpec.getDataTableSpec();
        DataColumnSpec targetColSpec = m_trainingSpec.getColumnSpec(targetVariableName);

        m_targetCategories = determineTargetCategories(targetColSpec, content);
        m_targetCategoryIndex = createTargetCategoryToOutputMap(m_targetCategories, targetColSpec);
        m_targetDomainValuesCount = targetColSpec.getDomain().getValues().size();

        m_values = determineFactorValues(m_content, m_trainingSpec);
        m_factors = m_values.keySet();
        m_beta = getBetaMatrix();
    }


    private int[] createParameter2PredictorIdxMap() {
        int[] idxMap = new int[m_parameters.size()];
        for (int i = 0; i < m_parameters.size(); i++) {
            Optional<String> optPredictor = findPredictor(m_parameters.get(i));
            if (optPredictor.isPresent()) {
                idxMap[i] = m_predictors.indexOf(optPredictor.get());
            } else {
                // intercept
                idxMap[i] = -1;
            }
        }
        return idxMap;
    }

    /**
     * Create the mapping from the indices of the values from m_targetCategories to
     * the domain values of the target column.
     * @param targetCategories the target categories from PMML general regression
     * @param targetColSpec the spec of the target column
     * @return the mapping from the indices of the values from m_targetCategories to
     * the domain values of the target column.
     */
    private Map<Integer, Integer> createTargetCategoryToOutputMap(final List<DataCell> targetCategories,
        final DataColumnSpec targetColSpec) {
        Map<Integer, Integer> targetCategoryIndex = new HashMap<>();
        List<DataCell> domainValues = new ArrayList<>();
        domainValues.addAll(targetColSpec.getDomain().getValues());
        int i = 0;
        for (DataCell cell : targetCategories) {
            int index = domainValues.indexOf(cell);
            targetCategoryIndex.put(i, index);
            i++;
        }
        return targetCategoryIndex;
    }


    /**
     * Retrieve the target values from the PMML model.
     * @throws InvalidSettingsException if PMML model is inconsistent or ambiguous
     */
    private static List<DataCell> determineTargetCategories(final DataColumnSpec targetCol,
            final PMMLGeneralRegressionContent content) throws InvalidSettingsException {
        Map<String, DataCell> domainValues = new HashMap<>();
        for (DataCell cell : targetCol.getDomain().getValues()) {
            domainValues.put(cell.toString(), cell);
        }
        // Collect target categories from model
        Set<DataCell> modelTargetCategories = new LinkedHashSet<>();
        for (PMMLPCell cell : content.getParamMatrix()) {
            modelTargetCategories.add(domainValues.get(cell.getTargetCategory()));
        }
        String targetReferenceCategory = content.getTargetReferenceCategory();
        if (targetReferenceCategory == null || targetReferenceCategory.isEmpty()) {
            List<DataCell> targetCategories = new ArrayList<>();
            targetCategories.addAll(targetCol.getDomain().getValues());
            Collections.sort(targetCategories, targetCol.getType().getComparator());
            if (targetCategories.size() == modelTargetCategories.size() + 1) {
                targetReferenceCategory = targetCategories.get(targetCategories.size() - 1).toString();
                // the last target category is the target reference category
                LOGGER.debug("The target reference category is not explicitly set in PMML. Automatically choose : "
                        + targetReferenceCategory);
            } else {
                throw new InvalidSettingsException("Please set the attribute \"targetReferenceCategory\" of the"
                        + "\"GeneralRegression\" element in the PMML file.");
            }
        }
        modelTargetCategories.add(domainValues.get(targetReferenceCategory));

        List<DataCell> toReturn = new ArrayList<>();
        toReturn.addAll(modelTargetCategories);
        return toReturn;
    }


    @Override
    public DataCell[] getCells(final DataRow row) {
        Optional<PredictionRow> predictionRow = createPredictionRow(row);
        if (!predictionRow.isPresent()) {
            return createMissingOutput();
        }
        double[] logits = predictionRow.get().sparseMarixMultiply(m_beta);
        return postProcessResult(logits);
    }


    private Optional<String> findPredictor(final String parameter) {
        String predictor;
        String value;
        for (final Iterator<String> iter = m_predictors.iterator(); iter.hasNext();) {
            predictor = iter.next();
            value = m_ppMatrix.getValue(parameter, predictor, null);
            if (null != value) {
                return Optional.of(predictor);
            }
        }
        return Optional.empty();
    }

    private Optional<PredictionRow> createPredictionRow(final DataRow row) {
        int[] nonZeroIndices = new int[m_parameters.size()];
        double[] nonZeroValues = new double[nonZeroIndices.length];
        int nonZeroCounter = 0;
        HelperStruct returnStruct = new HelperStruct();
        for (int i = 0; i < m_parameters.size(); i += returnStruct.m_step) {
            String parameter = m_parameters.get(i);
            int predictorIdx = m_paramIdx2PredictorIdx[i];
            if (predictorIdx != -1) {
                String predictor = m_predictors.get(predictorIdx);
                String value = m_ppMatrix.getValue(parameter, predictor, null);
                DataCell cell = row.getCell(m_parameterI.get(parameter));
                if (cell.isMissing()) {
                    return Optional.empty();
                }
                if (m_factors.contains(predictor)) {
                    handleFactor(predictor, cell, returnStruct);
                } else if (correspondsToVector(parameter)) {
                    handleVector(predictor, cell, Integer.parseInt(value), returnStruct);
                } else {
                    // numerical
                    handleNumerical(cell, Integer.parseInt(value), returnStruct);
                }
            } else {
                // intercept
                returnStruct.m_step = 1;
                returnStruct.m_deltaToNonZeroIdx = 0;
                returnStruct.m_value = 1.0;
            }

            if (returnStruct.m_value != 0.0) {
                nonZeroValues[nonZeroCounter] = returnStruct.m_value;
                nonZeroIndices[nonZeroCounter] = i + returnStruct.m_deltaToNonZeroIdx;
                nonZeroCounter++;
            }
        }

        if (nonZeroCounter < nonZeroIndices.length) {
            // mark end of sequence
            nonZeroIndices[nonZeroCounter] = -1;
        }

        return Optional.of(new PredictionRow(nonZeroValues, nonZeroIndices));
    }

    private boolean correspondsToVector(final String parameter) {
        return m_baseLabelToColName.containsKey(parameter) && m_vectorLengths.containsKey(m_baseLabelToColName.get(parameter));
    }

    private void handleFactor(final String predictor, final DataCell cell, final HelperStruct returnStruct) {
        List<DataCell> values = m_values.get(predictor);
        int index = values.indexOf(cell);
        // these are design variables
        /* When building a general regression model, for each
        categorical fields, there is one category used as the
        default baseline and therefore it didn't show in the
        ParameterList in PMML. This design for the training is fine,
        but in the prediction, when the input of Employment is
        the default baseline, the parameters should all be 0.
        See the commit message for an example and more details.
        */
        if (index > 0) {
            returnStruct.m_value = 1.0;
            returnStruct.m_deltaToNonZeroIdx = index - 1;
        } else {
            returnStruct.m_value = 0.0;
        }
        returnStruct.m_step = values.size() - 1;
    }

    private void handleVector(final String predictor, final DataCell cell, final double exponent,
        final HelperStruct returnStruct) {
        NameAndIndex vectorValue = VectorHandling.parse(predictor)
                .orElseThrow(()-> new IllegalStateException("Can't find vector value for " + predictor));
            int j = vectorValue.getIndex();
            double radix = RegressionTrainingRow.getValue(cell, j, MISSING_HANDLING);
            returnStruct.set(1, radix != 0.0 ? Math.pow(radix, exponent) : 0.0, 0);
    }

    private void handleNumerical(final DataCell cell, final double exponent, final HelperStruct returnStruct) {
        double radix = ((DoubleValue)cell).getDoubleValue();
        returnStruct.set(1, radix != 0.0 ? Math.pow(radix, exponent) : 0.0, 0);
    }

    private static class HelperStruct {
        private int m_step;
        private double m_value;
        private int m_deltaToNonZeroIdx;

        public void set(final int step, final double value, final int deltaToNonZeroIdx) {
            m_step = step;
            m_value = value;
            m_deltaToNonZeroIdx = deltaToNonZeroIdx;
        }
    }

    private int argMax(final double[] vector) {
        int maxIndex = 0;
        double maxValue = vector[0];
        for (int i = 1; i < vector.length; i++) {
            if (vector[i] > maxValue) {
                maxValue = vector[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    private DataCell[] postProcessResult(final double[] result) {
     // determine the column with highest probability
        int maxIndex = argMax(result);

        DataCell[] cells = m_includeProbs
                ? new DataCell[1 + m_targetDomainValuesCount]
                : new DataCell[1];


        if (m_includeProbs) {
            // compute probabilities of the target categories
            for (int i = 0; i < m_targetCategories.size(); i++) {
                    cells[m_targetCategoryIndex.get(i)] = calculateProbability(result, i);
            }
        }
        // the last cell is the prediction
        cells[cells.length - 1] = m_targetCategories.get(maxIndex);
        return cells;
    }

    private DoubleCell calculateProbability(final double[] logits, final int classIdx) {
     // test if calculation would overflow
        boolean overflow = false;
        for (int k = 0; k < logits.length; k++) {
            if ((logits[k] - logits[classIdx]) > OVERFLOW_LIMIT) {
                overflow = true;
            }
        }
        if (!overflow) {
            double sum = 0;
            for (int k = 0; k < logits.length; k++) {
                sum += Math.exp(logits[k] - logits[classIdx]);
            }
            return new DoubleCell(1.0 / sum);
        } else {
            return new DoubleCell(0);
        }
    }

    private DataCell[] createMissingOutput() {
        int numTargetCategories = m_targetCategories.size();

        DataCell[] cells = m_includeProbs
                                  ? new DataCell[1 + numTargetCategories]
                                  : new DataCell[1];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = DataType.getMissingCell();
        }
        return cells;
    }

    private RealMatrix getBetaMatrix() {
        ParamMatrix paramMatrix = new ParamMatrix(m_content.getParamMatrix());
        RealMatrix beta = MatrixUtils.createRealMatrix(m_parameters.size(), m_targetCategories.size());
        for (int k = 0; k < m_targetCategories.size() - 1; k++) {
            for (int i = 0; i < m_parameters.size(); i++) {
                double value = paramMatrix.getBeta(m_parameters.get(i), m_targetCategories.get(k).toString());
                beta.setEntry(i, k, value);
            }
        }
        return beta;
    }

    private static class PredictionRow {
        private final double[] m_nonZeroValues;
        private final int[] m_nonZeroIndices;

        public PredictionRow(final double[] nonZeroValues, final int[] nonZeroIndices) {
            assert nonZeroValues.length == nonZeroIndices.length;
            m_nonZeroValues = nonZeroValues;
            m_nonZeroIndices = nonZeroIndices;
        }

        public double[] sparseMarixMultiply(final RealMatrix matrix) {
            int columnCount = matrix.getColumnDimension();
            int nonZero = m_nonZeroIndices.length;
            double[] result = new double[columnCount];
            for (int i = 0; i < nonZero; i++) {
                int currentRowIndex = m_nonZeroIndices[i];
                if (currentRowIndex == -1) {
                    break;
                }
                double currentValue = m_nonZeroValues[i];
                for (int c = 0; c < columnCount; c++) {
                    result[c] += matrix.getEntry(currentRowIndex, c) * currentValue;
                }
            }
            return result;
        }
    }
}
