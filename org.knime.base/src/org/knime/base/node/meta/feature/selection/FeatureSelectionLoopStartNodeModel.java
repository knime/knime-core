/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.03.2016 (adrian): created
 */
package org.knime.base.node.meta.feature.selection;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.workflow.LoopStartNodeTerminator;

/**
 * Node Model of the start of a feature selection loop.
 *
 * @author Adrian Nembach, KNIME.com
 */
public class FeatureSelectionLoopStartNodeModel extends NodeModel implements LoopStartNodeTerminator {

    private FeatureSelectionLoopStartSettings m_settings = new FeatureSelectionLoopStartSettings();

    private FeatureSelector m_featureSelector;

    private int m_iteration = 0;

    private int m_maxIterations;

    /**
     * @param nrPorts the number of in ports
    *
     */
    public FeatureSelectionLoopStartNodeModel(final int nrPorts) {
        super(nrPorts, nrPorts);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        for (int i = 1; i < inSpecs.length; i++) {
            if (!inSpecs[0].equalStructure(inSpecs[i])) {
                throw new InvalidSettingsException("All input tables must have the same structure");
            }
        }

        final DataColumnSpecFilterConfiguration filterConfig = m_settings.getStaticColumnsFilterConfiguration();
        final FilterResult filterResult = filterConfig.applyTo(inSpecs[0]);
        final String[] constantColumns;
        // check if no feature columns are selected (e.g. before the dialog is opened the first time)
        if (filterResult.getIncludes().length == 0) {
            throw new InvalidSettingsException("No feature columns selected. Please specifiy the feature columns in the dialog.");
        }

        if (filterResult.getExcludes().length == 0) {
            setWarningMessage("No constant columns selected. Are you sure you don't need a target column?");
        }
        constantColumns = filterResult.getExcludes();

        for (String colName : constantColumns) {
            if (!inSpecs[0].containsName(colName)) {
                throw new InvalidSettingsException(
                    "The specified constant column \"" + colName + "\" is not contained in the input tables.");
            }
        }
        if (m_iteration == 0) {
            // get feature selector
            try {
                final AbstractColumnHandler columnHandler =
                    new DefaultColumnHandler(Arrays.asList(constantColumns), inSpecs[0]);
                final FeatureSelectionStrategy strategy =
                    FeatureSelectionStrategies.createFeatureSelectionStrategy(m_settings.getSelectionStrategy(),
                        m_settings.getNrFeaturesThreshold(), columnHandler.getAvailableFeatures());
                m_featureSelector = new FeatureSelector(strategy, columnHandler);
                // push max iterations flowvariable
                m_maxIterations = m_featureSelector.getNumberOfIterations();
                pushFlowVariableInt("maxIterations", m_maxIterations);
            } catch (Throwable t) {
                throw new InvalidSettingsException("Exception during feature selector setup", t);
            }
        }

        // push flowvariables
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableString("currentFeature", m_featureSelector.getCurrentFeatureName());

        final DataTableSpec outSpec = m_featureSelector.getOutSpec(inSpecs[0]);
        final DataTableSpec[] outSpecs = new DataTableSpec[inSpecs.length];
        Arrays.fill(outSpecs, outSpec);

        return outSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        // push flowvariables
        pushFlowVariableInt("maxIterations", m_maxIterations);
        pushFlowVariableInt("currentIteration", m_iteration);
        pushFlowVariableString("currentFeature", m_featureSelector.getCurrentFeatureName());

        m_iteration++;

        return m_featureSelector.getNextTables(exec, inData);
    }



    FeatureSelector getFeatureSelector() {
        return m_featureSelector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean terminateLoop() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.save(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_iteration = 0;
    }

}
