/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Jan 6, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLFalsePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLTruePredicate;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class TreeNodeNominalCondition extends TreeNodeColumnCondition {

    private final int m_valueIndex;

    /**
     * @param nomColumnMetaData
     * @param valueIndex
     */
    public TreeNodeNominalCondition(final TreeNominalColumnMetaData nomColumnMetaData, final int valueIndex,
        final boolean acceptsMissings) {
        super(nomColumnMetaData, acceptsMissings);
        assert valueIndex < nomColumnMetaData.getValues().length;
        m_valueIndex = valueIndex;
    }

    /**
     *  */
    TreeNodeNominalCondition(final TreeModelDataInputStream input, final TreeMetaData metaData) throws IOException {
        super(input, metaData);
        TreeColumnMetaData columnMetaData = super.getColumnMetaData();
        checkTypeCorrectness(columnMetaData, TreeNominalColumnMetaData.class);
        m_valueIndex = input.readInt();
        NominalValueRepresentation[] values = getColumnMetaData().getValues();
        if (m_valueIndex < 0 || m_valueIndex >= values.length) {
            throw new IOException("Invalid value index " + m_valueIndex);
        }
    }

    /** {@inheritDoc} */
    @Override
    public TreeNominalColumnMetaData getColumnMetaData() {
        return (TreeNominalColumnMetaData)super.getColumnMetaData();
    }

    /**
     * @return the value
     */
    public String getValue() {
        return getColumnMetaData().getValues()[m_valueIndex].getNominalValue();
    }

    /**
     * @return the valueIndex
     */
    public int getValueIndex() {
        return m_valueIndex;
    }

    /** {@inheritDoc} */
    @Override
    public boolean testCondition(final PredictorRecord record) {
        Object value = record.getValue(getColumnMetaData().getAttributeName());
        int valIdx = -1;
        if (value == null) {
            //            throw new UnsupportedOperationException("Missing values currently not supported");
            return acceptsMissings();
        }
        if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Can't test nominal condition (" + toString()
                + ") -- expected query object of type Integer (representing the nominal value) but got "
                + value.getClass().getSimpleName());
        } else {
            valIdx = (Integer)value;
        }
        return m_valueIndex == valIdx;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getColumnMetaData() + " = \"" + getValue() + "\"";
    }

    /** {@inheritDoc} */
    @Override
    protected void saveContent(final DataOutputStream s) throws IOException {
        s.writeInt(m_valueIndex);
    }

    /** {@inheritDoc} */
    @Override
    public PMMLCompoundPredicate toPMMLPredicate() {
        final PMMLSimplePredicate simplePredicate =
            new PMMLSimplePredicate(getAttributeName(), PMMLOperator.EQUAL, getValue());
        // add compound predicate to allow for missing values
        final PMMLCompoundPredicate compPredicate = new PMMLCompoundPredicate(PMMLBooleanOperator.SURROGATE);
        compPredicate.addPredicate(simplePredicate);
//        final PMMLSimplePredicate missing = new PMMLSimplePredicate();
//        missing.setSplitAttribute(getAttributeName());
//        if (acceptsMissings()) {
//            missing.setOperator(PMMLOperator.IS_MISSING);
//        } else {
//            missing.setOperator(PMMLOperator.IS_NOT_MISSING);
//        }
        final PMMLPredicate missing = acceptsMissings() ? new PMMLTruePredicate() : new PMMLFalsePredicate();
        compPredicate.addPredicate(missing);
        return compPredicate;
    }

}
