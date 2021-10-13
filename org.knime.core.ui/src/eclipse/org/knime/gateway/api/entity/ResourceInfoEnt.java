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
 *   Oct 13, 2021 (hornm): created
 */
package org.knime.gateway.api.entity;

import org.knime.core.webui.page.Page;

/**
 * Information around the web resource (e.g. html or javascript document) representing a node view.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public final class ResourceInfoEnt {

    private final String m_url;

    private final String m_path;

    private final String m_type;

    private final String m_id;

    ResourceInfoEnt(final String id, final String url, final String path, final Page page) {
        m_url = url;
        m_path = path;
        m_type = page.getType().toString();
        m_id = id;
    }

    /**
     * A globally unique resource id.
     *
     * @return the id
     */
    public String getId() {
        return m_id;
    }

    /**
     * The relative path to the resource.
     *
     * @return the relative path or <code>null</code> if {@link #getUrl()} is given
     */
    public String getPath() {
        return m_path;
    }

    /**
     * The resource url.
     *
     * @return the url or <code>null</code> if {@link #getPath()} is given
     */
    public String getUrl() {
        return m_url;
    }

    /**
     * @return the resource type
     */
    public String getType() {
        return m_type;
    }

}
