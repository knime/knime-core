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
 *   7 Sept 2023 (carlwitt): created
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
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.node.workflow.VariableType;

/**
 * Stores a hub item version in a configuration. Uses a string variable
 * <ul>
 * <li>in order to be compatible with the Version Creator node which currently outputs the created version (an integer)
 * as string</li>
 * <li>as an easy union type that can hold fixed versions (integers) and version references (enum constants)</li>
 * </ul>
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 * @since 5.2
 * @deprecated use {@link ItemVersion} and {@link ItemVersionStringPersistor} instead
 */
@Deprecated(since = "5.5", forRemoval = true)
public final class HubItemVersionPersistor {

    private HubItemVersionPersistor() {
    }

    /** The settings key used to store the version information */
    public static final String CONFIG_KEY = "hubItemVersion";

    private static String toString(final HubItemVersion version) {
        return Optional.ofNullable(version.versionNumber())//
            .map(Object::toString)//
            .orElse(version.linkType().toString());
    }

    private static HubItemVersion fromString(final String versionString) throws InvalidSettingsException {
        final var linkType = LinkType.fromString(versionString).orElse(LinkType.FIXED_VERSION);
        if (linkType == LinkType.FIXED_VERSION) {
            try {
                return HubItemVersion.of(Integer.parseInt(versionString));
            } catch (IllegalArgumentException e) {
                throw new InvalidSettingsException(
                    "Invalid hub item version %s. Must be %s or %s or an integer version number > 0."
                        .formatted(versionString, LinkType.LATEST_STATE.toString(), LinkType.LATEST_VERSION.toString()),
                    e);
            }
        } else {
            return new HubItemVersion(linkType, null);
        }
    }

    /**
     * Stores either the version number, e.g., "2" or the link type, e.g., "LATEST_STATE"
     *
     * @param version to save
     * @param config to save to
     */
    public static void save(final HubItemVersion version, final ConfigBaseWO config) {
        config.addString(CONFIG_KEY, toString(version));
    }

    /**
     * @param config to load from
     * @return the loaded version or empty if the settings do not contain the required key
     * @throws InvalidSettingsException if the settings contain broken hub item version data
     */
    public static Optional<HubItemVersion> load(final ConfigBaseRO config) throws InvalidSettingsException {
        if (!config.containsKey(CONFIG_KEY)) {
            return Optional.empty();
        }
        final var versionString = config.getString(CONFIG_KEY);
        return Optional.of(fromString(versionString));
    }

    /**
     * @param pane node dialog pane that manages the flow variables
     * @return a model that can be listened to in order to reflect flow variable state in legacy dialogs
     */
    public static FlowVariableModel createFlowVariableModel(final NodeDialogPane pane) {
        return pane.createFlowVariableModel(CONFIG_KEY, VariableType.StringType.INSTANCE);
    }

    /**
     * @param evt the change event
     * @return the version that was set in the flow variable or empty if the event does not contain a flow variable
     * @throws InvalidSettingsException if the flow variable contains an invalid value, i.e., invalid link type or
     *             non-parseable version number
     */
    public static Optional<HubItemVersion> fromFlowVariableChangeEvent(final EventObject evt)
        throws InvalidSettingsException {
        if (evt.getSource() instanceof FlowVariableModel fvm && fvm.isVariableReplacementEnabled()) {
            final var versionString = fvm.getVariableValue().map(FlowVariable::getStringValue);
            // can't do flatmap because of exception
            return versionString.isPresent() ? Optional.of(HubItemVersionPersistor.fromString(versionString.get()))
                : Optional.empty();
        }
        return Optional.empty();
    }
}