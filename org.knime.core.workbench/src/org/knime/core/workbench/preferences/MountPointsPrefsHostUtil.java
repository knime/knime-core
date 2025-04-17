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
 *   Jun 7, 2025 (lw): created
 */
package org.knime.core.workbench.preferences;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointState.WorkbenchMountPointStateSettings;

/**
 * Utilities regarding specifically the *host* of a remote mount point,
 * i.e. pointing to a KNIME Server or KNIME Hub.
 * <p>
 * Although not exposed by the {@link WorkbenchMountPointSettings} or {@link WorkbenchMountPoint}
 * classes, the {@link #getHost(WorkbenchMountPointSettings)} utility uses known property keys
 * to extract the host string out of a settings instance.
 * </p>
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 * @since 5.5
 */
public final class MountPointsPrefsHostUtil {

    /**
     * The key for the "server address" in the {@link WorkbenchMountPointStateSettings}
     * for content provider classes representing KNIME-Server-based mount points.
     */
    public static final String SERVER_ADDRESS_KEY = "address";

    /**
     * The key for the "server address" in the {@link WorkbenchMountPointStateSettings}
     * for content provider classes representing KNIME-Hub-based mount points.
     */
    public static final String HUB_ADDRESS_KEY = "serveraddress";

    /**
     * If the {@link WorkbenchMountPointSettings} are remote (i.e. server or hub),
     * extracts the host string of the properties map within the state settings.
     *
     * @param settings the mount point settings
     * @return host string if it can be found, otherwise {@link Optional#empty()}
     */
    public static Optional<String> getHost(final WorkbenchMountPointSettings settings) {
        return getServerAddress(settings).map(address -> {
            try {
                // Check if the address is in URI form...
                final var uri = new URI(address.trim());
                if (uri.getHost() != null) {
                    return uri.getHost();
                }
            } catch (URISyntaxException ex) { // NOSONAR
            }
            // ...otherwise assume a pure host name here, and use as is.
            return address;
        });
    }

    /**
     * Same as {@link #getHost(WorkbenchMountPointSettings)}, but only extracts the raw value
     * of the server address property. The method above then extracts the host string.
     *
     * @param settings the mount point settings
     * @return server address string if it can be found, otherwise {@link Optional#empty()}
     */
    private static Optional<String> getServerAddress(final WorkbenchMountPointSettings settings) {
        final var properties = settings.mountPointStateSettings().props();
        for (var key : List.of(SERVER_ADDRESS_KEY, HUB_ADDRESS_KEY)) {
            final var address = properties.get(key);
            if (address != null) {
                return Optional.of(address);
            }
        }
        return Optional.empty();
    }

    /**
     * Only a utility, not to be instantiated.
     */
    private MountPointsPrefsHostUtil() {
    }

}
