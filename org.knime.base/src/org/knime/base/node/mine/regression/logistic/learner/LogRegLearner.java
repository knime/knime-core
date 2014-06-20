/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 * History
 *   22.01.2010 (Heiko Hofer): created
 */
package org.knime.base.node.mine.regression.logistic.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DomainCreatorColumnSelection;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;

/**
 * Logistic Regression Learner implementation.
 *
 * @author Heiko Hofer
 */
public final class LogRegLearner {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(LogRegLearner.class);

    private final LogRegLearnerSettings m_settings;

    private Learner m_learner;

    private PMMLPortObjectSpec m_pmmlOutSpec;

    /**
     * @param specs The input specs.
     * @param settings The settings object.
     * @throws InvalidSettingsException when settings are not consistent
     * @see LogRegLearnerNodeModel#configure(PortObjectSpec[])
     */
    public LogRegLearner(final PortObjectSpec[] specs,
            final LogRegLearnerSettings settings)
            throws InvalidSettingsException {
        m_settings = settings;
        DataTableSpec dataSpec = (DataTableSpec)specs[0];
        PMMLPortObjectSpec pmmlSpec = (PMMLPortObjectSpec)specs[1];
        init(dataSpec, pmmlSpec);
    }


    /**
     * Compute logistic regression model.
     *
     * @param portObjects The input objects.
     * @param exec the execution context
     * @return a {@link LogisticRegressionContent} storing computed data
     * @throws Exception if computation of the logistic regression model is
     * not successful or if given data is inconsistent with the settings
     * defined in the constructor.
     * @see LogRegLearnerNodeModel#execute(PortObjectSpec[])
     */
    public LogisticRegressionContent execute(
            final PortObject[] portObjects, final ExecutionContext exec)
    throws Exception {
        BufferedDataTable data = (BufferedDataTable)portObjects[0];
        PMMLPortObject inPMMLPort = (PMMLPortObject)portObjects[1];
        PMMLPortObjectSpec inPMMLSpec = inPMMLPort.getSpec();
        init(data.getDataTableSpec(), inPMMLSpec);
        // the learner typically needs five steps with two runs over the data each step, calculating
        // the domain needs run over the data
        double calcDomainTime = 1.0 / (5.0 * 2.0 + 1.0);
        exec.setMessage("Analyzing categorical data.");
        DataTable dataTable = recalcDomainForTargetAndLearningFields(data, inPMMLSpec,
            exec.createSubExecutionContext(calcDomainTime));
        exec.setMessage("Computing logistic regression model.");
        return m_learner.perform(dataTable, exec.createSubExecutionContext(1.0 - calcDomainTime));
    }

