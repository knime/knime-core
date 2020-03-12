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
 *   Feb 25, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.workflow;

import static java.util.stream.Collectors.toList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.Config;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.CredentialsType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.InvalidConfigEntryException;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.core.node.workflow.VariableType.VariableValue;

/**
 * Collects {@link VariableType VariableTypes} from the corresponding extension point and provides them to the
 * framework.
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class VariableTypeRegistry {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(VariableTypeRegistry.class);

    private static final String EXT_POINT_ID = "org.knime.core.FlowVariableType";

    private static final VariableTypeRegistry INSTANCE = createInstance();

    private static VariableTypeRegistry createInstance() {
        /* The core variables are not registered via the extension point to ensure:
         * - that they are always listed first
         * - that StringArrayType is the first array type checked in #getCorrespondingVariableType
         *   (that's because empty array types are indistinguishable and string is the most sensible default)
         */
        final Map<String, VariableType<?>> variableTypes = Stream
            .of(StringType.INSTANCE, StringArrayType.INSTANCE, BooleanType.INSTANCE, BooleanArrayType.INSTANCE,
                IntType.INSTANCE, IntArrayType.INSTANCE, LongType.INSTANCE, LongArrayType.INSTANCE, DoubleType.INSTANCE,
                DoubleArrayType.INSTANCE, CredentialsType.INSTANCE)
            .collect(Collectors.toMap(VariableType::getIdentifier, Function.identity(),
                VariableTypeRegistry::handleConflictingIdentifiers, LinkedHashMap::new));
        final IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(EXT_POINT_ID);
        final Map<String, VariableType<?>> extensionPointTypes = Stream.of(point.getExtensions())
            .flatMap(e -> Stream.of(e.getConfigurationElements())).map(VariableTypeRegistry::readVariableType)
            .filter(Objects::nonNull).collect(Collectors.toMap(VariableType::getIdentifier, Function.identity(),
                VariableTypeRegistry::handleConflictingIdentifiers, LinkedHashMap::new));
        variableTypes.putAll(extensionPointTypes);
        return new VariableTypeRegistry(variableTypes);
    }

    private static VariableType<?> handleConflictingIdentifiers(final VariableType<?> first,
        final VariableType<?> second) {
        LOGGER.debugWithFormat(
            "Conflicting VariableType identifier '%s' detected. "
                + "Only the VariableType '%s' is taken, while the VariableType '%s' is dropped.",
            first.getIdentifier(), first.getClass().getName(), second.getClass().getName());
        return first;
    }

    private static VariableType<?> readVariableType(final IConfigurationElement cfe) {
        try {
            final VariableTypeExtension extension = (VariableTypeExtension)cfe.createExecutableExtension("extension");
            final VariableType<?> t = extension.getVariableType();
            LOGGER.debugWithFormat("Added flow variable type '%s' from '%s'", t.getClass().getName(),
                cfe.getContributor().getName());
            return t;
        } catch (CoreException ex) {
            LOGGER.error(String.format("Could not create '%s' from extension '%s': %s", VariableType.class.getName(),
                cfe.getContributor().getName(), ex.getMessage()), ex);
        }
        return null;
    }

    /**
     * Maps identifier -> VariableType.
     */
    private final Map<String, VariableType<?>> m_variableTypes;

    private VariableTypeRegistry(final Map<String, VariableType<?>> variableTypes) {
        m_variableTypes = variableTypes;
    }

    /**
     * @return the registry instance
     */
    public static VariableTypeRegistry getInstance() {
        return INSTANCE;
    }

    VariableValue<?> loadValue(final NodeSettingsRO sub) throws InvalidSettingsException {
        final String identifier = CheckUtils.checkSettingNotNull(sub.getString("class"), "'class' must not be null");
        final VariableType<?> type = m_variableTypes.get(identifier);
        CheckUtils.checkSetting(type != null, "No flow variable type for identifier/class '%s'", identifier);
        @SuppressWarnings("null") // the above check ensures that type is not null
        final VariableValue<?> value = type.loadValue(sub);
        return value;
    }

    /**
     * Returns an array of all available flow variable types.</br>
     * I.e. all types defined by the core and registered via the extension point.
     *
     * @return an array of all available flow variable types
     */
    public VariableType<?>[] getAllTypes() {
        return m_variableTypes.values().toArray(new VariableType[0]);
    }

    Stream<VariableType<?>> stream() {
        return m_variableTypes.values().stream();
    }

    /**
     * Returns the {@link VariableType VariableTypes} that can overwrite the entry in {@link Config config} identified
     * by <b>configKey</b>.
     *
     * @param config the {@link Config} whose entry with key <b>configKey</b> should be overwritten
     * @param configKey the key identifying the entry in {@link Config config} that should be overwritten
     * @return the {@link VariableType VariableTypes} that can overwrite the specified config entry
     */
    public VariableType<?>[] getOverwritingTypes(final Config config, final String configKey) {
        return stream().filter(t -> t.canOverwrite(config, configKey)).toArray(VariableType[]::new);
    }

    /**
     * Overwrites the config entry in {@link Config config} identified by <b>configKey</b> with the provided
     * {@link FlowVariable}.
     *
     * @param config the {@link Config} whose entry identified by <b>configKey</b> should be overwritten
     * @param configKey the key identifying the config entry to overwrite
     * @param variable the variable to overwrite the config entry with
     * @throws InvalidSettingsException if overwriting the config entry fails due to invalid settings
     * @throws InvalidConfigEntryException if overwriting the config entry fails due to incompatibility
     */
    public static void overwriteWithVariable(final Config config, final String configKey, final FlowVariable variable)
        throws InvalidSettingsException, InvalidConfigEntryException {
        final VariableType<?> variableType = variable.getVariableType();
        CheckUtils.checkSetting(variableType.canOverwrite(config, configKey),
            "Can't evaluate variable \"%s\" as %s expression, it is a %s (\"%s\"", variable.getName(),
            config.getEntry(configKey).getType(), variableType, variable);
        variable.getVariableValue().overwrite(config, configKey);
    }

    /**
     * Creates a {@link FlowVariable} from the entry in {@link Config config} with key <b>configKey</b>.
     *
     * @param name the name of the created variable
     * @param config the {@link Config} containing the entry from which to create the variable
     * @param configKey the key identifying the entry from which to create the variable
     * @return the flow variable corresponding to the provided config entry
     * @throws InvalidSettingsException if flow variable creation fails due to invalid settings
     * @throws InvalidConfigEntryException if flow variable creation fails due to incompatible settings
     */
    public FlowVariable createFromConfig(final String name, final Config config, final String configKey)
        throws InvalidSettingsException, InvalidConfigEntryException {

        final Optional<VariableType<?>> matchingType = getCorrespondingVariableType(config, configKey);
        // necessary to avoid compilation errors
        @SuppressWarnings("rawtypes")
        final VariableType type = matchingType.orElseThrow(() -> new InvalidSettingsException(
            String.format("No flow variable with a type available that can overwrite the config '%s'. "
                    + "Are you missing a KNIME extension?", config)));
        // argument compatibility is guaranteed through type
        @SuppressWarnings("unchecked")
        final VariableValue<?> value = type.newValue(type.createFrom(config, configKey));
        return new FlowVariable(name, value);
    }

    /**
     * Returns the {@link VariableType} corresponding to the type of config entry stored with key <b>configKey</b> in
     * {@link Config config}.
     *
     * @param config the {@link Config} containing the entry to create the {@link FlowVariable} from
     * @param configKey the key identifying the entry to create the FlowVariable from
     * @return an {@link Optional} containing the corresponding type if there is one, otherwise {@link Optional#empty()}
     */
    public Optional<VariableType<?>> getCorrespondingVariableType(final Config config, final String configKey) {
        final List<VariableType<?>> correspondingTypes =
            stream().filter(t -> t.canCreateFrom(config, configKey)).collect(toList());
        if (correspondingTypes.size() > 1) {
            LOGGER.debugWithFormat(
                "The following variable types could all correspond to the config '%s': %s. Taking the first.",
                config.getEntry(configKey), correspondingTypes);
        }
        return correspondingTypes.isEmpty() ? Optional.empty() : Optional.of(correspondingTypes.get(0));
    }

}
