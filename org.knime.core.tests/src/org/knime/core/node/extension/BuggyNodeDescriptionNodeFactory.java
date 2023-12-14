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
 *   29 Nov 2023 (carlwitt): created
 */
package org.knime.core.node.extension;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * NXT-2233: Test node factory that returns a node description that throws exceptions on all methods except those that
 * would currently prevent the node from being instantiated (as broken as possible). This must not interfere with node
 * repository creation, e.g., when computing node specifications.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class BuggyNodeDescriptionNodeFactory extends NodeFactory<BuggyNodeDescriptionNodeModel> {
    @Override
    public BuggyNodeDescriptionNodeModel createNodeModel() {
        // but node description provides only information for one input and output port
        return new BuggyNodeDescriptionNodeModel();
    }

    @Override
    protected int getNrNodeViews() {
        // but node description has only one view element
        return 2;
    }

    @Override
    public NodeView<BuggyNodeDescriptionNodeModel> createNodeView(final int viewIndex,
        final BuggyNodeDescriptionNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return false;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        final var defaultNodeDescription = super.createNodeDescription();
        return new NodeDescription() {

            @Override
            public Element getXMLDescription() {
                // the node factory cannot be created if this throws an exception
                return defaultNodeDescription.getXMLDescription();
            }

            @Override
            public String getViewName(final int index) {
                throw new IllegalStateException("getViewName of this test node description always fails");
            }

            @Override
            public String getViewDescription(final int index) {
                // the node instance cannot be created if this throws an exception
                return null;
            }

            @Override
            public int getViewCount() {
                // but node description has only one view element
                return 2;
            }

            @Override
            public NodeType getType() {
                throw new IllegalStateException("getType of this test node description always fails");
            }

            @Override
            public String getOutportName(final int index) {
                // the node instance cannot be created if this throws an exception
                return null;
            }

            @Override
            public String getOutportDescription(final int index) {
                throw new IllegalStateException("getOutportDescription of this test node description always fails");
            }

            @Override
            public String getNodeName() {
                // the node factory cannot be created if this throws an exception
                return defaultNodeDescription.getNodeName();
            }

            @Override
            public String getInteractiveViewName() {
                throw new IllegalStateException("getInteractiveViewName of this test node description always fails");
            }

            @Override
            public String getInportName(final int index) {
                // the node instance cannot be created if this throws an exception
                return null;
            }

            @Override
            public String getInportDescription(final int index) {
                throw new IllegalStateException("getInportDescription of this test node description always fails");
            }

            @Override
            public String getIconPath() {
                // the node factory cannot be created if this throws an exception
                return defaultNodeDescription.getIconPath();
            }
        };
    }

}