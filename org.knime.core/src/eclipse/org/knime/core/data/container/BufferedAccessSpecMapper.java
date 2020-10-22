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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   Oct 22, 2020 (dietzc): created
 */
package org.knime.core.data.container;

import java.util.stream.Stream;

import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.container.BufferedAccessSpecMapper.BufferedAccess;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.access.AccessSpec;
import org.knime.core.data.v2.access.AccessSpec.AccessSpecMapper;
import org.knime.core.data.v2.access.BooleanAccess.BooleanAccessSpec;
import org.knime.core.data.v2.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.data.v2.access.BooleanAccess.BooleanWriteAccess;
import org.knime.core.data.v2.access.ByteArrayAccess.ByteArrayAccessSpec;
import org.knime.core.data.v2.access.ByteArrayAccess.VarBinaryReadAccess;
import org.knime.core.data.v2.access.ByteArrayAccess.VarBinaryWriteAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleAccessSpec;
import org.knime.core.data.v2.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleWriteAccess;
import org.knime.core.data.v2.access.IntAccess.IntAccessSpec;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntWriteAccess;
import org.knime.core.data.v2.access.LongAccess.LongAccessSpec;
import org.knime.core.data.v2.access.LongAccess.LongReadAccess;
import org.knime.core.data.v2.access.LongAccess.LongWriteAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectAccessSpec;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectWriteAccess;
import org.knime.core.data.v2.access.ReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructAccessSpec;
import org.knime.core.data.v2.access.StructAccess.StructReadAccess;
import org.knime.core.data.v2.access.StructAccess.StructWriteAccess;
import org.knime.core.data.v2.access.VoidAccess.VoidAccessSpec;
import org.knime.core.data.v2.access.WriteAccess;

/**
 * Mapper to map {@link AccessSpec} to the corresponding buffered access implementation.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 */
final class BufferedAccessSpecMapper implements AccessSpecMapper<BufferedAccess> {

    final static BufferedAccessSpecMapper INSTANCE = new BufferedAccessSpecMapper();

    private BufferedAccessSpecMapper() {
    }

    /* Simple marker interface for combined ReadAccess and WriteAccess */
    interface BufferedAccess extends ReadAccess, WriteAccess {
    }

    @Override
    public BufferedAccess visit(final ObjectAccessSpec<?> spec) {
        return new BufferedObjectAccess<>();
    }

    @Override
    public BufferedAccess visit(final StructAccessSpec spec) {
        return new BufferedStructAccess(spec);
    }

    @Override
    public BufferedAccess visit(final DoubleAccessSpec spec) {
        return new BufferedDoubleAccess();
    }

    @Override
    public BufferedAccess visit(final BooleanAccessSpec spec) {
        return new BufferedBooleanAccess();
    }

    @Override
    public BufferedAccess visit(final IntAccessSpec spec) {
        return new BufferedIntAccess();
    }

    @Override
    public BufferedAccess visit(final LongAccessSpec spec) {
        return new BufferedLongAccess();
    }

    @Override
    public BufferedAccess visit(final VoidAccessSpec spec) {
        return BufferedVoidAccess.VOID_ACCESS_INSTANCE;
    }

    @Override
    public BufferedAccess visit(final ByteArrayAccessSpec spec) {
        return new BufferedByteArrayAccess();
    }

    private final static class BufferedByteArrayAccess
        implements VarBinaryReadAccess, VarBinaryWriteAccess, BufferedAccess {

        private boolean m_isMissing = true;

        private byte[] m_value;

        @Override
        public boolean isMissing() {
            return m_isMissing;
        }

        @Override
        public void setMissing() {
            m_isMissing = true;
        }

        @Override
        public void setByteArray(final byte[] value) {
            m_value = value;
            m_isMissing = false;
        }

        @Override
        public void setByteArray(final byte[] array, final int index, final int length) {
            m_value = new byte[length];
            System.arraycopy(array, 0, m_value, 0, length);
            m_isMissing = false;
        }

        @Override
        public byte[] getByteArray() {
            return m_value;
        }

    }

    private final static class BufferedVoidAccess implements BufferedAccess {

        private static final BufferedVoidAccess VOID_ACCESS_INSTANCE = new BufferedVoidAccess();

        private BufferedVoidAccess() {
        }

        @Override
        public boolean isMissing() {
            return true;
        }

        @Override
        public void setMissing() {
        }

    }

    private final static class BufferedBooleanAccess implements BooleanReadAccess, BooleanWriteAccess, BufferedAccess {

        private byte m_value;

        private boolean m_isMissing = true;

        @Override
        public boolean isMissing() {
            return m_isMissing;
        }

        @Override
        public boolean getBooleanValue() {
            return m_value == 1;
        }

        @Override
        public double getDoubleValue() {
            return m_value;
        }

        @Override
        public long getLongValue() {
            return m_value;
        }

        @Override
        public int getIntValue() {
            return m_value;
        }

        @Override
        public double getRealValue() {
            return m_value;
        }

        @Override
        public double getImaginaryValue() {
            return 0;
        }

        @Override
        public double getMinSupport() {
            return m_value;
        }

        @Override
        public double getCore() {
            return m_value;
        }

        @Override
        public double getMaxSupport() {
            return m_value;
        }

        @Override
        public double getMinCore() {
            return m_value;
        }

        @Override
        public double getMaxCore() {
            return m_value;
        }

        @Override
        public double getCenterOfGravity() {
            return m_value;
        }

        @Override
        public DataCell getDataCell() {
            return m_value == 1 ? BooleanCell.TRUE : BooleanCell.FALSE;
        }

        @Override
        public void setMissing() {
            m_isMissing = true;
        }

