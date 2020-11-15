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
 *   Oct 2, 2020 (dietzc): created
 */
package org.knime.core.data.v2.access;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.NominalValue;

/**
 * Definition of {@link ObjectAccess} to read and write arbitrary objects via serialization / deserialization into
 * byte[]. The underlying data table back-end is responsible for caching. Domains for {@link BoundedValue} and
 * {@link NominalValue}s will automatically be calculated. In case of {@link BoundedValue}s the
 * {@link DataValueComparator} is used to compare values.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @since 4.3
 *
 * @noreference This class is not intended to be referenced by clients.
 */
public final class ObjectAccess {

    private ObjectAccess() {
    }

    /**
     * {@link ReadAccess} to read objects.
     *
     * @param <T> type of object
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface ObjectReadAccess<T> extends ReadAccess {

        /**
         * @return the current object
         */
        T getObject();
    }

    /**
     * {@link WriteAccess} to write objects.
     *
     * @param <T> type of object
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface ObjectWriteAccess<T> extends WriteAccess {
        /**
         * @param object to be written
         */
        void setObject(T object);
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess}.
     *
     * @param <T> type of object
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface ObjectAccessSpec<T> extends AccessSpec<ObjectReadAccess<T>, ObjectWriteAccess<T>> {

        /**
         * @return serializer to read/write objects.
         */
        ObjectSerializer<T> getSerializer();

        @Override
        default <V> V accept(final AccessSpecMapper<V> v) {
            return v.visit(this);
        }

        /**
         * TODO we can add more 'dict encoding' options later in a DictEncodingConfig obj.
         *
         * @return <source>true</source> if dict encoded
         */
        default boolean isDictEncoded() {
            return false;
        }
    }

    /**
     * Serializes an object to bytes[] and vice-versa. Must be stateless.
     *
     * TODO Especially for serialization we want to provide an Consumer<byte[]> which has additional methods such as
     * set(byte[], from, to). Like this we avoid unncessary data copy as in the case of CustomRowKey or DataCell...
     *
     * @param <T> type of the object
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public interface ObjectSerializer<T> {

        /**
         * Deserializes byte[] to object
         *
         * @param access to bytes to deserialize
         * @return the deserialized object
         * @throws IOException error during serialization
         */
        T deserialize(final DataInput access) throws IOException;

        /**
         * Serializes object into byte[].
         *
         * @param object the obj to serialize
         * @param access to serialize to
         * @throws IOException error during serialization
         */
        void serialize(final T object, final DataOutput access) throws IOException;
    }
}
