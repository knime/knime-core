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
 * History
 *   31.03.2014 (Marcel Hanser): created
 */
package org.knime.base.node.preproc.pmml.missingval.utils;

import org.knime.core.api.node.NodeType;
import org.knime.core.node.NodeDescription;
import org.w3c.dom.Element;

/**
 * Delegates all method to the delegate object but the {@link #getXMLDescription()} one.
 *
 * @author Marcel Hanser, Alexander Fillbrunn
 * @since 2.12
 */
public final class NodeDescriptionXmlProxy extends NodeDescription {
    private NodeDescription m_delegate;

    private Element m_xmlDescription;

    /**
     * @param delegate to delegate most methods to
     * @param xmlDescription the xml description to inject to the NodeModel Html generation
     */
    public NodeDescriptionXmlProxy(final NodeDescription delegate, final Element xmlDescription) {
        super();
        m_delegate = delegate;
        m_xmlDescription = xmlDescription;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element getXMLDescription() {
        return m_xmlDescription;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIconPath() {
        return m_delegate.getIconPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportDescription(final int index) {
        return m_delegate.getInportDescription(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInportName(final int index) {
        return m_delegate.getInportName(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInteractiveViewName() {
        return m_delegate.getInteractiveViewName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeName() {
        return m_delegate.getNodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportDescription(final int index) {
        return m_delegate.getOutportDescription(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getOutportName(final int index) {
        return m_delegate.getOutportName(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeType getType() {
        return m_delegate.getType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewCount() {
        return m_delegate.getViewCount();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewDescription(final int index) {
        return m_delegate.getViewDescription(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewName(final int index) {
        return m_delegate.getViewName(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeprecated() {
        return m_delegate.isDeprecated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_delegate.toString();
    }
}
