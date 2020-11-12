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
 *   Nov 10, 2020 (bogenrieder): created
 */
package org.knime.core.node.workflow;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Functional wrapper class for the configuration sorter. This Provider uses a similar structure to the layout editor.
 *
 * @author Daniel Bogenrieder
 * @since 4.3
 */
public final class SubnodeContainerConfigurationStringProvider {

    private static final String DEFAULT_LATEST_CONFIGURATION_STRING = "{}";

    private String m_currentLayoutString;

    private final String m_loadedLayoutString;

    /**
     * Creates a default layout string provider for newly created components (SubNodeContainers).
     */
    public SubnodeContainerConfigurationStringProvider() {
        m_currentLayoutString = DEFAULT_LATEST_CONFIGURATION_STRING;
        m_loadedLayoutString = DEFAULT_LATEST_CONFIGURATION_STRING;
    }

    /**
     * Creates a layout string provider for components (SubNodeContainers) loaded from a saved layout string
     * representation. If the component was saved without a layout (which was the default before V4.3.0) it will detect
     * this and ensure default layout creation later in the component life-cycle maintains the expected,
     * backwards-compatible behavior.
     *
     * @param currentLayout the existing layout string as read in from the saved component settings.
     */
    public SubnodeContainerConfigurationStringProvider(final String currentLayout) {
        if (StringUtils.isEmpty(currentLayout)) {
            m_currentLayoutString = DEFAULT_LATEST_CONFIGURATION_STRING;
            m_loadedLayoutString = DEFAULT_LATEST_CONFIGURATION_STRING;
        } else {
            m_currentLayoutString = currentLayout;
            m_loadedLayoutString = currentLayout;
        }
    }

    /**
     * @return the current string representation of the layout.
     */
    public String getConfigurationLayoutString() {
        return m_currentLayoutString;
    }

    /**
     * @param layoutString current layoutString to set.
     */
    public void setConfigurationLayoutString(final String layoutString) {
        m_currentLayoutString = layoutString;
    }

    /**
     * Checks the layout is one of the default placeholder layouts. If true, the layout should be updated before its
     * functionally accessed. If not, this indicates downstream layout processing has occurred.
     *
     * @return true if the layout is equal to one of the default layouts.
     */
    public boolean isPlaceholderLayout() {
        return m_currentLayoutString.contentEquals(DEFAULT_LATEST_CONFIGURATION_STRING);
    }

    /**
     * Checks the original layout (as loaded from a saved workflow or created for a new component) contains the provided
     * search term string.
     *
     * @param searchTerm the term to search for in the original layout.
     * @return if the original layout contains the provided search term string.
     */
    public boolean checkOriginalContains(final String searchTerm) {
        if (searchTerm == null) {
            return false;
        }
        return m_loadedLayoutString.contains(searchTerm);
    }

    /**
     * Checks if the layout is null or empty.
     *
     * @return if the current layout of this provider is null or empty.
     */
    public boolean isEmptyLayout() {
        return StringUtils.isEmpty(m_currentLayoutString);
    }

    /**
     * Creates a new instance of this class with an identical internal state. Should be used when copying components.
     *
     * @return a new SubnodeContainerConfigurationStringProvider instance.
     */
    public SubnodeContainerConfigurationStringProvider copy() {
        SubnodeContainerConfigurationStringProvider newConfigurationStringProvider =
            new SubnodeContainerConfigurationStringProvider(m_loadedLayoutString);
        newConfigurationStringProvider.setConfigurationLayoutString(m_currentLayoutString);
        return newConfigurationStringProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        SubnodeContainerConfigurationStringProvider other = (SubnodeContainerConfigurationStringProvider)obj;
        return StringUtils.equals(m_currentLayoutString, other.m_currentLayoutString);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(m_currentLayoutString)
                .toHashCode();
    }
}
