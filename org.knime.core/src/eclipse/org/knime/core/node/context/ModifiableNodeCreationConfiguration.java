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
 *   Oct 8, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.node.context;

import java.net.URL;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.ModifiablePortsConfiguration;
import org.knime.core.node.context.url.ModifiableURLConfiguration;
import org.knime.core.node.context.url.impl.DefaultModifiableURLConfiguration;

/**
 * Class storing any additional information required for the appropriate initialization of a {@link NodeModel}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 */
public final class ModifiableNodeCreationConfiguration extends NodeCreationConfiguration
    implements DeepCopy<ModifiableNodeCreationConfiguration>, NodeSettingsPersistable {

    /** Node creation config key. */
    private static final String NODE_CREATION_CONFIG_KEY = "node_creation_config";

    /**
     * Constructor.
     *
     * @param portsConfig the ports config
     */
    public ModifiableNodeCreationConfiguration(final ModifiablePortsConfiguration portsConfig) {
        super(portsConfig);
    }

    private ModifiableNodeCreationConfiguration(final ModifiableURLConfiguration url,
        final ModifiablePortsConfiguration portsConfig) {
        super(url, portsConfig);
    }

    @Override
    public Optional<ModifiablePortsConfiguration> getPortConfig() {
        return super.getModifiablePortConfig();
    }

    @Override
    public Optional<ModifiableURLConfiguration> getURLConfig() {
        return super.getModifiableURLConfig();
    }

    /**
     * Sets the url configuration.
     *
     * @param url the url
     */
    public void setURLConfiguration(final URL url) {
        super.setURLConfiguration(new DefaultModifiableURLConfiguration(url));
    }

    @Override
    public ModifiableNodeCreationConfiguration copy() {
        return new ModifiableNodeCreationConfiguration(getModifiableURLConfig().map(cfg -> cfg.copy()).orElse(null),
            getModifiablePortConfig().map(cfg -> cfg.copy()).orElse(null));
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        Optional<ModifiableURLConfiguration> urlConfig = getModifiableURLConfig();
        Optional<ModifiablePortsConfiguration> portsConfig = getModifiablePortConfig();
        if (urlConfig.isPresent() || portsConfig.isPresent()) {
            final NodeSettingsWO creationConfig = settings.addNodeSettings(NODE_CREATION_CONFIG_KEY);
            urlConfig.ifPresent(urlCfg -> urlCfg.saveSettingsTo(creationConfig));
            portsConfig.ifPresent(portCfg -> portCfg.saveSettingsTo(creationConfig));
        }
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        Optional<ModifiablePortsConfiguration> portsConfig = getModifiablePortConfig();
        if (settings.containsKey(NODE_CREATION_CONFIG_KEY)) {
            final NodeSettingsRO creationConfig = settings.getNodeSettings(NODE_CREATION_CONFIG_KEY);
            if (creationConfig != null) {
                setURLConfiguration(DefaultModifiableURLConfiguration.loadFromSettings(creationConfig).orElse(null));
                if (portsConfig.isPresent()) {
                    portsConfig.get().loadSettingsFrom(creationConfig);
                }
            }
        }
    }

}
