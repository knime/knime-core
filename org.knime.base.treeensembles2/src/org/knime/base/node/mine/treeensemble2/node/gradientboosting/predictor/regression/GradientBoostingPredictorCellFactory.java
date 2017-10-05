/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   14.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.regression;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.GradientBoostingModelPortObject;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObjectSpec;
import org.knime.base.node.mine.treeensemble2.node.gradientboosting.predictor.GradientBoostingPredictor;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;

/**
 *
 * @author Adrian Nembach
 */
public class GradientBoostingPredictorCellFactory extends SingleCellFactory {

    private final GradientBoostingModelPortObject m_modelPO;

    private final DataTableSpec m_learnSpec;

    private final int[] m_learnColumnInRealDataIndices;

    /**
     * @param newColSpec
     */
    public GradientBoostingPredictorCellFactory(final DataColumnSpec newColSpec,
        final GradientBoostingModelPortObject modelPO, final DataTableSpec learnSpec,
        final int[] learnColumnInRealDataIndices) {
        super(newColSpec);
        m_modelPO = modelPO;
        m_learnSpec = learnSpec;
        m_learnColumnInRealDataIndices = learnColumnInRealDataIndices;
    }

    public static GradientBoostingPredictorCellFactory createFactory(final GradientBoostingPredictor predictor)
        throws InvalidSettingsException {
        TreeEnsembleModelPortObjectSpec modelSpec = predictor.getModelSpec();
        DataTableSpec learnSpec = modelSpec.getLearnTableSpec();
        DataTableSpec testSpec = predictor.getDataSpec();
        UniqueNameGenerator nameGen = new UniqueNameGenerator(testSpec);
        DataColumnSpec newColSpec = nameGen.newColumn(predictor.getConfig().getPredictionColumnName(), DoubleCell.TYPE);
        return new GradientBoostingPredictorCellFactory(newColSpec, predictor.getModelPO(), learnSpec,
            modelSpec.calculateFilterIndices(testSpec));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getCell(final DataRow row) {
        AbstractGradientBoostingModel model = m_modelPO.getEnsembleModel();
        DataRow filterRow = new FilterColumnRow(row, m_learnColumnInRealDataIndices);
        double prediction = model.predict(model.createPredictorRecord(filterRow, m_learnSpec));
        return new DoubleCell(prediction);
    }

}
