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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.function.FailableSupplier;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.workbench.WorkbenchConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mount point type that can be registered via extension point. All entries of the {@link WorkbenchMountTable} must be
 * of one of the registered mount point types.
 *
 * @author Bernd Wiswedel, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class WorkbenchMountPointType {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkbenchMountPointType.class);

    private static final String EXT_POINT_ID = "org.knime.core.workbench.MountPointType";

    private static final String EXT_ATT_TYPE_IDENTIFIER = "typeIdentifier";

    private static final String EXT_ATT_DEFAULT_MOUNT_ID = "defaultMountID";

    private static final String EXT_ATT_SUPPORTS_MULTIPLE_INSTANCES = "supportsMultipleInstances";

    private static final String EXT_ATT_SETTINGS_HANDLER_CLASS = "stateFactoryClass";

    private static final String EXT_ATT_SORT_PRIORITY = "sortPriority";

    private static final int DEFAULT_SORT_PRIO = 1;

    private final String m_typeIdentifier;

    private final boolean m_supportsMultipleInstances;

    private final int m_sortPriority;

    private final String m_defaultMountID;

    private final LazyInitializer<WorkbenchMountPointStateFactory<?>> m_stateFactoryInitializer;

    private WorkbenchMountPointType(final String typeIdentifier, final String defaultMountID,
        final boolean supportsMultipleInstances, final int sortPriority,
        final FailableSupplier<WorkbenchMountPointStateFactory<?>, Exception> initializer) {
        m_typeIdentifier = typeIdentifier;
        m_defaultMountID = defaultMountID;
        m_supportsMultipleInstances = supportsMultipleInstances;
        m_sortPriority = sortPriority;
        m_stateFactoryInitializer =
                LazyInitializer.<WorkbenchMountPointStateFactory<?>>builder().setInitializer(initializer).get();
    }

    @SuppressWarnings("java:S1452") // generics
    WorkbenchMountPointStateFactory<?> instantiateStateFactory() throws WorkbenchMountException {
        try {
            return m_stateFactoryInitializer.get();
        } catch (ConcurrentException ex) { // NOSONAR exception's cause is rethrown
            throw new WorkbenchMountException(
                String.format("Failed to create mountpoint state factory for extension with type identifier %s",
                    m_typeIdentifier),
                ex.getCause());
        }
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
     * @return the sort priority of this mount point type (at which position in the "new mount point dialog" the type is
     *         offered). The higher the number, the higher the priority.
     */
    public int getSortPriority() {
        return m_sortPriority;
    }

    /**
     * @return a unique mount ID if this mount point should appear by default in the mount table. Or empty if it
     *         shouldn't be mounted by default. If an ID is returned the instantiation of the corresponding content
     *         provider must not open any dialog (or cause any other interaction).
     */
    public Optional<String> getDefaultMountID() {
        return Optional.ofNullable(m_defaultMountID);
    }

    /**
     * @return default mount settings if a "canonical" configuration of a mount point of this type exists
     * @throws WorkbenchMountException if the state factory couldn't be instantiated
     */
    public Optional<WorkbenchMountPointSettings> getDefaultSettings() throws WorkbenchMountException {
        return instantiateStateFactory().getDefaultStateSettings() //
            .map(stateSettings -> new WorkbenchMountPointSettings(
                CheckUtils.checkNotNull(m_defaultMountID,
                    "default mount ID must not be null for mount point of type %s", getTypeIdentifier()),
                m_defaultMountID, m_typeIdentifier, stateSettings, true));
    }

    /**
     * Always returns <code>false</code>. Except for the one temp space provider implementation.
     * @return <code>false</code>. Almost always.
     */
    public boolean isTemporaryMountPoint() {
        return m_typeIdentifier.equals(WorkbenchConstants.TYPE_IDENTIFIER_TEMP_SPACE);
    }

    /**
     * Creates a mount point with the given settings.
     *
     * @param settings mount settings
     * @return created mount point
     * @throws WorkbenchMountException if mount point creation failed
     */
    WorkbenchMountPoint createMountPoint(final WorkbenchMountPointSettings settings) throws WorkbenchMountException {
        CheckUtils.check(m_typeIdentifier.equals(settings.factoryID()), WorkbenchMountException::new, //
            () -> "Mount point type mismatch: '%s vs. '%s'".formatted(m_typeIdentifier, settings.factoryID()));
        final WorkbenchMountPointStateFactory<?> stateFactory = instantiateStateFactory();
        final var mountPointState = stateFactory.newInstance(settings);
        return WorkbenchMountPoint.builder() //
                .withType(settings.getWorkbenchMountPointTypeOrFail()) //
                .withMountID(settings.mountID()) //
                .withDefaultMountID(m_defaultMountID) //
                .withIsActive(settings.isActive()) //
                .withState(mountPointState) //
                .build();
    }

    /**
     * @return the display name for this type
     * @throws WorkbenchMountException if the state factory could not be created
     */
    public String getDisplayName() throws WorkbenchMountException {
        return instantiateStateFactory().getDisplayName();
    }

    /**
     * Creates a short display string to help the user identify the specific mount point.
     *
     * @param mountSettings mount settings of the mount point to create a display string for
     * @return string identifying the mount point (e.g. {@code "<user>@knime-hub.<some-domain>.com"})
     * @throws WorkbenchMountException if no display string could be created
     */
    public String getContentDisplayString(final WorkbenchMountPointSettings mountSettings)
        throws WorkbenchMountException {
        return instantiateStateFactory().getContentDisplayString(mountSettings);
    }

    /**
     * Collects all available mount point types from the extension point.
     *
     * @return list of mount point types
     */
    public static Map<String, WorkbenchMountPointType> collectMountPointTypes() {
        final IExtensionRegistry registry = Platform.getExtensionRegistry();
        final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        final Map<String, WorkbenchMountPointType> result = new LinkedHashMap<>();
        for (IExtension ext : point.getExtensions()) {
            final String contributorName = ext.getContributor().getName();
            for (IConfigurationElement element : ext.getConfigurationElements()) {
                createMountPointType(element, contributorName) //
                    .ifPresent(mpType -> result.put(mpType.getTypeIdentifier(), mpType));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @SuppressWarnings("java:S6212") // `var` may hurt readability
    private static Optional<WorkbenchMountPointType> createMountPointType(final IConfigurationElement element,
        final String contributorName) {

        final String identifier = element.getAttribute(EXT_ATT_TYPE_IDENTIFIER);
        if (StringUtils.isBlank(identifier)) {
            LOGGER.error("Mount point identifier is missing in extension {}", contributorName);
            return Optional.empty();
        }

        final String defaultMountID = element.getAttribute(EXT_ATT_DEFAULT_MOUNT_ID);
        if (defaultMountID != null && !WorkbenchConstants.isValidMountID(defaultMountID)) { // null is OK
            LOGGER.error("Invalid {} \"{}\" in extension in {}", EXT_ATT_DEFAULT_MOUNT_ID, defaultMountID,
                contributorName);
            return Optional.empty();
        }

        if (StringUtils.isBlank(element.getAttribute(EXT_ATT_SETTINGS_HANDLER_CLASS))) {
            LOGGER.error("Settings handler class is missing in extension {}", contributorName);
            return Optional.empty();
        }

        final boolean supportsMultipleInstances =
                Boolean.parseBoolean(element.getAttribute(EXT_ATT_SUPPORTS_MULTIPLE_INSTANCES));

        final int sortPriority = readSortPriority(element, contributorName);

        // init lazily to avoid 3rd party bundle activation until needed
        return Optional.of(new WorkbenchMountPointType(identifier, defaultMountID, supportsMultipleInstances,
            sortPriority, () -> (WorkbenchMountPointStateFactory<?>)element
                .createExecutableExtension(EXT_ATT_SETTINGS_HANDLER_CLASS)));
    }

    private static int readSortPriority(final IConfigurationElement element, final String contributorName) {
        final String attribute = Objects.requireNonNullElse(element.getAttribute(EXT_ATT_SORT_PRIORITY),
            Integer.toString(DEFAULT_SORT_PRIO));
        int sortPriority;
        try {
            sortPriority = Integer.parseInt(attribute);
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid {} \"{}\" in extension {}", EXT_ATT_SORT_PRIORITY, attribute, contributorName);
            return DEFAULT_SORT_PRIO;
        }
        if (sortPriority < 0 || sortPriority > 100) {
            LOGGER.error("Invalid {} \"{}\" in extension {}, value must be [1, 100]", EXT_ATT_SORT_PRIORITY, attribute,
                contributorName);
            return DEFAULT_SORT_PRIO;
        }
        return sortPriority;
    }

    @Override
    public String toString() {
        return "WorkbenchMountPointType[id=" + m_typeIdentifier + "]";
    }
}
