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
 *   20.01.2016 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
public class MultiClassGradientBoostedTreesModel extends AbstractGradientBoostingModel {

    private int m_numClasses;

    private ArrayList<ArrayList<Map<TreeNodeSignature, Double>>> m_coefficientMaps;

    private String[] m_classLabels;

    /**
     * @param config
     * @param metaData
     * @param models
     * @param treeType
     * @param initialValue
     */
    private MultiClassGradientBoostedTreesModel(final TreeEnsembleLearnerConfiguration config,
        final TreeMetaData metaData, final TreeModelRegression[] models, final TreeType treeType,
        final double initialValue, final int numClasses,
        final ArrayList<ArrayList<Map<TreeNodeSignature, Double>>> coefficientMaps, final String[] classLabels) {
        super(config, metaData, models, treeType, initialValue);
        m_numClasses = numClasses;
        m_coefficientMaps = coefficientMaps;
        m_classLabels = classLabels;
    }

    /**
     * @param metaData
     * @param models
     * @param type
     * @param containsClassDistribution
     */
    public MultiClassGradientBoostedTreesModel(final TreeMetaData metaData, final AbstractTreeModel[] models, final TreeType type,
        final boolean containsClassDistribution) {
        super(metaData, models, type, containsClassDistribution);
    }

    public static MultiClassGradientBoostedTreesModel createMultiClassGradientBoostedTreesModel(
        final TreeEnsembleLearnerConfiguration config, final TreeMetaData metaData,
        final TreeModelRegression[][] models, final TreeType treeType, final double initialValue, final int numClasses,
        final ArrayList<ArrayList<Map<TreeNodeSignature, Double>>> coefficientMaps, final String[] classLabels) {
        TreeModelRegression[] modelArray = new TreeModelRegression[models.length * numClasses];
        for (int r = 0; r < models.length; r++) {
            for (int c = 0; c < numClasses; c++) {
                modelArray[r * numClasses + c] = models[r][c];
            }
        }
        return new MultiClassGradientBoostedTreesModel(config, metaData, modelArray, treeType, initialValue, numClasses,
            coefficientMaps, classLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double predict(final PredictorRecord record) {

        return 0;
    }

    public TreeModelRegression getModel(final int levelIdx, final int classIdx) {
        return getTreeModelRegression(levelIdx * m_numClasses + classIdx);
    }

    public Map<TreeNodeSignature, Double> getCoefficientMap(final int levelIdx, final int classIdx) {
        return m_coefficientMaps.get(levelIdx).get(classIdx);
    }

    public int getNrClasses() {
        return m_numClasses;
    }

    public String getClassLabel(final int classIdx) {
        return m_classLabels[classIdx];
    }

    public int getNrLevels() {
        return getNrModels() / m_numClasses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveData(final DataOutputStream dataOutput) throws IOException {
        super.saveData(dataOutput);
        dataOutput.writeInt(m_numClasses);
        dataOutput.writeInt(m_coefficientMaps.size());
        for (List<Map<TreeNodeSignature, Double>> classCoefficientMaps : m_coefficientMaps) {
            for (Map<TreeNodeSignature, Double> classCoefficentMap : classCoefficientMaps) {
                Set<Map.Entry<TreeNodeSignature, Double>> entrySet = classCoefficentMap.entrySet();
                dataOutput.writeInt(entrySet.size());
                for (Map.Entry<TreeNodeSignature, Double> entry : entrySet) {
                    entry.getKey().save(dataOutput);
                    dataOutput.writeDouble(entry.getValue());
                }
            }
        }
        for (String classLabel : m_classLabels) {
            dataOutput.writeUTF(classLabel);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadData(final TreeModelDataInputStream input) throws IOException {
        super.loadData(input);
        m_numClasses = input.readInt();
        int numModels = input.readInt();
        m_coefficientMaps = new ArrayList<ArrayList<Map<TreeNodeSignature, Double>>>(numModels);
        for (int i = 0; i < numModels; i++) {
            ArrayList<Map<TreeNodeSignature, Double>> classCoefficientMaps =
                new ArrayList<Map<TreeNodeSignature, Double>>(m_numClasses);
            for (int j = 0; j < m_numClasses; j++) {
                int entrySetSize = input.readInt();
                Map<TreeNodeSignature, Double> classCoefficientMap =
                    new HashMap<TreeNodeSignature, Double>((int)(entrySetSize / 0.75 + 1));
                for (int k = 0; k < entrySetSize; k++) {
                    TreeNodeSignature signature = TreeNodeSignature.load(input);
                    classCoefficientMap.put(signature, input.readDouble());
                }
                classCoefficientMaps.add(classCoefficientMap);
            }
            m_coefficientMaps.add(classCoefficientMaps);
        }
        m_classLabels = new String[m_numClasses];
        for (int i = 0; i < m_numClasses; i++) {
            m_classLabels[i] = input.readUTF();
        }
    }

}
