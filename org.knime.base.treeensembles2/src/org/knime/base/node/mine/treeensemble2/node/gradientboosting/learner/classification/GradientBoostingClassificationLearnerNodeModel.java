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
 *   14.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.classification;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble2.data.TreeData;
import org.knime.base.node.mine.treeensemble2.data.TreeDataCreator;
import org.knime.base.node.mine.treeensemble2.learner.gradientboosting.AbstractGradientBoostingLearner;
import org.knime.base.node.mine.treeensemble2.learner.gradientboosting.LKGradientBoostedTreesLearner;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.learner.GradientBoostingLearnerConfiguration;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration.FilterLearnColumnRearranger;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;

/**
 *
 * @author Adrian Nembach
 */
public class GradientBoostingClassificationLearnerNodeModel extends NodeModel {

    private GradientBoostingLearnerConfiguration m_configuration;

    /**
     * @param inPortTypes
     * @param outPortTypes
     */
    protected GradientBoostingClassificationLearnerNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE}, new PortType[]{GradientBoostingModelPortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // guaranteed to not be null (according to API)
        DataTableSpec inSpec = (DataTableSpec)inSpecs[0];
        if (m_configuration == null) {
            throw new InvalidSettingsException("No configuration available");
        }
        final FilterLearnColumnRearranger learnRearranger = m_configuration.filterLearnColumns(inSpec);
//        final String warn = learnRearranger.getWarning();
//        if (warn != null) {
//            setWarningMessage(warn);
//        }
        DataTableSpec learnSpec = learnRearranger.createSpec();
        TreeEnsembleModelPortObjectSpec ensembleSpec = m_configuration.createPortObjectSpec(learnSpec);
        ensembleSpec.assertTargetTypeMatches(false);
        // the following call may return null, which is OK during configure
        // but not upon execution (spec may not be populated yet, e.g.
        // predecessor not executed)
        // if the possible values is not null, the following call checks
        // for duplicates in the toString() representation
        ensembleSpec.getTargetColumnPossibleValueMap();

        return new PortObjectSpec[]{ensembleSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        BufferedDataTable t = (BufferedDataTable)inData[0];
        DataTableSpec spec = t.getDataTableSpec();
        final FilterLearnColumnRearranger learnRearranger = m_configuration.filterLearnColumns(spec);
        String warn = learnRearranger.getWarning();
        BufferedDataTable learnTable = exec.createColumnRearrangeTable(t, learnRearranger, exec.createSubProgress(0.0));
        DataTableSpec learnSpec = learnTable.getDataTableSpec();
        TreeEnsembleModelPortObjectSpec ensembleSpec = m_configuration.createPortObjectSpec(learnSpec);
        ExecutionMonitor readInExec = exec.createSubProgress(0.1);
        ExecutionMonitor learnExec = exec.createSubProgress(0.8);
        TreeDataCreator dataCreator = new TreeDataCreator(m_configuration, learnSpec, learnTable.getRowCount());
        exec.setProgress("Reading data into memory");
        TreeData data = dataCreator.readData(learnTable, m_configuration, readInExec);
//        m_hiliteRowSample = dataCreator.getDataRowsForHilite();
//        m_viewMessage = dataCreator.getViewMessage();
        String dataCreationWarning = dataCreator.getAndClearWarningMessage();
        if (dataCreationWarning != null) {
            if (warn == null) {
                warn = dataCreationWarning;
            } else {
                warn = warn + "\n" + dataCreationWarning;
            }
        }
        readInExec.setProgress(1.0);
        exec.setMessage("Learning trees");
        AbstractGradientBoostingLearner learner = new LKGradientBoostedTreesLearner(m_configuration, data);
        AbstractGradientBoostingModel model;
//        m_configuration.setMissingValueHandling(MissingValueHandling.XGBoost);
//        try {
            model = learner.learn(learnExec);
//        } catch (ExecutionException e) {
//            Throwable cause = e.getCause();
//            if (cause instanceof Exception) {
//                throw (Exception)cause;
//            }
//            throw e;
//        }
        GradientBoostingModelPortObject modelPortObject = new GradientBoostingModelPortObject(ensembleSpec, model);
        learnExec.setProgress(1.0);
        if (warn != null) {
            setWarningMessage(warn);
        }
        return new PortObject[]{modelPortObject};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        new GradientBoostingLearnerConfiguration(false).loadInModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        GradientBoostingLearnerConfiguration config = new GradientBoostingLearnerConfiguration(false);
        config.loadInModel(settings);
        m_configuration = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}
