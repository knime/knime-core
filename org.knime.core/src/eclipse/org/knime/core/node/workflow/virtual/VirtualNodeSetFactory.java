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
 *   Mar 20, 2020 (hornm): created
 */
package org.knime.core.node.workflow.virtual;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectInNodeFactory;
import org.knime.core.node.workflow.virtual.parchunk.VirtualParallelizedChunkPortObjectOutNodeFactory;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeInputNodeFactory;
import org.knime.core.node.workflow.virtual.subnode.VirtualSubNodeOutputNodeFactory;

/**
 * Contains the virtual inputs and outputs for components and parallel chunk 'executables'.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @noreference This class is not intended to be referenced by clients.
 */
public final class VirtualNodeSetFactory implements NodeSetFactory {

    private static Map<String, Class<? extends NodeFactory<? extends NodeModel>>> VIRTUAL_NODES;

    private static Map<String, Class<? extends NodeFactory<? extends NodeModel>>> getVirtualNodes() {
        if (VIRTUAL_NODES == null) {
            VIRTUAL_NODES = new HashMap<>();
            addClass(VirtualSubNodeInputNodeFactory.class);
            addClass(VirtualSubNodeOutputNodeFactory.class);
            addClass(VirtualParallelizedChunkPortObjectInNodeFactory.class);
            addClass(VirtualParallelizedChunkPortObjectOutNodeFactory.class);
            addClass(DefaultVirtualPortObjectInNodeFactory.class);
            addClass(DefaultVirtualPortObjectOutNodeFactory.class);
            addClass(VirtualPortObjectInNodeFactory.class);
            addClass(VirtualPortObjectOutNodeFactory.class);
        }
        return VIRTUAL_NODES;
    }

    private static void addClass(final Class<? extends NodeFactory<? extends NodeModel>> clazz) {
        VIRTUAL_NODES.put(clazz.getCanonicalName(), clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> getNodeFactoryIds() {
        return getVirtualNodes().keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends NodeFactory<? extends NodeModel>> getNodeFactory(final String id) {
        return getVirtualNodes().get(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategoryPath(final String id) {
        //nodes are hidden -> no category necessary
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAfterID(final String id) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigRO getAdditionalSettings(final String id) {
        return null;
    }

    @Override
    public boolean isHidden() {
        return true;
    }

}
