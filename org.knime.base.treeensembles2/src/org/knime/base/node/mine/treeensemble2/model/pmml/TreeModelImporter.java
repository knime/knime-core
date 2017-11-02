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
 *   04.09.2017 (Adrian): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.ArrayList;
import java.util.List;

import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.knime.base.node.mine.treeensemble2.data.TreeTargetColumnMetaData;
import org.knime.base.node.mine.treeensemble2.learner.TreeNodeSignatureFactory;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;

/**
 * Handles the import of {@link AbstractTreeModel} objects from PMML.
 * This includes the handling of conditions as those are independent of the node type.
 *
 * @author Adrian Nembach, KNIME
 */
class TreeModelImporter<N extends AbstractTreeNode, M extends AbstractTreeModel<N>,
T extends TreeTargetColumnMetaData> {
    private final MetaDataMapper<T> m_metaDataMapper;
    private final ConditionParser m_conditionParser;
    private final TreeNodeSignatureFactory m_signatureFactory;
    private final ContentParser<N, T> m_contentParser;
    private final TreeFactory<N, M> m_treeFactory;

    public TreeModelImporter(final MetaDataMapper<T> metaDataMapper, final ConditionParser conditionParser,
        final TreeNodeSignatureFactory signatureFactory,
        final ContentParser<N, T> contentParser, final TreeFactory<N, M> treeFactory) {
        m_metaDataMapper = metaDataMapper;
        m_conditionParser = conditionParser;
        m_signatureFactory = signatureFactory;
        m_contentParser = contentParser;
        m_treeFactory = treeFactory;
    }

    /**
     * Imports an {@link AbstractTreeModel} from PMML.
     *
     * @param treeModel PMML tree model to import
     * @return a {@link AbstractTreeModel} initialized from <b>treeModel</b>
     */
    public M importFromPMML(final TreeModel treeModel) {
        Node rootNode = treeModel.getNode();
        N root = createNodeFromPMML(rootNode, m_signatureFactory.getRootSignature());
        return m_treeFactory.createTree(root);
    }

    private N createNodeFromPMML(final Node pmmlNode, final TreeNodeSignature signature) {
        List<N> children = new ArrayList<>();
        byte i = 0;
        for (Node child : pmmlNode.getNodeList()) {
            TreeNodeSignature childSignature = m_signatureFactory.getChildSignatureFor(signature, i);
            i++;
            children.add(createNodeFromPMML(child, childSignature));
        }
        TreeNodeCondition condition = m_conditionParser.parseCondition(pmmlNode);
        N node = m_contentParser.createNode(pmmlNode, m_metaDataMapper.getTargetColumnHelper(), signature, children);
        node.setTreeNodeCondition(condition);
        return node;
    }

}
