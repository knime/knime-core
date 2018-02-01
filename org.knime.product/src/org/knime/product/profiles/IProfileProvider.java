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
 *   31.01.2018 (thor): created
 */
package org.knime.product.profiles;

import java.net.URI;
import java.util.List;

/**
 * Interface for a profile provider. A provider can be registered at the extension point
 * <tt>org.knime.product.profileProvider</tt>. The registered provider is then asked for a list of profiles and their
 * location during startup.
 * <b>Implementors should use a few plug-in dependecies as possible because otherwise those plug-in get initialized
 * and applying custom default preferences is no longer possible. Ideally an implementation if this interface sits in
 * its own plug-in and only has a dependency to <tt>org.knime.product</tt> and doesn't use any other KNIME classes.</b>
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public interface IProfileProvider {
    /**
     * The path suffix on a KNIME Server to the profiles resource. You have to prepend the server address and the
     * context root of the KNIME Server application in order to get a complete URL that can be returned in
     * {@link #getProfilesLocation()}.
     */
    static final String SERVER_PROFILE_PATH = "/rest/v4/profiles";

    /**
     * Returns an ordered list of profiles that the clients requests.
     *
     * @return an order list of profile names; must not be <code>null</code> but may be empty
     */
    List<String> getRequestedProfiles();

    /**
     * Returns the location of the profiles. This can either be a remote location pointing to the profiles resource
     * of a KNIME Server or a local directory. In the first case a request is sent to the server that includes the
     * profile list ({@link #getRequestedProfiles()}) in the latter case the returned directory is expected to contain
     * subdirectories for every requested profile (same layout as on the server).
     *
     * @return a URI pointing to the profiles location; can only be <code>null</code> if the list of profiles is empty
     */
    URI getProfilesLocation();
}
