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
 * ---------------------------------------------------------------------
 *
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.api.node.workflow.ConnectionUIInformation;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.filestore.internal.WorkflowFileStoreHandlerRepository;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeAndBundleInformation;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FileWorkflowPersistor.LoadVersion;
import org.knime.core.node.workflow.WorkflowManager.AuthorInformation;

/**
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public interface WorkflowPersistor extends NodeContainerPersistor {

    /** Key for nodes. */
    public static final String KEY_NODES = "nodes";

    /** Key for connections. */
    public static final String KEY_CONNECTIONS = "connections";

    public static final String KEY_UI_INFORMATION = "extraInfoClassName";

    /** Key for this node's internal ID. */
    static final String KEY_ID = "id";

    /** Identifier for KNIME workflows when saved to disc. */
    public static final String WORKFLOW_FILE = "workflow.knime";

    /** Identifier for KNIME meta mode templates when saved to disc. */
    public static final String TEMPLATE_FILE = "template.knime";

    /** Identifier for KNIME workflows SVG export when saved to disc.
     * @since 2.8 */
    public static final String SVG_WORKFLOW_FILE = "workflow.svg";

    /** Identifier for KNIME templates SVG export when saved to disc.
     * @since 2.8 */
    public static final String SVG_TEMPLATE_FILE = "template.svg";

    /** File used to signal that workflow was saved in usual manner. It will
     * always be present in the workflow directory unless the workflow is
     * exported with the "exclude data" flag being set. */
    public static final String SAVED_WITH_DATA_FILE = ".savedWithData";

    /** Constant for the meta info file name. */
    public static final String METAINFO_FILE = "workflowset.meta";

    /** @return The version of the workflow or template being loaded. */
    public LoadVersion getLoadVersion();

    /** @return if the persistor represent a workflow project.
     * @since 2.6 */
    boolean isProject();

    /**
     * @return The workflow context for projects (only reasonable if §{@link #isProject()}).
     * @since 2.8
     */
    WorkflowContext getWorkflowContext();

    /** The map of node ID suffix to persistor.
     * @return The persistor map. */
    Map<Integer, ? extends NodeContainerPersistor> getNodeLoaderMap();

    /** The connections between the nodes that are loaded. The set only contains
     * connections between nodes of the loader map, it does not contain
     * any other connection (see {@link #getAdditionalConnectionSet()}).
     * @return The connections */
    Set<ConnectionContainerTemplate> getConnectionSet();

    /** Get additional connections. These are connections that connect nodes
     * that were loaded to nodes that are already in the workflow. In most
     * cases this list is empty except for undo commands that restore deleted
     * nodes (and their connections to other nodes in the workflow).
     * @return Such a set (often empty but never null). */
    Set<ConnectionContainerTemplate> getAdditionalConnectionSet();

    HashMap<Integer, ContainerTable> getGlobalTableRepository();

    /** The repository of file store handlers.
     * @return
     * @since 2.6*/
    WorkflowFileStoreHandlerRepository getFileStoreHandlerRepository();

    String getName();

    /** @return cipher associated with the metanode/workflow, often just
     * {@link WorkflowCipher#NULL_CIPHER}, never null. */
    WorkflowCipher getWorkflowCipher();

    /** Get the workflow variables associated with this metanode/workflow.
     * This method must not return null (but possibly an empty list). The result
     * may be unmodifiable.
     * @return The workflow variables.
     */
    List<FlowVariable> getWorkflowVariables();

    /** Get (non-null) map of credentials.
     * @return The credentials defined on this metanode.
     */
    List<Credentials> getCredentials();

    /** @return (non-mull) map of annotations. */
    List<WorkflowAnnotation> getWorkflowAnnotations();

    /** NodeSettings used to save the wizard state (usually null). */
    NodeSettingsRO getWizardExecutionControllerState();

    /** List of node directories, whose corresponding nodes failed
     * to load. These directories will be deleted in the next save invocation.
     * @return List of obsolete node directories
     */
    List<ReferencedFile> getObsoleteNodeDirectories();

    WorkflowPortTemplate[] getInPortTemplates();

    WorkflowPortTemplate[] getOutPortTemplates();

    /** Get UI information for workflow input ports.
     * @return the ui info or null if not set.
     */
    public NodeUIInformation getInPortsBarUIInfo();

    /** Get UI information for workflow output ports.
     * @return the ui info or null if not set.
     */
    public NodeUIInformation getOutPortsBarUIInfo();

    /** @return editor UI infos.
     * @since 2.6  */
    public EditorUIInformation getEditorUIInformation();

    /** @return the mustWarnOnDataLoadError */
    public boolean mustWarnOnDataLoadError();

    /** template information associated with the workflow, e.g. whether it
     * is linking to same central master metanode.
     * @return The template info
     */
    public MetaNodeTemplateInformation getTemplateInformation();

    /** The workflow author information or null for metanodes (or copied WFMs, which have not been saved yet).
     * @return ...
     * @since 2.8
     */
    public AuthorInformation getAuthorInformation();

    /** Open decryption stream for locked metanodes. Implementations will
     * also call the decipher method on their parent workflow persistors.
     * @param input The input to decipher.
     * @return The decipherd input, mostly just the input.
     * @throws IOException If that fails, e.g. crypto init fails
     */
    public InputStream decipherInput(
            final InputStream input) throws IOException;

    /** Called after all nodes have been instantiated but no configure storm is launched. Used to sneak some more
     * data into some nodes (sub node container to push itself into the virtual in/out)
     * @param wfm The workflow itself, as returned by {@link #getNodeContainer(WorkflowManager, NodeID)}.
     * @param loadResult TODO*/
    void postLoad(WorkflowManager wfm, LoadResult loadResult);

    /** Helper class representing a connection. */
    static class ConnectionContainerTemplate {
        private int m_sourceSuffix;
        private int m_sourcePort;
        private int m_destSuffix;
        private int m_destPort;
        private boolean m_isDeletable;
        private final ConnectionUIInformation m_uiInfo;

        /**
         * Creates new template connection.
         * @param source ID Suffix of source node
         * @param sourcePort Source port
         * @param dest ID Suffix of destination node
         * @param destPort Destination port
         * @param isDeletable whether connection is deletable
         * @param uiInfo Corresponding UI info, maybe null
         */
        ConnectionContainerTemplate(final int source,
                final int sourcePort, final int dest, final int destPort,
                final boolean isDeletable,
                final ConnectionUIInformation uiInfo) {
            m_sourceSuffix = source;
            m_sourcePort = sourcePort;
            m_destSuffix = dest;
            m_destPort = destPort;
            m_isDeletable = isDeletable;
            m_uiInfo = uiInfo;
        }

        /** Copies an existing connection (used for copy&amp;paste).
         * @param original To copy.
         * @param preserveDeletableFlag Whether to retain the deletable status
         * of the original connection. */
        ConnectionContainerTemplate(final ConnectionContainer original,
                final boolean preserveDeletableFlag) {
            m_sourcePort = original.getSourcePort();
            m_destPort = original.getDestPort();
            switch (original.getType()) {
            case STD:
                m_sourceSuffix = original.getSource().getIndex();
                m_destSuffix = original.getDest().getIndex();
                break;
            case WFMIN:
                m_sourceSuffix = -1;
                m_destSuffix = original.getDest().getIndex();
                break;
            case WFMOUT:
                m_sourceSuffix = original.getSource().getIndex();
                m_destSuffix = -1;
                break;
            case WFMTHROUGH:
                m_sourceSuffix = -1;
                m_destSuffix = -1;
                break;
            default:
                throw new InternalError("Unknown type " + original.getType());
            }
            m_isDeletable = !preserveDeletableFlag || original.isDeletable();
            ConnectionUIInformation origUIInfo = original.getUIInfo();
            m_uiInfo = origUIInfo == null ? null : origUIInfo.clone();
        }

        /** @return the source identifier */
        int getSourceSuffix() {
            return m_sourceSuffix;
        }

        /** @return the source port */
        int getSourcePort() {
            return m_sourcePort;
        }

        /** @return the destination identifier. */
        int getDestSuffix() {
            return m_destSuffix;
        }

        /** @return the destination port */
        int getDestPort() {
            return m_destPort;
        }

        /**
         * @return the isDeletable
         */
        boolean isDeletable() {
            return m_isDeletable;
        }

        /**
         * @param isDeletable the isDeletable to set
         */
        void setDeletable(final boolean isDeletable) {
            m_isDeletable = isDeletable;
        }

        /** @param destPort the destPort to set */
        void setDestPort(final int destPort) {
            m_destPort = destPort;
        }

        /** @param destSuffix the destSuffix to set */
        void setDestSuffix(final int destSuffix) {
            m_destSuffix = destSuffix;
        }

        /** @param sourcePort the sourcePort to set */
        void setSourcePort(final int sourcePort) {
            m_sourcePort = sourcePort;
        }

        /** @param sourceSuffix the sourceSuffix to set */
        void setSourceSuffix(final int sourceSuffix) {
            m_sourceSuffix = sourceSuffix;
        }

        /** @return the uiInfo */
        ConnectionUIInformation getUiInfo() {
            return m_uiInfo;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return "[" + getSourceSuffix() + "("
                + getSourcePort() + ") -> " + getDestSuffix()
                + "( " + getDestPort() + ")]";
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return m_sourceSuffix + (m_sourcePort << 8)
                + (m_destSuffix << 16) + (m_destPort << 24);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof ConnectionContainerTemplate)) {
                return false;
            }
            ConnectionContainerTemplate c = (ConnectionContainerTemplate)obj;
            return c.m_sourceSuffix == m_sourceSuffix
                && c.m_sourcePort == m_sourcePort
                && c.m_destSuffix == m_destSuffix
                && c.m_destPort == m_destPort;
        }

    }

    static final class WorkflowPortTemplate {
        private final int m_portIndex;
        private final PortType m_portType;
        private String m_portName;
        WorkflowPortTemplate(final int index, final PortType type) {
            m_portIndex = index;
            m_portType = type;
        }
        /** @return the portIndex */
        int getPortIndex() {
            return m_portIndex;
        }
        /** @return the portType */
        PortType getPortType() {
            return m_portType;
        }
        /** @param name the name to set */
        void setPortName(final String name) {
            m_portName = name;
        }
        /** @return the name */
        String getPortName() {
            return m_portName;
        }
        /** {@inheritDoc} */
        @Override
        public String toString() {
            return String.format("%d: %s", getPortIndex(), getPortType());
        }
    }

    public static class LoadResultEntry {

        public enum LoadResultEntryType {
            // sorted according to severity
            Ok,
            Warning,
            Error,
            DataLoadError
        }

        private final LoadResultEntryType m_type;
        private final String m_message;

        public LoadResultEntry(
                final LoadResultEntryType type, final String message) {
            m_type = type;
            m_message = message;
        }

        /**
         * @return the type
         */
        public LoadResultEntryType getType() {
            return m_type;
        }

        /**
         * @return the message
         */
        public String getMessage() {
            return m_message;
        }

        /**
         * @return the hasErrorDuringNonDataLoad
         */
        public boolean hasWarningEntries() {
            switch (getType()) {
            case Warning:
                return true;
            default:
            }
            for (LoadResultEntry e : getChildren()) {
                if (e.hasWarningEntries()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the hasErrorDuringNonDataLoad
         */
        public boolean hasErrors() {
            switch (getType()) {
            case Error:
            case DataLoadError:
                return true;
            default:
                return false;
            }
        }

        private static final LoadResultEntry[] EMPTY = new LoadResultEntry[0];

        public LoadResultEntry[] getChildren() {
            return EMPTY;
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return getFilteredError("", LoadResultEntryType.Ok);
        }

        /** Returns error message of this element and all of its children.
         * @param indent The indentation of the string (increased for children)
         * @param filter A filter for the least severity level
         * @return The string.
         */
        public String getFilteredError(final String indent,
                final LoadResultEntryType filter) {
            StringBuilder b = new StringBuilder(indent);
            b.append("Status: ");
            b.append(getType()).append(": ").append(getMessage());
            for (LoadResultEntry c : getChildren()) {
                if (c.getType().ordinal() >= filter.ordinal()) {
                    b.append("\n");
                    b.append(c.getFilteredError(indent + "  ", filter));
                }
            }
            return b.toString();
        }

    }

    public static class LoadResult extends LoadResultEntry {
        private final List<LoadResultEntry> m_errors =
            new ArrayList<LoadResultEntry>();

        private final List<NodeAndBundleInformation> m_missingNodes = new ArrayList<>();

        /** */
        public LoadResult(final String name) {
            super(LoadResultEntryType.Ok, name);
        }

        public void addError(final String error) {
            addError(error, false);
        }

        public void addError(final String error,
                final boolean isErrorDuringDataLoad) {
            LoadResultEntryType t = isErrorDuringDataLoad
            ? LoadResultEntryType.DataLoadError : LoadResultEntryType.Error;
            m_errors.add(new LoadResultEntry(t, error));
        }

        public void addWarning(final String warning) {
            LoadResultEntryType t = LoadResultEntryType.Warning;
            m_errors.add(new LoadResultEntry(t, warning));
        }

        public void addChildError(final LoadResult loadResult) {
            m_errors.add(loadResult);
        }

        /** {@inheritDoc} */
        @Override
        public LoadResultEntryType getType() {
            int max = super.getType().ordinal();
            for (LoadResultEntry e : m_errors) {
                max = Math.max(e.getType().ordinal(), max);
            }
            return LoadResultEntryType.values()[max];
        }

        /** {@inheritDoc} */
        @Override
        public LoadResultEntry[] getChildren() {
            return m_errors.toArray(new LoadResultEntry[m_errors.size()]);
        }

        /**
         * Adds information about missing nodes.
         *
         * @param missingNodes information about missing nodes; must not be <code>null</code>
         */
        void addMissingNodes(final Collection<NodeAndBundleInformation> missingNode) {
            m_missingNodes.addAll(missingNode);
        }

        /**
         * Adds information about a missing node.
         *
         * @param missingNode information about a missing node; must not be <code>null</code>
         */
        void addMissingNode(final NodeAndBundleInformation missingNode) {
            m_missingNodes.add(missingNode);
        }

        /**
         * Returns an unmodifiable list of missing nodes.
         *
         * @return a (possibly empty) list of information about missing nodes
         * @since 3.2
         */
        public List<NodeAndBundleInformation> getMissingNodes() {
            return Collections.unmodifiableList(m_missingNodes);
        }
    }

    public static final class WorkflowLoadResult extends MetaNodeLinkUpdateResult {
        /** @param name */
        public WorkflowLoadResult(final String name) {
            super(name);
        }

        public WorkflowManager getWorkflowManager() {
            return (WorkflowManager)super.getLoadedInstance();
        }
    }

    public static class MetaNodeLinkUpdateResult extends LoadResult {

        private NodeContainerTemplate m_instance;
        private boolean m_guiMustReportDataLoadErrors = false;

        /**
         * @param name
         */
        public MetaNodeLinkUpdateResult(final String name) {
            super(name);
        }

        /** @param instance the instance to set */
        public void setLoadedInstance(final NodeContainerTemplate instance) {
            m_instance = instance;
        }

        /** @return the instance (or null in case of errors) */
        public NodeContainerTemplate getLoadedInstance() {
            return m_instance;
        }

        /** @param guiMustReportDataLoadErrors the guiMustReportError to set */
        void setGUIMustReportDataLoadErrors(final boolean guiMustReportDataLoadErrors) {
            m_guiMustReportDataLoadErrors = guiMustReportDataLoadErrors;
        }

        /** @return the guiMustReportError */
        public boolean getGUIMustReportDataLoadErrors() {
            return m_guiMustReportDataLoadErrors;
        }

        /** Generate a user friendly message about the load procedure.
         * @return This message.
         */
        @Override
        public String getMessage() {
            String name = getLoadedInstance() == null ? "<none>" : getLoadedInstance().getNameWithID();
            StringBuilder b = new StringBuilder(name);
            b.append(" loaded");
            switch (getType()) {
            case Ok:
                b.append(" with no errors");
                break;
            case DataLoadError:
                b.append(" with error during data load");
                break;
            case Error:
                b.append(" with errors");
                break;
            case Warning:
                b.append(" with warnings");
                break;
            default:
                b.append(" with ").append(getType());
            }
            return b.toString();
        }
    }

    public static class NodeContainerTemplateLinkUpdateResult extends LoadResult {

        private NodeContainerTemplate m_ncTemplate;
        private WorkflowPersistor m_undoPersistor;

        /** @param name Forwarded to super. */
        public NodeContainerTemplateLinkUpdateResult(final String name) {
            super(name);
        }

        /** @return the metaNode */
        public NodeContainerTemplate getNCTemplate() {
            return m_ncTemplate;
        }

        /** @param ncTemplate the metaNode to set */
        void setNCTemplate(final NodeContainerTemplate ncTemplate) {
            m_ncTemplate = ncTemplate;
        }

        /** @return the undoPersistor */
        public WorkflowPersistor getUndoPersistor() {
            return m_undoPersistor;
        }

        /** @param undoPersistor the undoPersistor to set */
        void setUndoPersistor(final WorkflowPersistor undoPersistor) {
            m_undoPersistor = undoPersistor;
        }

    }

    /** Thrown when node factory is not available. Error message is descriptive.
     * @since 2.6
     */
    @SuppressWarnings("serial")
    static final class NodeFactoryUnknownException extends InvalidSettingsException {

        private final NodeAndBundleInformation m_nodeAndBundleInformation;
        private final NodeSettingsRO m_additionalFactorySettings;

        /**
         * @param message ...
         * @param nodeAndBundleInformation ...
         * @param additionalFactorySettings ...
         * @param cause ... */
        NodeFactoryUnknownException(final String message, final NodeAndBundleInformation nodeAndBundleInformation,
                final NodeSettingsRO additionalFactorySettings, final Throwable cause) {
            super(message, cause);
            m_nodeAndBundleInformation = nodeAndBundleInformation;
            m_additionalFactorySettings = additionalFactorySettings;
        }

        /**
         * @param nodeAndBundleInformation ...
         * @param additionalFactorySettings ...
         * @param cause ... */
        NodeFactoryUnknownException(final NodeAndBundleInformation nodeAndBundleInformation,
                final NodeSettingsRO additionalFactorySettings, final Throwable cause) {
            this(nodeAndBundleInformation.getErrorMessageWhenNodeIsMissing(),
                nodeAndBundleInformation, additionalFactorySettings, cause);
        }

        /**
         * @return the nodeAndBundleInformation
         */
        NodeAndBundleInformation getNodeAndBundleInformation() {
            return m_nodeAndBundleInformation;
        }

        /**
         * @return the additionalFactorySettings (or null)
         */
        NodeSettingsRO getAdditionalFactorySettings() {
            return m_additionalFactorySettings;
        }

    }

}
