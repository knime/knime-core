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
 *   Aug 13, 2025 (wiswedel): created
 */
package org.knime.core.workbench.mountpoint.api.knimeurl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects {@link MountPointURLServiceFactory} instances from the extension point.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.8
 * @noreference This class is not intended to be referenced by clients.
 */
public final class MountPointURLServiceFactoryCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(MountPointURLServiceFactoryCollector.class);

    private static final String EXTENSION_POINT_ID = "org.knime.core.workbench.MountPointURLService";

    private static final String EXTENSION_POINT_TYPE_IDENTIFIER = "typeIdentifier";

    private static final String EXTENSION_POINT_URL_SERVICE_CLASS_FACTORY = "urlMountPointServiceClassFactory";

    private static final MountPointURLServiceFactoryCollector INSTANCE = new MountPointURLServiceFactoryCollector();

    /**
     * Gets the singleton {@link MountPointURLServiceFactoryCollector} instance.
     *
     * @return the only {@link MountPointURLServiceFactoryCollector} instance.
     */
    public static MountPointURLServiceFactoryCollector getInstance() {
        return INSTANCE;
    }

    private final Map<String, MountPointURLServiceFactory> m_typeIdToServiceFactory;

    private MountPointURLServiceFactoryCollector() {
        final Map<String, MountPointURLServiceFactory> typeIdToServiceFactory = new HashMap<>();
        final IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID);
        for (final Iterator<IConfigurationElement> it = Stream.of(point.getExtensions()) //
                .flatMap(ext -> Stream.of(ext.getConfigurationElements())).iterator(); it.hasNext();) {
            final IConfigurationElement configuration = it.next();
            final String extensionNamespace = configuration.getDeclaringExtension().getNamespaceIdentifier();
            final String typeIdentifier =
                StringUtils.stripToNull(configuration.getAttribute(EXTENSION_POINT_TYPE_IDENTIFIER));
            if (typeIdentifier == null) {
                LOGGER.warn("Blank type identifier used in extension: {}", extensionNamespace);
                continue;
            }
            final MountPointURLServiceFactory factory;
            try {
                factory = (MountPointURLServiceFactory)configuration
                    .createExecutableExtension(EXTENSION_POINT_URL_SERVICE_CLASS_FACTORY);
            } catch (final Exception e) { // NOSONAR - catch all exceptions (3rd party code)
                LOGGER.warn("Failed to create {} for type identifier {} in "
                    + "extension {}: {}", MountPointURLServiceFactory.class.getSimpleName(),
                    typeIdentifier, extensionNamespace, e.getMessage());
                continue;
            }
            typeIdToServiceFactory.put(typeIdentifier, factory);
            LOGGER.debug("Register {} for type identifier {} in extension {}.",
                MountPointURLServiceFactory.class.getSimpleName(), typeIdentifier, extensionNamespace);
        }
        m_typeIdToServiceFactory = Collections.unmodifiableMap(typeIdToServiceFactory);
    }

    /**
     * Gets the {@linkplain MountPointURLServiceFactory} for the specified type identifier if it has been registered.
     *
     * @param typeIdentifier the type identifier to retrieve the factory for.
     * @return {@linkplain Optional optionally} the registered {@link MountPointURLServiceFactory} or {@linkplain Optional#empty() empty}.
     */
    public Optional<MountPointURLServiceFactory> getMountPointURLServiceFactory(final String typeIdentifier) {
        return Optional.ofNullable(m_typeIdToServiceFactory.get(typeIdentifier));
    }

    /**
     * Gets all registered type identifiers.
     *
     * @return an unmodifiable set of all registered type identifiers.
     */
    public java.util.Set<String> getRegisteredTypeIdentifiers() {
        return Collections.unmodifiableSet(m_typeIdToServiceFactory.keySet());
    }
}