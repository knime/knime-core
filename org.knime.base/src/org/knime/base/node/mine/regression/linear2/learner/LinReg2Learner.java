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
 *   22.01.2010 (Heiko Hofer): created
 */
package org.knime.base.node.mine.regression.linear2.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * Linear Regression Learner implementation.
 *
 * @author Heiko Hofer
 */
final class LinReg2Learner {
    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(LinReg2Learner.class);

    private final LinReg2LearnerSettings m_settings;

    private Learner m_learner;

    private PMMLPortObjectSpec m_pmmlOutSpec;

    private String m_warningMessage;

    /**
     * @param specs The input specs.
     * @param settings The settings object.
     * @throws InvalidSettingsException when settings are not consistent
     * @see LinReg2LearnerNodeModel#configure(PortObjectSpec[])
     */
    public LinReg2Learner(final PortObjectSpec[] specs,
            final LinReg2LearnerSettings settings)
            throws InvalidSettingsException {
        this(specs, true, settings);
    }

    /**
     * @param specs The input specs.
     * @param hasPMMLIn whether the specs contain PMML specs at position 1
     * @param settings The settings object.
     * @throws InvalidSettingsException when settings are not consistent
     * @see LinReg2LearnerNodeModel#configure(PortObjectSpec[])
     */
    public LinReg2Learner(final PortObjectSpec[] specs, final boolean hasPMMLIn,
            final LinReg2LearnerSettings settings)
            throws InvalidSettingsException {
        m_settings = settings;
        DataTableSpec dataSpec = (DataTableSpec)specs[0];
        PMMLPortObjectSpec pmmlSpec = hasPMMLIn ? (PMMLPortObjectSpec)specs[1] : null;
        init(dataSpec, pmmlSpec, Collections.<String>emptySet());
    }


    /**
     * Compute linear regression model.
     *
     * @param portObjects The input objects.
     * @param exec the execution context
     * @return a {@link LinearRegressionContent} storing computed data
     * @throws Exception if computation of the linear regression model is
     * not successful or if given data is inconsistent with the settings
     * defined in the constructor.
     * @see LinReg2LearnerNodeModel#execute(PortObject[], ExecutionContext)
     */
    public LinearRegressionContent execute(
            final PortObject[] portObjects, final ExecutionContext exec)
    throws Exception {
        BufferedDataTable data = (BufferedDataTable)portObjects[0];
        PMMLPortObject inPMMLPort = (PMMLPortObject)portObjects[1];
        PMMLPortObjectSpec inPMMLSpec = inPMMLPort.getSpec();
        init(data.getDataTableSpec(), inPMMLSpec, Collections.<String>emptySet());
        double calcDomainTime = 0.2;
        exec.setMessage("Analyzing categorical data");
        BufferedDataTable dataTable = recalcDomainOfLearningFields(data, inPMMLSpec,
            exec.createSubExecutionContext(calcDomainTime));
        exec.setMessage("Computing linear regression model");
        return m_learner.perform(dataTable, exec.createSubExecutionContext(1.0 - calcDomainTime));
    }

