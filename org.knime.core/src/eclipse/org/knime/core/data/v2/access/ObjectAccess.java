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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZonedDateTime;

/**
 * Definition of {@link ObjectAccess} to read and write arbitrary objects via serialization / deserialization into
 * byte[]. The underlying data table back-end is responsible for caching.
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
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for arbitrary object that can be
     * serialized with a given {@link ObjectSerializer}.
     *
     * @param <T> type of object
     *
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class GenericObjectAccessSpec<T>
        implements AccessSpec<ObjectReadAccess<T>, ObjectWriteAccess<T>> {

        private final ObjectSerializer<T> m_serializer;

        /**
         * Create a spec for {@link ObjectReadAccess} and {@link ObjectWriteAccess} with the given serializer.
         *
         * @param serializer the serializer to write the object to a {@link DataOutput} and read it from a
         *            {@link DataInput}.
         */
        public GenericObjectAccessSpec(final ObjectSerializer<T> serializer) {
            m_serializer = serializer;
        }

        /**
         * @return serializer to read/write objects.
         */
        public ObjectSerializer<T> getSerializer() {
            return m_serializer;
        }

        @Override
        public <V> V accept(final AccessSpecMapper<V> mapper) {
            return mapper.visit(this);
        }

        /**
         * TODO we can add more 'dict encoding' options later in a DictEncodingConfig obj.
         *
         * @return <source>true</source> if dict encoded
         */
        public boolean isDictEncoded() {
            return false;
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link String Strings}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class StringAccessSpec
        implements AccessSpec<ObjectReadAccess<String>, ObjectWriteAccess<String>> {

        /** The final stateless instance of {@link StringAccessSpec} */
        public static final StringAccessSpec INSTANCE = new StringAccessSpec();

        private StringAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }

        // TODO(bejamin) add dict encoding
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link LocalDate}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class LocalDateAccessSpec
        implements AccessSpec<ObjectReadAccess<LocalDate>, ObjectWriteAccess<LocalDate>> {

        /** The final stateless instance of {@link LocalDateAccessSpec} */
        public static final LocalDateAccessSpec INSTANCE = new LocalDateAccessSpec();

        private LocalDateAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link LocalDateTime}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class LocalDateTimeAccessSpec
        implements AccessSpec<ObjectReadAccess<LocalDateTime>, ObjectWriteAccess<LocalDateTime>> {

        /** The final stateless instance of {@link LocalDateTimeAccessSpec} */
        public static final LocalDateTimeAccessSpec INSTANCE = new LocalDateTimeAccessSpec();

        private LocalDateTimeAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link LocalTime}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class LocalTimeAccessSpec
        implements AccessSpec<ObjectReadAccess<LocalTime>, ObjectWriteAccess<LocalTime>> {

        /** The final stateless instance of {@link LocalTimeAccessSpec} */
        public static final LocalTimeAccessSpec INSTANCE = new LocalTimeAccessSpec();

        private LocalTimeAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link Duration}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class DurationAccessSpec
        implements AccessSpec<ObjectReadAccess<Duration>, ObjectWriteAccess<Duration>> {

        /** The final stateless instance of {@link DurationAccessSpec} */
        public static final DurationAccessSpec INSTANCE = new DurationAccessSpec();

        private DurationAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link Period}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class PeriodAccessSpec
        implements AccessSpec<ObjectReadAccess<Period>, ObjectWriteAccess<Period>> {

        /** The final stateless instance of {@link PeriodAccessSpec} */
        public static final PeriodAccessSpec INSTANCE = new PeriodAccessSpec();

        private PeriodAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
        }
    }

    /**
     * Specification of {@link ObjectReadAccess} and {@link ObjectWriteAccess} for {@link ZonedDateTime}.
     *
     * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
     * @since 4.3
     */
    public static final class ZonedDateTimeAccessSpec
        implements AccessSpec<ObjectReadAccess<ZonedDateTime>, ObjectWriteAccess<ZonedDateTime>> {

        /** The final stateless instance of {@link ZonedDateTimeAccessSpec} */
        public static final ZonedDateTimeAccessSpec INSTANCE = new ZonedDateTimeAccessSpec();

        private ZonedDateTimeAccessSpec() {
        }

        @Override
        public <T> T accept(final AccessSpecMapper<T> mapper) {
            return mapper.visit(this);
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
