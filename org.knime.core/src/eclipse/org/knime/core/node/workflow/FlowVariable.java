/*
 * ------------------------------------------------------------------------
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
 *   Mar 15, 2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsStore.CredentialsFlowVariableValue;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableType.VariableValue;
import org.knime.core.util.CoreConstants;

/**
 * FlowVariable holding local variables of basic types which can be passed along connections in a workflow.
 *
 * @author M. Berthold, University of Konstanz
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public final class FlowVariable extends FlowObject {

    /** reserved prefix for global flow variables.
     * @deprecated Use {@link Scope#getPrefix()} of enum constant
     * {@link Scope#Global} instead. */
    @Deprecated
    public static final String GLOBAL_CONST_ID = "knime";

    /**
     * The type of a variable.
     *
     * @deprecated use {@link VariableType} instead
     */
    @Deprecated
    public enum Type {
        /** double type. */
        DOUBLE,
        /** int type. */
        INTEGER,
        /** String type. */
        STRING,
        /** Credentials, currently filtered from {@link FlowObjectStack#getAvailableFlowVariables()}.
         * @since 3.1 */
        CREDENTIALS,
        /** All other (new) types, which were added after this enum got deprecated (4.1). Filtered from
         * {@link FlowObjectStack#getAvailableFlowVariables()}.
         * @since 4.1 */
        OTHER;
    }

    /** Scope of variable. */
    @SuppressWarnings("java:S115") // naming
    public enum Scope {
        /** (VM-)Global constant, such as workspace location. */
        Global("knime"),
        /** Ordinary workflow or flow variable. */
        Flow(""),
        /** Node local flow variable, e.g. node drop location. */
        Local("knime.node"),
        /** Hides any variable definition defined upstream until up to the next opening context (e.g. loop start).
         * @since 5.3*/
        Hide("knime.hide");

        private final String m_prefix;

        /** Create scope with given prefix. */
        Scope(final String prefix) {
            m_prefix = prefix;
        }

        /** @return the prefix */
        public String getPrefix() {
            return m_prefix;
        }

        /**
         * Throws {@link IllegalFlowVariableNameException} if the name of the variable is inconsistent for this scope.
         *
         * @param name Name to test
         * @throws IllegalFlowVariableNameException If name is invalid
         */
        public void verifyName(final String name) {
            if (m_prefix.length() > 0 && !name.startsWith(m_prefix)) {
                throw new IllegalFlowVariableNameException(
                    "Invalid " + name() + " variable, name must start with \"" + m_prefix + "\": " + name);
            }
            if (this == Flow) {
                final Optional<String> reservedPrefix = getReservedPrefix(name);
                if (reservedPrefix.isPresent()) {
                    throw new IllegalFlowVariableNameException(
                        String.format(
                            "The flow variable '%s' has a reserved prefix ('%s'). Please change the name.", name,
                            reservedPrefix.get()));
                }
            }
        }

        private static Optional<String> getReservedPrefix(final String name) {
            if (name.startsWith(Local.m_prefix)) {
                return Optional.of(Local.m_prefix);
            } else if (name.startsWith(Hide.m_prefix)) {
                return Optional.of(Hide.m_prefix);
            } else if (name.startsWith(Global.m_prefix)) {
                return Optional.of(Global.m_prefix);
            } else {
                return Optional.empty();
            }
        }
    }

    private final Scope m_scope;
    private final String m_name;
    private final VariableValue<?> m_value;

    private FlowVariable(final String name, final VariableValue<?> value, final Scope scope) {
        m_name = CheckUtils.checkArgumentNotNull(name, "Name must not be null");
        if (StringUtils.isBlank(name)) {
            throw new IllegalFlowObjectStackException("Invalid (empty) flow variable name");
        }

        m_value = CheckUtils.checkArgumentNotNull(value, "Value must not be null");

        CheckUtils.checkArgumentNotNull(scope, "Scope must not be null");
        scope.verifyName(name);
        m_scope = scope;
    }

    private <T> FlowVariable(final String name, final VariableType<T> type, final T value, final Scope scope) {
        this(name, CheckUtils.checkArgumentNotNull(type, "Type must not be null").newValue(value), scope);
    }

    /**
     * Creates a new {@link FlowVariable} that holds the provided {@link VariableValue value}
     *
     * Needed for {@link VariableTypeRegistry#createFromConfig(String, org.knime.core.node.config.Config)}.
     * @param name the name of the variable
     * @param value the {@link VariableValue} of the variable
     */
    FlowVariable(final String name, final VariableValue<?> value) {
        this(name, value, Scope.Flow);
    }

    /**
     * Creates a FlowVariable using {@link VariableType#defaultValue()}.
     *
     * @param name the name of the variable
     * @param type the type of the variable
     *
     * @since 4.2
     */
    public <T> FlowVariable(final String name, final VariableType<T> type) {
        this(name, CheckUtils.checkArgumentNotNull(type, "Type must not be null").defaultValue(), Scope.Flow);
    }


    /**
     * Create a new {@link FlowVariable} that holds a value of some {@link VariableType}.
     *
     * @param name the name of the variable
     * @param type the type of the variable
     * @param value the simple value held by this variable
     * @param <T> the simple value type held by this variable
     *
     * @since 4.1
     */
    public <T> FlowVariable(final String name, final VariableType<T> type, final T value) {
        this(name, type, value, Scope.Flow);
    }

    /**
     * create new FlowVariable representing a string.
     *
     * @param name of the variable
     * @param valueS string value
     */
    public FlowVariable(final String name, final String valueS) {
        this(name, StringType.INSTANCE, valueS);
    }

    /**
     * create new FlowVariable representing a double.
     *
     * @param name of the variable
     * @param valueD double value
     */
    public FlowVariable(final String name, final double valueD) {
        this(name, DoubleType.INSTANCE, valueD);
    }

    /**
     * create new FlowVariable representing an integer.
     *
     * @param name of the variable
     * @param valueI int value
     */
    public FlowVariable(final String name, final int valueI) {
        this(name, IntType.INSTANCE, valueI);
    }

    /**
     * create new FlowVariable representing a credentials object. (Flow Scope)
     *
     * @param name of the variable
     * @param valueC credentials object (usually has the same name except for name uniquification in subnode).
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public FlowVariable(final String name, final CredentialsFlowVariableValue valueC) {
        this(name, CredentialsType.INSTANCE, CheckUtils.checkArgumentNotNull(valueC, "Value must not be null"),
            getScopeFromName(name));
    }

    /**
     * (Added as part of AP-22551) Determines the scope of a variable, usually <code>Scope.Flow</code> but for one
     * special case it is <code>Scope.Global</code>: {@value CoreConstants#CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID} workflow
     * credentials are temporarily translated to flow variables in some selected nodes. These magic credentials, and
     * workflow credentials in general, are discouraged but to guarantee backwards compatibility (for some customers),
     * they are still supported.
     *
     * @param name Name of the variable
     * @return <code>Scope.Flow</code> if the name is not {@value CoreConstants#CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID},
     *         <code>Scope.Global</code> otherwise
     */
    private static Scope getScopeFromName(final String name) {
        return CoreConstants.CREDENTIALS_KNIME_SYSTEM_DEFAULT_ID.equals(name) ? Scope.Global : Scope.Flow;
    }

    /**
     * create new FlowVariable representing a string which can either be a global workflow variable or a local one.
     *
     * @param name of the variable
     * @param valueS string value
     * @param scope Scope of variable
     */
    FlowVariable(final String name, final String valueS, final Scope scope) {
        this(name, StringType.INSTANCE, valueS, scope);
    }

    /**
     * create new FlowVariable representing a double which can either be a global workflow variable or a local one.
     *
     * @param name of the variable
     * @param valueD double value
     * @param scope Scope of variable
     */
    FlowVariable(final String name, final double valueD, final Scope scope) {
        this(name, DoubleType.INSTANCE, valueD, scope);
    }

    /**
     * create new FlowVariable representing an integer which can either be a global workflow variable or a local one.
     *
     * @param name of the variable
     * @param valueI int value
     * @param scope Scope of variable
     */
    FlowVariable(final String name, final int valueI, final Scope scope) {
        this(name, IntType.INSTANCE, valueI, scope);
    }

    /**
     * @return name of variable.
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return true if the variable is a global workflow variable.
     */
    public boolean isGlobalConstant() {
        return m_scope.equals(Scope.Global);
    }

    /** @return the scope */
    public Scope getScope() {
        return m_scope;
    }

    /**
     * @return the type
     * @deprecated use {@link #getVariableType()} instead
     */
    @Deprecated
    public Type getType() {
        return getVariableType().getType();
    }

    /**
     * Method for obtaining the {@link VariableType} of this {@link FlowVariable}.
     *
     * @return the {@link VariableType} of this {@link FlowVariable}
     * @since 4.1
     */
    public VariableType<?> getVariableType() {
        return m_value.getType();
    }

    /**
     * Get the simple value of this variable.
     *
     * @param expectedType the expected {@link VariableType} of the to-be-returned value (use {@link #getVariableType()
     *            to obtain the actual type and compare it against the expected type}
     * @param <T> the simple type of the to-be-returned value
     * @return simple value object represented by this variable
     * @throws IllegalArgumentException if the argument is null or not of the correct class.
     * @since 4.1
     */
    public <T> T getValue(final VariableType<T> expectedType) {
        CheckUtils.checkArgumentNotNull(expectedType);
        final VariableType<?> actualType = m_value.getType();
        CheckUtils.checkArgument(actualType.getConvertibleTypes().contains(expectedType),
            "Flow variable does not represent value of class \"%s\" (but \"%s\") and also can't be converted to it.",
            actualType, expectedType);
        return m_value.getAs(expectedType);
    }

    /**
     * Retrieves the {@link VariableValue} held by this {@link FlowVariable}.
     *
     * @return the {@link VariableValue} held by this {@link FlowVariable}
     */
    VariableValue<?> getVariableValue() {//NOSONAR
        return m_value;
    }

    /**
     * @return get string value of the variable or null if it's not a string.
     */
    public String getStringValue() {
        return m_value.getType().equals(StringType.INSTANCE) ? (String)m_value.get() : null;
    }

    /**
     * @return get double value of the variable or Double.NaN if it's not a double.
     */
    public double getDoubleValue() {
        return m_value.getType().equals(DoubleType.INSTANCE) ? (Double)m_value.get() : Double.NaN;
    }

    /**
     * @return get int value of the variable or 0 if it's not an integer.
     */
    public int getIntValue() {
        return m_value.getType().equals(IntType.INSTANCE) ? (Integer)m_value.get() : 0;
    }

    /** Temporary workaround to represent credentials in flow variables. This is a framework method that is going
     * to change in future versions.
     * @return the valueCredentials or null.
     * @noreference This method is not intended to be referenced by clients.
     */
    CredentialsFlowVariableValue getCredentialsValue() {
        return m_value.getType().equals(CredentialsType.INSTANCE) ? (CredentialsFlowVariableValue)m_value.get() : null;
    }

    /**
     * @return value of the variable as string (independent of type).
     * @since 2.6
     */
    public String getValueAsString() {
        return m_value.asString();
    }

    /** Saves this flow variable to a settings object. This method writes
     * directly into the argument object (no creating of intermediate child).
     * @param settings To save to.
     * @since 4.1
     * @noreference This method is not intended to be referenced by clients.
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString("name", getName());
        m_value.save(settings);
    }

    /**
     * Read a flow variable from a settings object. This is the counterpart to {@link #save(NodeSettingsWO)}.
     *
     * @param sub To load from.
     * @return A new {@link FlowVariable} read from the settings object.
     * @throws InvalidSettingsException If that fails for any reason.
     * @since 4.1
     * @noreference This method is not intended to be referenced by clients.
     */
    public static FlowVariable load(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String name = CheckUtils.checkSettingNotNull(sub.getString("name"), "name must not be null");
        final VariableValue<?> value = VariableType.load(sub);
        final Scope scope = Stream.of(Scope.Local, Scope.Hide).filter(s -> StringUtils.startsWith(name, s.getPrefix()))
            .findFirst().orElse(Scope.Flow);
        return new FlowVariable(name, value, scope);
    }

    /**
     * Clones this object, setting a new name (used in subnode, puts a unique identifier).
     *
     * @param name new identifier, not null.
     * @return a 'clone' of this with a new name.
     * @noreference This method is not intended to be referenced by clients.
     */
    public FlowVariable withNewName(final String name) {
        return new FlowVariable(name, m_value, m_scope);
    }

    @Override
    public String toString() {
        return String.format("\"%s\" (%s: %s)", m_name, m_value.getType().getIdentifier(), m_value.asString());
    }

    /** {@inheritDoc}
     * Only compares type and value, not the owner and not the scope. */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FlowVariable)) {
            return false;
        }
        final FlowVariable v = (FlowVariable)obj;
        if (!v.getName().equals(getName())) {
            return false;
        }
        return m_value.equals(v.m_value);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getType().hashCode() ^ getName().hashCode();
    }

    /**
     * Framework method to create a {@link Scope#Hide hidden} string variable to denote that a variable with the given
     * name is to be removed/hidden from a node's variable stack.
     *
     * @param s The name of the variable to be removed/hidden in a node's output.
     * @return A new flow variable with a predefined name that is used to hide variables.
     *
     * @noreference This method is not intended to be referenced by clients.
     * @since 5.3
     */
    // added as part of AP-16515
    public static FlowVariable newHidingVariable(final String s) {
        CheckUtils.checkArgument(StringUtils.isNotBlank(s), "name must not be blank: \"%s\"", s);
        // can only hide "Flow" scope variables
        Scope.Flow.verifyName(s);
        return new FlowVariable(String.format("%s (%s)", Scope.Hide.getPrefix(),s), s, Scope.Hide);
    }

    /** Extracts the name of the variable hidden by a hiding variable, e.g. "knime.hide (some name)" -> "some name". */
    static String extractIdentifierFromHidingFlowVariable(final FlowVariable fv) {
        CheckUtils.checkArgument(fv.getScope() == Scope.Hide, "Variable should be 'Hide' scope: %s", fv.getScope());
        CheckUtils.checkArgument(fv.getVariableType().equals(VariableType.StringType.INSTANCE),
            "'Hide' variable must be of type string: %s", fv.getVariableType().getSimpleType().getSimpleName());
        return fv.getStringValue();
    }

}
