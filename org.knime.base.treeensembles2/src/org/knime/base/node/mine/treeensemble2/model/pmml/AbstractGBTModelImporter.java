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
 *   02.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dmg.pmml.MiningModelDocument.MiningModel;
import org.dmg.pmml.SegmentDocument.Segment;
import org.dmg.pmml.SegmentationDocument.Segmentation;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.AbstractGradientBoostingModel;
import org.knime.base.node.mine.treeensemble2.model.TreeModelRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.core.util.Pair;

/**
 *
 * @author Adrian Nembach, KNIME
 */
abstract class AbstractGBTModelImporter <M extends AbstractGradientBoostingModel> {

    private final ConditionParser m_conditionParser;
    private final TreeNodeSignatureFactory m_signatureFactory;
    private final TreeFactory<TreeNodeRegression, TreeModelRegression> m_treeFactory;
    private final MetaDataMapper<TreeTargetNumericColumnMetaData> m_metaDataMapper;

    public AbstractGBTModelImporter(final ConditionParser conditionParser,
        final TreeNodeSignatureFactory signatureFactory, final TreeFactory<TreeNodeRegression,
        TreeModelRegression> treeFactory, final MetaDataMapper<TreeTargetNumericColumnMetaData> metaDataMapper) {
        m_conditionParser = conditionParser;
        m_signatureFactory = signatureFactory;
        m_treeFactory = treeFactory;
        m_metaDataMapper = metaDataMapper;
    }

    /**
     * Imports a Gradient Boosted Trees model from the provided PMML mining model
     * @param miningModel the PMML model from which to read the Gradient Boosted Trees model
     * @return a KNIME Gradient Boosted Trees model corresponding to the provided PMML model
     */
    public abstract M importFromPMML(final MiningModel miningModel);

    protected Pair<List<TreeModelRegression>, List<Map<TreeNodeSignature, Double>>> readSumSegmentation(
        final Segmentation segmentation) {
        List<Segment> segments = segmentation.getSegmentList();
        List<TreeModelRegression> trees = new ArrayList<>(segments.size());
        List<Map<TreeNodeSignature, Double>> coefficientMaps = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            Pair<TreeModelRegression, Map<TreeNodeSignature, Double>> treeCoeffientMapPair = readTreeModel(segment);
            trees.add(treeCoeffientMapPair.getFirst());
            coefficientMaps.add(treeCoeffientMapPair.getSecond());
        }
        return new Pair<>(trees, coefficientMaps);
    }

    protected MetaDataMapper<TreeTargetNumericColumnMetaData> getMetaDataMapper() {
        return m_metaDataMapper;
    }

    private Pair<TreeModelRegression, Map<TreeNodeSignature, Double>> readTreeModel(final Segment segment) {
        GBTRegressionContentParser contentParser = new GBTRegressionContentParser();
        TreeModelImporter<TreeNodeRegression, TreeModelRegression, TreeTargetNumericColumnMetaData> treeImporter =
                new TreeModelImporter<TreeNodeRegression, TreeModelRegression, TreeTargetNumericColumnMetaData>(
                        m_metaDataMapper, m_conditionParser, m_signatureFactory, contentParser, m_treeFactory);
        TreeModel treeModel = segment.getTreeModel();
        TreeModelRegression tree = treeImporter.importFromPMML(treeModel);
        Map<TreeNodeSignature, Double> coefficientMap = contentParser.getCoefficientMap();
        return new Pair<>(tree, coefficientMap);
    }

}
