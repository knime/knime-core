/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
package org.knime.base.node.mine.regression.logistic.predict;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionPortObject;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

import Jama.Matrix;

/**
 * A Predictor for a logistic regression model.
 *
 * @author Heiko Hofer
 */
public final class LogRegPredictor extends AbstractCellFactory {
    private PMMLGeneralRegressionContent m_content;
    private PPMatrix m_ppMatrix;
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

    /**
     * Creates the spec of the output if possible.
     *
     * @param portSpec the spec of the pmml input port
     * @param tableSpec the spec of the data input port
     * @param includeProbabilites add probabilities to the output
     * @return The spec of the output or null
     * @throws InvalidSettingsException when tableSpec and portSpec do not match
     */
    public static DataColumnSpec[] createColumnSpec(
            final PMMLPortObjectSpec portSpec,
            final DataTableSpec tableSpec,
            final boolean includeProbabilites) throws InvalidSettingsException {
        // Assertions
        if (portSpec.getTargetCols().isEmpty()) {
            throw new InvalidSettingsException("The general regression model"
                    + " does not specify a target column.");
        }

        for (DataColumnSpec learningColSpec : portSpec.getLearningCols()) {
            String learningCol = learningColSpec.getName();
            if (tableSpec.containsName(learningCol)) {
                DataColumnSpec colSpec = tableSpec.getColumnSpec(learningCol);
                if (learningColSpec.getType().isCompatible(NominalValue.class)
                    && !colSpec.getType().isCompatible(NominalValue.class)) {
                    throw new InvalidSettingsException("The column \""
                            + learningCol + "\" in the table of prediction "
                            + "is expected to be  compatible with "
                            + "\"NominalValue\".");
                } else if (learningColSpec.getType().isCompatible(
                        DoubleValue.class)
                        && !colSpec.getType().isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("The column \""
                            + learningCol + "\" in the table of prediction "
                            + "is expected to be numeric.");
                }
            } else {
                throw new InvalidSettingsException("The table for prediction "
                        + "does not contain the column \""
                        + learningCol + "\".");
            }
        }

        // The list of added columns
        List<DataColumnSpec> newColsSpec = new ArrayList<DataColumnSpec>();
        String targetCol = portSpec.getTargetFields().get(0);

        String oldTargetName = targetCol;
        if (tableSpec.containsName(oldTargetName)
                && !oldTargetName.toLowerCase().endsWith("(prediction)")) {
            oldTargetName = oldTargetName + " (prediction)";
        }
        String newTargetColName =
            DataTableSpec.getUniqueColumnName(tableSpec, oldTargetName);
        DataColumnSpec targetColSpec =
            portSpec.getDataTableSpec().getColumnSpec(targetCol);
        DataColumnSpecCreator targetColSpecCreator =
                new DataColumnSpecCreator(newTargetColName,
                        targetColSpec.getType());
        DataColumnDomainCreator targetDomainCreator =
                new DataColumnDomainCreator(targetColSpec.getDomain());
        targetColSpecCreator.setDomain(targetDomainCreator.createDomain());
        newColsSpec.add(targetColSpecCreator.createSpec());

        if (includeProbabilites) {
            if (!targetColSpec.getDomain().hasValues()) {
                return null;
            }
            List<DataCell> targetValues = new ArrayList<DataCell>();
            targetValues.addAll(targetColSpec.getDomain().getValues());
            Collections.sort(targetValues,
                    targetColSpec.getType().getComparator());
            for (DataCell value : targetValues) {
                String name = "P(" + targetCol + "=" + value.toString() + ")";
                String newColName =
                        DataTableSpec.getUniqueColumnName(tableSpec, name);
                DataColumnSpecCreator colSpecCreator =
                        new DataColumnSpecCreator(newColName, DoubleCell.TYPE);
                DataColumnDomainCreator domainCreator =
                        new DataColumnDomainCreator(new DoubleCell(0.0),
                                new DoubleCell(1.0));
                colSpecCreator.setDomain(domainCreator.createDomain());
                newColsSpec.add(colSpecCreator.createSpec());
            }
        }


        return newColsSpec.toArray(new DataColumnSpec[0]);
    }


    /**
     * This constructor should be used during the configure phase of a node.
     * The created instance will give a valid spec of the output but cannot
     * be used to compute the cells.
     *
     * @param portSpec the spec of the pmml input port
     * @param tableSpec the spec of the data input port
     * @param includeProbabilites add probabilities to the output
     * @throws InvalidSettingsException when tableSpec and portSpec do not match
     */
    public LogRegPredictor(final PMMLPortObjectSpec portSpec,
            final DataTableSpec tableSpec,
            final boolean includeProbabilites
            ) throws InvalidSettingsException {
        super(LogRegPredictor.createColumnSpec(portSpec, tableSpec,
                includeProbabilites));
    }

    /**
     * This constructor should be used when executing the node. Use it when
     * you want to compute output cells.
     *
     * @param regModel the data of the pmml input port
     * @param inSpec the spec of the data input port
     * @param includeProbabilites add probabilities to the output
     * @throws InvalidSettingsException when inSpec and regModel do not match
     */
    public LogRegPredictor(final PMMLGeneralRegressionPortObject regModel,
            final DataTableSpec inSpec, final boolean includeProbabilites)
            throws InvalidSettingsException {
        super(LogRegPredictor.createColumnSpec(regModel.getSpec(), inSpec,
                includeProbabilites));

        m_includeProbs = includeProbabilites;
        m_content = regModel.getContent();
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
        m_trainingSpec = regModel.getSpec().getDataTableSpec();
        DataColumnSpec targetCol =
            m_trainingSpec.getColumnSpec(regModel.getTargetVariableName());
        m_targetCategories = new ArrayList<DataCell>();
        m_targetCategories.addAll(targetCol.getDomain().getValues());
        Collections.sort(m_targetCategories,
                targetCol.getType().getComparator());

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
     * {@inheritDoc}
     */
    public DataCell[] getCells(final DataRow row) {
        if (hasMissingValues(row)) {
            return createMissingOutput();
        }

        int numTargetCategories = m_targetCategories.size();

        DataCell[] cells = m_includeProbs
                                ? new DataCell[1 + numTargetCategories]
                                : new DataCell[1];
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
                    x.set(0, i + index - 1, 1);
                    i += values.size() - 2;
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
        cells[0] = m_targetCategories.get(maxIndex);

        if (m_includeProbs) {
            // compute probabilities of the target categories
            for (int i = 0; i < numTargetCategories; i++) {
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
                    cells[i + 1] = new DoubleCell(1.0 / sum);
                } else {
                    cells[i + 1] = new DoubleCell(0);
                }
            }
        }
        return cells;
    }


    private boolean hasMissingValues(final DataRow row) {
        for (DataCell cell : row) {
            if (cell.isMissing()) {
                return true;
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