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
 * Created on 06.05.2013 by Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 */
package org.knime.core.node.interactive;

/**
 * Default {@lin WebViewTemplate} implementation.
 * 
 * @author Christian Albrecht, KNIME.com AG, Zurich, Switzerland
 * @since 2.8
 */
public final class DefaultWebViewTemplate implements WebViewTemplate {

    private final WebResourceLocator[] m_webResources;
    private final WebDependency[] m_dependencies;
    private final String m_namespace;

    /**
     * @param webResources An array of {@link WebResourceLocator}, which is the actual implementation of the view.
     * These can be Javascript, CSS or other files.
     * @param dependencies An array of {@link WebDependency}, which are the Javascript dependencies the view uses.
     * @param namespace An optional namespace, which is prepended to all method calls of the view implementation.
     *
     */
    public DefaultWebViewTemplate(final WebResourceLocator[] webResources,
                                  final WebDependency[] dependencies, final String namespace) {
        this.m_webResources = webResources;
        this.m_dependencies = dependencies;
        this.m_namespace = namespace;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebResourceLocator[] getWebResources() {
        return m_webResources;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WebDependency[] getDependencies() {
        return m_dependencies;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace() {
        return m_namespace;
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     * {@inheritDoc}
     */
    @Override
    public String getInitMethodName() {
        return "init";
    }

    /**
     * @noreference This method is not intended to be referenced by clients.
     * {@inheritDoc}
     */
    @Override
    public String getPullViewContentMethodName() {
        return "pullViewContent";
    }

}
