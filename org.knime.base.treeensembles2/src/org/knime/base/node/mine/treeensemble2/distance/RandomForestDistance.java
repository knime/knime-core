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
 *   16.02.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.distance;

import java.util.List;

import org.knime.base.data.filter.column.FilterColumnRow;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModel;
import org.knime.base.node.mine.treeensemble2.model.TreeEnsembleModelPortObject;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.distance.DistanceMeasure;
import org.knime.distance.DistanceMeasurementException;

/**
 *
 * @author Adrian Nembach
 */
public class RandomForestDistance extends DistanceMeasure<RandomForestDistanceConfig> {


    /**
     * @param config
     * @param spec
     * @throws InvalidSettingsException
     */
    protected RandomForestDistance(final RandomForestDistanceConfig config, final DataTableSpec spec) throws InvalidSettingsException {
        super(config, spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeDistance(final DataRow row1, final DataRow row2) throws DistanceMeasurementException {
        List<Integer> filterIndicesList = getColumnIndices();
        int[] filterIndices = new int[filterIndicesList.size()];
        int i = 0;
        for (Integer index : filterIndicesList) {
            filterIndices[i++] = index;
        }
        DataRow filterRow1 = new FilterColumnRow(row1, filterIndices);
        DataRow filterRow2 = new FilterColumnRow(row2, filterIndices);
        TreeEnsembleModelPortObject ensemblePO = getConfig().getEnsemblePO();
        TreeEnsembleModel ensemble = ensemblePO.getEnsembleModel();
        DataTableSpec learnSpec = ensemblePO.getSpec().getLearnTableSpec();
        PredictorRecord record1 = ensemble.createPredictorRecord(filterRow1, learnSpec);
        PredictorRecord record2 = ensemble.createPredictorRecord(filterRow2, learnSpec);

        double proximity = 0.0;

        for (int t = 0; t < ensemble.getNrModels(); t++) {
            AbstractTreeModel<?> tree = ensemble.getTreeModel(t);
            AbstractTreeNode leaf1 = tree.findMatchingNode(record1);
            AbstractTreeNode leaf2 = tree.findMatchingNode(record2);
            if (leaf1.getSignature().equals(leaf2.getSignature())) {
                proximity += 1.0;
            }
        }
        proximity /= ensemble.getNrModels();
        // completely similar records will have a proximity of 1 (maximum)
        // to get a distance measure, we have to subtract the proximity from 1
        return 1 - proximity;
    }

}
