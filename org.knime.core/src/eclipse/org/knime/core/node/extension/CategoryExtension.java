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
 *   Mar 19, 2021 (hornm): created
 */
package org.knime.core.node.extension;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.node.util.CheckUtils;

/**
 * A node category contributed via the respective extension point. This class encapsulates everything that is defined
 * via the extension point.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public final class CategoryExtension {

    private static final String NAME = "name";

    private static final String PATH = "path";

    private static final String LEVEL_ID = "level-id";

    private final String m_name;

    private final String m_path;

    private final String m_levelId;

    private final IConfigurationElement m_configurationElement;

    CategoryExtension(final IConfigurationElement configurationElement) {
        m_configurationElement = configurationElement;
        m_name = configurationElement.getAttribute(NAME);
        m_path = configurationElement.getAttribute(PATH);
        m_levelId = configurationElement.getAttribute(LEVEL_ID);
        CheckUtils.checkArgument(StringUtils.isNotBlank(m_name),
            "Category name in attribute \"%s\" must not be blank (contributing plug-in %s)", NAME,
            configurationElement.getContributor().getName());
        CheckUtils.checkArgument(StringUtils.isNotBlank(m_name),
            "Cageory path in attribute \"%s\" must not be blank (contributing plug-in %s)", PATH,
            configurationElement.getContributor().getName());
        CheckUtils.checkArgument(StringUtils.isNotBlank(m_name),
            "Category level-id in attribute \"%s\" must not be blank (contributing plug-in %s)", LEVEL_ID,
            configurationElement.getContributor().getName());
    }

    /**
     * @return the category name
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the path this category is located at
     */
    public String getPath() {
        return m_path;
    }

    /**
     * Composes and returns the complete path which is made of the path and the level-id.
     *
     * @return the complete path, i.e. the path concatenated with the level-id
     */
    public String getCompletePath() {
        String path = m_path.endsWith("/") ? m_path : (m_path + "/"); // NOSONAR
        return path + m_levelId;
    }

    /**
     * @return the category id
     */
    public String getLevelId() {
        return m_levelId;
    }

    /** @return the "after" field in the extension point or an empty string. */
    public String getAfterID() {
        return ObjectUtils.defaultIfNull(m_configurationElement.getAttribute("after"), "");
    }

    /** @return symbolic name of the plug-in contributing the category/extension. */
    public String getContributingPlugin() {
        return m_configurationElement.getDeclaringExtension().getNamespaceIdentifier();
    }

}
