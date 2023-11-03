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
 *   Oct 17, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.core.node.func;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Registry for the {@link NodeFunc} extension point.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 5.2
 */
public final class NodeFuncRegistry {

    private static final String EXT_POINT_ID = "org.knime.core.NodeFunc";

    private final Map<String, NodeFunc> m_nodeFuncsById;

    private static NodeFuncRegistry INSTANCE;

    private NodeFuncRegistry() {
        var extPoint = Platform.getExtensionRegistry().getExtensionPoint(EXT_POINT_ID);
        var nodeFuncs = new HashMap<String, NodeFunc>();
        for (var extension : extPoint.getExtensions()) {
            for (var element : extension.getConfigurationElements()) {
                createNodeFunc(element).ifPresent(n -> nodeFuncs.put(n.getApi().getName(), n));
            }
        }
        m_nodeFuncsById = Collections.unmodifiableMap(nodeFuncs);
    }

    private static Optional<NodeFunc> createNodeFunc(final IConfigurationElement element) {
        try {
            var nodeFunc = (NodeFunc)element.createExecutableExtension("impl");
            return Optional.of(nodeFunc);
        } catch (CoreException ex) {
            NodeLogger.getLogger(NodeFuncRegistry.class).error("Failed to create NodeFunc.", ex);
            return Optional.empty();
        }
    }

    /**
     * @param funcName name of the NodeFunc to retrieve
     * @return the NodeFunc if it is registered or {@link Optional#empty()}
     */
    public static Optional<NodeFunc> getNodeFunc(final String funcName) {
        return getInstance().getNodeFuncInternal(funcName);
    }

    /**
     * @return all registered NodeFuncs
     */
    public static Collection<NodeFunc> getAllNodeFuncs() {
        return getInstance().m_nodeFuncsById.values();
    }

    private Optional<NodeFunc> getNodeFuncInternal(final String funcName) {
        return Optional.ofNullable(m_nodeFuncsById.get(funcName));
    }

    private static NodeFuncRegistry getInstance() {
        if (INSTANCE == null) {
            initRegistry();
        }
        return INSTANCE;
    }

    private static synchronized void initRegistry() {
        if (INSTANCE == null) {
            INSTANCE = new NodeFuncRegistry();
        }
    }

}
