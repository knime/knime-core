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
 *   13.09.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.List;

import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.ScoreDistributionDocument.ScoreDistribution;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeClassification;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;

/**
 * Parses the content of a {@link TreeNodeClassification}.
 * This includes the majority class and class distribution.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ClassificationContentParser
implements ContentParser<TreeNodeClassification, TreeTargetNominalColumnMetaData> {

    private final NomValMapper m_nomValMapper;

    /**
     * @param nomValMapper the nominal value mapper for the target column
     */
    public ClassificationContentParser(final NomValMapper nomValMapper) {
        m_nomValMapper = nomValMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeNodeClassification createNode(final Node node,
        final TargetColumnHelper<TreeTargetNominalColumnMetaData> targetMetaData, final TreeNodeSignature signature,
        final List<TreeNodeClassification> children) {
        float[] scoreDistribution = parseDistribution(node);
        int majorityIndex = parseMajorityIndex(node);
        return new TreeNodeClassification(signature, targetMetaData.getMetaData(), majorityIndex, scoreDistribution,
            children.toArray(new TreeNodeClassification[children.size()]));
    }

    private float[] parseDistribution(final Node node) {
        List<ScoreDistribution> list = node.getScoreDistributionList();
        float[] targetDistribution = new float[list.size()];
        for (ScoreDistribution x : list) {
            int idx = m_nomValMapper.getRepresentation(x.getValue()).getAssignedInteger();
            targetDistribution[idx] = (float)x.getRecordCount();
        }
        return targetDistribution;
    }

    private int parseMajorityIndex(final Node node) {
        return m_nomValMapper.getRepresentation(node.getScore()).getAssignedInteger();
    }


}
