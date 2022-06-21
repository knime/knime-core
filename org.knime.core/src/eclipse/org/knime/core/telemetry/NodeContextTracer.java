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
 *   21 Jun 2022 (carlwitt): created
 */
package org.knime.core.telemetry;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;

import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;

/**
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class NodeContextTracer {

    /**
     *
     */
    public static final String ROOT_CONTEXT = "org.knime.core.NodeContext.ROOT";

    /**
     * Extract info from org.knime.core specific objects and add it to the given span.
     * @param nes the span to add attributes to
     * @param contextObject provides, e.g., node name, factory, etc.
     */
    public static void addContextInfo(final NodeExecutionSpan nes, final Object contextObject) {
        if(contextObject instanceof NodeContainer) {
            NodeContainer nodeContainer = (NodeContainer)contextObject;
            nes.setNodeContext(getNodeName(nodeContainer));
            getNodeFactory(nodeContainer).ifPresent(nes::setNodeFactory);
            nes.setNodeId(nodeContainer.getID().toString());
        } else if (contextObject != null){
            nes.setNodeContext(contextObject.toString());
        } else {
            nes.setNodeContext(ROOT_CONTEXT);
        }
    }

    private static Optional<String> getNodeFactory(final NodeContainer nodeContainer) {
        if (nodeContainer instanceof NativeNodeContainer) {
            return Optional.of(((NativeNodeContainer)nodeContainer).getNode().getFactory().getClass().getName());
        } else {
            return Optional.empty();
        }
    }

    private static String getNodeName(final NodeContainer nodeContainer) {
        if (nodeContainer instanceof NativeNodeContainer) {
            return ((NativeNodeContainer)nodeContainer).getNode().getName();
        } else if (nodeContainer instanceof SubNodeContainer) {
            return nodeContainer.getClass().getName();
        } else {
            return "NodeContainer";
        }
    }

    /**
     * @param data
     * @param keyGenerator
     * @return
     */
    public static AttributesBuilder rowCountsToAttributes(final PortObject[] data,
        final Function<Integer, String> keyGenerator) {
        var attributesBuilder = Attributes.builder();
        for (int i = 0; i < data.length; i++) {
            final int portIndex = i;
            var optionalRows = NodeContextTracer.numRows(data[i]);
            optionalRows.ifPresent(rows -> attributesBuilder.put(keyGenerator.apply(portIndex), rows));
        }
        return attributesBuilder;
    }

    /**
     * @param portObject the port object to inspect for number of rows
     * @return the number of rows in the port object, if it holds a {@link BufferedDataTable}
     */
    public static OptionalLong numRows(final PortObject portObject) {
        if (portObject instanceof BufferedDataTable) {
            return OptionalLong.of(((BufferedDataTable)portObject).size());
        }
        return OptionalLong.empty();
    }

}