        @Override
        public void setBooleanValue(final boolean value) {
            m_value = (byte)(value ? 1 : 0);
            m_isMissing = false;
        }

        @Override
        public void setValue(final BooleanValue value) {
            setBooleanValue(value.getBooleanValue());
        }

    }

    private final static class BufferedLongAccess implements LongReadAccess, LongWriteAccess, BufferedAccess {

        private long m_value;

        private boolean m_isMissing = true;

        @Override
        public boolean isMissing() {
            return m_isMissing;
        }

        @Override
        public long getLongValue() {
            return m_value;
        }

        @Override
        public double getDoubleValue() {
            return m_value;
        }

        @Override
        public DataCell getDataCell() {
            return new LongCell(m_value);
        }

        @Override
        public double getRealValue() {
            return m_value;
        }

        @Override
        public double getImaginaryValue() {
            return 0;
        }

        @Override
        public double getMinSupport() {
            return m_value;
        }

        @Override
        public double getCore() {
            return m_value;
        }

        @Override
        public double getMaxSupport() {
            return m_value;
        }

        @Override
        public double getMinCore() {
            return m_value;
        }

        @Override
        public double getMaxCore() {
            return m_value;
        }

        @Override
        public double getCenterOfGravity() {
            return m_value;
        }

        @Override
        public void setMissing() {
            m_isMissing = true;
        }

        @Override
        public void setLongValue(final long value) {
            m_value = value;
            m_isMissing = false;
        }

        @Override
        public void setValue(final LongValue value) {
            setLongValue(value.getLongValue());
        }

    }

    private final static class BufferedIntAccess implements IntReadAccess, IntWriteAccess, BufferedAccess {

        private int m_value;

        private boolean m_isMissing = true;

        @Override
        public boolean isMissing() {
            return m_isMissing;
        }

        @Override
        public DataCell getDataCell() {
            return new IntCell(m_value);
        }

        @Override
        public int getIntValue() {
            return m_value;
        }

        @Override
        public double getDoubleValue() {
            return m_value;
        }

        @Override
        public double getRealValue() {
            return m_value;
        }

        @Override
        public double getImaginaryValue() {
            return 0;
        }

        @Override
        public double getMinSupport() {
            return m_value;
        }

        @Override
        public double getCore() {
            return m_value;
        }

        @Override
        public double getMaxSupport() {
            return m_value;
        }

        @Override
        public double getMinCore() {
            return m_value;
        }

        @Override
        public double getMaxCore() {
            return m_value;
        }

        @Override
        public double getCenterOfGravity() {
            return m_value;
        }

        @Override
        public long getLongValue() {
            return m_value;
        }

        @Override
        public void setMissing() {
            m_isMissing = true;
        }

        @Override
        public void setIntValue(final int value) {
            m_value = value;
            m_isMissing = false;
        }

        @Override
        public void setValue(final IntValue value) {
            setIntValue(value.getIntValue());
        }

    }

    private final static class BufferedDoubleAccess implements DoubleWriteAccess, DoubleReadAccess, BufferedAccess {

        private double m_value;

        private boolean m_isMissing;

        @Override
        public DataCell getDataCell() {
            return new DoubleCell(m_value);
        }

        @Override
        public void setDoubleValue(final double value) {
            m_value = value;
            m_isMissing = false;
        }

        @Override
        public void setValue(final DoubleValue value) {
            setDoubleValue(value.getDoubleValue());
        }

        @Override
        public double getDoubleValue() {
            return m_value;
        }

        @Override
        public double getRealValue() {
            return m_value;
        }

        @Override
        public double getImaginaryValue() {
            return 0;
        }

        @Override
        public double getMinSupport() {
            return m_value;
        }

        @Override
        public double getCore() {
            return m_value;
        }

        @Override
        public double getMaxSupport() {
            return m_value;
        }

        @Override
        public double getMinCore() {
            return m_value;
        }

        @Override
        public double getMaxCore() {
            return m_value;
        }

        @Override
        public double getCenterOfGravity() {
            return m_value;
        }

        @Override
        public boolean isMissing() {
            return m_isMissing;
        }

        @Override
        public void setMissing() {
            m_isMissing = true;
        }
    }

    private final static class BufferedStructAccess implements StructWriteAccess, StructReadAccess, BufferedAccess {

        private final BufferedAccess[] m_inner;

        BufferedStructAccess(final StructAccessSpec spec) {
            m_inner = Stream.of(spec).map(inner -> inner.accept(BufferedAccessSpecMapper.INSTANCE))
                .toArray(BufferedAccess[]::new);
        }

        @Override
        public <R extends ReadAccess> R getInnerReadAccessAt(final int index) {
            @SuppressWarnings("unchecked")
            final R cast = (R)m_inner[index];
            return cast;
        }

        @Override
        public <W extends WriteAccess> W getWriteAccessAt(final int index) {
            @SuppressWarnings("unchecked")
            final W cast = (W)m_inner[index];
            return cast;
        }

        @Override
        public boolean isMissing() {
            // if one value was set in any of the inner access, we consider the struct to be valid
            for (final BufferedAccess access : m_inner) {
                if (!access.isMissing()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void setMissing() {
            for (final BufferedAccess access : m_inner) {
                access.setMissing();
            }
        }
    }

    private final static class BufferedObjectAccess<T>
        implements ObjectWriteAccess<T>, ObjectReadAccess<T>, BufferedAccess {

        private T m_object = null;

        @Override
        public T getObject() {
            return m_object;
        }

        @Override
        public void setObject(final T object) {
            m_object = object;
        }

        @Override
        public boolean isMissing() {
            return m_object == null;
        }

        @Override
        public void setMissing() {
            m_object = null;
        }
    }
}
