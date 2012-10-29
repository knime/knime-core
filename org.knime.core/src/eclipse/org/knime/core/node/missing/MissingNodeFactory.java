/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * Created on Oct 26, 2012 by wiswedel
 */
package org.knime.core.node.missing;

import org.knime.core.node.DynamicNodeFactory;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeView;
import org.knime.core.node.config.ConfigWO;
import org.knime.core.node.port.PortType;
import org.knime.node2012.FullDescriptionDocument.FullDescription;
import org.knime.node2012.InPortDocument.InPort;
import org.knime.node2012.IntroDocument.Intro;
import org.knime.node2012.KnimeNodeDocument;
import org.knime.node2012.KnimeNodeDocument.KnimeNode;
import org.knime.node2012.OutPortDocument.OutPort;
import org.knime.node2012.PDocument.P;
import org.knime.node2012.PortsDocument.Ports;

/**
 * No API. Factory for missing node placeholder node.
 * @author wiswedel
 */
public class MissingNodeFactory extends DynamicNodeFactory<MissingNodeModel> {

    private final NodeAndBundleInformation m_nodeInfo;
    private final PortType[] m_inTypes;
    private final PortType[] m_outTypes;
    private final NodeSettingsRO m_additionalFactorySettings;
    private boolean m_copyInternDirForWorkflowVersionChange;

    /** Constructs factories. Copies as much as possible from original node settings (provided by persistor).
     * Args are all non-null.
     * @param nodeInfo ...
     * @param additionalFactorySettings ...
     * @param ins ...
     * @param outs ...
     */
    public MissingNodeFactory(final NodeAndBundleInformation nodeInfo, final NodeSettingsRO additionalFactorySettings,
                  final PortType[] ins, final PortType[] outs) {
        m_nodeInfo = nodeInfo;
        m_additionalFactorySettings = additionalFactorySettings;
        m_inTypes = ins;
        m_outTypes = outs;
    }

    /**
     * Set to true by persistor if the loaded workflow was saved in an old format so that a new save would convert all
     * nodes into the new format. The node will then copy the content of the "internal" dir into temp and save it
     * the next time the flow is saved. Most of times this flag is not set.
     *
     * @param value the copyInternDirForWorkflowVersionChange to set
     */
    public void setCopyInternDirForWorkflowVersionChange(final boolean value) {
        m_copyInternDirForWorkflowVersionChange = value;
    }

    /** {@inheritDoc} */
    @Override
    public MissingNodeModel createNodeModel() {
        return new MissingNodeModel(m_nodeInfo, m_inTypes, m_outTypes, m_copyInternDirForWorkflowVersionChange);
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<MissingNodeModel> createNodeView(final int viewIndex, final MissingNodeModel nodeModel) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new MissingNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAdditionalFactorySettings(final ConfigWO config) {
        if (m_additionalFactorySettings != null) {
            m_additionalFactorySettings.copyTo(config);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void addNodeDescription(final KnimeNodeDocument doc) {
        KnimeNode node = doc.addNewKnimeNode();
        node.setIcon("./missing.png");
        node.setType(KnimeNode.Type.UNKNOWN);
        node.setName("MISSING " + m_nodeInfo.getNodeNameNotNull());

        String shortDescription = "Placeholder node for missing \"" + m_nodeInfo.getNodeNameNotNull() + "\".";
        node.setShortDescription(shortDescription);

        FullDescription fullDesc = node.addNewFullDescription();
        Intro intro = fullDesc.addNewIntro();
        P p = intro.addNewP();
        p.newCursor().setTextValue(shortDescription);
        p = intro.addNewP();
        p.newCursor().setTextValue(m_nodeInfo.getErrorMessageWhenNodeIsMissing());

        Ports ports = node.addNewPorts();
        for (int i = 0; i < m_inTypes.length; i++) {
            InPort inPort = ports.addNewInPort();
            inPort.setIndex(i);
            inPort.setName("Port " + i);
            inPort.newCursor().setTextValue("Port guessed from the workflow connection table.");
        }
        for (int i = 0; i < m_outTypes.length; i++) {
            OutPort outPort = ports.addNewOutPort();
            outPort.setIndex(i);
            outPort.setName("Port " + i);
            outPort.newCursor().setTextValue("Port guessed from the workflow connection table.");
        }
    }

}
