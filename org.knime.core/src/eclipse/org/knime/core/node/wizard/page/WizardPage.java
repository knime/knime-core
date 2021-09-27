package org.knime.core.node.wizard.page;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.workflow.CompositeViewController;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.WizardExecutionController;

/**
 * Result value of {@link WizardExecutionController#getCurrentWizardPage()} and
 * {@link CompositeViewController#getWizardPage()}.
 *
 * @since 4.5
 */
public final class WizardPage {

    private final NodeID m_pageNodeID;

    private final Map<NodeIDSuffix, NativeNodeContainer> m_pageMap;

    private Map<NodeIDSuffix, WizardPage> m_nestedContent;

    private Map<NodeIDSuffix, WizardPageNodeInfo> m_infoMap;

    private final String m_layoutInfo;

    private final List<HiLiteTranslator> m_hiLiteTranslators;

    private final List<HiLiteManager> m_hiliteManagers;

    /**
     * @param pageNodeID
     * @param pageMap
     * @param layoutInfo
     */
    WizardPage(final NodeID pageNodeID, final Map<NodeIDSuffix, NativeNodeContainer> pageMap,
        final String layoutInfo, final List<HiLiteTranslator> hiLiteTranslators,
        final List<HiLiteManager> hiLiteManagers) {
        m_pageNodeID = pageNodeID;
        m_pageMap = pageMap;
        m_infoMap = new LinkedHashMap<>();
        m_layoutInfo = layoutInfo;
        m_hiLiteTranslators = hiLiteTranslators;
        m_hiliteManagers = hiLiteManagers;
    }

    /**
     * @return the pageNodeID
     * @since 3.3
     */
    public NodeID getPageNodeID() {
        return m_pageNodeID;
    }

    /**
     * @return the pageMap
     */
    public Map<NodeIDSuffix, NativeNodeContainer> getPageMap() {
        return m_pageMap;
    }

    /**
     * @return the nestedContent
     * @since 3.7
     */
    public Map<NodeIDSuffix, WizardPage> getNestedContent() {
        return m_nestedContent;
    }

    /**
     * @param nestedContent the nestedContent to set
     * @since 3.7
     */
    public void setNestedContent(final Map<NodeIDSuffix, WizardPage> nestedContent) {
        m_nestedContent = nestedContent;
    }

    /**
     * @return the layoutInfo
     * @since 3.1
     */
    public String getLayoutInfo() {
        return m_layoutInfo;
    }

    /**
     * @return the hiLiteTranslators
     * @since 3.4
     */
    public List<HiLiteTranslator> getHiLiteTranslators() {
        return m_hiLiteTranslators;
    }

    /**
     * @return the hiliteManagers
     * @since 3.4
     */
    public List<HiLiteManager> getHiliteManagers() {
        return m_hiliteManagers;
    }

    /**
     * @param infoMap the infoMap to set
     * @since 3.5
     */
    public void setInfoMap(final Map<NodeIDSuffix, WizardPageNodeInfo> infoMap) {
        m_infoMap = infoMap;
    }

    /**
     * @return the infoMap
     * @since 3.5
     */
    public Map<NodeIDSuffix, WizardPageNodeInfo> getInfoMap() {
        return m_infoMap;
    }

    /**
     * Info object for individual nodes, containing e.g. node state
     * and possible warn/error messages.
     * @since 3.5
     */
    public static final class WizardPageNodeInfo {

        private String m_nodeName;
        private String m_nodeAnnotation;
        private NodeContainerState m_nodeState;
        private NodeMessage m_nodeMessage;

        /**
         * @return the nodeName
         */
        public String getNodeName() {
            return m_nodeName;
        }

        /**
         * @param nodeName the nodeName to set
         */
        public void setNodeName(final String nodeName) {
            m_nodeName = nodeName;
        }

        /**
         * @return the nodeAnnotation
         */
        public String getNodeAnnotation() {
            return m_nodeAnnotation;
        }

        /**
         * @param nodeAnnotation the nodeAnnotation to set
         */
        public void setNodeAnnotation(final String nodeAnnotation) {
            m_nodeAnnotation = nodeAnnotation;
        }

        /**
         * @return the nodeState
         */
        public NodeContainerState getNodeState() {
            return m_nodeState;
        }

        /**
         * @param nodeState the nodeState to set
         */
        public void setNodeState(final NodeContainerState nodeState) {
            m_nodeState = nodeState;
        }

        /**
         * @return the nodeMessage
         */
        public NodeMessage getNodeMessage() {
            return m_nodeMessage;
        }

        /**
         * @param nodeMessage the nodeMessage to set
         */
        public void setNodeMessage(final NodeMessage nodeMessage) {
            m_nodeMessage = nodeMessage;
        }

    }

}