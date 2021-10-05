package org.knime.core.node.wizard.page;

import java.util.List;
import java.util.Map;

import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.workflow.CompositeViewController;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeID.NodeIDSuffix;
import org.knime.core.node.workflow.WizardExecutionController;

/**
 * Result value of {@link WizardExecutionController#getCurrentWizardPage()} and
 * {@link CompositeViewController#getWizardPage()}.
 *
 * @noreference This class is not intended to be referenced by clients.
 *
 * @since 4.5
 */
public final class WizardPage {

    private final NodeID m_pageNodeID;

    private final Map<NodeIDSuffix, NativeNodeContainer> m_pageMap;

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

}