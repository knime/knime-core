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
 * Created on 28.05.2013 by thor
 */
package org.knime.core.node;

import org.apache.xmlbeans.XmlObject;
import org.knime.core.node.NodeFactory.NodeType;
import org.w3c.dom.Element;

/**
 * This abstract class describes the meta information about a node. It is used by the {@link NodeFactory}. Usually the
 * meta information is read from the XML files which accompany every node factory. But also other means of providing
 * node descriptions are possible.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.8
 */
public abstract class NodeDescription {
    private boolean m_deprecated;

    /**
     * Returns the path to the node's icon. The path should be either absolute (with the root being the root of the
     * classpath) or relative to the node factory's location.
     *
     * @return the path to the node icon, or <code>null</code> if no icon exists
     * @see NodeFactory#getIcon()
     */
    public abstract String getIconPath();

    /**
     * Returns a description for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port description or <code>null</code> if no description exists
     * @see NodeFactory#getInportDescription(int)
     */
    public abstract String getInportDescription(final int index);

    /**
     * Returns a name for an input port.
     *
     * @param index the index of the input port, starting at 0
     * @return an input port name or <code>null</code> if not name is known
     * @see NodeFactory#getInportName(int)
     */
    public abstract String getInportName(final int index);

    /**
     * Returns the name of the interactive view if such a view exists. Otherwise <code>null</code> is returned.
     *
     * @return name of the interactive view or <code>null</code>
     * @see NodeFactory#getInteractiveViewName()
     */
    public abstract String getInteractiveViewName();

    /**
     * Returns the name of this node.
     *
     * @return the node's name or <code>null</code> if no name is known
     * @see NodeFactory#getNodeName()
     */
    public abstract String getNodeName();

    /**
     * Returns a description for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port description or <code>null</code> if not description exists
     * @see NodeFactory#getOutportDescription(int)
     */
    public abstract String getOutportDescription(final int index);

    /**
     * Returns a name for an output port.
     *
     * @param index the index of the output port, starting at 0
     * @return an output port name or <code>null</code> if no name is known
     * @see NodeFactory#getOutportName(int)
     */
    public abstract String getOutportName(final int index);

    /**
     * Returns the type of the node.
     *
     * @return the node's type
     * @see NodeFactory#getType()
     */
    public abstract NodeType getType();

    /**
     * Returns the number of views which are listed in the meta information.
     *
     * @return the number of views
     */
    public abstract int getViewCount();

    /**
     * Returns a description for a view.
     *
     * @param index the index of the view, starting at 0
     * @return a view description or <code>null</code> if no description exists
     * @see NodeFactory#getViewDescription(int)
     */
    public abstract String getViewDescription(final int index);

    /**
     * Returns the name for this node's view at the given index.
     *
     * @param index the view index, starting at 0
     * @return the view's name or <code>null</code> if no name is known
     */
    public abstract String getViewName(final int index);

    /**
     * The XML description can be used with the
     * <code>NodeFactoryHTMLCreator</code> in order to get a converted HTML
     * description of it, which fits the overall KNIME HTML style.
     *
     * @return XML description of this node
     */
    public abstract Element getXMLDescription();

    /**
     * Utility function which strips the <tt>&lt;xml-fragment&gt;</tt> start and end tags from the strings returned by
     * <tt>xmlText()</tt>.
     *
     * @param xmlObject an xmlObject whose contents should be serialized into a string
     * @return a string representation of the XML contents
     */
    protected static String stripXmlFragment(final XmlObject xmlObject) {
        String contents = xmlObject.xmlText();
        int first = 0;
        while ((first < contents.length()) && (contents.charAt(first) != '>')) {
            first++;
        }
        int last = contents.length() - 1;
        while ((last >= 0) && (contents.charAt(last) != '<')) {
            last--;
        }

        if (last <= first) {
            return "";
        } else {
            return contents.substring(first + 1, last);
        }
    }

    /**
     * Flags this node as deprecated node. Subclasses should override this method and modify the XML description
     * accordingly.
     *
     * @param b <code>true</code> if this node is node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    protected void setIsDeprecated(final boolean b) {
        m_deprecated = b;
    }

    /**
     * Returns whether the node is deprecated.
     *
     * @return <code>true</code> if the node is deprecated, <code>false</code> otherwise
     * @since 3.0
     */
    public boolean isDeprecated() {
        return m_deprecated;
    }
}
