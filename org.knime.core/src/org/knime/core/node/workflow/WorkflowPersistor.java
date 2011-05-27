/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * History
 *   11.09.2007 (mb): created
 */
package org.knime.core.node.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.core.data.container.ContainerTable;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.port.PortType;

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

    /** File used to signal that workflow was saved in usual manner. It will
     * always be present in the workflow directory unless the workflow is
     * exported with the "exclude data" flag being set. */
    public static final String SAVED_WITH_DATA_FILE = ".savedWithData";

    /** Constant for the meta info file name. */
    public static final String METAINFO_FILE = "workflowset.meta";

    String getLoadVersionString();

    Map<Integer, NodeContainerPersistor> getNodeLoaderMap();

    Set<ConnectionContainerTemplate> getConnectionSet();

    HashMap<Integer, ContainerTable> getGlobalTableRepository();

    String getName();

    /** Get the workflow variables associated with this meta node/workflow.
     * This method must not return null (but possibly an empty list). The result
     * may be unmodifiable.
     * @return The workflow variables.
     */
    List<FlowVariable> getWorkflowVariables();

    /** Get (non-null) map of credentials.
     * @return The credentials defined on this meta node.
     */
    List<Credentials> getCredentials();

    /** @return (non-mull) map of annotations. */
    List<WorkflowAnnotation> getWorkflowAnnotations();

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
    public UIInformation getInPortsBarUIInfo();

    /** Get UI information for workflow output ports.
     * @return the ui info or null if not set.
     */
    public UIInformation getOutPortsBarUIInfo();

    /** @return the shouldFailOnLoadDataError */
    public boolean mustWarnOnDataLoadError();

    /** template information associated with the workflow, e.g. whether it
     * is linking to same central master meta node.
     * @return The template info
     */
    public MetaNodeTemplateInformation getTemplateInformation();

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

        /** Copies an existing connection (used for copy&paste).
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

    }

    public static final class WorkflowLoadResult extends LoadResult {

        private WorkflowManager m_workflowManager;
        private boolean m_guiMustReportDataLoadErrors = false;

        /**
         * @param name
         */
        public WorkflowLoadResult(final String name) {
            super(name);
        }

        /**
         * @param workflowManager the workflowManager to set
         */
        void setWorkflowManager(final WorkflowManager workflowManager) {
            m_workflowManager = workflowManager;
        }

        /**
         * @return the workflowManager
         */
        public WorkflowManager getWorkflowManager() {
            return m_workflowManager;
        }

        /**
         * @param guiMustReportDataLoadErrors the guiMustReportError to set
         */
        void setGUIMustReportDataLoadErrors(
                final boolean guiMustReportDataLoadErrors) {
            m_guiMustReportDataLoadErrors = guiMustReportDataLoadErrors;
        }

        /**
         * @return the guiMustReportError
         */
        public boolean getGUIMustReportDataLoadErrors() {
            return m_guiMustReportDataLoadErrors;
        }

        /** Generate a user friendly message about the load procedure.
         * @return This message.
         */
        @Override
        public String getMessage() {
            String name = getWorkflowManager() == null ? "<none>"
                    : getWorkflowManager().getNameWithID();
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

    public static class MetaNodeLinkUpdateResult extends LoadResult {

        private WorkflowManager m_metaNode;
        private WorkflowPersistor m_undoPersistor;

        /** @param name Forwarded to super. */
        public MetaNodeLinkUpdateResult(final String name) {
            super(name);
        }

        /** @return the metaNode */
        public WorkflowManager getMetaNode() {
            return m_metaNode;
        }

        /** @param metaNode the metaNode to set */
        void setMetaNode(final WorkflowManager metaNode) {
            m_metaNode = metaNode;
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

}
