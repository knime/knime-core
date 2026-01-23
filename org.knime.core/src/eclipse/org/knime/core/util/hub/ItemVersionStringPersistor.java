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
 *   17 Mar 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.util.hub;

import java.util.EventObject;
import java.util.Optional;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;

/**
 * Stores a Hub item version in a config as a string
 * <ul>
 * <li>in order to be compatible with the <i>Version Creator</i> node which currently outputs the created version (an
 * integer) as string</li>
 * <li>as an easy union type that can hold specific versions (integers) and unspecific version references (most-recent,
 * current-state).</li>
 * </ul>
 *
 * This is {@link HubItemVersionPersistor} but for {@link ItemVersion}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class ItemVersionStringPersistor {

    static final String CONFIG_KEY = "hubItemVersion"; // do not make public

    // LinkType#toString uses this string value to represent this "non-version" current-state
    private static final String CURRENT_STATE_LINK_TYPE = "LATEST_STATE";

    // LinkType#toString uses this to represent the most-recent version
    private static final String MOST_RECENT_LINK_TYPE = "LATEST_VERSION";

    private ItemVersionStringPersistor() {
        // hidden
    }

    /**
     * Converts the given {@link ItemVersion} into its string representation used for persistence.
     * <p>
     * Specific versions are represented by their version string, while unspecific versions such as
     * {@code CurrentState} and {@code MostRecent} are mapped to the special string constants
     * {@value #CURRENT_STATE_LINK_TYPE} and {@value #MOST_RECENT_LINK_TYPE}.
     * </p>
     *
     * @param version the {@link ItemVersion} to convert; may be a specific version, the current state, or the most
     *            recent version of an item
     * @return the string representation of the given version, suitable for storing in configuration
     * @since 5.11
     */
    public static String toString(final ItemVersion version) {
        // this is the same as the code for HubItemVersion#toString(HubItemVersion)
        if (version instanceof SpecificVersion sv) {
            return sv.getVersionString();
        } else if (version instanceof CurrentState) {
            return CURRENT_STATE_LINK_TYPE;
        } else if (version instanceof MostRecent) {
            return MOST_RECENT_LINK_TYPE;
        }
        // should not happen b/c sealed interface (good candidate for exhaustive pattern-match switch check)
        throw new IllegalStateException("Unexpected version class: " + version.getClass());
    }

    /**
     * Matches the given config value potentially representing an {@link ItemVersion} to the proper
     * {@link ItemVersion}.
     *
     * @param versionString version value to match to {@link ItemVersion}
     * @return {@link ItemVersion} if it can be matched (parsed)
     * @throws InvalidSettingsException if the version string is invalid
     */
    private static ItemVersion fromString(final String versionString) throws InvalidSettingsException {
        if (CURRENT_STATE_LINK_TYPE.equals(versionString)) {
            return ItemVersion.currentState();
        }
        if (MOST_RECENT_LINK_TYPE.equals(versionString)) {
            return ItemVersion.mostRecent();
        }
        try {
            return ItemVersion.of(Integer.parseUnsignedInt(versionString));
        } catch (final NumberFormatException e) {
            throw new InvalidSettingsException(
                "Invalid Hub item version \"%s\". Valid values are \"%s\", \"%s\", or a non-negative integer value."
                    .formatted(versionString, CURRENT_STATE_LINK_TYPE, MOST_RECENT_LINK_TYPE));
        }
    }


    /**
     * Stores the version number, e.g. "2" or the link type e.g. "LATEST_STATE" or "LATEST_VERSION".
     * @param version version to save
     * @param config config to save to
     */
    public static void save(final ItemVersion version, final ConfigBaseWO config) {
        config.addString(CONFIG_KEY, toString(version));
    }

    /**
     * Loads the version from the config.
     * @param config config to load from
     * @return the version or empty if not present
     * @throws InvalidSettingsException if the config contains broken Hub item version data
     */
    public static Optional<ItemVersion> load(final ConfigBaseRO config) throws InvalidSettingsException {
        if (!config.containsKey(CONFIG_KEY)) {
            return Optional.empty();
        }
        final var versionString = config.getString(CONFIG_KEY);
        return Optional.of(fromString(versionString));
    }

    /**
     * Creates a flow variable model for a version string.
     * @param pane node dialog pane that manages the flow variables
     * @return a model that can be listened to in order to reflect flow variable state in legacy dialogs
     */
    public static FlowVariableModel createFlowVariableModel(final NodeDialogPane pane) {
        return pane.createFlowVariableModel(CONFIG_KEY, VariableType.StringType.INSTANCE);
    }

    /**
     * Extracts the version from a flow variable change event.
     *
     * @param evt the change event
     * @return the version that was set in the flow variable or empty if the event does not contain a flow variable
     * @throws InvalidSettingsException if the flow variable contains an invalid value, i.e., invalid link type or
     *             non-parseable version number
     */
    public static Optional<ItemVersion> fromFlowVariableChangeEvent(final EventObject evt)
        throws InvalidSettingsException {
        if (evt.getSource() instanceof FlowVariableModel fvm && fvm.isVariableReplacementEnabled()) {
            final var versionString = fvm.getVariableValue().map(FlowVariable::getStringValue);
            // no flatmap b/c InvalidSettingsException
            if (versionString.isPresent()) {
                return Optional.of(fromString(versionString.get()));
            }
        }
        return Optional.empty();
    }

}
