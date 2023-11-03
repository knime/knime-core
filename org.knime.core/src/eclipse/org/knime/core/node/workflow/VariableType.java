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

import static org.knime.core.node.workflow.VariableTypeUtils.createFormattedException;
import static org.knime.core.node.workflow.VariableTypeUtils.createFormattedICEE;
import static org.knime.core.node.workflow.VariableTypeUtils.createTypePredicate;
import static org.knime.core.node.workflow.VariableTypeUtils.isBoolean;
import static org.knime.core.node.workflow.VariableTypeUtils.isBooleanArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isByteArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isCharArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isDouble;
import static org.knime.core.node.workflow.VariableTypeUtils.isDoubleArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isFloatArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isInt;
import static org.knime.core.node.workflow.VariableTypeUtils.isIntArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isLong;
import static org.knime.core.node.workflow.VariableTypeUtils.isLongArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isShortArray;
import static org.knime.core.node.workflow.VariableTypeUtils.isString;
import static org.knime.core.node.workflow.VariableTypeUtils.isStringArray;
import static org.knime.core.node.workflow.VariableTypeUtils.toBoolean;
import static org.knime.core.node.workflow.VariableTypeUtils.toBooleans;
import static org.knime.core.node.workflow.VariableTypeUtils.toByte;
import static org.knime.core.node.workflow.VariableTypeUtils.toChar;
import static org.knime.core.node.workflow.VariableTypeUtils.toChars;
import static org.knime.core.node.workflow.VariableTypeUtils.toDoubles;
import static org.knime.core.node.workflow.VariableTypeUtils.toFloats;
import static org.knime.core.node.workflow.VariableTypeUtils.toLongs;
import static org.knime.core.node.workflow.VariableTypeUtils.toShort;
import static org.knime.core.node.workflow.VariableTypeUtils.toStrings;
import static org.knime.core.node.workflow.VariableTypeUtils.wrap;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.swing.Icon;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.config.base.ConfigEntries;
import org.knime.core.node.config.base.ConfigPasswordEntry;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;

import com.google.common.collect.Sets;

/**
 * The type of a {@link FlowVariable}, replacing {@link FlowVariable.Type}. By convention, subclasses of this type are
 * singletons. The list of these singleton subclasses is not API and may change between versions of KNIME. To create a
 * new {@link FlowVariable} of a certain type, use {@link FlowVariable#FlowVariable(String, VariableType, Object)}.
 *
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
 * @since 4.1
 */
