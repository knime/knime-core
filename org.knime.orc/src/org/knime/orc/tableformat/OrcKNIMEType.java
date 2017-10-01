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
 *   Mar 18, 2016 (wiswedel): created
 */
package org.knime.orc.tableformat;

import java.nio.charset.Charset;

import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.orc.TypeDescription;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 *
 * @author wiswedel
 */
public abstract class OrcKNIMEType<C extends ColumnVector> {

    public static final DoubleOrcKNIMEType DOUBLE = new DoubleOrcKNIMEType();
    public static final StringOrcKNIMEType STRING = new StringOrcKNIMEType();
    public static final IntOrcKNIMEType INT = new IntOrcKNIMEType();
    public static final LongOrcKNIMEType LONG = new LongOrcKNIMEType();

//    private final AbstractPrimitiveWritableObjectInspector m_factory;
//    private final Type m_prestoType;
//    private final Supplier<Object> m_hadoopObjectSupplier;
    private final TypeDescription m_orcTypeDescription;

    /**
     * @param factory
     */
    OrcKNIMEType(final TypeDescription typeDescription) {
        m_orcTypeDescription = typeDescription;
    }

    void writeValue(final ColumnVector columnVector, final int rowInBatch, final DataCell cell) {
        if (cell.isMissing()) {
            columnVector.isNull[rowInBatch] = true;
        } else {
            writeValueNonNull((C)columnVector, rowInBatch, cell);
        }
    }

    abstract void writeValueNonNull(final C columnVector, final int rowInBatch, final DataCell cell);

    @SuppressWarnings("unchecked")
    DataCell readValue(final ColumnVector columnVector, final int rowInBatch) {
        if (columnVector.noNulls) {
            if (columnVector.isRepeating) {
                return readValueNonNull((C)columnVector, 0);
            } else if (columnVector.isNull[rowInBatch]) {
                return DataType.getMissingCell();
            } else {
                return readValueNonNull((C)columnVector, rowInBatch);
            }
        } else {
            return DataType.getMissingCell();
        }
    }

    abstract DataCell readValueNonNull(final C columnVector, int rowInBatchOrZero);

    final TypeDescription getTypeDescription() {
        return m_orcTypeDescription;
    }

    void save(final NodeSettingsWO settings) {
        String value;
        if (this == OrcKNIMEType.DOUBLE) {
            value = "double";
        } else if (this == OrcKNIMEType.STRING) {
            value = "string";
        } else if (this == OrcKNIMEType.INT) {
            value = "int";
        } else if (this == OrcKNIMEType.LONG) {
            value = "long";
        } else {
            throw new IllegalStateException("Not support orc type");
        }
        settings.addString("orctype", value);
    }

    static OrcKNIMEType load(final NodeSettingsRO settings) throws InvalidSettingsException {
        String value = settings.getString("orctype");
        switch (value) {
            case "double":
                return OrcKNIMEType.DOUBLE;
            case "int":
                return OrcKNIMEType.INT;
            case "long":
                return OrcKNIMEType.LONG;
            case "string":
                return OrcKNIMEType.STRING;
            default:
                throw new InvalidSettingsException("Unsupported ORC type: " + value);
        }
    }


    static final class DoubleOrcKNIMEType extends OrcKNIMEType<DoubleColumnVector> {
        private DoubleOrcKNIMEType() {
            super(TypeDescription.createDouble());
        }

        @Override
        void writeValueNonNull(final DoubleColumnVector columnVector, final int rowInBatch, final DataCell cell) {
            columnVector.vector[rowInBatch] = ((DoubleValue)cell).getDoubleValue();
        }

        @Override
        DataCell readValueNonNull(final DoubleColumnVector columnVector, final int rowInBatchOrZero) {
            return new DoubleCell(columnVector.vector[rowInBatchOrZero]);
        }

    }

    static final class IntOrcKNIMEType extends OrcKNIMEType<LongColumnVector> {
        private IntOrcKNIMEType() {
            super(TypeDescription.createInt());
        }

        @Override
        void writeValueNonNull(final LongColumnVector columnVector, final int rowInBatch, final DataCell cell) {
            columnVector.vector[rowInBatch] = ((IntValue)cell).getIntValue();
        }

        @Override
        DataCell readValueNonNull(final LongColumnVector columnVector, final int rowInBatchOrZero) {
            long valueL = columnVector.vector[rowInBatchOrZero];
            int valueI = (int)valueL;
            if (valueI != valueL) {
                throw new RuntimeException(String.format(
                    "Written as int but read as a long (overflow): (long)%d != (int)%d", valueL, valueI));
            }
            return new IntCell(valueI);
        }
    }

    static final class LongOrcKNIMEType extends OrcKNIMEType<LongColumnVector> {
        private LongOrcKNIMEType() {
            super(TypeDescription.createLong());
        }

        @Override
        void writeValueNonNull(final LongColumnVector columnVector, final int rowInBatch, final DataCell cell) {
            columnVector.vector[rowInBatch] = ((LongValue)cell).getLongValue();
        }

        @Override
        DataCell readValueNonNull(final LongColumnVector columnVector, final int rowInBatchOrZero) {
            return new LongCell(columnVector.vector[rowInBatchOrZero]);
        }
    }

    static final class StringOrcKNIMEType extends OrcKNIMEType<BytesColumnVector> {
        private static final Charset UTF_8 = Charset.forName("UTF-8");
        private StringOrcKNIMEType() {
            super(TypeDescription.createString());
        }

        @Override
        void writeValueNonNull(final BytesColumnVector columnVector, final int rowInBatch, final DataCell cell) {
            byte[] b = ((StringValue)cell).getStringValue().getBytes(UTF_8);
            columnVector.setRef(rowInBatch, b, 0, b.length);
        }

        @Override
        DataCell readValueNonNull(final BytesColumnVector columnVector, final int rowInBatchOrZero) {
            return new StringCell(new String(columnVector.vector[rowInBatchOrZero], UTF_8));
        }

        // TODO new method with byte[]
        final void writeValue(final ColumnVector columnVector, final int rowInBatch, final String string) {
            final BytesColumnVector byteVectorColumn = (BytesColumnVector)columnVector;
            if (string == null) {
                byteVectorColumn.isNull[rowInBatch] = true;
            } else {
                byte[] b = string.getBytes(UTF_8);
                byteVectorColumn.setRef(rowInBatch, b, 0, b.length);
            }
        }

        /**
         * @param columnVector
         * @param rowInBatch
         * @return
         */
        public String readString(final ColumnVector columnVector, final int rowInBatch) {
            final BytesColumnVector byteVectorColumn = (BytesColumnVector)columnVector;
            if (byteVectorColumn.noNulls) {
                if (byteVectorColumn.isRepeating) {
                    return new String(byteVectorColumn.vector[0], UTF_8);
                } else if (byteVectorColumn.isNull[rowInBatch]) {
                    return null;
                }
            }
            return null;
        }

    }

}
