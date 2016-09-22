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
 * -------------------------------------------------------------------
 *
 * History
 *   21.06.2012 (Peter Ohl): created
 */
package org.knime.workbench.editor2.meta;

import java.util.List;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.Wizard;
import org.knime.core.api.node.port.MetaPortInfo;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.commands.ReconfigureMetaNodeCommand;

/**
 * One page wizard to reconfigure a metanode by changing the number and type, order or number of in
 * and out ports.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 */
public class ReconfigureMetaNodeWizard extends Wizard {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ReconfigureMetaNodeWizard.class);

    private AddMetaNodePage m_addPage;

    private final WorkflowManager m_metaNode;
    private final EditPartViewer m_viewer;
    private final SubNodeContainer m_subNode;

    /**
     * @param viewer The viewer
     * @param metaNode The metanode
     */
    public ReconfigureMetaNodeWizard(final EditPartViewer viewer, final WorkflowManager metaNode) {
        super();
        m_metaNode = metaNode;
        m_subNode = null;
        m_viewer = viewer;
        setHelpAvailable(false);
    }

    /**
     * @param viewer The viewer
     * @param subNode The sub node
     */
    public ReconfigureMetaNodeWizard(final EditPartViewer viewer, final SubNodeContainer subNode) {
        super();
        m_metaNode = null;
        m_subNode = subNode;
        m_viewer = viewer;
        setHelpAvailable(false);
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        String name = m_metaNode != null ? "Metanode" : "Wrapped Metanode";
        setWindowTitle("Reconfigure " + name + " Wizard");
        setDefaultPageImageDescriptor(ImageDescriptor.createFromImage(
                ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/meta_node_wizard2.png")));
        m_addPage = new AddMetaNodePage("Change the " + name + " configuration");
        if (m_metaNode != null) {
            m_addPage.setMetaNode(m_metaNode);
        } else {
            m_addPage.setSubNode(m_subNode);
        }
        m_addPage.setTemplate(null);
        addPage(m_addPage);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_addPage.isPageComplete();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        NodeContainer node = m_metaNode != null ? m_metaNode : m_subNode;
        List<MetaPortInfo> inPorts = m_addPage.getInPorts();
        List<MetaPortInfo> outPorts = m_addPage.getOutPorts();
        String name = m_addPage.getMetaNodeName();

        // fix the indicies
        for (int i = 0; i < inPorts.size(); i++) {
            m_addPage.replaceInPortAtIndex(i, MetaPortInfo.builder(inPorts.get(i)).setNewIndex(i).build());
        }
        for (int i = 0; i < outPorts.size(); i++) {
            m_addPage.replaceOutPortAtIndex(i, MetaPortInfo.builder(outPorts.get(i)).setNewIndex(i).build());
        }

        inPorts = m_addPage.getInPorts();
        outPorts = m_addPage.getOutPorts();

        // determine what has changed
        boolean inPortChanges = node.getNrInPorts() != inPorts.size();
        for (MetaPortInfo inInfo : inPorts) {
            // new port types would create a new info object - which would have an unspecified old index -> change
            inPortChanges |= inInfo.getOldIndex() != inInfo.getNewIndex();
        }
        boolean outPortChanges = node.getNrOutPorts() != outPorts.size();
        for (MetaPortInfo outInfo : outPorts) {
            // new port types would create a new info object - which would have an unspecified old index -> change
            outPortChanges |= outInfo.getOldIndex() != outInfo.getNewIndex();
        }
        boolean nameChange = !node.getName().equals(name);
        StringBuilder infoStr = new StringBuilder();
        if (inPortChanges) {
            infoStr.append("the input ports - ");
        }
        if (outPortChanges) {
            infoStr.append("the output ports - ");
        }
        if (nameChange) {
            infoStr.append("the name - ");
        }
        if (infoStr.length() == 0) {
            LOGGER.info("No changes made in the configuration wizard. Nothing to do.");
            return true;
        }
        infoStr.insert(0, "Changing - ");
        infoStr.append("of MetaNode " + node.getID());
        LOGGER.info(infoStr);

        ReconfigureMetaNodeCommand reconfCmd = new ReconfigureMetaNodeCommand(node.getParent(),
                node.getID());
        if (nameChange) {
            reconfCmd.setNewName(name);
        }
        if (inPortChanges) {
            reconfCmd.setNewInPorts(inPorts);
        }
        if (outPortChanges) {
            reconfCmd.setNewOutPorts(outPorts);
        }
        m_viewer.getEditDomain().getCommandStack().execute(reconfCmd);
        return true;
    }

}
