/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   18.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.node.learner.TreeEnsembleLearnerConfiguration;

/**
 *
 * @author Adrian Nembach
 */
public class GradientBoostedTreesModel extends AbstractGradientBoostingModel {

    private List<Map<TreeNodeSignature, Double>> m_coefficientMaps;

    /**
     * @param configuration
     * @param metaData
     * @param models
     * @param treeType
     * @param initialValue
     * @param coefficientMaps
     */
    public GradientBoostedTreesModel(final TreeEnsembleLearnerConfiguration configuration, final TreeMetaData metaData,
        final TreeModelRegression[] models, final TreeType treeType, final double initialValue,
        final List<Map<TreeNodeSignature, Double>> coefficientMaps) {
        super(configuration, metaData, models, treeType, initialValue);
        m_coefficientMaps = coefficientMaps;
    }

    /**
     * Constructor to be used only for serialization.
     *
     * @param metaData
     * @param models
     * @param type
     * @param containsClassDistribution
     */
    public GradientBoostedTreesModel(final TreeMetaData metaData, final AbstractTreeModel[] models, final TreeType type,
        final boolean containsClassDistribution) {
        super(metaData, models, type, containsClassDistribution);
    }

    /**
     * Constructor to be used when reading a {@link GradientBoostedTreesModel} from PMML.
     * @param metaData the meta information of the model
     * @param trees the tree models
     * @param type the tree type
     * @param coefficientMaps a list with the coefficient maps for all trees
     */
    public GradientBoostedTreesModel(final TreeMetaData metaData, final TreeModelRegression[] trees, final TreeType type,
        final List<Map<TreeNodeSignature, Double>> coefficientMaps) {
        super(metaData, trees, type, false);
        m_coefficientMaps = coefficientMaps;
    }

    @Override
    public double predict(final PredictorRecord record) {
        double prediction = getInitialValue();
        for (int i = 0; i < getNrModels(); i++) {
            TreeNodeRegression leaf = getTreeModelRegression(i).findMatchingNode(record);
            prediction += m_coefficientMaps.get(i).get(leaf.getSignature());
        }
        return prediction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveData(final DataOutputStream dataOutput) throws IOException {
        super.saveData(dataOutput);
        dataOutput.writeInt(m_coefficientMaps.size());
        for (Map<TreeNodeSignature, Double> coefficientMap : m_coefficientMaps) {
            Set<Map.Entry<TreeNodeSignature, Double>> entrySet = coefficientMap.entrySet();
            dataOutput.writeInt(entrySet.size());
            for (Map.Entry<TreeNodeSignature, Double> entry : entrySet) {
                entry.getKey().save(dataOutput);
                dataOutput.writeDouble(entry.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadData(final TreeModelDataInputStream input) throws IOException {
        super.loadData(input);
        int numMaps = input.readInt();
        m_coefficientMaps = new ArrayList<Map<TreeNodeSignature, Double>>(numMaps);
        for (int i = 0; i < numMaps; i++) {
            int mapSize = input.readInt();
            HashMap<TreeNodeSignature, Double> map = new HashMap<TreeNodeSignature, Double>((int)(mapSize / 0.75 + 1));
            for (int j = 0; j < mapSize; j++) {
                TreeNodeSignature key = TreeNodeSignature.load(input);
                double value = input.readDouble();
                map.put(key, value);
            }
            m_coefficientMaps.add(map);
        }
    }

    /**
     * @return the coefficient maps for all trees
     */
    public Collection<Map<TreeNodeSignature, Double>> getCoeffientMaps() {
        return m_coefficientMaps;
    }

}
