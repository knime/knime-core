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
 *   Apr 28, 2019 (wiswedel): created
 */
package org.knime.core.node.workflow;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;

/**
 * The type of a {@link FlowVariable}, replacing {@link FlowVariable.Type}. By convention, subclasses of this type are
 * singletons. The list of these singleton subclasses is not API and may change between versions of KNIME. To create a
 * new {@link FlowVariable} of a certain type, use {@link FlowVariable#FlowVariable(String, VariableType, Object)}.
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
 * @since 4.1
 */
public abstract class VariableType<T> {

    static abstract class VariableValue<T> {

        private final VariableType<T> m_type;

        private final T m_value;

        private VariableValue(final VariableType<T> type, final T value) {
            m_type = CheckUtils.checkArgumentNotNull(type);
            m_value = value;
        }

        String asString() {
            return m_value.toString();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final VariableValue<?> other = (VariableValue<?>)obj;
            return getType().equals(other.getType()) && Objects.equals(m_value, other.m_value);
        }

        T get() {
            return m_value;
        }

        VariableType<T> getType() {
            return m_type;
        }

        @Override
        public int hashCode() {
            return 31 * m_type.hashCode() + Objects.hashCode(m_value);
        }

        void save(final NodeSettingsWO settings) {
            getType().save(this, settings);
        }

