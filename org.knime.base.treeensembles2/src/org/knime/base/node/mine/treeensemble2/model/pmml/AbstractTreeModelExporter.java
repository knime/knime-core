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
 *   04.09.2017 (Adrian): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import org.apache.xmlbeans.SchemaType;
import org.dmg.pmml.MININGFUNCTION;
import org.dmg.pmml.MISSINGVALUESTRATEGY;
import org.dmg.pmml.NOTRUECHILDSTRATEGY;
import org.dmg.pmml.NodeDocument;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.TreeModelDocument;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.dmg.pmml.TreeModelDocument.TreeModel.SplitCharacteristic;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.port.pmml.PMMLMiningSchemaTranslator;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;

/**
 * Handles the export of {@link AbstractTreeModel} objects to PMML.
 *
 * @author Adrian Nembach, KNIME
 */
abstract class AbstractTreeModelExporter<T extends AbstractTreeNode> extends AbstractWarningHolder {

    private static final int BINARY = 2;

    private final AbstractTreeModel<T> m_treeModel;
    private int m_nodeIndex;

    public AbstractTreeModelExporter(final AbstractTreeModel<T> treeModel) {
        m_treeModel = treeModel;
    }

    /**
     * Writes the KNIME tree model to a PMML tree model.
     * @param treeModel the PMML node which should be filled with the KNIME model
     * @param spec the {@link PMMLPortObjectSpec} of the full PMML document
     * @return the {@link SchemaType} of the tree model
     */
    public SchemaType writeModelToPMML(final TreeModel treeModel, final PMMLPortObjectSpec spec) {
        checkColumnTypes(spec);
        PMMLMiningSchemaTranslator.writeMiningSchema(spec, treeModel);
        treeModel.setModelName("DecisionTree");
        treeModel.setFunctionName(getMiningFunction());
        T rootNode = m_treeModel.getRootNode();
        // ----------------------------------------------
        // set up splitCharacteristic
        if (isMultiSplitRecursive(rootNode)) {
            treeModel.setSplitCharacteristic(SplitCharacteristic.MULTI_SPLIT);
        } else {
            treeModel.setSplitCharacteristic(SplitCharacteristic.BINARY_SPLIT);
        }

        // ----------------------------------------------
        // set up missing value strategy
        treeModel.setMissingValueStrategy(MISSINGVALUESTRATEGY.NONE);

        // -------------------------------------------------
        // set up no true child strategy
        treeModel.setNoTrueChildStrategy(NOTRUECHILDSTRATEGY.RETURN_LAST_PREDICTION);
     // --------------------------------------------------
        // set up tree node
        NodeDocument.Node rootPMMLNode = treeModel.addNewNode();
        addTreeNode(rootPMMLNode, rootNode);
        return TreeModelDocument.TreeModel.type;
    }

    private void checkColumnTypes(final PMMLPortObjectSpec spec) {
        if (!spec.getLearningCols().stream().allMatch(this::canSavelyBeExported)) {
            addWarning("The model was learned on a vector column. "
                + "It's possible to export the model to PMML but won't be possible"
                + " to import it from the exported PMML.");
        }
    }

    private boolean canSavelyBeExported(final DataColumnSpec colSpec) {
        return !TranslationUtil.isVectorFieldName(colSpec.getName());
    }


    protected abstract MININGFUNCTION.Enum getMiningFunction();

    /**
     * @param pmmlNode
     * @param node
     */
    @SuppressWarnings("unchecked")
    private void addTreeNode(final Node pmmlNode, final T node) {
        int index = m_nodeIndex;
        m_nodeIndex++;
        pmmlNode.setId(Integer.toString(index));
        addNodeContent(index, pmmlNode, node);


        TreeNodeCondition condition = node.getCondition();

        ConditionExporter.exportCondition(condition, pmmlNode);

        for (int i = 0; i < node.getNrChildren(); i++) {
            addTreeNode(pmmlNode.addNewNode(), (T)node.getChild(i));
        }
    }

    /**
     *
     * @param nodeId the id the current node got assigned (don't assign again)
     * @param pmmlNode the pmml node in which to write the information contained in <b>node</b>
     * @param node the KNIME tree node
     */
    protected abstract void addNodeContent(final int nodeId, Node pmmlNode, T node);

    private static boolean isMultiSplitRecursive(final AbstractTreeNode node) {
        final int nrChildren = node.getNrChildren();
        if (nrChildren > BINARY) {
            return true;
        }
        for (int i = 0; i < nrChildren; i++) {
            AbstractTreeNode child = node.getChild(i);
            if (isMultiSplitRecursive(child)) {
                return true;
            }
        }
        return false;
    }



}
