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
package org.knime.base.node.mine.regression.predict3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLParameter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;


/**
 * A Predictor for a linear regression model.
 *
 * @author Heiko Hofer
 * @author Adrian Nembach, KNIME.com
 */
final class LinReg2Predictor extends RegressionPredictorCellFactory {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(LinReg2Predictor.class);
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

    // beta column vector
    // length of beta rows
    // one column
    private RealMatrix m_beta = null;

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
    public LinReg2Predictor(final PMMLGeneralRegressionContent content,
            final DataTableSpec inSpec,
            final PMMLPortObjectSpec  portSpec,
            final String targetVariableName,
            final RegressionPredictorSettings settings)
            throws InvalidSettingsException {
        super(portSpec, inSpec, settings);

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

        m_values = determineFactorValues(m_content, m_trainingSpec);
        m_factors = m_values.keySet();
        m_beta = getBetaMatrix();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell[] getCells(final DataRow row) {
        if (hasMissingValues(row)) {
            return createMissingOutput();
        }

        DataCell[] cells = new DataCell[1];

        // column vector
        RealMatrix x = MatrixUtils.createRealMatrix(1, m_parameters.size());
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
                x.setEntry(0, i, 1);
            } else {
                if (m_factors.contains(predictor)) {
                    List<DataCell> values = m_values.get(predictor);
                    DataCell cell = row.getCell(m_parameterI.get(parameter));
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

        double estimate = r.getEntry(0, 0);
        if (m_content.getOffsetValue() != null) {
            estimate = estimate + m_content.getOffsetValue();
        }
        cells[0] = new DoubleCell(estimate);

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
        return new DataCell[] {DataType.getMissingCell()};
    }

    private RealMatrix getBetaMatrix() {
        ParamMatrix paramMatrix = new ParamMatrix(m_content.getParamMatrix());
        RealMatrix beta = MatrixUtils.createRealMatrix(m_parameters.size(), 1);
        for (int i = 0; i < m_parameters.size(); i++) {
            double value = paramMatrix.getBeta(m_parameters.get(i), null);
            beta.setEntry(i, 0, value);
        }
        return beta;
    }


}