public abstract class VariableType<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VariableType.class);

    private static final String CFG_CLASS = "class";

    private static final String CFG_VALUE = "value";

    /**
     * AP-14067: It's not actually possible to overwrite an xpassword but we pretend to anyway for backward
     * compatibility
     */
    private static final String PASSWORD_OVERWRITE_ERROR =
        "Attempt to overwrite the password with config key '%s' failed. "
            + "It's not possible to overwrite passwords with flow variables.";

    /**
     * The value of a {@link FlowVariable}. Associates a simple value (e.g. String, Double, Integer) with a {@link VariableType}.
     *
     * @noextend This class is not intended to be subclassed by clients.
     *
     * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
     * @author Marc Bux, KNIME GmbH, Berlin, Germany
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
     * @since 4.2
     */
    protected abstract static class VariableValue<T> {

        private final VariableType<T> m_type;

        private final T m_value;

        /**
         * Constructor.
         *
         * @param type of the variable
         * @param value of the variable
         */
        protected VariableValue(final VariableType<T> type, final T value) {
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

        /**
         * @return the stored value
         */
        protected T get() {
            return m_value;
        }

        <U> U getAs(final VariableType<U> expectedType) {
            CheckUtils.checkArgument(m_type.getConvertibleTypes().contains(expectedType),
                "The type '%s' is incompatible with the type '%s'.", m_type, expectedType);
            return m_type.getAs(get(), expectedType);
        }

        void overwrite(final Config config, final String configKey)
            throws InvalidConfigEntryException {
            m_type.overwrite(m_value, config, configKey);
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
     * Abstract implementation of a {@link VariableType} that handles the {@link #canCreateFrom(Config, String)}
     * and {@link #canOverwrite(Config, String)} methods.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @param <T> The simple value type that is used by clients (e.g. String, Double, Integer)
     * @since 4.2
     */
    protected abstract static class DefaultVariableType<T> extends VariableType<T> {

        private final BiPredicate<Config, String> m_overwritableTypesPredicate;

        private final BiPredicate<Config, String> m_creationCompatibleTypesPredicate;

        /**
         * Constructor.
         *
         * @param overwritableTypesPredicate predicate that checks if a config can be overwritten with
         *            this variable type
         * @param creationCompatibleTypesPredicate predicate that checks if values of this variable type can be
         *            created from a config
         */
        protected DefaultVariableType(final BiPredicate<Config, String> overwritableTypesPredicate,
            final BiPredicate<Config, String> creationCompatibleTypesPredicate) {
            m_overwritableTypesPredicate = CheckUtils.checkArgumentNotNull(overwritableTypesPredicate);
            m_creationCompatibleTypesPredicate = CheckUtils.checkArgumentNotNull(creationCompatibleTypesPredicate);
        }

        /**
         * Convenience constructor for types where the set of overwritable and creation compatible configurations are
         * the same.
         *
         * @param overwritableAndCreationCompatiblePredicate predicate identifying overwritable AND creation compatible
         *            configurations
         */
        protected DefaultVariableType(final BiPredicate<Config, String> overwritableAndCreationCompatiblePredicate) {
            this(overwritableAndCreationCompatiblePredicate, overwritableAndCreationCompatiblePredicate);
        }


        @Override
        protected final boolean canOverwrite(final Config config, final String configKey) {
            return m_overwritableTypesPredicate.test(config, configKey);
        }

        @Override
        protected final boolean canCreateFrom(final Config config, final String configKey) {
            return m_creationCompatibleTypesPredicate.test(config, configKey);
        }
    }

    /**
     * Exception that indicates a compatibility problem of {@link VariableType VariableTypes} with a provided config. </br>
     * E.g. if a variable type is used to overwrite an incompatible config, or if a variable type is asked to create a
     * value from an incompatible config.
     *
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     * @since 4.2
     */
    public static final class InvalidConfigEntryException extends Exception {

        private static final long serialVersionUID = 1L;

        private final UnaryOperator<String> m_varNameToError;

        /**
         * Constructs an {@link InvalidConfigEntryException} with the specified detail message.
         *
         * @param msg the detail message
         */
        public InvalidConfigEntryException(final String msg) {
            this(msg, (UnaryOperator<String>)null);
        }

        /**
         * Constructs an {@link InvalidConfigEntryException} with the specified detail message and the provided {@link UnaryOperator}
         * that is used by {@link #getErrorMessageWithVariableName(String)} to create a variable name dependent error message.
         *
         * @param msg the detail message
         * @param varNameToError creates a variable name dependent error message
         */
        public InvalidConfigEntryException(final String msg, final UnaryOperator<String> varNameToError) {
            super(msg);
            m_varNameToError = varNameToError;
        }

        /**
         * Constructs an {@link InvalidConfigEntryException} with the specified detail message and a cause.
         *
         * @param msg the detail message
         * @param cause the root cause
         */
        public InvalidConfigEntryException(final String msg, final Throwable cause) {
            this(msg, cause, null);
        }
        /**
         * Constructs an {@link InvalidConfigEntryException} with the specified detail message and a cause, as well as
         * the provided {@link UnaryOperator} that is used by {@link #getErrorMessageWithVariableName(String)} to create a variable
         * name dependent error message.
         *
         * @param msg the detail message
         * @param cause the root cause
         * @param varNameToError creates a variable name dependent error message
         */
        public InvalidConfigEntryException(final String msg, final Throwable cause,
            final UnaryOperator<String> varNameToError) {
            super(msg, cause);
            m_varNameToError = varNameToError;
        }

        /**
         * Creates an error message using the provided <b>variableName</b>.
         *
         * @param variableName name of the variable that was involved in the error
         * @return an {@link Optional} containing a <b>variableName</b> dependent error message or
         *         {@link Optional#empty()} if no such error message can be generated
         */
        public Optional<String> getErrorMessageWithVariableName(final String variableName) {
            if (m_varNameToError != null) {
                return Optional.ofNullable(m_varNameToError.apply(variableName));
            } else {
                return Optional.empty();
            }
        }

    }

    private static InvalidConfigEntryException createCannotOverwriteException(final AbstractConfigEntry entry,
        final String typeName) {
        return createFormattedICEE(varName -> String
            .format("Can't overwrite the config %s (%s) with flow variable '%s' of type '%s'.", entry, entry.getType(),
                varName, typeName),
            "Can't overwrite the config %s (%s) with a %s flow variable", entry, entry.getType(), typeName);
    }

    private static InvalidConfigEntryException createCannotCreateFromException(final AbstractConfigEntry entry,
        final String typeName) {
        return new InvalidConfigEntryException(
            String.format("Can't create %s flow variable from config %s (%s).", typeName, entry, entry.getType()));
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Boolean} values. The singleton instance is accessible
     * via the {@link BooleanType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanType extends DefaultVariableType<Boolean> {

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
            super(createOverwritablePredicate(), createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xboolean));
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createTypePredicate(
                // AP-14067: We pretend to overwrite xpassword for backward compatibility
                EnumSet.of(ConfigEntries.xboolean, ConfigEntries.xstring, ConfigEntries.xtransientstring,
                    ConfigEntries.xpassword)).or(VariableTypeUtils::isBooleanArray)
                        .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN.get();
        }

        @Override
        protected VariableValue<Boolean> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new BooleanType.BooleanValue(settings.getBoolean(CFG_VALUE));
        }

        @Override
        protected VariableValue<Boolean> newValue(final Boolean v) {
            return new BooleanType.BooleanValue(v);
        }

        @Override
        protected VariableValue<Boolean> defaultValue() {
            return newValue(false);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean> v) {
            settings.addBoolean(CFG_VALUE, v.get());
        }

        @Override
        public Class<Boolean> getSimpleType() {
            return Boolean.class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(BooleanType.INSTANCE, BooleanArrayType.INSTANCE,
                StringType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Boolean value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isBoolean(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isBooleanArray(conversionTarget)) {
                return simpleType.cast(new Boolean[] {value});
            } else if (isString(conversionTarget)) {
                return simpleType.cast(value.toString());
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(new String[] {value.toString()});
            } else {
                throw createFormattedException(IllegalArgumentException::new, "Can't convert %s to %s.", this,
                    conversionTarget);
            }
        }

        @Override
        protected void overwrite(final Boolean value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            final ConfigEntries type = config.getEntry(configKey).getType();
            switch (type) {
                case xboolean:
                    config.addBoolean(configKey, value);
                    break;
                case xstring:
                    config.addString(configKey, value.toString());
                    break;
                case xtransientstring:
                    config.addTransientString(configKey, value.toString());
                    break;
                case xpassword:
                    // AP-14067: xpassword is not actually overwritable
                    // but we have to support it for backward compatibility
                    LOGGER.errorWithFormat(PASSWORD_OVERWRITE_ERROR, configKey);
                    break;
                case config:
                    if (overwriteArray(value, config, configKey)) {
                        break;
                    } else {
                        // can't overwrite this type of config -> fall through to default
                    }
                default:
                    throw createCannotOverwriteException(config.getEntry(configKey), "boolean");
            }
        }

        private static boolean overwriteArray(final Boolean value, final Config config, final String configKey) {
            if (isBooleanArray(config, configKey)) {
                config.addBooleanArray(configKey, value);
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value.toString());
            } else {
                return false;
            }
            return true;
        }

        @Override
        protected Boolean createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            if (entry.getType() == ConfigEntries.xboolean) {
                return config.getBoolean(configKey);
            } else {
                throw createCannotCreateFromException(entry, "boolean");
            }
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Boolean} values. The singleton instance is
     * accessible via the {@link BooleanArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class BooleanArrayType extends DefaultVariableType<Boolean[]> {

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
            super(createOverwritablePredicate(),
                createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return VariableTypeUtils::isBooleanArray;
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createCreationCompatiblePredicate().or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_BOOLEAN_ARRAY.get();
        }

        @Override
        protected VariableValue<Boolean[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            final boolean[] bools = settings.getBooleanArray(CFG_VALUE);
            return new BooleanArrayType.BooleanArrayFlowVariableValue(
                IntStream.range(0, bools.length).mapToObj(i -> bools[i]).toArray(Boolean[]::new));
        }

        @Override
        protected VariableValue<Boolean[]> newValue(final Boolean[] v) {
            return new BooleanArrayType.BooleanArrayFlowVariableValue(v);
        }

        @Override
        protected VariableValue<Boolean[]> defaultValue() {
            return newValue(new Boolean[] {false});
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Boolean[]> v) {
            settings.addBooleanArray(CFG_VALUE, ArrayUtils.toPrimitive(v.get()));
        }

        @Override
        public Class<Boolean[]> getSimpleType() {
            return Boolean[].class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(BooleanArrayType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        protected <U> U getAs(final Boolean[] value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isBooleanArray(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(Arrays.stream(value).map(Object::toString).toArray(String[]::new));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected void overwrite(final Boolean[] value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isBooleanArray(config, configKey)) {
                config.addBooleanArray(configKey, ArrayUtils.toPrimitive(value));
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, Arrays.stream(value).map(Object::toString).toArray(String[]::new));
            } else {
                throw createCannotOverwriteException(config, "boolean array");
            }
        }

        @Override
        protected Boolean[] createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (isBooleanArray(config, configKey)) {
                return ArrayUtils.toObject(config.getBooleanArray(configKey));
            } else {
                throw createCannotCreateFromException(config.getEntry(configKey), "boolean array");
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Double} values. The singleton instance is accessible
     * via the {@link DoubleType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleType extends DefaultVariableType<Double> {

        private static final class DoubleValue extends VariableValue<Double> {

            private DoubleValue(final Double d) {
                super(INSTANCE, d);
            }
        }

        /**
         * The singleton instance of the {@link DoubleType} type.
         */
        public static final DoubleType INSTANCE = new DoubleType();

        // The varargs are safe because the created array is neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(
                Sets.newHashSet(INSTANCE, DoubleArrayType.INSTANCE, StringType.INSTANCE, StringArrayType.INSTANCE));

        private DoubleType() {
            super(createOverwritablePredicate(), createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xfloat, ConfigEntries.xdouble));
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xfloat, ConfigEntries.xdouble, ConfigEntries.xstring,
                // AP-14067: xpassword is not actually overwritable but we have to support it for backward compatibility
                ConfigEntries.xtransientstring, ConfigEntries.xpassword)).or(VariableTypeUtils::isFloatArray)
                    .or(VariableTypeUtils::isDoubleArray).or(VariableTypeUtils::isStringArray);
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
        protected VariableValue<Double> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleType.DoubleValue(settings.getDouble(CFG_VALUE));
        }

        @Override
        protected VariableValue<Double> newValue(final Double v) {
            return new DoubleType.DoubleValue(v);
        }

        @Override
        protected VariableValue<Double> defaultValue() {
            return newValue(0.0);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Double> v) {
            settings.addDouble(CFG_VALUE, v.get());
        }

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Double value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isDouble(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isDoubleArray(conversionTarget)) {
                return simpleType.cast(new Double[] {value});
            } else if (isString(conversionTarget)) {
                return simpleType.cast(value.toString());
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(new String[] {value.toString()});
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        public Class<Double> getSimpleType() {
            return Double.class;
        }

        @Override
        protected void overwrite(final Double value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xfloat:
                    config.addFloat(configKey, value.floatValue());
                    break;
                case xdouble:
                    config.addDouble(configKey, value.doubleValue());
                    break;
                case xstring:
                    config.addString(configKey, value.toString());
                    break;
                case xtransientstring:
                    config.addTransientString(configKey, value.toString());
                    break;
                case xpassword:
                    // AP-14067: xpassword is not actually overwritable
                    // but we have to support it for backward compatibility
                    LOGGER.errorWithFormat(PASSWORD_OVERWRITE_ERROR, configKey);
                    break;
                case config:
                    if (overwriteArray(value, config, configKey)) {
                        break;
                    } else {
                        // unsupported config -> fall through to default
                    }
                default:
                    throw createCannotOverwriteException(entry, "double");
            }
        }

        private static boolean overwriteArray(final Double value, final Config config, final String configKey) {
            if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, value);
            } else if(isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, value.floatValue());
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value.toString());
            } else {
                return false;
            }
            return true;
        }

        @Override
        protected Double createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xfloat:
                    return Double.valueOf(config.getFloat(configKey));
                case xdouble:
                    return config.getDouble(configKey);
                default:
                    throw createCannotCreateFromException(entry, "double");
            }
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Double} values. The singleton instance is
     * accessible via the {@link DoubleArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class DoubleArrayType extends DefaultVariableType<Double[]> {

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
            super(createOverwritablePredicate(),
                createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return wrap(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isDoubleArray);
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return wrap(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isDoubleArray)//
                    .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_DOUBLE_ARRAY.get();
        }

        @Override
        protected VariableValue<Double[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new DoubleArrayType.DoubleArrayValue(
                DoubleStream.of(settings.getDoubleArray(CFG_VALUE)).boxed().toArray(Double[]::new));
        }

        @Override
        protected VariableValue<Double[]> newValue(final Double[] v) {
            return new DoubleArrayType.DoubleArrayValue(v);
        }

        @Override
        protected VariableValue<Double[]> defaultValue() {
            return newValue(new Double[] {0.0});
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Double[]> v) {
            settings.addDoubleArray(CFG_VALUE, Stream.of(v.get()).mapToDouble(Double::doubleValue).toArray());
        }

        @Override
        public Class<Double[]> getSimpleType() {
            return Double[].class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(DoubleArrayType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Double[] value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isDoubleArray(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(toStrings((Object[])value));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final Double[] value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, toFloats(value));
            } else if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, ArrayUtils.toPrimitive(value));
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, Arrays.stream(value).map(Object::toString).toArray(String[]::new));
            } else {
                throw createCannotOverwriteException(config, "double array");
            }
        }

        @Override
        protected Double[] createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (isDoubleArray(config, configKey)) {
                return ArrayUtils.toObject(config.getDoubleArray(configKey));
            } else if (isFloatArray(config, configKey)) {
                final float[] floats = config.getFloatArray(configKey);
                return IntStream.range(0, floats.length)//
                    .mapToObj(idx -> Double.valueOf(floats[idx]))//
                    .toArray(Double[]::new);
            } else {
                throw createCannotCreateFromException(config.getEntry(configKey), "double array");
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Integer} values. The singleton instance is accessible
     * via the {@link IntType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntType extends DefaultVariableType<Integer> {

        private static final class IntValue extends VariableValue<Integer> {

            private IntValue(final Integer i) {
                super(INSTANCE, i);
            }
        }

        /**
         * The singleton instance of the {@link IntType} type.
         */
        public static final IntType INSTANCE = new IntType();

        private IntType() {
            super(createOverwritablePredicate(), createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xbyte, ConfigEntries.xshort, ConfigEntries.xint));
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xbyte, ConfigEntries.xshort, ConfigEntries.xint,
                ConfigEntries.xlong, ConfigEntries.xfloat, ConfigEntries.xdouble, ConfigEntries.xchar,
                // AP-14067: We pretend to overwrite xpassword for backward compatibility
                ConfigEntries.xstring, ConfigEntries.xtransientstring, ConfigEntries.xpassword))//
                    .or(VariableTypeUtils::isStringArray)//
                    .or(VariableTypeUtils::isDoubleArray)//
                    .or(VariableTypeUtils::isLongArray)//
                    .or(VariableTypeUtils::isIntArray)//
                    .or(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isShortArray)//
                    .or(VariableTypeUtils::isByteArray)//
                    .or(VariableTypeUtils::isCharArray);
        }

        // Safe because the array is neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(
                Sets.newHashSet(INSTANCE, IntArrayType.INSTANCE, DoubleType.INSTANCE, DoubleArrayType.INSTANCE,
                    LongType.INSTANCE, LongArrayType.INSTANCE, StringType.INSTANCE, StringArrayType.INSTANCE));

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
        protected VariableValue<Integer> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntType.IntValue(settings.getInt(CFG_VALUE));
        }

        @Override
        protected VariableValue<Integer> newValue(final Integer v) {
            return new IntType.IntValue(v);
        }

        @Override
        protected VariableValue<Integer> defaultValue() {
            return newValue(0);
        }

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Integer value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isInt(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isIntArray(conversionTarget)) {
                return simpleType.cast(new Integer[] {value});
            } else if (isLong(conversionTarget)) {
                return simpleType.cast(value.longValue());
            } else if (isLongArray(conversionTarget)) {
                return simpleType.cast(new Long[] {value.longValue()});
            } else if (isDouble(conversionTarget)) {
                return simpleType.cast(value.doubleValue());
            } else if (isDoubleArray(conversionTarget)) {
                return simpleType.cast(new Double[] {value.doubleValue()});
            } else if (isString(conversionTarget)) {
                return simpleType.cast(value.toString());
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(new String[] {value.toString()});
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Integer> v) {
            settings.addInt(CFG_VALUE, v.get());
        }

        @Override
        public Class<Integer> getSimpleType() {
            return Integer.class;
        }

        @Override
        protected void overwrite(final Integer value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xint:
                    config.addInt(configKey, value);
                    break;
                case xbyte:
                    config.addByte(configKey, toByte(value, configKey));
                    break;
                case xshort:
                    config.addShort(configKey, toShort(value, configKey));
                    break;
                case xlong:
                    config.addLong(configKey, value.longValue());
                    break;
                case xfloat:
                    config.addFloat(configKey, value.floatValue());
                    break;
                case xdouble:
                    config.addDouble(configKey, value.doubleValue());
                    break;
                case xchar:
                    config.addChar(configKey, toChar(value, configKey));
                    break;
                case xtransientstring:
                    config.addTransientString(configKey, value.toString());
                    break;
                case xpassword:
                    // AP-14067: xpassword is not actually overwritable
                    // but we have to support it for backward compatibility
                    LOGGER.errorWithFormat(PASSWORD_OVERWRITE_ERROR, configKey);
                    break;
                case xstring:
                    config.addString(configKey, value.toString());
                    break;
                case config:
                    if (overwriteArray(value, config, configKey)) {
                        break;
                    } else {
                        // the config was not an array we can overwrite -> fall through to the default
                    }
                default:
                    throw createCannotOverwriteException(entry, "integer");
            }
        }

        private static boolean overwriteArray(final Integer value, final Config config,
            final String configKey) throws InvalidConfigEntryException {
            if (isByteArray(config, configKey)) {
                config.addByteArray(configKey, value.byteValue());
            } else if (isShortArray(config, configKey)) {
                config.addShortArray(configKey, value.shortValue());
            } else if (isIntArray(config, configKey)) {
                config.addIntArray(configKey, value);
            } else if (isLongArray(config, configKey)) {
                config.addLongArray(configKey, value.longValue());
            } else if (isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, value.floatValue());
            } else if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, value.doubleValue());
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value.toString());
            } else if (isCharArray(config, configKey)) {
                config.addCharArray(configKey, toChar(value, configKey));
            } else {
                return false;
            }
            return true;
        }

        @Override
        protected Integer createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xbyte:
                    return (int)config.getByte(configKey);
                case xshort:
                    return (int)config.getShort(configKey);
                case xint:
                    return config.getInt(configKey);
                default:
                    throw createCannotCreateFromException(entry, "integer");
            }
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Integer} values. The singleton instance is
     * accessible via the {@link IntArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class IntArrayType extends DefaultVariableType<Integer[]> {

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
            super(createOverwritablePredicate(), createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return wrap(VariableTypeUtils::isShortArray)//
                    .or(VariableTypeUtils::isByteArray)
                    .or(VariableTypeUtils::isIntArray);
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return wrap(VariableTypeUtils::isByteArray)//
                    .or(VariableTypeUtils::isShortArray)//
                    .or(VariableTypeUtils::isIntArray)//
                    .or(VariableTypeUtils::isLongArray)//
                    .or(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isDoubleArray)//
                    .or(VariableTypeUtils::isCharArray)//
                    .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_INTEGER_ARRAY.get();
        }

        @Override
        protected VariableValue<Integer[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new IntArrayType.IntArrayValue(
                IntStream.of(settings.getIntArray(CFG_VALUE)).boxed().toArray(Integer[]::new));
        }

        @Override
        protected VariableValue<Integer[]> newValue(final Integer[] v) {
            return new IntArrayType.IntArrayValue(v);
        }

        @Override
        protected VariableValue<Integer[]> defaultValue() {
            return newValue(new Integer[] {0});
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Integer[]> v) {
            settings.addIntArray(CFG_VALUE, Stream.of(v.get()).mapToInt(Integer::intValue).toArray());
        }

        @Override
        public Class<Integer[]> getSimpleType() {
            return Integer[].class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(IntArrayType.INSTANCE, LongArrayType.INSTANCE,
                DoubleArrayType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Integer[] value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isIntArray(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isLongArray(conversionTarget)) {
                return simpleType.cast(toLongs(value));
            } else if (isDoubleArray(conversionTarget)) {
                return simpleType
                    .cast(toDoubles(value));
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(toStrings((Object[])value));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final Integer[] value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isByteArray(config, configKey)) {
                final byte[] array = new byte[value.length];
                for (int i = 0; i < array.length; i++) {
                    array[i] = toByte(value[i], configKey);
                }
                config.addByteArray(configKey, array);
            } else if (isShortArray(config, configKey)) {
                final short[] array = new short[value.length];
                for (int i = 0; i < array.length; i++) {
                    array[i] = toShort(value[i], configKey);
                }
                config.addShortArray(configKey, array);
            } else if (isIntArray(config, configKey)) {
                config.addIntArray(configKey, ArrayUtils.toPrimitive(value));
            } else if (isLongArray(config, configKey)) {
                config.addLongArray(configKey, Arrays.stream(value).mapToLong(Integer::longValue).toArray());
            } else if (isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, toFloats(value));
            } else if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, Arrays.stream(value).mapToDouble(Integer::doubleValue).toArray());
            } else if (isCharArray(config, configKey)) {
                config.addCharArray(configKey, toChars(value, configKey));
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, Arrays.stream(value).map(Object::toString).toArray(String[]::new));
            } else {
                throw createCannotOverwriteException(config.getEntry(configKey), "integer array");
            }
        }

        @Override
        protected Integer[] createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (isByteArray(config, configKey)) {
                final byte[] bytes = config.getByteArray(configKey);
                return IntStream.range(0, bytes.length)//
                    .mapToObj(i -> Integer.valueOf(bytes[i]))//
                    .toArray(Integer[]::new);
            } else if (isShortArray(config, configKey)) {
                final short[] shorts = config.getShortArray(configKey);
                return IntStream.range(0, shorts.length)//
                    .mapToObj(i -> Integer.valueOf(shorts[i]))//
                    .toArray(Integer[]::new);
            } else if (isIntArray(config, configKey)) {
                return ArrayUtils.toObject(config.getIntArray(configKey));
            } else {
                throw createCannotCreateFromException(config.getEntry(configKey), configKey);
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link Long} values. The singleton instance is accessible via
     * the {@link LongType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongType extends DefaultVariableType<Long> {

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
            super(
                createOverwritablePredicate(),
                createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xlong));
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xlong, ConfigEntries.xfloat, ConfigEntries.xdouble,
                // AP-14067: xpassword is not actually overwritable but we have to support it for backward compatibility
                ConfigEntries.xstring, ConfigEntries.xtransientstring, ConfigEntries.xpassword))//
                    .or(VariableTypeUtils::isLongArray)//
                    .or(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isDoubleArray)//
                    .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG.get();
        }

        @Override
        protected VariableValue<Long> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongType.LongValue(settings.getLong(CFG_VALUE));
        }

        @Override
        protected VariableValue<Long> newValue(final Long v) {
            return new LongType.LongValue(v);
        }

        @Override
        protected VariableValue<Long> defaultValue() {
            return newValue(0L);
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Long> v) {
            settings.addLong(CFG_VALUE, v.get());
        }

        @Override
        public Class<Long> getSimpleType() {
            return Long.class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(LongType.INSTANCE, LongArrayType.INSTANCE, DoubleType.INSTANCE,
                DoubleArrayType.INSTANCE, StringType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Long value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isLong(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isLongArray(conversionTarget)) {
                return simpleType.cast(new Long[] {value});
            } else if (isDouble(conversionTarget)) {
                return simpleType.cast(value.doubleValue());
            } else if (isDoubleArray(conversionTarget)) {
                return simpleType.cast(toDoubles(value));
            } else if (isString(conversionTarget)) {
                return simpleType.cast(value.toString());
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(toStrings(value));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final Long value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xlong:
                    config.addLong(configKey, value);
                    break;
                case xfloat:
                    config.addFloat(configKey, value.floatValue());
                    break;
                case xdouble:
                    config.addDouble(configKey, value.doubleValue());
                    break;
                case xstring:
                    config.addString(configKey, value.toString());
                    break;
                case xtransientstring:
                    config.addTransientString(configKey, value.toString());
                    break;
                case xpassword:
                    // AP-14067: xpassword is not actually overwritable
                    // but we have to support it for backward compatibility
                    LOGGER.errorWithFormat(PASSWORD_OVERWRITE_ERROR, configKey);
                    break;
                case config:
                    if (overwriteArray(value, config, configKey)) {
                        break;
                    } else {
                        // the config is not a compatible array -> fall through to default
                    }
                default:
                    throw createCannotOverwriteException(entry, "long");
            }
        }

        private static boolean overwriteArray(final Long value, final Config config, final String configKey) {
            if (isLongArray(config, configKey)) {
                config.addLongArray(configKey, value);
            } else if (isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, value.floatValue());
            } else if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, value.doubleValue());
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value.toString());
            } else {
                return false;
            }
            return true;
        }

        @Override
        protected Long createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            if (entry.getType() == ConfigEntries.xlong) {
                return config.getLong(configKey);
            } else {
                throw createCannotCreateFromException(entry, "long");
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link Long} values. The singleton instance is
     * accessible via the {@link LongArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class LongArrayType extends DefaultVariableType<Long[]> {

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
            super(createOverwritablePredicate(), VariableTypeUtils::isLongArray);
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return wrap((BiPredicate<Config, String>)VariableTypeUtils::isLongArray)
                    .or(VariableTypeUtils::isFloatArray)//
                    .or(VariableTypeUtils::isDoubleArray)//
                    .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_LONG_ARRAY.get();
        }

        @Override
        protected VariableValue<Long[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new LongArrayType.LongArrayValue(
                LongStream.of(settings.getLongArray(CFG_VALUE)).boxed().toArray(Long[]::new));
        }

        @Override
        protected VariableValue<Long[]> newValue(final Long[] v) {
            return new LongArrayType.LongArrayValue(v);
        }

        @Override
        protected VariableValue<Long[]> defaultValue() {
            return newValue(new Long[] {0L});
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<Long[]> v) {
            settings.addLongArray(CFG_VALUE, Stream.of(v.get()).mapToLong(Long::longValue).toArray());
        }

        @Override
        public Class<Long[]> getSimpleType() {
            return Long[].class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE = Collections.unmodifiableSet(
            Sets.newHashSet(LongArrayType.INSTANCE, DoubleArrayType.INSTANCE, StringArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final Long[] value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isLongArray(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isDoubleArray(conversionTarget)) {
                return simpleType.cast(toDoubles(value));
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(toStrings((Object[])value));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final Long[] value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isLongArray(config, configKey)) {
                config.addLongArray(configKey, ArrayUtils.toPrimitive(value));
            } else if (isFloatArray(config, configKey)) {
                config.addFloatArray(configKey, toFloats(value));
            } else if (isDoubleArray(config, configKey)) {
                config.addDoubleArray(configKey, Arrays.stream(value).mapToDouble(Long::doubleValue).toArray());
            } else if (isCharArray(config, configKey)) {
                config.addCharArray(configKey, toChars(value, configKey));
            } else if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, Arrays.stream(value).map(Object::toString).toArray(String[]::new));
            } else {
                throw createCannotOverwriteException(config.getEntry(configKey), "long array");
            }
        }

        @Override
        protected Long[] createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (isLongArray(config, configKey)) {
                return ArrayUtils.toObject(config.getLongArray(configKey));
            } else {
                throw createCannotCreateFromException(config.getEntry(configKey), "long array");
            }
        }
    }

    /**
     * Singleton type of {@link FlowVariable} for handling {@link String} values. The singleton instance is accessible
     * via the {@link StringType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringType extends DefaultVariableType<String> {

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
            super(createOverwritablePredicate(), createCreationCompatiblePredicate());
        }

        private static BiPredicate<Config, String> createCreationCompatiblePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xchar, ConfigEntries.xstring, ConfigEntries.xpassword));
        }

        private static BiPredicate<Config, String> createOverwritablePredicate() {
            return createTypePredicate(EnumSet.of(ConfigEntries.xboolean, ConfigEntries.xchar, ConfigEntries.xstring,
                // AP-14067: We only pretend to overwrite xpassword for backward compatibility
                ConfigEntries.xtransientstring, ConfigEntries.xpassword))//
                    .or(VariableTypeUtils::isBooleanArray)//
                    .or(VariableTypeUtils::isCharArray)//
                    .or(VariableTypeUtils::isStringArray);
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
        protected VariableValue<String> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringType.StringValue(settings.getString(CFG_VALUE));
        }

        @Override
        protected VariableValue<String> newValue(final String v) {
            return new StringType.StringValue(v);
        }

        @Override
        protected VariableValue<String> defaultValue() {
            return newValue("");
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<String> v) {
            settings.addString(CFG_VALUE, v.get());
        }

        @Override
        public Class<String> getSimpleType() {
            return String.class;
        }

        // the varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(StringType.INSTANCE, StringArrayType.INSTANCE,
                BooleanType.INSTANCE, BooleanArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final String value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isString(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isStringArray(conversionTarget)) {
                return simpleType.cast(new String[] {value});
            } else if (isBoolean(conversionTarget)) {
                return simpleType.cast(toBoolean(value));
            } else if (isBooleanArray(conversionTarget)) {
                return simpleType.cast(new Boolean[] {toBoolean(value)});
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final String value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xboolean:
                    config.addBoolean(configKey, toBoolean(value, configKey));
                    break;
                case xstring:
                    config.addString(configKey, value);
                    break;
                case xtransientstring:
                    config.addTransientString(configKey, value);
                    break;
                case xchar:
                    config.addChar(configKey, toChar(value, configKey));
                    break;
                case xpassword:
                    // AP-14067: xpassword is not actually overwritable
                    // but we have to support it for backward compatibility
                    LOGGER.errorWithFormat(PASSWORD_OVERWRITE_ERROR, configKey);
                    break;
                case config:
                    if (overwriteArray(value, config, configKey)) {
                        break;
                    } else {
                        // the config is not a compatible array type -> fall through to default
                    }
                default:
                    throw createCannotOverwriteException(entry, "string");
            }
        }

        private static boolean overwriteArray(final String value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value);
            } else if (isCharArray(config, configKey)) {
                config.addCharArray(configKey, toChar(value, configKey));
            } else if (isBooleanArray(config, configKey)) {
                config.addBooleanArray(configKey, toBoolean(value, configKey));
            }else {
                return false;
            }
            return true;
        }

        @Override
        protected String createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            final AbstractConfigEntry entry = config.getEntry(configKey);
            switch (entry.getType()) {
                case xstring:
                    return config.getString(configKey);
                case xchar:
                    return Character.toString(config.getChar(configKey));
                case xpassword:
                    return ((ConfigPasswordEntry)entry).getPassword();
                default:
                    throw createCannotCreateFromException(entry, "string");
            }
        }

    }

    /**
     * Singleton type of {@link FlowVariable} for handling arrays of {@link String} values. The singleton instance is
     * accessible via the {@link StringArrayType#INSTANCE} field.
     *
     * @since 4.1
     */
    public static final class StringArrayType extends DefaultVariableType<String[]> {

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
            super(createOverwritableConfigs(), createCreationCompatibleConfigs());
        }

        private static BiPredicate<Config, String> createOverwritableConfigs() {
            return wrap(VariableTypeUtils::isCharArray)//
                .or(VariableTypeUtils::isStringArray)//
                .or(VariableTypeUtils::isBooleanArray);
        }

        private static BiPredicate<Config, String> createCreationCompatibleConfigs() {
            return wrap(VariableTypeUtils::isCharArray)//
                .or(VariableTypeUtils::isStringArray);
        }

        @Override
        public Icon getIcon() {
            return SharedIcons.FLOWVAR_STRING_ARRAY.get();
        }

        @Override
        protected VariableValue<String[]> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new StringArrayType.StringArrayValue(settings.getStringArray(CFG_VALUE));
        }

        @Override
        protected VariableValue<String[]> newValue(final String[] v) {
            return new StringArrayType.StringArrayValue(v);
        }

        @Override
        protected VariableValue<String[]> defaultValue() {
            return newValue(new String[] {""});
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<String[]> v) {
            settings.addStringArray(CFG_VALUE, v.get());
        }

        @Override
        public Class<String[]> getSimpleType() {
            return String[].class;
        }

        // The varargs are safe because they are neither modified nor exposed
        @SuppressWarnings("unchecked")
        private static final Set<VariableType<?>> CONVERTIBLE =
            Collections.unmodifiableSet(Sets.newHashSet(StringArrayType.INSTANCE, BooleanArrayType.INSTANCE));

        @Override
        public Set<VariableType<?>> getConvertibleTypes() {
            return CONVERTIBLE;
        }

        @Override
        protected <U> U getAs(final String[] value, final VariableType<U> conversionTarget) {
            final Class<U> simpleType = conversionTarget.getSimpleType();
            if (isStringArray(conversionTarget)) {
                return simpleType.cast(value);
            } else if (isBooleanArray(conversionTarget)) {
                return simpleType.cast(toBooleans(value));
            } else {
                throw createNotConvertibleException(this, conversionTarget);
            }
        }

        @Override
        protected void overwrite(final String[] value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (isStringArray(config, configKey)) {
                config.addStringArray(configKey, value);
            } else if (isBooleanArray(config, configKey)) {
                config.addBooleanArray(configKey, toBooleans(value, configKey));
            } else if (isCharArray(config, configKey)) {
                config.addCharArray(configKey, toChars(value, configKey));
            } else {
                throw createCannotOverwriteException(config.getEntry(configKey), "string array");
            }
        }

        @Override
        protected String[] createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (isStringArray(config, configKey)) {
                return config.getStringArray(configKey);
            } else if (isCharArray(config, configKey)) {
                final char[] chars = config.getCharArray(configKey);
                return IntStream.range(0, chars.length)//
                    .mapToObj(idx -> Character.toString(chars[idx]))//
                    .toArray(String[]::new);
            } else {
                throw createCannotCreateFromException(config.getEntry(configKey), "string array");
            }
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
        }

        @SuppressWarnings("deprecation")
        @Override
        FlowVariable.Type getType() {
            return FlowVariable.Type.CREDENTIALS;
        }

        @Override
        protected VariableValue<CredentialsFlowVariableValue> loadValue(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return new CredentialsType.CredentialsValue(
                CredentialsFlowVariableValue.load(settings.getNodeSettings(CFG_VALUE)));
        }

        @Override
        protected VariableValue<CredentialsFlowVariableValue> newValue(final CredentialsFlowVariableValue v) {
            return new CredentialsType.CredentialsValue(v);
        }

        @Override
        protected VariableValue<CredentialsFlowVariableValue> defaultValue() {
            throw new NotImplementedException("The CredentialsType does not support the creation of default values.");
        }

        @Override
        protected void saveValue(final NodeSettingsWO settings, final VariableValue<CredentialsFlowVariableValue> v) {
            v.get().save(settings.addNodeSettings(CFG_VALUE));
        }

        @Override
        public Class<CredentialsFlowVariableValue> getSimpleType() {
            return CredentialsFlowVariableValue.class;
        }

        private static boolean isCredentials(final Config config, final String configKey) {
            return (config.getEntry(configKey) instanceof Config subConfig)
                && subConfig.containsKey(CredentialsFlowVariableValue.CFG_NAME)
                && subConfig.containsKey(CredentialsFlowVariableValue.CFG_LOGIN)
                && subConfig.containsKey(CredentialsFlowVariableValue.CFG_PWD)
                && subConfig.containsKey(CredentialsFlowVariableValue.CFG_SECOND_FACTOR);
        }

        @Override
        protected boolean canOverwrite(final Config config, final String configKey) {
            return isCredentials(config, configKey);
        }

        @Override
        protected void overwrite(final CredentialsFlowVariableValue value, final Config config, final String configKey)
            throws InvalidConfigEntryException {
            if (!canOverwrite(config, configKey)) {
                throw new InvalidConfigEntryException("The provided config does not correspond to a credential.",
                    v -> String.format(
                        "The variable '%s' can't overwrite the setting '%s' because it is not a credential.",
                        v, config.getEntry(configKey)));
            }
            value.store(config.addConfig(configKey), true);
        }

        @Override
        protected boolean canCreateFrom(final Config config, final String configKey) {
            return isCredentials(config, configKey);
        }

        @Override
        protected CredentialsFlowVariableValue createFrom(final Config config, final String configKey)
            throws InvalidSettingsException, InvalidConfigEntryException {
            if (!canCreateFrom(config, configKey)) {
                throw new InvalidConfigEntryException("The provided config does not correspond to a credential.",
                    v -> String.format("The settings stored in '%s' can't be exposed as flow variable '%s'.",
                        config.getEntry(configKey), v));
            }
            return CredentialsFlowVariableValue.load(config.getConfig(configKey));
        }
    }

    /**
     * Lazy-initialized array of all supported types (lazy as otherwise it causes class loading race conditions). In the
     * future this list may or may not be filled by means of an extension point.
     */
    static VariableType<?>[] getAllTypes() {
        return VariableTypeRegistry.getInstance().getAllTypes();
    }

    static VariableValue<?> load(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String identifier = CheckUtils.checkSettingNotNull(sub.getString(CFG_CLASS), "'class' must not be null");
        final VariableType<?> type = Arrays.stream(getAllTypes())//
            .filter(t -> identifier.equals(t.getIdentifier()))//
            .findFirst()//
            .orElseThrow(
                () -> new InvalidSettingsException(
                    String.format("No flow variable type for identifier/class '%s'", identifier)));
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
        final boolean isOtherType = getType() == FlowVariable.Type.OTHER;
        return isOtherType ? getClass().getSimpleName().replace("Type", "").toUpperCase() : getType().toString();
    }

    /**
     * Checks if this type can be converted to {@link VariableType type}, i.e. if getAs(VariableValue<T>,
     * VariableType<U>) can convert the T of VariableValue<T> to the U of VariableType<U>.
     *
     * @param type to check for convertibility
     * @return <code>true</code> if <b>type</b> is compatible with this type
     * @since 4.2
     */
    public final boolean isConvertible(final VariableType<?> type) {
        return getConvertibleTypes().contains(type);
    }

    /**
     * Returns the set of {@link VariableType VariableTypes} this type can be converted to, i.e. all types for which
     * {@link VariableType#getAs(Object, VariableType)} is properly defined.
     *
     * @return the set of convertible {@link VariableType VariableTypes}
     * @since 4.2
     */
    public Set<VariableType<?>> getConvertibleTypes() {
        return Collections.singleton(this);
    }

    /**
     * Converts the value stored in {@link VariableValue value} to the type of {@link VariableType compatibleType}.
     *
     * @param value to convert
     * @param conversionTarget to convert <b>value</b> to
     * @return the converted value stored in <b>value</b>
     * @since 4.2
     */
    protected <U> U getAs(final T value, final VariableType<U> conversionTarget) {
        CheckUtils.checkArgumentNotNull(value);
        CheckUtils.checkArgumentNotNull(conversionTarget);
        CheckUtils.checkArgument(this.equals(conversionTarget), "The type '%s' is incompatible with the type '%s'.",
            conversionTarget, this);
        return conversionTarget.getSimpleType().cast(value);
    }

    /**
     * Creates the exception to be thrown if a user attempts to convert one flow variable type to a type it is not
     * convertible to.
     *
     * @param from the type to convert to {@link VariableType to}
     * @param to the type to convert to
     * @return the not convertible exception
     * @since 4.2
     */
    protected static IllegalArgumentException createNotConvertibleException(final VariableType<?> from,
        final VariableType<?> to) {
        return new IllegalArgumentException(String.format(
            "Flow variables of the type '%s' can't be converted to flow variables of the type '%s'.", from, to));
    }



    @SuppressWarnings("deprecation")
    FlowVariable.Type getType() {
        return FlowVariable.Type.OTHER;
    }

    /**
     * Returns the class object of the simple type represented by this {@link VariableType}.
     *
     * @return the class object of the simple type represented by this {@link VariableType}
     * @since 4.2
     */
    public abstract Class<T> getSimpleType();

    /**
     * Loads a {@link VariableValue} corresponding to this type from the provided settings.
     *
     * @param settings the settings to load the {@link VariableValue} from
     * @return the loaded {@link VariableValue}
     * @throws InvalidSettingsException if the value can't be loaded from the provided settings
     * @since 4.2
     */
    protected abstract VariableValue<T> loadValue(final NodeSettingsRO settings) throws InvalidSettingsException;

    /**
     * Saves the provided {@link VariableValue v} into {@link NodeSettingsWO settings}.
     *
     * @param settings the settings to save {@link VariableValue v} to
     * @param v the {@link VariableValue} to save
     * @since 4.2
     */
    protected abstract void saveValue(final NodeSettingsWO settings, final VariableValue<T> v);

    /**
     * Creates a {@link VariableValue} from the provided object <b>v</b>.
     *
     * @param v the object the created {@link VariableValue} holds
     * @return a new {@link VariableValue} holding <b>v</b>
     * @since 4.2
     */
    protected abstract VariableValue<T> newValue(final T v);

    /**
     * Creates a default {@link VariableValue}.</br>
     * E.g. in case of IntType, this creates a VariableValue that holds a 0.
     *
     * @return a default {@link VariableValue}
     * @since 4.2
     */
    protected abstract VariableValue<T> defaultValue();

    /**
     * Checks if the entry with key <b>configKey</b> in {@link Config config} can be overwritten by this variable.</br>
     * That is if this method returns {@code true} then it can be expected that a call of
     * {@link #overwrite(Object, Config, String)} for the same {@link Config config} and <b>configKey</b> will
     * succeed.
     *
     * @param config the {@link Config} in which the entry with key <b>configKey</b> should be overwritten
     * @param configKey the key of the entry that should be overwritten
     * @return {@code true} if the entry identified by the arguments can be overwritten with this type of variable
     * @since 4.2
     */
    protected abstract boolean canOverwrite(final Config config, final String configKey);

    /**
     * Overwrites the entry in {@link Config config} identified by <b>configKey</b> with the provided <b>value</b>.
     *
     * @param value to overwrite the config entry with
     * @param config the {@link Config} in which to overwrite the entry with key <b>configKey</b>
     * @param configKey the key that identifies the entry in {@link Config config} that should be overwritten
     * @throws InvalidConfigEntryException if the entry can't be overwritten (either because this entry type is not
     *             supported or <b>value</b> is incompatible
     * @since 4.2
     */
    protected abstract void overwrite(final T value, final Config config, final String configKey)
        throws InvalidConfigEntryException;

    /**
     * Checks if the entry with key <b>configKey</b> in {@link Config config} can be used to create a variable of this
     * type.</br>
     * That is if this method returns {@code true} then it can be expected that a call of
     * {@link #createFrom(Config, String)} for the same {@link Config config} and <b>configKey</b> will succeed.
     *
     * @param config the {@link Config} in which the entry with key <b>configKey</b> should be used to create a variable
     *            of this type
     * @param configKey the key of the entry that should be used to create a variable of this type
     * @return {@code true} if the entry identified by the arguments can be used to create a variable of this type
     * @since 4.2
     */
    protected abstract boolean canCreateFrom(final Config config, final String configKey);

    /**
     * Creates a {@link VariableValue} from the config entry stored in {@link Config config} with key <b>configKey</b>.
     *
     * @param config the {@link Config} whose entry with key <b>configKey</b> should be used to create the variable
     * @param configKey the key that identifies the entry to create the variable from
     * @return the {@link VariableValue} corresponding to the config entry
     * @throws InvalidSettingsException if accessing the settings fails
     * @throws InvalidConfigEntryException if the settings are not compatible with the variable type
     * @since 4.2
     */
    protected abstract T createFrom(final Config config, final String configKey)
        throws InvalidSettingsException, InvalidConfigEntryException;

    @Override
    public String toString() {
        return getIdentifier();
    }

    final void save(final VariableValue<T> value, final NodeSettingsWO settings) {
        settings.addString(CFG_CLASS, getIdentifier());
        value.getType().saveValue(settings, value);
    }


}