    private BufferedDataTable recalcDomainOfLearningFields(
            final BufferedDataTable data, final PMMLPortObjectSpec inPMMLSpec,
            final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        DataTableDomainCreator domainCreator = new DataTableDomainCreator(data.getDataTableSpec(),
            new DomainCreatorColumnSelection() {

            @Override
            public boolean dropDomain(final DataColumnSpec colSpec) {
                return true;
            }

            @Override
            public boolean createDomain(final DataColumnSpec colSpec) {
                return colSpec.getType().isCompatible(NominalValue.class)
                  && (m_pmmlOutSpec.getLearningFields().contains(colSpec.getName()) || m_pmmlOutSpec.getTargetFields().contains(colSpec.getName()));
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
        BufferedDataTable newDataTable = exec.createSpecReplacerTable(data, spec);


        // bug fix 5793, similar to 5580 in LogReg2Learner - ignore columns with too many different values.
        // But because this would change behavior, we cannot drop the domain, which means that even
        // prepending a domain calculator to this node will node help when the column has too many values.
        Set<String> columnWithTooManyDomainValues = new LinkedHashSet<>();
        for (String learningField : m_pmmlOutSpec.getLearningFields()) {
            DataColumnSpec columnSpec = spec.getColumnSpec(learningField);
            if (columnSpec.getType().isCompatible(NominalValue.class) && !columnSpec.getDomain().hasValues()) {
                columnWithTooManyDomainValues.add(learningField);
            }
        }
        // initialize m_learner so that it has the correct DataTableSpec of
        // the input
        init(newDataTable.getDataTableSpec(), inPMMLSpec, columnWithTooManyDomainValues);

        if (!columnWithTooManyDomainValues.isEmpty()) {
            StringBuilder warning = new StringBuilder();
            warning.append(columnWithTooManyDomainValues.size() == 1 ? "Column " : "Columns ");
            warning.append(ConvenienceMethods.getShortStringFrom(columnWithTooManyDomainValues, 5));
            warning.append(columnWithTooManyDomainValues.size() == 1 ? " has " : " have ");
            warning.append("too many different values - will be ignored during training");
            //warning.append("(enforce inclusion by using a domain calculator node before)");
            LOGGER.warn(warning.toString());
            m_warningMessage = (m_warningMessage == null ? "" : m_warningMessage + "\n") + warning.toString();
        }

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
                "Variable", "Coeff.", "Std. Err.", "t-value"
                , "P>|t|"},
                new DataType[] {StringCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE
                , DoubleCell.TYPE});
        return new PortObjectSpec[] {m_pmmlOutSpec, tableOutSpec};
    }

    /**
     * The warning message generated by this learner.
     * @return the warning message
     * @since 2.11
     */
    public String getWarningMessage() {
        return m_warningMessage;
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec,
            final PMMLPortObjectSpec pmmlSpec, final Set<String> exclude)
            throws InvalidSettingsException {
        m_warningMessage = null;
        // Auto configuration when target is not set
        if (m_settings.getTargetColumn() == null) {
            List<DataColumnSpec> possibleTargets = new ArrayList<DataColumnSpec>();
            for (DataColumnSpec colSpec : inSpec) {
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    possibleTargets.add(colSpec);
               }
            }
            if (possibleTargets.size() > 1) {
                String colName = possibleTargets.get(possibleTargets.size() - 1).getName();
                m_settings.setTargetColumn(colName);
                String warning = "The target column is not set. Using " + colName;
                m_warningMessage = (m_warningMessage == null ? "" : m_warningMessage + "\n") + warning;
            } else if (possibleTargets.size() == 1) {
                m_settings.setTargetColumn(possibleTargets.get(0).getName());
            } else {
                throw new InvalidSettingsException("No column in "
                        + "spec with numeric data.");
            }
        }

        FilterResult colFilter = m_settings.getFilterConfiguration().applyTo(inSpec);

        List<String> inputCols = new ArrayList<String>();
        inputCols.addAll(Arrays.asList(colFilter.getIncludes()));
        inputCols.remove(m_settings.getTargetColumn());

        // remove all columns that should not be used
        inputCols.removeAll(exclude);

        if (inputCols.isEmpty()) {
            throw new InvalidSettingsException("At least one column must be included.");
        }

        DataColumnSpec targetColSpec = null;
        List<DataColumnSpec> regressorColSpecs = new ArrayList<DataColumnSpec>();

        // Check type of target and input columns
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            String colName = colSpec.getName();
            if (m_settings.getTargetColumn().equals(colName)) {
                if (colSpec.getType().isCompatible(DoubleValue.class)) {
                    targetColSpec = colSpec;
                } else {
                    throw new InvalidSettingsException("Type of column \""
                            + colName + "\" is not numeric.");
                }
            } else if (inputCols.contains(colName)) {
                if (colSpec.getType().isCompatible(DoubleValue.class)
                        || colSpec.getType().isCompatible(NominalValue.class)) {
                    regressorColSpecs.add(colSpec);
                } else {
                    throw new InvalidSettingsException("Type of column \""
                            + colName + "\" is not one of the allowed types, "
                            + "which are numeric or nominal.");
                }
            }
        }

        if (targetColSpec != null) {
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
            m_learner = new Learner(m_pmmlOutSpec, m_settings.getIncludeConstant(), m_settings.getOffsetValue(),
                m_settings.getMissingValueHandling2().equals(MissingValueHandling.fail));
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
        LinReg2LearnerSettings s = new LinReg2LearnerSettings();
        s.loadSettings(settings);

        String target = s.getTargetColumn();
        if (target == null) {
            throw new InvalidSettingsException("No target set.");
        }
    }

}