    private DataTable recalcDomainForTargetAndLearningFields(
            final BufferedDataTable data, final PMMLPortObjectSpec inPMMLSpec,
            final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        final String targetCol = m_pmmlOutSpec.getTargetFields().get(0);
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(data.getDataTableSpec(),
            new DomainCreatorColumnSelection() {

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return false;
            }

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getName().equals(targetCol) || (colSpec.getType().isCompatible(NominalValue.class)
                  && m_pmmlOutSpec.getLearningFields().contains(colSpec.getName()));
            }
        }, new DomainCreatorColumnSelection() {

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return false;
            }

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return false;
            }
        });
        domainCreator.updateDomain(data, exec);

        DataTableSpec spec = domainCreator.createSpec();
        DataTable newDataTable = exec.createSpecReplacerTable(data, spec);
        // initialize m_learner so that it has the correct DataTableSpec of
        // the input
        init(newDataTable.getDataTableSpec(), inPMMLSpec);
        return newDataTable;
    }

    /**
     * @return  The spec of the output.
     * @throws InvalidSettingsException If settings and spec given in
     * the constructor are invalid.
     */
    public PortObjectSpec[] getOutputSpec() throws InvalidSettingsException {
        DataTableSpec tableOutSpec = new DataTableSpec(
                "Coefficients and Statistics", new String[] {
                "Logit", "Variable", "Coeff.", "Std. Err.", "z-score"
                , "P>|z|"},
                new DataType[] {StringCell.TYPE, StringCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE
                , DoubleCell.TYPE});
        return new PortObjectSpec[] {m_pmmlOutSpec, tableOutSpec};
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec,
            final PMMLPortObjectSpec pmmlSpec)
            throws InvalidSettingsException {

        List<String> inputCols = new ArrayList<String>();
        for (DataColumnSpec column : inSpec) {
            inputCols.add(column.getName());
        }
        if (!m_settings.getIncludeAll()) {
            List<String> included =
                Arrays.asList(m_settings.getIncludedColumns());
            if (!inputCols.containsAll(included)) {
                LOGGER.warn("Input does not contain all learning columns. "
                        + "Proceed with the remaining learning columns.");
            }
            inputCols.retainAll(included);
        }
        inputCols.remove(m_settings.getTargetColumn());
        if (inputCols.isEmpty()) {
            throw new InvalidSettingsException("At least one column must "
                    + "be included.");
        }

        DataColumnSpec targetColSpec = null;
        List<DataColumnSpec> regressorColSpecs =
            new ArrayList<DataColumnSpec>();

        // Auto configuration when target is not set
        if (null == m_settings.getTargetColumn()
                && m_settings.getIncludeAll()) {
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataColumnSpec colSpec = inSpec.getColumnSpec(i);
                String colName = colSpec.getName();
                inputCols.remove(colName);
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                     m_settings.setTargetColumn(colName);
                }
            }
            // when there is no column with nominal data
            if (null == m_settings.getTargetColumn()) {
                throw new InvalidSettingsException("No column in "
                        + "spec compatible to \"NominalValue\".");
            }
        }


        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            String colName = colSpec.getName();
            if (m_settings.getTargetColumn().equals(colName)) {
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    targetColSpec = colSpec;
                } else {
                    throw new InvalidSettingsException("Type of column \""
                            + colName + "\" is not nominal.");
                }
            } else if (inputCols.contains(colName)) {
                if (colSpec.getType().isCompatible(DoubleValue.class)
                        || colSpec.getType().isCompatible(NominalValue.class)) {
                    regressorColSpecs.add(colSpec);
                } else {
                    throw new InvalidSettingsException("Type of column \""
                            + colName + "\" is not one of the allowed types, "
                            + "which are numeric or nomial.");
                }
            }
        }

        if (null != targetColSpec) {
            // Check if target has at least two categories.
            final Set<DataCell> targetValues = targetColSpec.getDomain().getValues();
            if (targetValues != null && targetValues.size() < 2) {
                throw new InvalidSettingsException("The target column \""
                        + targetColSpec.getName() + "\" has one value, only. "
                        + "At least two target categories are expected.");
            }

            String[] learnerCols = new String[regressorColSpecs.size() + 1];
            for (int i = 0; i < regressorColSpecs.size(); i++) {
                learnerCols[i] = regressorColSpecs.get(i).getName();
            }
            learnerCols[learnerCols.length - 1] = targetColSpec.getName();
            PMMLPortObjectSpecCreator creator =
                new PMMLPortObjectSpecCreator(pmmlSpec, inSpec);
            creator.setTargetCols(Arrays.asList(targetColSpec));
            creator.setLearningCols(regressorColSpecs);
            m_pmmlOutSpec = creator.createSpec();
            m_learner = new Learner(m_pmmlOutSpec, m_settings.getTargetReferenceCategory(),
                m_settings.getSortTargetCategories(), m_settings.getSortIncludesCategories());
        } else {
            throw new InvalidSettingsException("The target is "
                    + "not in the input.");
        }
    }

    /**
     * Validates the settings in the passed <code>NodeSettings</code> object.
     * The specified settings is checked for completeness and
     * consistency.
     *
     * @param settings The settings to validate.
     * @throws InvalidSettingsException If the validation of the settings
     *             failed.
     */
    public static void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        LogRegLearnerSettings s = new LogRegLearnerSettings();
        s.loadSettings(settings);

        String target = s.getTargetColumn();
        if (target == null) {
            throw new InvalidSettingsException("No target set.");
        }

        if (!s.getIncludeAll()) {
            // check for null in the includes
            List<String> includes = Arrays.asList(s.getIncludedColumns());
            if (includes.contains(null)) {
                throw new InvalidSettingsException("Included columns "
                        + "must not contain null values");
            }
            if (includes.contains(target)) {
                throw new InvalidSettingsException("Included columns "
                        + "must not contain target value: " + target);
            }
        }
    }

}
