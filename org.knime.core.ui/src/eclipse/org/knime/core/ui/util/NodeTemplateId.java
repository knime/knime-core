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
 *   Oct 25, 2022 (hornm): created
 */
package org.knime.core.ui.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.Function;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.missing.MissingNodeFactory;
import org.knime.core.util.workflowalizer.NodeAndBundleInformation;

/**
 * Utility methods creating unique node template IDs, which are especially helpful for the {@NodeRecommendationManager}
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @author Kai Franze, KNIME GmbH
 */
public final class NodeTemplateId {

    private static final String NODE_NAME_SEP = "#";

    private NodeTemplateId() {
        // utility class
    }

    /**
     * Finds node instances for different node template ID variants
     *
     * @param <T> The return type of the {@code callee} function
     * @param nodeFactoryClassName
     * @param nodeName
     * @param callee A function to lookup nodes by node template ID
     * @return The node instance if found using the {@code callee}
     */
    public static <T> T callWithNodeTemplateIdVariants(final String nodeFactoryClassName, final String nodeName,
        final Function<String, T> callee) {
        T res = callee.apply(nodeFactoryClassName);
        if (res == null) {
            res = callee.apply(ofDynamicNodeFactory(nodeFactoryClassName, nodeName));
        }
        return res;
    }

    /**
     * Creates a node template ID based on the node type
     *
     * @param nodeFactory The node factory class
     * @return The node template ID string
     */
    public static String of(final NodeFactory<? extends NodeModel> nodeFactory) {
        return of(nodeFactory, false);
    }

    /**
     * Creates a node template ID based on the node type
     *
     * @param nodeFactory The node factory class
     * @param encodeNodeNameWithURLEscapeCodes whether to escape the node names with URL escape codes (only relevant in
     *            case of dynamic node factories)
     * @return The node template ID string
     */
    public static String of(final NodeFactory<? extends NodeModel> nodeFactory,
        final boolean encodeNodeNameWithURLEscapeCodes) {
        String nodeFactoryClassName;
        if (nodeFactory instanceof MissingNodeFactory) {
            NodeAndBundleInformation nodeInfo = ((MissingNodeFactory)nodeFactory).getNodeAndBundleInfo();
            nodeFactoryClassName =
                nodeInfo.getFactoryClass().orElseGet(() -> "unknown_missing_node_factory_" + UUID.randomUUID());
        } else {
            nodeFactoryClassName = nodeFactory.getClass().getName();
        }

        if (nodeFactory instanceof DynamicNodeFactory) {
            var nodeName = nodeFactory.getNodeName();
            if (encodeNodeNameWithURLEscapeCodes) {
                nodeName = URLEncoder.encode(nodeName, StandardCharsets.UTF_8);
            }
            return ofDynamicNodeFactory(nodeFactoryClassName, nodeName);
        } else {
            return nodeFactoryClassName;
        }

    }

    /**
     * Creates a node template ID for nodes with a {@link DynamicNodeFactory}
     *
     * @param nodeFactoryClassName
     * @param nodeName
     * @return The node template ID string
     */
    public static String ofDynamicNodeFactory(final String nodeFactoryClassName, final String nodeName) {
        return nodeFactoryClassName + NODE_NAME_SEP + nodeName;
    }

}
