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
import org.knime.shared.workflow.def.ConfigMapDef;
import org.knime.shared.workflow.def.FilestoreDef;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.def.VendorDef;
import org.knime.shared.workflow.storage.multidir.util.LoaderUtils;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * Provides a {@link NativeNodeDef} view on a native node (a node with a factory) in a workflow.
 *
 * @author hornm
 * @author Carl Witt, KNIME GmbH, Berlin, Germany
 */
public class NativeNodeContainerToDefAdapter extends SingleNodeContainerToDefAdapter implements NativeNodeDef {

    private final NativeNodeContainer m_nc;

    /**
     * @param nc
     * @param passwordHandler
     */
    public NativeNodeContainerToDefAdapter(final NativeNodeContainer nc, final PasswordRedactor passwordHandler) {
        super(nc, passwordHandler);
        m_nc = nc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigMapDef getFactorySettings() {
        NodeSettings s = new NodeSettings("factory_settings");
        m_nc.getNode().getFactory().saveAdditionalFactorySettings(s);
        try {
            return LoaderUtils.toConfigMapDef(s, PasswordRedactor.asNull());
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFactory() {
        // AP-22086: Return the node factory name from the bundle information
        //           If the node is missing, this still returns the original factory name instead of MissingNodeFactory
        return m_nc.getNodeAndBundleInformation().getFactoryClassNotNull();
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
            return LoaderUtils.toConfigMapDef(creationConfig, PasswordRedactor.asNull());
        } catch (InvalidSettingsException ex) {
            // TODO
            throw new RuntimeException(ex);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNodeName() {
        // AP-22086: Return the node name from the bundle information
        //           If the node is missing, this still returns the original node name instead of "MISSING ..."
        return m_nc.getNodeAndBundleInformation().getNodeNameNotNull();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VendorDef getFeature() {
        return CoreToDefUtil.toFeatureVendorDef(m_nc.getNodeAndBundleInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VendorDef getBundle() {
        return CoreToDefUtil.toBundleVendorDef(m_nc.getNodeAndBundleInformation());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilestoreDef getFilestore() {
        // TODO implement;
        return null; //new FilestoreDefBuilder()//
//                .setId()//
//                .setLocation()//
//                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeTypeEnum getNodeType() {
        return NodeTypeEnum.NATIVENODE;
    }

}
