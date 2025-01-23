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
 *   Oct 22, 2024 (wiswedel): created
 */
package org.knime.core.workbench.mountpoint.api;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.preferences.MountSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author wiswedel
 */
public final class WorkbenchMountPointType {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchMountPointType.class);

    private static final String EXT_POINT_ID = "org.knime.core.workbench.mount";

    private static final String EXT_ATT_TYPE_IDENTIFIER = "typeIdentifier";

    private static final String EXT_ATT_DEFAULT_MOUNT_ID = "defaultMountID";

    private static final String EXT_ATT_SUPPORTS_MULTIPLE_INSTANCES = "supportsMultipleInstances";

    private static final String EXT_ATT_SETTINGS_HANDLER_CLASS = "stateFactoryClass";

    private final String m_typeIdentifier;

    private final boolean m_supportsMultipleInstances;

    private final String m_defaultMountID;

    private final LazyInitializer<WorkbenchMountPointStateFactory<WorkbenchMountPointState>> m_stateFactoryInitializer;

    private WorkbenchMountPointType(final String typeIdentifier, final String defaultMountID,
        final boolean supportsMultipleInstances,
        final LazyInitializer<WorkbenchMountPointStateFactory<WorkbenchMountPointState>> stateFactoryInitializer) {
        m_typeIdentifier = typeIdentifier;
        m_defaultMountID = defaultMountID;
        m_supportsMultipleInstances = supportsMultipleInstances;
        m_stateFactoryInitializer = stateFactoryInitializer;
    }

    /**
     * @return a unique ID (e.g. "com.knime.explorer.filesystem", etc.)
     */
    public String getTypeIdentifier() {
        return m_typeIdentifier;
    }

    /**
     * @return if multiple instances of this mount point type can be created/added. For instance, a local workspace can
     *         only be added once, while a hub mount point can be added multiple times.
     */
    public boolean supportsMultipleInstances() {
        return m_supportsMultipleInstances;
    }

    /**
     * @return a unique mount ID if this mount point should appear by default in the mount table. Or null, if it
     *         shouldn't be mounted by default. If an ID is returned the instantiation of the corresponding content
     *         provider must not open any dialog (or cause any other interaction).
     */
    public Optional<String> getDefaultMountID() {
        return Optional.ofNullable(m_defaultMountID);
    }

    /**
     * Always returns <code>false</code>. Except for the one temp space provider implementation.
     * @return <code>false</code>. Almost always.
     */
    public boolean isTemporaryMountPoint() {
        return m_typeIdentifier.equals(WorkbenchConstants.TYPE_IDENTIFIER_TEMP_SPACE);
    }

    public WorkbenchMountPoint createMountPoint(final MountSettings settings) throws IOException {
        final WorkbenchMountPointStateFactory<?> stateFactory;
        try {
            stateFactory = m_stateFactoryInitializer.get();
        } catch (ConcurrentException ex) { // NOSONAR ignoring exception, but using cause
            throw new IOException(String.format("Failed to create settings handler for extension with "
                + "type identifier %s", m_typeIdentifier), ex.getCause());
        }
        final var mountPointState = stateFactory.newInstance(settings);
        return new WorkbenchMountPoint(this, settings, mountPointState);
    }

    public static Map<String, WorkbenchMountPointType> collectDefinitions() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        Map<String, WorkbenchMountPointType> result = new LinkedHashMap<>();
        for (IExtension ext : point.getExtensions()) {
            for (IConfigurationElement element : ext.getConfigurationElements()) {
                final String identifier = element.getAttribute(EXT_ATT_TYPE_IDENTIFIER);
                if (StringUtils.isBlank(identifier)) {
                    LOGGER.error("Mount point identifier is missing in extension {}", ext.getContributor().getName());
                    continue;
                }
                final String defaultMountID = element.getAttribute(EXT_ATT_DEFAULT_MOUNT_ID);
                if (defaultMountID != null && !WorkbenchMountTable.isValidMountID(defaultMountID)) { // null is OK
                    LOGGER.error("Invalid {} \"{}\" in extension in {}", EXT_ATT_DEFAULT_MOUNT_ID, defaultMountID,
                        ext.getContributor().getName());
                    continue;
                }
                final String supportsMultipleInstancesS = element.getAttribute(EXT_ATT_SUPPORTS_MULTIPLE_INSTANCES);
                final boolean supportsMultipleInstances = Boolean.parseBoolean(supportsMultipleInstancesS);
                final String settingsHandlerClassS = element.getAttribute(EXT_ATT_SETTINGS_HANDLER_CLASS);
                if (StringUtils.isBlank(settingsHandlerClassS)) {
                    LOGGER.error("Settings handler class is missing in extension {}", ext.getContributor().getName());
                    continue;
                }
                // init lazy to avoid 3rd party bundle activation until needed
                @SuppressWarnings("unchecked")
                LazyInitializer<WorkbenchMountPointStateFactory<WorkbenchMountPointState>> settingsHandlerInitializer = // NOSONAR
                    LazyInitializer.<WorkbenchMountPointStateFactory<WorkbenchMountPointState>> builder() //
                        .setInitializer(() -> (WorkbenchMountPointStateFactory<WorkbenchMountPointState>)element
                            .createExecutableExtension(EXT_ATT_SETTINGS_HANDLER_CLASS)) //
                        .get();
                result.put(identifier, new WorkbenchMountPointType(identifier,
                    defaultMountID, supportsMultipleInstances, settingsHandlerInitializer));
            }
        }
        return Collections.unmodifiableMap(result);
    }

}
