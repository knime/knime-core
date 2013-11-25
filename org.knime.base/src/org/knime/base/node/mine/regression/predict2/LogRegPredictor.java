/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   19.05.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.predict2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPCell;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
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

import Jama.Matrix;

/**
 * A Predictor for a logistic regression model.
 *
 * @author Heiko Hofer
 */
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
    private DataTableSpec m_trainingSpec;
    private Set<String> m_factors;
    private Map<String, List<DataCell>> m_values;
    private List<DataCell> m_targetCategories;
    // matrix
    // Number of Rows: dim(x)
    // Number of Cols: numTargetCategories
    private Matrix m_beta = null;
    private boolean m_includeProbs;
    /** maps the indices of the values from m_targetCategories to the domain values of the target column. */
    private Map<Integer, Integer> m_targetCategoryIndex;
    /** the number of domain values of the target column. */
    private int m_targetDomainValuesCount;

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
        m_ppMatrix = new PPMatrix(m_content.getPPMatrix());
        m_parameters = new ArrayList<String>();
        m_predictors = new ArrayList<String>();
        m_parameterI = new HashMap<String, Integer>();
        for (PMMLParameter parameter : m_content.getParameterList()) {
            m_parameters.add(parameter.getName());
            String predictor = m_ppMatrix.getPredictor(parameter.getName());
            m_predictors.add(predictor);
            m_parameterI.put(parameter.getName(),
                    inSpec.findColumnIndex(predictor));
        }

        m_trainingSpec = portSpec.getDataTableSpec();
        DataColumnSpec targetColSpec = m_trainingSpec.getColumnSpec(targetVariableName);

        m_targetCategories = determineTargetCategories(targetColSpec, content);
        m_targetCategoryIndex = createTargetCategoryToOutputMap(m_targetCategories, targetColSpec);
        m_targetDomainValuesCount = targetColSpec.getDomain().getValues().size();

        m_values = new HashMap<String, List<DataCell>>();

        for (PMMLPredictor factor : m_content.getFactorList()) {
            String factorName = factor.getName();
            DataColumnSpec colSpec = m_trainingSpec.getColumnSpec(factorName);
            List<DataCell> values = new ArrayList<DataCell>();
            values.addAll(colSpec.getDomain().getValues());
            Collections.sort(values, colSpec.getType().getComparator());
            m_values.put(factorName, values);
        }
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
        List<DataCell> targetCategories = new ArrayList<DataCell>();
        targetCategories.addAll(targetCol.getDomain().getValues());
        Collections.sort(targetCategories,
                targetCol.getType().getComparator());
        // Collect target categories from model
        Set<String> modelTargetCategories = new HashSet<String>();
        for (PMMLPCell cell : content.getParamMatrix()) {
            modelTargetCategories.add(cell.getTargetCategory());
        }
        String targetReferenceCategory = content.getTargetReferenceCategory();
        if (targetReferenceCategory == null || targetReferenceCategory.isEmpty()) {
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
        modelTargetCategories.add(targetReferenceCategory);
        // Remove all target categories not found in the logistic regression model. When PMML is load from file the
        // spec of the target column may contain more domain values than used for training the
        // logistic regression model.
        for (Iterator<DataCell> iter = targetCategories.iterator(); iter.hasNext();) {
            String str = iter.next().toString();
            if (!modelTargetCategories.contains(str)) {
                iter.remove();
            }
        }
        return targetCategories;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        if (hasMissingValues(row)) {
            return createMissingOutput();
        }

        DataCell[] cells = m_includeProbs
                                ? new DataCell[1 + m_targetDomainValuesCount]
                                : new DataCell[1];
        Arrays.fill(cells, new IntCell(0));

        // column vector
        Matrix x = new Matrix(1, m_parameters.size());
        for (int i = 0; i < m_parameters.size(); i++) {
            String parameter = m_parameters.get(i);
            String predictor = null;
            String value = null;
            boolean rowIsEmpty = true;
            for (Iterator<String> iter = m_predictors.iterator();
            iter.hasNext();) {
                predictor = iter.next();
                value =
                    m_ppMatrix.getValue(parameter, predictor, null);
                if (null != value) {
                    rowIsEmpty = false;
                    break;
                }
            }
            if (rowIsEmpty) {
                x.set(0, i, 1);
            } else {
                if (m_factors.contains(predictor)) {
                    List<DataCell> values = m_values.get(predictor);
                    DataCell cell =
                        row.getCell(m_parameterI.get(parameter));
                    int index = values.indexOf(cell);
                    // these are design variables
                    /* When building ageneral regression model, for each
                    categorical fields, there is one category used as the
                    default baseline and therefore it didn't show in the
                    ParameterList in PMML. This design for the training is fine,
                    but in the prediction, when the input of Employment is
                    the default baseline, the parameters should all be 0.
                    See the commit message for an example and more details.
                    */
                    if (index > 0) {
                        x.set(0, i + index - 1, 1);
                        i += values.size() - 2;
                    }
                } else {
                    DataCell cell =
                        row.getCell(m_parameterI.get(parameter));
                    double radix = ((DoubleValue)cell).getDoubleValue();
                    double exponent = Integer.valueOf(value);
                    x.set(0, i, Math.pow(radix, exponent));
                }
            }
        }


        // column vector
        Matrix r = x.times(m_beta);

        // determine the column with highest probability
        int maxIndex = 0;
        double maxValue = r.get(0, 0);
        for (int i = 1; i < r.getColumnDimension(); i++) {
            if (r.get(0, i) > maxValue) {
                maxValue = r.get(0, i);
                maxIndex = i;
            }
        }


        if (m_includeProbs) {
            // compute probabilities of the target categories
            for (int i = 0; i < m_targetCategories.size(); i++) {
                // test if calculation would overflow
                boolean overflow = false;
                for (int k = 0; k < r.getColumnDimension(); k++) {
                    if ((r.get(0, k) - r.get(0, i)) > 700) {
                        overflow = true;
                    }
                }
                if (!overflow) {
                    double sum = 0;
                    for (int k = 0; k < r.getColumnDimension(); k++) {
                        sum += Math.exp(r.get(0, k) - r.get(0, i));
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

    private Matrix getBetaMatrix() {
        ParamMatrix paramMatrix =
            new ParamMatrix(m_content.getParamMatrix());
        Matrix beta = new Matrix(m_parameters.size(),
                m_targetCategories.size());
        for (int i = 0; i < m_parameters.size(); i++) {
            for (int k = 0; k < m_targetCategories.size() - 1; k++) {
                double value = paramMatrix.getBeta(m_parameters.get(i),
                        m_targetCategories.get(k).toString());
                beta.set(i, k, value);
            }
        }
        return beta;
    }


}