        @Override
        public String toString() {
            return asString();
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Boolean} values. The singleton instance is accessible
     * via the {@link BooleanType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanType extends VariableType<Boolean> {

        private static final class BooleanValue extends VariableValue<Boolean> {

            private BooleanValue(final Boolean i) {
                super(INSTANCE, i);
            }
        }

        /**
         * The singleton instance of the {@link BooleanType} type.
         */
        public static final BooleanType INSTANCE = new BooleanType();

        private BooleanType() {
            //singleton
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN.get();
        }

        @Override
        VariableValue<Boolean> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new BooleanType.BooleanValue(settings.getBoolean("value"));
        }

        @Override
        VariableValue<Boolean> newValue(final Boolean v) {
            return new BooleanType.BooleanValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean> v) {
            settings.addBoolean("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Boolean} values. The singleton instance is
     * accessible via the {@link BooleanArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanArrayType extends VariableType<Boolean[]> {

        private static final class BooleanArrayFlowVariableValue extends VariableValue<Boolean[]> {

            private BooleanArrayFlowVariableValue(final Boolean[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * The singleton instance of the {@link BooleanArrayType} type.
         */
        public static final BooleanArrayType INSTANCE = new BooleanArrayType();

        private BooleanArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN_ARRAY.get();
        }

        @Override
        VariableValue<Boolean[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            final boolean[] bools = settings.getBooleanArray("value");
            return new BooleanArrayType.BooleanArrayFlowVariableValue(
                IntStream.range(0, bools.length).mapToObj(i -> bools[i]).toArray(Boolean[]::new));
        }

        @Override
        VariableValue<Boolean[]> newValue(final Boolean[] v) {
            return new BooleanArrayType.BooleanArrayFlowVariableValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean[]> v) {
            settings.addBooleanArray("value", ArrayUtils.toPrimitive(v.get()));
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Double} values. The singleton instance is accessible
     * via the {@link DoubleType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleType extends VariableType<Double> {

        private static final class DoubleValue extends VariableValue<Double> {

            private DoubleValue(final Double d) {
                super(INSTANCE, d);
            }
        }

        /**
         * The singleton instance of the {@link DoubleType} type.
         */
        public static final DoubleType INSTANCE = new DoubleType();

        private DoubleType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_DOUBLE.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.DOUBLE;
        }

        @Override
        VariableValue<Double> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleType.DoubleValue(settings.getDouble("value"));
        }

        @Override
        VariableValue<Double> newValue(final Double v) {
            return new DoubleType.DoubleValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Double> v) {
            settings.addDouble("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Double} values. The singleton instance is
     * accessible via the {@link DoubleArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleArrayType extends VariableType<Double[]> {

        private static final class DoubleArrayValue extends VariableValue<Double[]> {

            private DoubleArrayValue(final Double[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * The singleton instance of the {@link DoubleArrayType} type.
         */
        public static final DoubleArrayType INSTANCE = new DoubleArrayType();

        private DoubleArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_DOUBLE_ARRAY.get();
        }

        @Override
        VariableValue<Double[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleArrayType.DoubleArrayValue(
                DoubleStream.of(settings.getDoubleArray("value")).boxed().toArray(Double[]::new));
        }

        @Override
        VariableValue<Double[]> newValue(final Double[] v) {
            return new DoubleArrayType.DoubleArrayValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Double[]> v) {
            settings.addDoubleArray("value", Stream.of(v.get()).mapToDouble(Double::doubleValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Integer} values. The singleton instance is accessible
     * via the {@link IntType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntType extends VariableType<Integer> {

        private static final class IntValue extends VariableValue<Integer> {

            private IntValue(final Integer i) {
                super(INSTANCE, i);
            }
        }

        /**
         * The singleton instance of the {@link IntType} type.
         */
        public static final IntType INSTANCE = new IntType();

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_INTEGER.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.INTEGER;
        }

        @Override
        VariableValue<Integer> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntType.IntValue(settings.getInt("value"));
        }

        @Override
        VariableValue<Integer> newValue(final Integer v) {
            return new IntType.IntValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Integer> v) {
            settings.addInt("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Integer} values. The singleton instance is
     * accessible via the {@link IntArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntArrayType extends VariableType<Integer[]> {

        private static final class IntArrayValue extends VariableValue<Integer[]> {

            private IntArrayValue(final Integer[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * The singleton instance of the {@link IntArrayType} type.
         */
        public static final IntArrayType INSTANCE = new IntArrayType();

        private IntArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_INTEGER_ARRAY.get();
        }

        @Override
        VariableValue<Integer[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntArrayType.IntArrayValue(
                IntStream.of(settings.getIntArray("value")).boxed().toArray(Integer[]::new));
        }

        @Override
        VariableValue<Integer[]> newValue(final Integer[] v) {
            return new IntArrayType.IntArrayValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Integer[]> v) {
            settings.addIntArray("value", Stream.of(v.get()).mapToInt(Integer::intValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Long} values. The singleton instance is accessible via
     * the {@link LongType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongType extends VariableType<Long> {

        private static final class LongValue extends VariableValue<Long> {

            private LongValue(final Long i) {
                super(INSTANCE, i);
            }
        }

        /**
         * The singleton instance of the {@link LongType} type.
         */
        public static final LongType INSTANCE = new LongType();

        private LongType() {
            //singleton
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG.get();
        }

        @Override
        VariableValue<Long> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongType.LongValue(settings.getLong("value"));
        }

        @Override
        VariableValue<Long> newValue(final Long v) {
            return new LongType.LongValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Long> v) {
            settings.addLong("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Long} values. The singleton instance is
     * accessible via the {@link LongArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongArrayType extends VariableType<Long[]> {

        private static final class LongArrayValue extends VariableValue<Long[]> {

            private LongArrayValue(final Long[] d) {
                super(INSTANCE, d);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * The singleton instance of the {@link LongArrayType} type.
         */
        public static final LongArrayType INSTANCE = new LongArrayType();

        private LongArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG_ARRAY.get();
        }

        @Override
        VariableValue<Long[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongArrayType.LongArrayValue(
                LongStream.of(settings.getLongArray("value")).boxed().toArray(Long[]::new));
        }

        @Override
        VariableValue<Long[]> newValue(final Long[] v) {
            return new LongArrayType.LongArrayValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<Long[]> v) {
            settings.addLongArray("value", Stream.of(v.get()).mapToLong(Long::longValue).toArray());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link String} values. The singleton instance is accessible
     * via the {@link StringType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringType extends VariableType<String> {

        private static final class StringValue extends VariableValue<String> {

            private StringValue(final String string) {
                super(INSTANCE, string);
            }

            @Override
            String asString() {
                return get();
            }
        }

        /**
         * The singleton instance of the {@link StringType} type.
         */
        public static final StringType INSTANCE = new StringType();

        private StringType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_STRING.get();
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.STRING;
        }

        @Override
        VariableValue<String> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringType.StringValue(settings.getString("value"));
        }

        @Override
        VariableValue<String> newValue(final String v) {
            return new StringType.StringValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<String> v) {
            settings.addString("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link String} values. The singleton instance is
     * accessible via the {@link StringArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringArrayType extends VariableType<String[]> {

        private static final class StringArrayValue extends VariableValue<String[]> {

            private StringArrayValue(final String[] string) {
                super(INSTANCE, string);
            }

            @Override
            String asString() {
                return Arrays.toString(get());
            }
        }

        /**
         * The singleton instance of the {@link StringArrayType} type.
         */
        public static final StringArrayType INSTANCE = new StringArrayType();

        private StringArrayType() {
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_STRING_ARRAY.get();
        }

        @Override
        VariableValue<String[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringArrayType.StringArrayValue(settings.getStringArray("value"));
        }

        @Override
        VariableValue<String[]> newValue(final String[] v) {
            return new StringArrayType.StringArrayValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<String[]> v) {
            settings.addStringArray("value", v.get());
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of CredentialsFlowVariableValue values. The singleton
     * instance is accessible via the {@link CredentialsType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class CredentialsType extends VariableType<CredentialsFlowVariableValue> {

        private static final class CredentialsValue extends VariableValue<CredentialsFlowVariableValue> {

            private CredentialsValue(final CredentialsFlowVariableValue c) {
                super(INSTANCE, c);
            }

            @Override
            String asString() {
                return "Credentials: " + get().getName();
            }
        }

        /**
         * The singleton instance of the {@link CredentialsType} type.
         */
        public static final CredentialsType INSTANCE = new CredentialsType();

        private CredentialsType() {
            // singleton
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.CREDENTIALS;
        }

        @Override
        VariableValue<CredentialsFlowVariableValue> loadValue(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return new CredentialsType.CredentialsValue(
                CredentialsFlowVariableValue.load(settings.getNodeSettings("value")));
        }

        @Override
        VariableValue<CredentialsFlowVariableValue> newValue(final CredentialsFlowVariableValue v) {
            return new CredentialsType.CredentialsValue(v);
        }

        @Override
        void saveValue(final NodeSettingsWO settings, final VariableValue<CredentialsFlowVariableValue> v) {
            v.get().save(settings.addNodeSettings("value"));
        }
    }

    private static final LazyInitializer<VariableType<?>[]> ALL_TYPES_INITER =
        new LazyInitializer<VariableType<?>[]>() {

            @Override
            protected VariableType<?>[] initialize() throws ConcurrentException {
                return new VariableType[]{ //
                    StringType.INSTANCE, //
                    StringArrayType.INSTANCE, //
                    DoubleType.INSTANCE, //
                    DoubleArrayType.INSTANCE, //
                    IntType.INSTANCE, //
                    IntArrayType.INSTANCE, //
                    LongType.INSTANCE, //
                    LongArrayType.INSTANCE, //
                    BooleanType.INSTANCE, //
                    BooleanArrayType.INSTANCE, //
                    CredentialsType.INSTANCE //
                };
            }
        };

    /**
     * Lazy-initialized array of all supported types (lazy as otherwise it causes class loading race conditions). In the
     * future this list may or may not be filled by means of an extension point.
     */
    static VariableType<?>[] getAllTypes() {
        try {
            return ALL_TYPES_INITER.get();
        } catch (ConcurrentException ex) {
            throw new InternalError(ex);
        }
    }

    static VariableValue<?> load(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String identifier = CheckUtils.checkSettingNotNull(sub.getString("class"), "'class' must not be null");
        final VariableType<?> type = Arrays.stream(getAllTypes())//
            .filter(t -> identifier.equals(t.getIdentifier()))//
            .findFirst()//
            .orElseThrow(
                () -> new InvalidSettingsException("No flow variable type for identifier/class '" + identifier + "'"));
        return type.loadValue(sub);
    }

    /**
     * Method for obtaining the icon associated with a {@link VariableType}.
     *
     * @return the type's icon
     */
    public Icon getIcon() {
        return SharedIcons.FLOWVAR_GENERAL.get();
    }

    /**
     * Method for obtaining the String that uniquely identifies a {@link VariableType}.
     *
     * @return the type's unique identifier
     */
    public String getIdentifier() {
        @SuppressWarnings("deprecation")
        final boolean isOtherType = getType().equals(FlowVariable.Type.OTHER);
        return isOtherType ? getClass().getSimpleName().replace("Type", "").toUpperCase() : getType().toString();
    }

    @SuppressWarnings("deprecation")
    FlowVariable.Type getType() {
        return FlowVariable.Type.OTHER;
    }

    abstract VariableValue<T> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException;

    abstract VariableValue<T> newValue(final T v);

    final void save(final VariableValue<T> value, final NodeSettingsWO settings) {
        settings.addString("class", getIdentifier());
        value.getType().saveValue(settings, value);
    }

    abstract void saveValue(final NodeSettingsWO settings, final VariableValue<T> v);

    @Override
    public String toString() {
        return getIdentifier();
    }

}
