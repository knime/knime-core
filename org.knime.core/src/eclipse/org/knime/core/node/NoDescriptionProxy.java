/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 29.05.2013 by thor
 */
package org.knime.core.node;


import org.knime.core.def.node.NodeType;
import org.w3c.dom.Element;

/**
 * Implementation of {@link NodeDescription} for missing node descriptions. This can be used if no XML files are
 * available for a node. It returns <code>null</code> for almost all methods.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public final class NoDescriptionProxy extends NodeDescription {
    private final String m_nodeName;

    /**
     * Creates a new empty node description for the given node factory.
     *
     * @param factoryClass the node factory's class
     */
    public NoDescriptionProxy(@SuppressWarnings("rawtypes") final Class<? extends NodeFactory> factoryClass) {
        m_nodeName = factoryClass.getSimpleName().replaceFirst("(?:[nN]ode)?[fF]actory$", "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIconPath() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportDescription(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportName(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeName() {
        return m_nodeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportDescription(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportName(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        return NodeType.Unknown;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewCount() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewDescription(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewName(final int index) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        return null;
    }
}
