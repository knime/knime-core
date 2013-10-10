/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on 30.04.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.web;

import org.knime.core.node.web.WebResourceLocator.WebResourceType;

/**
 *
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @since 2.9
 */
public enum WebDependency {

    /** RequireJS
     * @since 2.9*/
    REQUIRE_JS(new WebResourceLocator[]{new WebResourceLocator("org.knime.core",
        "js-lib/requireJS/require.js", WebResourceType.JAVASCRIPT)}),

    /** JQuery in version 1.9.1. */
    JQUERY_1_9_1(new WebResourceLocator[]{new WebResourceLocator("org.knime.core",
        "js-lib/jQuery/jquery-1.9.1.min.js", WebResourceType.JAVASCRIPT)}),

    /** JQuery in version 1.10.2.
     * @since 2.9*/
    JQUERY_1_10_2(new WebResourceLocator[]{new WebResourceLocator("org.knime.core",
        "js-lib/jQuery/jquery-1.10.2.min.js", WebResourceType.JAVASCRIPT)}),

    /** D3 in version 3.2.8.
     * @since 2.9*/
    D3_3_2_8(new WebResourceLocator[]{new WebResourceLocator("org.knime.core",
        "js-lib/d3/d3.v3_2_8.min.js", WebResourceType.JAVASCRIPT)}),

    /** KNIME Javascript table in version 1.0.0.
     * @since 2.9 */
    KNIME_JS_TABLE_1_0_0(new WebResourceLocator[]{new WebResourceLocator("org.knime.core",
        "js-lib/knime/knime_table_1_0_0.js", WebResourceType.JAVASCRIPT)});

    private WebResourceLocator[] m_locators;

    /**  */
    private WebDependency(final WebResourceLocator[] locators) {
        m_locators = locators;
    }

    /**
     * @return The {@link WebResourceLocator}s for the dependency.
     */
    public WebResourceLocator[] getResourceLocators() {
        return m_locators;
    }

}
