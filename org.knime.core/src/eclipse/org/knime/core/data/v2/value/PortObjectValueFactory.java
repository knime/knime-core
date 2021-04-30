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
 *   Nov 10, 2020 (benjamin): created
 */
package org.knime.core.data.v2.value;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.model.PortObjectCell;
import org.knime.core.data.model.PortObjectValue;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortUtil;
import org.knime.core.table.access.ByteArrayAccess.VarBinaryReadAccess;
import org.knime.core.table.access.ByteArrayAccess.VarBinaryWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

/**
 * {@link ValueFactory} implementation for {@link PortObjectCell}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public class PortObjectValueFactory
    implements ValueFactory<VarBinaryReadAccess, VarBinaryWriteAccess> {

    /** Stateless instance of of {@link PortObjectValueFactory} */
    public static final PortObjectValueFactory INSTANCE = new PortObjectValueFactory();

    @Override
    public ReadValue createReadValue(final VarBinaryReadAccess access) {
        return new DefaultPortObjectReadValue(access);
    }

    @Override
    public WriteValue<?> createWriteValue(final VarBinaryWriteAccess access) {
        return new DefaultPortObjectWriteValue(access);
    }

    @Override
    public VarBinaryDataSpec getSpec() {
        return VarBinaryDataSpec.INSTANCE;
    }

    /**
     * {@link ReadValue} equivalent to {@link PortObjectCell}.
     *
     * @since 4.3
     */
    public interface PortObjectReadValue extends ReadValue, PortObjectValue {
    }

    /**
     * {@link WriteValue} equivalent to {@link PortObjectCell}.
     *
     * @since 4.3
     */
    public interface PortObjectWriteValue extends WriteValue<PortObjectValue> {

        /**
         * @param portObject the {@link PortObject} to set
         */
        void setPortObject(PortObject portObject);
    }

    /** An {@link InputStream} wrapping a {@link DataInput}. */
    private static final class DataInputWrappingInputStream extends InputStream {

        private final DataInput m_input;

        private DataInputWrappingInputStream(final DataInput input) {
            m_input = input;
        }

        @Override
        public int read(final byte[] b, final int off, final int len) throws IOException {
            try {
                m_input.readFully(b, off, len);
                return len;
            } catch (final EOFException ex) { // NOSONAR: We can handle the exception
                return super.read(b, off, len);
            }
        }

        @Override
        public int read() throws IOException {
            try {
                return m_input.readUnsignedByte();
            } catch (final EOFException ex) { // NOSONAR: We can handle the exception
                return -1;
            }
        }
    }

    /** An {@link OutputStream} wrapping a {@link DataOutput}. */
    private static final class DataOutputWrappingOutputStream extends OutputStream {

        private final DataOutput m_output;

        public DataOutputWrappingOutputStream(final DataOutput output) {
            m_output = output;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            m_output.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            m_output.write(b);
        }
    }

    /** Default implementation of {@link PortObjectReadValue}. */
    private static final class DefaultPortObjectReadValue implements PortObjectReadValue {

        private final VarBinaryReadAccess m_access;

        private final ObjectDeserializer<PortObject> m_deserializer;

        private DefaultPortObjectReadValue(final VarBinaryReadAccess access) {
            m_access = access;
            m_deserializer = input -> {
                try (final DataInputWrappingInputStream stream = new DataInputWrappingInputStream(input)) {
                  return PortUtil.readObjectFromStream(stream, null);
              } catch (final CanceledExecutionException e) {
                  // This cannot happen because the execution context is null
                  throw new IllegalStateException("Deserializing the PortObject was canceled.", e);
              }
            };
        }

        @Override
        public DataCell getDataCell() {
            return new PortObjectCell(getPortObject());
        }

        @Override
        public PortObject getPortObject() {
            return m_access.getObject(m_deserializer);
        }
    }

    /** Default implementation of {@link PortObjectWriteValue}. */
    private static final class DefaultPortObjectWriteValue implements PortObjectWriteValue {

        private final VarBinaryWriteAccess m_access;

        private final ObjectSerializer<PortObject> m_serializer;

        private DefaultPortObjectWriteValue(final VarBinaryWriteAccess access) {
            m_access = access;
            m_serializer = (output, object) -> {
                try (final DataOutputWrappingOutputStream stream = new DataOutputWrappingOutputStream(output)) {
                  PortUtil.writeObjectToStream(object, stream, new ExecutionMonitor());
              } catch (final CanceledExecutionException e) {
                  // This cannot happen because the execution monitor won't be canceld
                    throw new IllegalStateException("Serializing the PortObject was canceled.", e);
              }
            };
        }

        @Override
        public void setValue(final PortObjectValue value) {
            setPortObject(value.getPortObject());
        }

        @Override
        public void setPortObject(final PortObject portObject) {
            m_access.setObject(portObject, m_serializer);
        }
    }
}
