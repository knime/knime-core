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
 *   Jan 10, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.node.regressiontree.predictor;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.mine.treeensemble.model.RegressionTreeModelPortObject;
import org.knime.base.node.mine.treeensemble.model.RegressionTreeModelPortObjectSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
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
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class RegressionTreePredictorNodeModel extends NodeModel {

    private RegressionTreePredictorConfiguration m_configuration;

    /**
     *  */
    RegressionTreePredictorNodeModel() {
        super(new PortType[]{RegressionTreeModelPortObject.TYPE, BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE});
    }

    /** {@inheritDoc} */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        RegressionTreeModelPortObjectSpec modelSpec = (RegressionTreeModelPortObjectSpec)inSpecs[0];
        String targetColName = modelSpec.getTargetColumn().getName();
        if (m_configuration == null) {
            m_configuration = RegressionTreePredictorConfiguration.createDefault(targetColName);
        } else if (!m_configuration.isChangePredictionColumnName()) {
            m_configuration.setPredictionColumnName(RegressionTreePredictorConfiguration.getPredictColumnName(targetColName));
        }
//        modelSpec.assertTargetTypeMatches(true);
        DataTableSpec dataSpec = (DataTableSpec)inSpecs[1];
        final RegressionTreePredictor pred = new RegressionTreePredictor(modelSpec, null, dataSpec, m_configuration);
        ColumnRearranger rearranger = pred.getPredictionRearranger();
        // rearranger may be null if confidence values are appended but the
        // model does not have a list of possible target values
        DataTableSpec outSpec = rearranger != null ? rearranger.createSpec() : null;
        return new DataTableSpec[]{outSpec};
    }

    /** {@inheritDoc} */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        RegressionTreeModelPortObject model = (RegressionTreeModelPortObject)inObjects[0];
        RegressionTreeModelPortObjectSpec modelSpec = model.getSpec();
        BufferedDataTable data = (BufferedDataTable)inObjects[1];
        DataTableSpec dataSpec = data.getDataTableSpec();
        final RegressionTreePredictor pred = new RegressionTreePredictor(modelSpec, model, dataSpec, m_configuration);
        ColumnRearranger rearranger = pred.getPredictionRearranger();
        BufferedDataTable outTable = exec.createColumnRearrangeTable(data, rearranger, exec);
        return new BufferedDataTable[]{outTable};
    }

    /** {@inheritDoc} */
    @Override
    protected void reset() {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_configuration != null) {
            m_configuration.save(settings);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        RegressionTreePredictorConfiguration config = new RegressionTreePredictorConfiguration("");
        config.loadInModel(settings);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        RegressionTreePredictorConfiguration config = new RegressionTreePredictorConfiguration("");
        config.loadInModel(settings);
        m_configuration = config;
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no internals
    }

}
