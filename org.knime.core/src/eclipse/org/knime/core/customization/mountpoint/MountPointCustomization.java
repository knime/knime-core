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
 *   Apr 17, 2025 (lw): created
 */
package org.knime.core.customization.mountpoint;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.knime.core.customization.mountpoint.filter.MountPointFilter;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointHostFilter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Customizations for mountpoints.
 *
 * @author Leon Wenzler
 * @since 5.5
 */
public final class MountPointCustomization implements WorkbenchMountPointHostFilter {

    /**
     * Default customization, allows all mountpoint hosts.
     */
    public static final MountPointCustomization DEFAULT = new MountPointCustomization(null);

    private final List<MountPointFilter> m_filters;

    @JsonCreator
    MountPointCustomization(@JsonProperty("filter") final List<MountPointFilter> filters) {
        m_filters = Collections.unmodifiableList(Objects.requireNonNullElse(filters, Collections.emptyList()));
    }

    @JsonProperty("filter")
    List<MountPointFilter> getFilter() {
        return m_filters;
    }

    @Override
    public boolean test(final String host) {
        return m_filters.stream().allMatch(t -> t.isHostAllowed(host));
    }

    @Override
    public String toString() {
        return String.format("MountPointCustomization{#filters=%d}", m_filters.size());
    }
}
