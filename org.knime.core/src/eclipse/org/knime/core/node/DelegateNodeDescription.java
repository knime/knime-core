/*
 * ------------------------------------------------------------------------
 *
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
 *   Jul 3, 2014 (wiswedel): created
 */
package org.knime.core.node;

import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.util.CheckUtils;
import org.w3c.dom.Element;

/**
 * An adapter on {@link NodeDescription} that allows selected methods to be overwritten. If not overwritten it
 * delegates to node description passed in the constructor.
 *
 * <p>This class is used for {@link DynamicNodeFactory} instances that are "almost" static but have, for instance
 * a runtime variable port number.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DelegateNodeDescription extends NodeDescription {

    private final NodeDescription m_delegate;

    /** New instance with delegate.
     * @param delegate To delegate to, not null.
     */
    public DelegateNodeDescription(final NodeDescription delegate) {
        CheckUtils.checkArgumentNotNull(delegate, "Argument must not be null");
        m_delegate = delegate;
    }

    /** {@inheritDoc} */
    @Override
    public String getIconPath() {
        return m_delegate.getIconPath();
    }

    /** {@inheritDoc} */
    @Override
    public String getInportDescription(final int index) {
        return m_delegate.getInportDescription(index);
    }

    /** {@inheritDoc} */
    @Override
    public String getInportName(final int index) {
        return m_delegate.getInportName(index);
    }

    /** {@inheritDoc} */
    @Override
    public String getInteractiveViewName() {
        return m_delegate.getInteractiveViewName();
    }

    /** {@inheritDoc} */
    @Override
    public String getNodeName() {
        return m_delegate.getNodeName();
    }

    /** {@inheritDoc} */
    @Override
    public String getOutportDescription(final int index) {
        return m_delegate.getOutportDescription(index);
    }

    /** {@inheritDoc} */
    @Override
    public String getOutportName(final int index) {
        return m_delegate.getOutportName(index);
    }

    /** {@inheritDoc} */
    @Override
    public NodeType getType() {
        return m_delegate.getType();
    }

    /** {@inheritDoc} */
    @Override
    public int getViewCount() {
        return m_delegate.getViewCount();
    }

    /** {@inheritDoc} */
    @Override
    public String getViewDescription(final int index) {
        return m_delegate.getViewDescription(index);
    }

    /** {@inheritDoc} */
    @Override
    public String getViewName(final int index) {
        return m_delegate.getViewName(index);
    }

    /** {@inheritDoc} */
    @Override
    public Element getXMLDescription() {
        return m_delegate.getXMLDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeprecated() {
        return m_delegate.isDeprecated();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_delegate.toString();
    }
}
