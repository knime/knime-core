/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   19.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict2;

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
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;


/**
 * A Predictor for a logistic regression model.
 * <p>Despite being public no official API.
 * @author Heiko Hofer
 * @deprecated as of 3.5.0
 */
@Deprecated
public final class LogRegPredictor extends RegressionPredictorCellFactory {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LogRegPredictor.class);
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
        m_baseLabelToColName = Arrays.asList(m_content.getParameterList()).stream().collect(Collectors.toMap(k -> k.getName(), v->{
            final String param = v.getLabel();
            return param.matches("(.*?)\\[(\\d+)\\]") ?
                param.substring(0, param.lastIndexOf('['))
                : param;}));
        m_ppMatrix = new PPMatrix(m_content.getPPMatrix());
        m_parameters = new ArrayList<String>();
        m_predictors = new ArrayList<String>();
        m_vectorLengths = m_content.getVectorLengths();
        m_parameterI = new HashMap<String, Integer>();
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

        m_trainingSpec = portSpec.getDataTableSpec();
        DataColumnSpec targetColSpec = m_trainingSpec.getColumnSpec(targetVariableName);

        m_targetCategories = determineTargetCategories(targetColSpec, content);
        m_targetCategoryIndex = createTargetCategoryToOutputMap(m_targetCategories, targetColSpec);
        m_targetDomainValuesCount = targetColSpec.getDomain().getValues().size();

        m_values = determineFactorValues(m_content, m_trainingSpec);
        m_factors = m_values.keySet();
        m_beta = getBetaMatrix();
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
        Map<Integer, Integer> targetCategoryIndex = new HashMap<Integer, Integer>();
        List<DataCell> domainValues = new ArrayList<DataCell>();
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
        Map<String, DataCell> domainValues = new HashMap<String, DataCell>();
        for (DataCell cell : targetCol.getDomain().getValues()) {
            domainValues.put(cell.toString(), cell);
        }
        // Collect target categories from model
        Set<DataCell> modelTargetCategories = new LinkedHashSet<DataCell>();
        for (PMMLPCell cell : content.getParamMatrix()) {
            modelTargetCategories.add(domainValues.get(cell.getTargetCategory()));
        }
        String targetReferenceCategory = content.getTargetReferenceCategory();
        if (targetReferenceCategory == null || targetReferenceCategory.isEmpty()) {
            List<DataCell> targetCategories = new ArrayList<DataCell>();
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

        List<DataCell> toReturn = new ArrayList<DataCell>();
        toReturn.addAll(modelTargetCategories);
        return toReturn;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        if (hasMissingValues(row)) {
            return createMissingOutput();
        }
        final MissingHandling missingHandling = new MissingHandling(true);

        DataCell[] cells = m_includeProbs
                                ? new DataCell[1 + m_targetDomainValuesCount]
                                : new DataCell[1];
        Arrays.fill(cells, new IntCell(0));

        // column vector
        final RealMatrix x = MatrixUtils.createRealMatrix(1, m_parameters.size());
        for (int i = 0; i < m_parameters.size(); i++) {
            String parameter = m_parameters.get(i);
            String predictor = null;
            String value = null;
            boolean rowIsEmpty = true;
            for (final Iterator<String> iter = m_predictors.iterator(); iter.hasNext();) {
                predictor = iter.next();
                value = m_ppMatrix.getValue(parameter, predictor, null);
                if (null != value) {
                    rowIsEmpty = false;
                    break;
                }
            }
            if (rowIsEmpty) {
                x.setEntry(0, i, 1);
            } else {
                if (m_factors.contains(predictor)) {
                    List<DataCell> values = m_values.get(predictor);
                    DataCell cell =
                        row.getCell(m_parameterI.get(parameter));
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
                        x.setEntry(0, i + index - 1, 1);
                        i += values.size() - 2;
                    }
                } else if (m_baseLabelToColName.containsKey(parameter) && m_vectorLengths.containsKey(m_baseLabelToColName.get(parameter))) {
                    final DataCell cell = row.getCell(m_parameterI.get(parameter));
                    Optional<NameAndIndex> vectorValue = VectorHandling.parse(predictor);
                    if (vectorValue.isPresent()) {
                        int j = vectorValue.get().getIndex();

                        value = m_ppMatrix.getValue(parameter, predictor, null);
                        double exponent = Integer.valueOf(value);
                        double radix = RegressionTrainingRow.getValue(cell, j, missingHandling);
                        x.setEntry(0, i, Math.pow(radix, exponent));
                    }
                } else {
                    DataCell cell =
                            row.getCell(m_parameterI.get(parameter));

                    double radix = ((DoubleValue)cell).getDoubleValue();
                    double exponent = Integer.valueOf(value);
                    x.setEntry(0, i, Math.pow(radix, exponent));
                }
            }
        }


        // column vector
        RealMatrix r = x.multiply(m_beta);

        // determine the column with highest probability
        int maxIndex = 0;
        double maxValue = r.getEntry(0, 0);
        for (int i = 1; i < r.getColumnDimension(); i++) {
            if (r.getEntry(0, i) > maxValue) {
                maxValue = r.getEntry(0, i);
                maxIndex = i;
            }
        }


        if (m_includeProbs) {
            // compute probabilities of the target categories
            for (int i = 0; i < m_targetCategories.size(); i++) {
                // test if calculation would overflow
                boolean overflow = false;
                for (int k = 0; k < r.getColumnDimension(); k++) {
                    if ((r.getEntry(0, k) - r.getEntry(0, i)) > 700) {
                        overflow = true;
                    }
                }
                if (!overflow) {
                    double sum = 0;
                    for (int k = 0; k < r.getColumnDimension(); k++) {
                        sum += Math.exp(r.getEntry(0, k) - r.getEntry(0, i));
                    }
                    cells[m_targetCategoryIndex.get(i)] = new DoubleCell(1.0 / sum);
                } else {
                    cells[m_targetCategoryIndex.get(i)] = new DoubleCell(0);
                }
            }
        }
        // the last cell is the prediction
        cells[cells.length - 1] = m_targetCategories.get(maxIndex);
        return cells;
    }


    private boolean hasMissingValues(final DataRow row) {
        for (int i : m_parameterI.values()) {
            if (i > -1) {
               DataCell cell = row.getCell(i);
               if (cell.isMissing()) {
                   return true;
               }
           }
       }
       return false;
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
}
