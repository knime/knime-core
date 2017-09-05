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

import java.util.Arrays;
import java.util.Optional;

import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate;
import org.dmg.pmml.CompoundPredicateDocument.CompoundPredicate.BooleanOperator.Enum;
import org.dmg.pmml.FalseDocument.False;
import org.dmg.pmml.NodeDocument.Node;
import org.dmg.pmml.SimplePredicateDocument.SimplePredicate;
import org.dmg.pmml.SimpleSetPredicateDocument.SimpleSetPredicate;
import org.dmg.pmml.TreeModelDocument.TreeModel;
import org.dmg.pmml.TrueDocument.True;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeModel;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNode;
import org.knime.base.node.mine.treeensemble2.model.AbstractTreeNodeSurrogateCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeColumnCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNominalCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeNumericCondition.NumericOperator;
import org.knime.base.node.mine.treeensemble2.model.TreeNodeTrueCondition;
import org.knime.core.node.util.CheckUtils;

/**
 * Handles the import of {@link AbstractTreeModel} objects from PMML.
 * This includes the handling of conditions as those are independent of the node type.
 *
 * @author Adrian Nembach, KNIME
 */
abstract class AbstractTreeModelImporter<T extends AbstractTreeNode> {
    private MetaDataMapper m_metaDataMapper;

    public AbstractTreeModel<T> importFromPMML(final TreeModel treeModel) {
        Node rootNode = treeModel.getNode();

        return null;
    }

    private T createNodeFromPMML(final Node pmmlNode) {

        return null;
    }

    private TreeNodeCondition extractCondition(final Node pmmlNode) {
        TreeNodeCondition predicate;
        CompoundPredicate compound = pmmlNode.getCompoundPredicate();
        if (compound != null) {
            return handleCompoundPredicate(compound);
        }
        SimplePredicate simplePred = pmmlNode.getSimplePredicate();
        if (simplePred != null) {
            return handleSimplePredicate(simplePred, false);
        }
        SimpleSetPredicate simpleSetPred = pmmlNode.getSimpleSetPredicate();
        if (simpleSetPred != null) {
            return handleSimpleSetPredicate(simpleSetPred);
        }
        True truePred = pmmlNode.getTrue();
        if (truePred != null) {
            return TreeNodeTrueCondition.INSTANCE;
        }
        False falsePred = pmmlNode.getFalse();
        if (falsePred != null) {
            throw new IllegalArgumentException("There is no False condition in KNIME.");
        }
        throw new IllegalStateException("The pmmlNode contains no valid Predicate.");

    }

    private TreeNodeCondition handleCompoundPredicate(final CompoundPredicate compound) {
        Enum operator = compound.getBooleanOperator();
        if (operator == CompoundPredicate.BooleanOperator.SURROGATE) {

        }
        return null;
    }

    private AbstractTreeNodeSurrogateCondition handleSurrogate(final CompoundPredicate compound) {
        return null;
    }

    private TreeNodeColumnCondition handleSimplePredicate(final SimplePredicate simplePred, final boolean acceptsMissings) {
        String field = simplePred.getField();
        if (m_metaDataMapper.isNominal(field)) {
            TreeNominalColumnMetaData metaData = m_metaDataMapper.getNominalColumnMetaData(field);
            return new TreeNodeNominalCondition(metaData, getValueIndex(metaData, simplePred.getValue()), acceptsMissings);
        } else {
            TreeNumericColumnMetaData metaData = m_metaDataMapper.getNumericColumnMetaData(field);
            double value = Double.parseDouble(simplePred.getValue());
            return new TreeNodeNumericCondition(metaData, value, parseNumericOperator(simplePred.getOperator()), acceptsMissings);
        }
    }

    private static NumericOperator parseNumericOperator(final SimplePredicate.Operator.Enum operator) {
        if (operator == SimplePredicate.Operator.LESS_OR_EQUAL) {
            return NumericOperator.LessThanOrEqual;
        } else if (operator == SimplePredicate.Operator.GREATER_THAN) {
            return NumericOperator.LargerThan;
        }
        throw new IllegalArgumentException("The numeric operator \"" + operator + "\" is currently not supported.");
    }

    private static int getValueIndex(final TreeNominalColumnMetaData metaData, final String value) {
        // this implementation is slow as it has to scan the nominal value representation for all values
        NominalValueRepresentation[] values = metaData.getValues();
        Optional<NominalValueRepresentation> optional = Arrays.stream(values).filter(v -> v.getNominalValue().equals(value)).findFirst();
        CheckUtils.checkArgument(optional.isPresent(), "The value \"%s\" is not a valid value for field \"%s\".", value, metaData.getAttributeName());
        return optional.get().getAssignedInteger();
    }

    private TreeNodeColumnCondition handleSimpleSetPredicate(final SimpleSetPredicate simpleSetPred) {
        return null;
    }

}
