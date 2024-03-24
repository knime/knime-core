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
 *   Mar 25, 2024 (wiswedel): created
 */
package org.knime.core.customization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.knime.core.internal.CorePlugin;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.osgi.framework.FrameworkUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Provides an {@link APCustomization} based on a YAML configuration file specified in Eclipse preferences.
 *
 * @since 5.3
 * @noreference This class is not intended to be referenced by clients.
 * @author Bernd Wiswedel
 */
public final class APCustomizationProviderServiceImpl implements APCustomizationProviderService {

    static final String PREF_KEY_CUSTOMIZATION_CONFIG_PATH = "knime.core.ap-customization-configuration";

    private APCustomization m_apCustomization;

    /**
     * Lazily loads and returns the current {@link APCustomization} instance.
     * If not already loaded, it attempts to read the customization settings from a YAML file
     * whose path is retrieved from Eclipse preferences.
     *
     * @return The loaded {@link APCustomization} instance, or {@link APCustomization#DEFAULT} on failure.
     */
    @Override
    public synchronized APCustomization getCustomization() {
        if (m_apCustomization == null) {
            try {
                m_apCustomization = readCustomization();
            } catch (IOException ioe) {
                NodeLogger.getLogger(CorePlugin.class)
                    .error("Unable to read customization, using fallback (noop) customization", ioe);
                m_apCustomization = APCustomization.DEFAULT;
            }
        }
        return m_apCustomization;
    }

    private static APCustomization readCustomization() throws IOException {
        // "org.knime.core"
        final var pluginSymbolicName =
            FrameworkUtil.getBundle(APCustomizationProviderServiceImpl.class).getSymbolicName();

        final String ymlConfigFilePathS = DefaultScope.INSTANCE.getNode(pluginSymbolicName)
                .get(PREF_KEY_CUSTOMIZATION_CONFIG_PATH, null);

        if (StringUtils.isBlank(ymlConfigFilePathS)) {
            return APCustomization.DEFAULT;
        } else {
            final Path ymlConfigFilePath = Path.of(ymlConfigFilePathS);
            CheckUtils.check(Files.isRegularFile(ymlConfigFilePath) && Files.isReadable(ymlConfigFilePath),
                IOException::new, () -> String.format("Unable to read yaml configuration file defining AP "
                    + "customization. File \"%s\" does not exist or is not readable.", ymlConfigFilePathS));
            NodeLogger.getLogger(CorePlugin.class).debugWithFormat(
                "Applying customization as per configuration file \"%s\"", ymlConfigFilePath.getFileName());
            try (final var reader = Files.newBufferedReader(ymlConfigFilePath, StandardCharsets.UTF_8)) {
                return new ObjectMapper(new YAMLFactory()).readValue(reader, APCustomization.class);
            }
        }
    }

}
