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
 *   Jan 18, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.node.regressiontree.predictor;

import java.util.Optional;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModel;
import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.predictor.PredictionRearrangerCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class RegressionTreePredictionHandler {

    private final RegressionTreeModel m_model;

    private final RegressionTreeModelPortObjectSpec m_modelSpec;

    private final RegressionTreePredictorConfiguration m_configuration;

    private final DataTableSpec m_dataSpec;

    /**
     * @param model the {@link RegressionTreeModel}
     * @param modelSpec the spec of <b>model</b>
     * @param dataSpec of the input table
     * @param configuration of the predictor
     */
    public RegressionTreePredictionHandler(final RegressionTreeModel model,
        final RegressionTreeModelPortObjectSpec modelSpec, final DataTableSpec dataSpec,
        final RegressionTreePredictorConfiguration configuration) {
        m_model = model;
        m_modelSpec = modelSpec;
        m_dataSpec = dataSpec;
        m_configuration = configuration;
    }

    PortObjectSpec[] configure() throws InvalidSettingsException {
        Optional<DataTableSpec> spec = createRearrangerCreator().createSpec();
        // in case of regression it must always be possible to create the spec (we just append another column)
        return new PortObjectSpec[]{spec.orElseThrow(() -> new IllegalStateException("Can't create output spec."))};
    }

    ColumnRearranger createExecutionRearranger() throws InvalidSettingsException {
        return createRearrangerCreator().createExecutionRearranger();
    }

    private PredictionRearrangerCreator createRearrangerCreator() throws InvalidSettingsException {
        int[] filterIndices = m_modelSpec.calculateFilterIndices(m_dataSpec);
        PredictionRearrangerCreator prc =
            new PredictionRearrangerCreator(m_dataSpec, new RegressionTreePredictor(m_model, r -> m_model
                .createPredictorRecord(new FilterColumnRow(r, filterIndices), m_modelSpec.getLearnTableSpec())));
        prc.addRegressionPrediction(m_configuration.getPredictionColumnName());
        return prc;
    }

}
