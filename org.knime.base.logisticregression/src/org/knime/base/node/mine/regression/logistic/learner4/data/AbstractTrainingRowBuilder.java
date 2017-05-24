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
 *   17.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.MathUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.doublevector.DoubleVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * This class is NOT thread-safe!
 *
 * @author Adrian Nembach, KNIME.com
 */
abstract class AbstractTrainingRowBuilder <T extends TrainingRow> {
    private final List<Integer> m_featureCellIndices;
    private final int m_featureCount;
    private final Map<Integer, Integer> m_vectorLengths;
    private final Map<Integer, List<DataCell>> m_nominalDomainValues;
    // used as cache for the indices to avoid the overhead of creating arrays
    private final int[] m_nonZeroIndices;
    // same for the values
    private final float[] m_nonZerovalues;

    /**
     * @param data
     * @param pmmlSpec
     * @param sortFactorsCategories
     *
     */
    public AbstractTrainingRowBuilder(final BufferedDataTable data,
        final PMMLPortObjectSpec pmmlSpec,
        final boolean sortFactorsCategories) {

        final List<DataColumnSpec> learningCols = new ArrayList<>(pmmlSpec.getLearningCols());
        m_featureCellIndices = new ArrayList<>(learningCols.size());
        m_nominalDomainValues = new HashMap<>();
        m_vectorLengths = new HashMap<>();
        int featureCount = 0;
        // for a one-hot encoded nominal feature only one value will effectively be non zero
        int effectiveFeatureCount = 0;
        int visited = 0;
        DataTableSpec tableSpec = data.getDataTableSpec();
        for (DataColumnSpec colSpec : learningCols) {
            int colIdx = tableSpec.findColumnIndex(colSpec.getName());
            m_featureCellIndices.add(colIdx);
            DataType cellType = colSpec.getType();
            if (cellType.isCompatible(NominalValue.class)) {
                List<DataCell> valueList = new ArrayList<DataCell>();
                valueList.addAll(colSpec.getDomain().getValues());
                if (sortFactorsCategories) {
                    Collections.sort(valueList, cellType.getComparator());
                }
                m_nominalDomainValues.put(colIdx, valueList);
                featureCount += Math.max(0, valueList.size() - 1);
                effectiveFeatureCount++;
            } else if (cellType.isCompatible(BitVectorValue.class) || cellType.isCompatible(ByteVectorValue.class)
                    || cellType.isCompatible(DoubleVectorValue.class)
                    || (cellType.isCollectionType() && cellType.getCollectionElementType().isCompatible(DoubleValue.class))) {
                    final int maxLength = getMaximalVectorLength(data, colIdx);
                    CheckUtils.checkArgument(maxLength > 0 && maxLength <= Integer.MAX_VALUE, "For column " + colSpec + " the maximal length for the values is wrong: " + maxLength);
                    m_vectorLengths.put(colIdx, maxLength);
                    CheckUtils.checkState(featureCount + maxLength <= Integer.MAX_VALUE, "Too many values in " + colSpec.getName());
                    featureCount += maxLength;
                    effectiveFeatureCount += maxLength;
            } else {
                effectiveFeatureCount++;
                featureCount++;
            }
        }
        m_featureCount = featureCount;
        m_nonZeroIndices = new int[effectiveFeatureCount];
        m_nonZerovalues = new float[effectiveFeatureCount];
    }

