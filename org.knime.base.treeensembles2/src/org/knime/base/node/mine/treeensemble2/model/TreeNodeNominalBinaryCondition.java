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
 *   09.11.2015 (Adrian Nembach): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.knime.base.node.mine.decisiontree2.PMMLArrayType;
import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSetOperator;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimpleSetPredicate;
import org.knime.base.node.mine.treeensemble2.data.NominalValueRepresentation;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNominalColumnMetaData;
import org.knime.core.node.util.ConvenienceMethods;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class TreeNodeNominalBinaryCondition extends TreeNodeColumnCondition {

    public enum SetLogic {
            IS_IN(" is in {%s}", true, PMMLSetOperator.IS_IN),
            IS_NOT_IN(" is not in {%s}", false, PMMLSetOperator.IS_NOT_IN);

        private final String m_toString;

        private final boolean m_logic;

        private final PMMLSetOperator m_pmmlSetOperator;

        SetLogic(final String toString, final boolean logic, final PMMLSetOperator pmmlSetOperator) {
            m_toString = toString;
            m_logic = logic;
            m_pmmlSetOperator = pmmlSetOperator;
        }

        boolean eval(final boolean isContainedInSet) {
            return m_logic == isContainedInSet;
        }

        String toString(final String[] values) {
            return String.format(m_toString, ConvenienceMethods.getShortStringFrom(Arrays.asList(values), 8));
        }

        /**
         * @return the pmmlSetOperator
         */
        public PMMLSetOperator getPmmlSetOperator() {
            return m_pmmlSetOperator;
        }

    }

    private final BigInteger m_valuesMask;

    private final SetLogic m_setLogic;

    /**
     * @param nomColumnMetaData ...
     * @param valuesMask ...
     * @param isINSet true for "is-in", false for "is-not-in"
     */
    public TreeNodeNominalBinaryCondition(final TreeNominalColumnMetaData nomColumnMetaData,
        final BigInteger valuesMask, final boolean isINSet, final boolean acceptsMissings) {
        super(nomColumnMetaData, acceptsMissings);
        assert checkValuesMask(valuesMask) == null : checkValuesMask(valuesMask);
        m_valuesMask = valuesMask;
        m_setLogic = isINSet ? SetLogic.IS_IN : SetLogic.IS_NOT_IN;
    }

    /**
     *  */
    TreeNodeNominalBinaryCondition(final TreeModelDataInputStream input, final TreeMetaData metaData,
        final TreeBuildingInterner treeBuildingInterner) throws IOException {
        super(input, metaData);
        TreeColumnMetaData columnMetaData = super.getColumnMetaData();
        checkTypeCorrectness(columnMetaData, TreeNominalColumnMetaData.class);
        byte v = input.readByte();
        // most significant bit is set logic bit
        m_setLogic = ((v & 0x80) != 0) ? SetLogic.IS_IN : SetLogic.IS_NOT_IN;
        // 7 LSB is byte array length
        int length = v & 0x7F;
        byte[] maskAsBytes = new byte[length];
        input.readFully(maskAsBytes);
        m_valuesMask = treeBuildingInterner.internBigInt(new BigInteger(maskAsBytes));
        if (checkValuesMask(m_valuesMask) != null) {
            throw new IOException(checkValuesMask(m_valuesMask));
        }
    }

    private String checkValuesMask(final BigInteger valuesMask) {
        if (valuesMask.bitCount() == -1) {
            return "Invalid value mask: No values set (big integer as 0 bits set)";
        }
        BigInteger upperBound = BigInteger.ZERO.setBit(getColumnMetaData().getValues().length);
        if (valuesMask.compareTo(upperBound) > 0) {
            int highestSetBit;
            BigInteger copy = valuesMask;
            do {
                highestSetBit = copy.getLowestSetBit();
                copy = copy.clearBit(highestSetBit);
            } while (copy.compareTo(BigInteger.ZERO) > 0);
            return String.format("Invalid value mask: Invalid index %d (max allowed %d):  %s", highestSetBit,
                getColumnMetaData().getValues().length - 1, valuesMask.toString(2));
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public TreeNominalColumnMetaData getColumnMetaData() {
        return (TreeNominalColumnMetaData)super.getColumnMetaData();
    }

    /**
     * @return the values as per filter mask - inclusion or exclusion defined via {@link #getSetLogic()}.
     */
    public String[] getValues() {
        String[] values = new String[m_valuesMask.bitCount()];
        NominalValueRepresentation[] valueRepresentation = getColumnMetaData().getValues();
        for (int i = 0, valid = 0; i < valueRepresentation.length; i++) {
            if (m_valuesMask.testBit(i)) {
                values[valid++] = valueRepresentation[i].getNominalValue();
            }
        }
        return values;
    }

    /**
     * @return the setLogic
     */
    public SetLogic getSetLogic() {
        return m_setLogic;
    }

    /** {@inheritDoc} */
    @Override
    public boolean testCondition(final PredictorRecord record) {
        Object value = record.getValue(getColumnMetaData().getAttributeName());
        Integer assignedInteger = null;
        if (value == null) {
            //            throw new UnsupportedOperationException("Missing values currently not supported");
            //            NominalValueRepresentation[] values = getColumnMetaData().getValues();
            //            int l = values.length;
            //            assignedInteger = values[l - 1].equals(NominalValueRepresentation.MISSING_VALUE) ? l - 1 : l;
            return acceptsMissings();
        } else if (!(value instanceof Integer)) {
            throw new IllegalArgumentException("Can't test nominal condition (" + toString()
                + ") -- expected query object of type Integer (representing the nominal value) but got "
                + value.getClass().getSimpleName());
        } else {
            assignedInteger = (Integer)value;
        }

        return testCondition(assignedInteger);
    }

    public boolean testCondition(final int valueIndex) {
        return m_setLogic.eval(valueIndex >= 0 && m_valuesMask.testBit(valueIndex));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getColumnMetaData() + m_setLogic.toString(getValues());
    }

    /** {@inheritDoc} */
    @Override
    protected void saveContent(final DataOutputStream s) throws IOException {
        byte[] byteArray = m_valuesMask.toByteArray();
        // saved as MSB
        int logicInt = SetLogic.IS_IN.equals(m_setLogic) ? 0x80 : 0x0;
        int v = logicInt | byteArray.length;
        s.writeByte(v);
        s.write(byteArray);
    }

    /** {@inheritDoc} */
    @Override
    public PMMLPredicate toPMMLPredicate() {
        final PMMLSimpleSetPredicate setPredicate =
            new PMMLSimpleSetPredicate(getAttributeName(), m_setLogic.getPmmlSetOperator());
        setPredicate.setValues(Arrays.asList(getValues()));
        setPredicate.setArrayType(PMMLArrayType.STRING);
        if (!acceptsMissings()) {
            // if condition rejects missing values return the set predicate
            return setPredicate;
        }
        // otherwise create compound condition that allows missing values
        final PMMLCompoundPredicate compPredicate = new PMMLCompoundPredicate(PMMLBooleanOperator.OR);
        final PMMLSimplePredicate missing = new PMMLSimplePredicate();
        missing.setSplitAttribute(getAttributeName());
            missing.setOperator(PMMLOperator.IS_MISSING);
        compPredicate.addPredicate(setPredicate);
        compPredicate.addPredicate(missing);
        return compPredicate;
    }

}
