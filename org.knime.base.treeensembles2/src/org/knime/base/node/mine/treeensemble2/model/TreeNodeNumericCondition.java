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
 *   Jan 8, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble2.model;

import java.io.DataOutputStream;
import java.io.IOException;

import org.knime.base.node.mine.decisiontree2.PMMLBooleanOperator;
import org.knime.base.node.mine.decisiontree2.PMMLCompoundPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLOperator;
import org.knime.base.node.mine.decisiontree2.PMMLPredicate;
import org.knime.base.node.mine.decisiontree2.PMMLSimplePredicate;
import org.knime.base.node.mine.treeensemble2.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble2.data.TreeColumnMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeMetaData;
import org.knime.base.node.mine.treeensemble2.data.TreeNumericColumnMetaData;
import org.knime.base.node.util.DoubleFormat;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeNodeNumericCondition extends TreeNodeColumnCondition {

    public enum NumericOperator {
            LessThanOrEqual("<=", (byte)'s', PMMLOperator.LESS_OR_EQUAL),
            LargerThan(">", (byte)'l', PMMLOperator.GREATER_THAN),
            LessThanOrEqualOrMissing("<= || ?", (byte) 'm', null),
            LargerThanOrMissing("> || ?", (byte) 'n', null);

        private final String m_sign;

        private final byte m_persistByte;

        private PMMLOperator m_pmmlOperator;

        /**
         *  */
        private NumericOperator(final String sign, final byte persistByte, final PMMLOperator pmmlOperator) {
            m_sign = sign;
            m_persistByte = persistByte;
            m_pmmlOperator = pmmlOperator;
        }

        public PMMLOperator getPMMLOperator() {
            return m_pmmlOperator;
        }

        @Override
        public String toString() {
            return m_sign;
        };

        public void save(final DataOutputStream output) throws IOException {
            output.writeByte(m_persistByte);
        }

        public static final NumericOperator load(final TreeModelDataInputStream input) throws IOException {
            byte b = input.readByte();
            for (NumericOperator op : NumericOperator.values()) {
                if (op.m_persistByte == b) {
                    return op;
                }
            }
            throw new IOException("Unknown operator byte '" + (char)b + "'");
        }
    };

    private final double m_splitValue;

    private final NumericOperator m_numericOperator;

    /**
     * @param columnMetaData
     * @param splitValue
     */
    public TreeNodeNumericCondition(final TreeNumericColumnMetaData columnMetaData, final double splitValue,
        final NumericOperator operator, final boolean acceptsMissings) {
        super(columnMetaData, acceptsMissings);
        m_numericOperator = operator;
        m_splitValue = splitValue;
    }

    /**
     *  */
    TreeNodeNumericCondition(final TreeModelDataInputStream input, final TreeMetaData metaData) throws IOException {
        super(input, metaData);
        TreeColumnMetaData columnMetaData = super.getColumnMetaData();
        checkTypeCorrectness(columnMetaData, TreeNumericColumnMetaData.class);
        m_numericOperator = NumericOperator.load(input);
        m_splitValue = input.readDouble();
    }

    /**
     * @return the column meta data
     */
    @Override
    public TreeNumericColumnMetaData getColumnMetaData() {
        return (TreeNumericColumnMetaData)super.getColumnMetaData();
    }

    /**
     * @return the splitValue
     */
    public double getSplitValue() {
        return m_splitValue;
    }

    /**
     * @return the numericOperator
     */
    public NumericOperator getNumericOperator() {
        return m_numericOperator;
    }

    /** {@inheritDoc} */
    @Override
    public boolean testCondition(final PredictorRecord record) {
        Object value = record.getValue(getColumnMetaData().getAttributeName());
        double v = 0;
        if (value == null) {
//            throw new UnsupportedOperationException("Missing values currently not supported");
//            v = Double.NaN;
            return acceptsMissings();
        } else if (!(value instanceof Integer || value instanceof Double)) {
            throw new IllegalArgumentException("Can't test numeric condition (" + toString()
                + ") -- expected query object of type Double " + "but got " + value.getClass().getSimpleName());
        } else if (value instanceof Integer) {
            v = (Integer)value;
        } else {
            v = (Double)value;
        }


        switch (m_numericOperator) {
            case LargerThan:
                return v > m_splitValue;
            case LessThanOrEqual:
                return v <= m_splitValue;
            case LessThanOrEqualOrMissing:
                return Double.isNaN(v) ? true : v <= m_splitValue;
            case LargerThanOrMissing:
                return Double.isNaN(v) ? true : v > m_splitValue;
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + m_numericOperator);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PMMLPredicate toPMMLPredicate() {
        PMMLCompoundPredicate compound = new PMMLCompoundPredicate(PMMLBooleanOperator.OR);
        switch (m_numericOperator) {
            case LargerThanOrMissing :
                compound.addPredicate(new PMMLSimplePredicate(getAttributeName(), PMMLOperator.GREATER_THAN, Double.toString(m_splitValue)));
                compound.addPredicate(new PMMLSimplePredicate(getAttributeName(), PMMLOperator.IS_MISSING, Double.toString(m_splitValue)));
                return compound;
            case LessThanOrEqualOrMissing :
                compound.addPredicate(new PMMLSimplePredicate(getAttributeName(), PMMLOperator.LESS_OR_EQUAL, Double.toString(m_splitValue)));
                compound.addPredicate(new PMMLSimplePredicate(getAttributeName(), PMMLOperator.IS_MISSING, Double.toString(m_splitValue)));
                return compound;
        }
        final PMMLOperator pmmlOperator = m_numericOperator.m_pmmlOperator;
        if (pmmlOperator == null) {
            throw new IllegalStateException("There is no equivalent PMMLOperator for this NumericOperator.");
        }
        return new PMMLSimplePredicate(getAttributeName(), pmmlOperator, Double.toString(m_splitValue));
    }

    /** {@inheritDoc} */
    @Override
    protected void saveContent(final DataOutputStream output) throws IOException {
        m_numericOperator.save(output);
        output.writeDouble(m_splitValue);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String splitVal = DoubleFormat.formatDouble(m_splitValue);
        return getColumnMetaData() + " " + m_numericOperator + " " + splitVal;
    }

}
