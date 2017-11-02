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
 *   02.10.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model.pmml;

import java.util.Map;

import org.dmg.pmml.ExtensionDocument.Extension;
import org.dmg.pmml.NodeDocument.Node;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeRegression;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeSignature;
import org.knime.core.node.util.CheckUtils;

/**
 * Handles the export of regression trees used in gradient boosted trees.
 *
 * @author Adrian Nembach, KNIME
 */
final class GBTRegressionTreeModelExporter extends RegressionTreeModelExporter {

    private final Map<TreeNodeSignature, Double> m_coefficientMap;

    /**
     * @param treeModel the tree model to export (must be a regression model)
     * @param coefficientMap must contain the coefficients for all leafs in <b>treeModel</b>
     */
    public GBTRegressionTreeModelExporter(final AbstractTreeModel<TreeNodeRegression> treeModel,
        final Map<TreeNodeSignature, Double> coefficientMap) {
        super(treeModel);
        m_coefficientMap = coefficientMap;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addNodeContent(final int nodeId, final Node pmmlNode, final TreeNodeRegression node) {
        // overwrite method of super type as we potentially need to alter the score that is saved (for leaf nodes)
        addExtension(pmmlNode, node);
        // pull coefficient into score for leaf nodes
        double score = node.getMean();
        if (node.getNrChildren() == 0) {
            CheckUtils.checkArgument(m_coefficientMap.containsKey(node.getSignature()),
                "The GBT model contains no coefficient for the leaf %s.", node);
            score = m_coefficientMap.get(node.getSignature());
        }
        pmmlNode.setScore(Double.toString(score));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addExtension(final Node pmmlNode, final TreeNodeRegression node) {
        super.addExtension(pmmlNode, node);
        // store the gbt coefficient for leafs
        if (node.getNrChildren() == 0) {
            Extension ext = pmmlNode.addNewExtension();
            ext.setName(TranslationUtil.MEAN_KEY);
            CheckUtils.checkArgument(m_coefficientMap.containsKey(node.getSignature()),
                "The GBT model contains no coefficient for the leaf %s.", node);
            ext.setValue(Double.toString(node.getMean()));
        }
    }


}
