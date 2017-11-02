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
 *   22.12.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;

/**
 *
 * @author Adrian Nembach
 */
public abstract class AbstractTreeNodeSurrogateCondition extends TreeNodeCondition {

//    private final TreeNodeColumnCondition[] m_conditions;

    private final boolean m_defaultResponse;

    public AbstractTreeNodeSurrogateCondition(final boolean defaultResponse) {
        m_defaultResponse = defaultResponse;
    }

    public AbstractTreeNodeSurrogateCondition(final TreeModelDataInputStream input, final TreeMetaData metaData)
        throws IOException {
        m_defaultResponse = input.readBoolean();
//        int length = input.readInt();
//        m_conditions = new TreeNodeColumnCondition[length];
//        for (int i = 0; i < length; i++) {
//            m_conditions[i] = (TreeNodeColumnCondition)TreeNodeCondition.load(input, metaData);
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean testCondition(final PredictorRecord record) {
        final int numSurrogates = getNumSurrogates();
        for (int i = 0; i < numSurrogates + 1; i++) {
            // evaluate column condition
            TreeNodeColumnCondition columnCondition = getColumnCondition(i);
            Object value = record.getValue(columnCondition.getColumnMetaData().getAttributeName());
            if (value != null) {
                return columnCondition.testCondition(record);
            }
        }
        // all evaluated columns were missing and no other condition was encountered
        return m_defaultResponse;
    }

    /**
     * @return the first condition (corresponding to the best split determined during training)
     */
    public TreeNodeColumnCondition getFirstCondition() {
        return getColumnCondition(0);
    }

    /**
     * This function only returns the column conditions, meaning the first condition and its surrogates, but not
     * the default surrogate with the default direction.
     *
     * @param index
     * @return TreeNodeColumnCondition at <b>index</b>
     */
    protected abstract TreeNodeColumnCondition getColumnCondition(final int index);

    /**
     *
     * @return the number of TRUE surrogates (meaning not the main condition and not the default direction)
     */
    public abstract int getNumSurrogates();

    /**
     * {@inheritDoc}
     */
    @Override
    public PMMLCompoundPredicate toPMMLPredicate() {
        PMMLCompoundPredicate compound = new PMMLCompoundPredicate("surrogate");
        for (int i = 0; i < getNumSurrogates() + 1; i++) {
            TreeNodeCondition condition = getColumnCondition(i);
            compound.addPredicate(condition.toPMMLPredicate());
        }
        if (m_defaultResponse) {
            compound.addPredicate(new PMMLTruePredicate());
        } else {
            compound.addPredicate(new PMMLFalsePredicate());
        }
        return compound;
    }

    @Override
    public void save(final DataOutputStream dataOutput) throws IOException {
        super.save(dataOutput);
        dataOutput.writeBoolean(m_defaultResponse);
//        dataOutput.writeInt(m_conditions.length);
//        for (TreeNodeCondition condition : m_conditions) {
//            condition.save(dataOutput);
//        }
    }

    @Override
    public String toString() {
        return getColumnCondition(0).toString();
    }

}