    T build(final DataRow row, final int id) {
        int nonZeroFeatures = 0;
        int accumulatedIdx = 0;
        for (int i = 0; i < m_featureCellIndices.size(); i++) {
            Integer cellIdx = m_featureCellIndices.get(i);
            DataCell cell = row.getCell(cellIdx);
            DataType cellType = cell.getType();
            if (cellType.isCompatible(NominalValue.class)) {
                List<DataCell> nominalDomainValues = m_nominalDomainValues.get(cellIdx);
                int oneHotIdx = nominalDomainValues.indexOf(cell);
                if (oneHotIdx == -1) {
                    throw new IllegalStateException(
                        "DataCell \"" + cell.toString() + "\" is not in the DataColumnDomain. Please apply a "
                            + "Domain Calculator on the columns with nominal values.");
                } else if (oneHotIdx > 0) {
                    m_nonZeroIndices[nonZeroFeatures] = accumulatedIdx + oneHotIdx - 1;
                    m_nonZerovalues[nonZeroFeatures] = 1.0F;
                    nonZeroFeatures++;
                }
                accumulatedIdx += nominalDomainValues.size() - 1;
            } else if (m_vectorLengths.containsKey(cellIdx)) {
                if (cellType.isCompatible(BitVectorValue.class)) {
                    BitVectorValue bv = (BitVectorValue)cell;
                    for (long s = bv.nextSetBit(0L); s >= 0; s = bv.nextSetBit(s + 1)) {
                        m_nonZeroIndices[nonZeroFeatures] = (int)(accumulatedIdx + s);
                        m_nonZerovalues[nonZeroFeatures++] = 1.0F;
                    }
                } else if (cellType.isCompatible(ByteVectorValue.class)) {
                    ByteVectorValue bv = (ByteVectorValue)cell;
                    for (long s = bv.nextCountIndex(0L); s >= 0; s = bv.nextCountIndex(s + 1)) {
                        m_nonZeroIndices[nonZeroFeatures] = (int)(accumulatedIdx + s);
                        m_nonZerovalues[nonZeroFeatures++] = bv.get(s);
                    }
                } else if (cellType.isCompatible(DoubleVectorValue.class)) {
                    // DoubleVectorValue also implements CollectionDataValue but
                    // as it then first boxes its values into DataCells, it is much more
                    // efficient to access its values via the DoubleVectorValue interface
                    DoubleVectorValue dv = (DoubleVectorValue)cell;
                    for (int s = 0; s < dv.getLength(); s++) {
                        float val = (float)dv.getValue(s);
                        if (!MathUtils.equals(val, 0.0)) {
                            m_nonZeroIndices[nonZeroFeatures] = accumulatedIdx + s;
                            m_nonZerovalues[nonZeroFeatures++] = val;
                        }
                    }
                } else if (cellType.isCollectionType() && cellType.getCollectionElementType().isCompatible(DoubleValue.class)) {
                    CollectionDataValue cv = (CollectionDataValue)cell;
                    int s = 0;
                    for (DataCell c : cv) {
                        // we already checked above that cv contains DoubleValues
                        DoubleValue dv = (DoubleValue)c;
                        double val = dv.getDoubleValue();
                        if (!MathUtils.equals(val, 0.0)) {
                            m_nonZeroIndices[nonZeroFeatures] = accumulatedIdx + s;
                            m_nonZerovalues[nonZeroFeatures] = (float)val;
                        }
                        s++;
                    }
                } else {
                    // should never be thrown because we check the compatibility in the constructor
                    throw new IllegalStateException("DataCell \"" + cell.toString() +
                        "\" is of an unknown vector/collections type.");
                }
                accumulatedIdx += m_vectorLengths.get(cellIdx);
            } else if (cellType.isCompatible(DoubleValue.class)) {
                double val = ((DoubleValue)cell).getDoubleValue();
                if (!MathUtils.equals(val, 0.0)) {
                    m_nonZeroIndices[nonZeroFeatures] = accumulatedIdx;
                    m_nonZerovalues[nonZeroFeatures++] = (float)val;
                }
                accumulatedIdx++;
            } else {
                // again this can only be thrown if a otherwise compatible column contains
                // a different DataCell of incompatible type.
                throw new IllegalStateException("The DataCell \"" + cell.toString() +
                    "\" is of incompatible type \"" + cellType.toPrettyString() + "\".");
            }
        }
        int[] nonZero = Arrays.copyOf(m_nonZeroIndices, nonZeroFeatures);
        float[] values = Arrays.copyOf(m_nonZerovalues, nonZeroFeatures);

        return createTrainingRow(row, nonZero, values, id);
    }

    protected abstract T createTrainingRow(DataRow row, int[] nonZeroFeatures, float[] values, int id);

    int getFeatureCount() {
        return m_featureCount;
    }

    abstract int getTargetDimension();

    private static int getMaximalVectorLength(final BufferedDataTable data, final int colIdx) {
        long length = 0;
        for (DataRow row : data) {
            DataCell cell = row.getCell(colIdx);
            DataType cellType = cell.getType();
            long vectorLength = 0;
            if (cellType.isCompatible(BitVectorValue.class)) {
                BitVectorValue bv = (BitVectorValue)cell;
                vectorLength = bv.length();
            } else if (cellType.isCompatible(ByteVectorValue.class)) {
                ByteVectorValue bv = (ByteVectorValue)cell;
                vectorLength = bv.length();
            } else if (cellType.isCompatible(DoubleVectorValue.class)) {
                DoubleVectorValue dv = (DoubleVectorValue)cell;
                vectorLength = dv.getLength();
            } else if (cell instanceof ListDataValue) {
                ListDataValue ldv = (ListDataValue)cell;
                vectorLength = ldv.size();
            } else {
                throw new IllegalStateException("Wrong type of value in the column with index (from 1): " + (colIdx + 1) + " : " + cell.getType());
            }
            if (length < vectorLength) {
                length = vectorLength;
            }
        }
        if (length > Integer.MAX_VALUE) {
            throw new IllegalStateException("The vector column with index (from 1) \"" + (colIdx + 1) + "\" contains too many values.");
        }
        return (int)length;
    }

}
