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

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.AbstractPrimitiveWritableObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectCellFactory;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

import com.facebook.presto.spi.block.Block;
import com.facebook.presto.spi.type.BigintType;
import com.facebook.presto.spi.type.DoubleType;
import com.facebook.presto.spi.type.Type;
import com.facebook.presto.spi.type.VarbinaryType;
import com.facebook.presto.spi.type.VarcharType;

/**
 *
 * @author wiswedel
 */
public abstract class OrcKNIMEType {
    public static final DoubleOrcKNIMEType DOUBLE = new DoubleOrcKNIMEType();
    public static final LongOrcKNIMEType LONG = new LongOrcKNIMEType();
    public static final StringOrcKNIMEType STRING = new StringOrcKNIMEType();
    public static final ByteArrayOrcKNIMEType BYTE_ARRAY = new ByteArrayOrcKNIMEType();

    private final AbstractPrimitiveWritableObjectInspector m_factory;
    private final Type m_prestoType;
    private final Supplier<Object> m_hadoopObjectSupplier;

    /**
     * @param factory
     */
    OrcKNIMEType(final AbstractPrimitiveWritableObjectInspector factory, final Type prestoType,
        final Supplier<Object> hadoopObjectSupplier) {
        m_factory = factory;
        m_prestoType = prestoType;
        m_hadoopObjectSupplier = hadoopObjectSupplier;
    }

    AbstractPrimitiveWritableObjectInspector getObjectInspectorFactory() {
        return m_factory;
    }

    final Type getPrestoType() {
        return m_prestoType;
    }

    Object createHadoopObject() {
        return m_hadoopObjectSupplier.get();
    }

    void save(final NodeSettingsWO settings) {
        String value;
        if (this == OrcKNIMEType.DOUBLE) {
            value = "double";
        } else if (this == OrcKNIMEType.LONG) {
            value = "long";
        } else if (this == OrcKNIMEType.STRING) {
            value = "string";
        } else if (this == OrcKNIMEType.BYTE_ARRAY) {
            value = "bytearray";
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
            case "long":
                return OrcKNIMEType.LONG;
            case "string":
                return OrcKNIMEType.STRING;
            case "bytearray":
                return OrcKNIMEType.BYTE_ARRAY;
            default:
                throw new InvalidSettingsException("Not support orc type");
        }
    }

    abstract void set(final DataCell knimeValue, final Object hadoopValue);

    final DataCell get(final Block block, final int position) {
        if (block.isNull(position)) {
            return DataType.getMissingCell();
        }
        return getNonMissing(block, position);
    }

    abstract DataCell getNonMissing(final Block block, final int position);

    static final class DoubleOrcKNIMEType extends OrcKNIMEType {
        private DoubleOrcKNIMEType() {
            super(PrimitiveObjectInspectorFactory.writableDoubleObjectInspector, DoubleType.DOUBLE, () -> new DoubleWritable());
        }
        @Override
        void set(final DataCell knimeValue, final Object hadoopValue) {
            ((DoubleWritable)hadoopValue).set(((DoubleValue)knimeValue).getDoubleValue());
        }
        @Override
        DataCell getNonMissing(final Block block, final int position) {
            return new DoubleCell(getPrestoType().getDouble(block, position));
        }
    }

    static final class LongOrcKNIMEType extends OrcKNIMEType {
        private LongOrcKNIMEType() {
            super(PrimitiveObjectInspectorFactory.writableLongObjectInspector, BigintType.BIGINT, () -> new LongWritable());

        }
        @Override
        void set(final DataCell knimeValue, final Object hadoopValue) {
            ((LongWritable)hadoopValue).set(((LongValue)knimeValue).getLongValue());
        }
        @Override
        DataCell getNonMissing(final Block block, final int position) {
            return new IntCell((int)getPrestoType().getLong(block, position));
        }
    }

    static final class StringOrcKNIMEType extends OrcKNIMEType {
        private StringOrcKNIMEType() {
            super(PrimitiveObjectInspectorFactory.writableStringObjectInspector, VarcharType.VARCHAR, () -> new Text());
        }
        @Override
        void set(final DataCell knimeValue, final Object hadoopValue) {
            ((Text)hadoopValue).set(((StringValue)knimeValue).getStringValue());
        }
        @Override
        DataCell getNonMissing(final Block block, final int position) {
            return new StringCell(decode(getPrestoType().getObjectValue(null, block, position)));
        }
        private String decode(final Object hadoopValue) {
            try {
                if (true) {
                    return (String)hadoopValue;
                }
                return Text.decode(((Text)hadoopValue).getBytes());
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }
        }
        void set(final String str, final Object hadoopValue) {
            ((Text)hadoopValue).set(str);
        }
        public String getString(final Object value) {
            return decode(value);
        }
    }

    static final class ByteArrayOrcKNIMEType extends OrcKNIMEType {
        private ByteArrayOrcKNIMEType() {
            super(PrimitiveObjectInspectorFactory.writableBinaryObjectInspector, VarbinaryType.VARBINARY , () -> new BytesWritable());
        }
        @Override
        void set(final DataCell knimeValue, final Object hadoopValue) {
            byte[] bytes;
            try {
                bytes = IOUtils.toByteArray(((BinaryObjectDataValue)knimeValue).openInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ((BytesWritable)hadoopValue).set(bytes, 0, bytes.length);
        }
        @Override
        DataCell getNonMissing(final Block block, final int position) {
            try {
                return new BinaryObjectCellFactory().create(
                    ((BytesWritable)getPrestoType().getObjectValue(null, block, position)).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
