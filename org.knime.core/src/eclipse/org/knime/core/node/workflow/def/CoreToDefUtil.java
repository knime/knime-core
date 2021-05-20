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
 *   May 19, 2021 (hornm): created
 */
package org.knime.core.node.workflow.def;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.Node;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.AbstractConfigEntry;
import org.knime.core.node.workflow.NodeContainer.NodeLocks;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.workflow.def.ConfigDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeLocksDef;
import org.knime.core.workflow.def.NodeUISettingsDef;
import org.knime.core.workflow.def.impl.DefaultConfigMapDef;
import org.knime.core.workflow.def.impl.DefaultConfigValueDef;

/**
 *
 * @author hornm
 */
public class CoreToDefUtil {

    public static NativeNodeDef toNativeNodeDef(final Node node) {
        //TODO
        return null;
    }

    /**
     * @param settings settings to persist
     * @return the node settings in a representation that can be converted to various formats
     * @throws InvalidSettingsException
     */
    public static ConfigDef toConfigDef(final NodeSettings settings) throws InvalidSettingsException {
        return toConfigDef(settings, settings.getKey());
    }

    /**
     * Recursive function to create a node settings tree (comprising {@link AbstractConfigEntry}s) from a
     * {@link ConfigDef} tree.
     *
     * @param settings an entity containing the recursive node settings
     * @param key the name of this subtree
     * @throws InvalidSettingsException
     */
    private static ConfigDef toConfigDef(final NodeSettings settings, final String key)
        throws InvalidSettingsException {

        // create ConfigDef from AbstractConfigurationEntry
        final Function<AbstractConfigEntry, ConfigDef> aceToDef = e -> DefaultConfigValueDef.builder()
            .setValue(e.toStringValue()).setValueType(e.getType().name()).build();

        // recursion anchor
        if (settings.isLeaf()) {
            return aceToDef.apply(settings);
        } else {
            // recurse
            final Map<String, ConfigDef> children = new LinkedHashMap<>();
            for (String childKey: settings.keySet()) {
                final AbstractConfigEntry child = settings.getEntry(childKey);
                if (settings.getEntry(childKey).isLeaf()) {
                    children.put(childKey, aceToDef.apply(child));
                } else {
                    children.put(childKey, toConfigDef((NodeSettings)settings.getConfig(childKey)));
                }
            }
            return DefaultConfigMapDef.builder().setKey(key).setChildren(children).build();
        }
    }

    public static NativeNodeDef toNativeNodeDef(final NodeAndBundleInformationPersistor def) {
        //TODO
        return null;
    }

    public static NodeUISettingsDef toNodeUIInformationDef(final NodeUIInformation uiInfoDef) {
        //TODO
        return null;
    }

    public static NodeLocksDef toNodeLocksDef(final NodeLocks def) {
        //TODO
        return null;
    }

}
