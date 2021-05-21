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
 *   May 20, 2021 (hornm): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.def.CoreToDefUtil;
import org.knime.core.workflow.def.ConfigMapDef;
import org.knime.core.workflow.def.NativeNodeDef;
import org.knime.core.workflow.def.NodeAndBundleInfoDef;

/**
 *
 * @author hornm
 */
public class DefNativeNodeContainerWrapper extends DefSingleNodeContainerWrapper implements NativeNodeDef {

    private final NativeNodeContainer m_nc;

    /**
     * @param nc
     */
    public DefNativeNodeContainerWrapper(final NativeNodeContainer nc) {
        super(nc);
        m_nc = nc;
    }

    @Override
    public String getKind() {
        return "NativeNode";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeAndBundleInfoDef getNodeAndBundleInfo() {
        return CoreToDefUtil.toNodeAndBundleInfoDef(m_nc.getNodeAndBundleInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigMapDef getFactorySettings() {
        NodeSettings s = new NodeSettings("factory_settings");
        m_nc.getNode().getFactory().saveAdditionalFactorySettings(s);
        try {
            return CoreToDefUtil.toConfigMapDef(s);
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_nc.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFactory() {
        return m_nc.getNode().getFactory().getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigMapDef getNodeCreationConfig() {
        NodeSettings creationConfig = m_nc.getNode().getCopyOfCreationConfig().map(c -> {
            NodeSettings s = new NodeSettings("creation_config");
            c.saveSettingsTo(s);
            return s;
        }).orElse(null);
        try {
            return CoreToDefUtil.toConfigMapDef(creationConfig);
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }

    }

}
