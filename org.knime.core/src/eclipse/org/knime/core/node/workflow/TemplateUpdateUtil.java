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
 *   27 Jun 2022 (leon.wenzler): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.ItemVersion;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;
import org.knime.core.util.urlresolve.URLResolverUtil;

/**
 * Encapsulates util functionality for updating template links (a.k.a Components, Metanodes).
 *
 * @author Leon Wenzler, KNIME AG, Konstanz, Germany
 * @since 4.7
 */
public final class TemplateUpdateUtil {

    /**
     * A link to a template on Hub can contain three different kinds of version information through a query parameter.
     * <ul>
     *      <li>A specific item version can be referenced: {@code version=42}</li>
     *      <li>The link can point to the <i>latest</i> version: {@code version=most-recent}</li>
     *      <li>If no {@code version} query parameter is given or {@code version=current-state} the current state
     * (i.e., the staging area) of the space is referenced.</li>
     * </ul>
     *
     * @since 5.0
     * @deprecated prefer {@link ItemVersion} and {@link ItemVersionURIUtil} for working with Hub item versions
     */
    @Deprecated(since = "5.5", forRemoval = true)
    public enum LinkType {
        /** Specific version is selected, no updates will be available. */
        FIXED_VERSION(Object::toString, null, null),

        /** <i>Latest version</i> is selected. */
        LATEST_VERSION(v -> "most-recent", "most-recent", "latest"),

        /** <i>Current state</i> is selected, which includes unversioned changes. */
        LATEST_STATE(v -> null, "current-state", "-1");

        private final Function<Integer, String> m_paramString;
        private final String m_identifier;
        private final String m_legacyIdentifier;

        /**
         * The key used in KNIME hub catalog service query parameters to specify the version of a repository item.
         * Note that the hub execution service uses "itemVersion" instead.
         * @since 5.1
         */
        public static final String VERSION_QUERY_PARAM = "version";

        /**
         * spaceVersion query parameter was used prior to item level versioning. Space versions do not exist anymore.
         * However, when the KNIME hub migrated from space versions to item versions, item versions were created such
         * that item version X matches the content in space version X.
         * @since 5.1
         */
        public static final String LEGACY_SPACE_VERSION_QUERY_PARAM = "spaceVersion";

        LinkType(final Function<Integer, String> paramString, final String identifier, final String legacyIdentifier) {
            m_paramString = paramString;
            m_identifier = identifier;
            m_legacyIdentifier = legacyIdentifier;
        }

        /**
         * Creates the value for the {@code version} query parameter.
         *
         * @param version item version number, only used for {@link #FIXED_VERSION}
         * @return parameter value, e.g. {@code "most-recent"}, {@code "current-state"} or {@code "123"}
         */
        public String getParameterString(final Integer version) {
            return m_paramString.apply(version);
        }

        /**
         * @return the value used in query parameters to denote a specific link type. {@code null} for FIXED_VERSION.
         * @since 5.1
         */
        public String getIdentifier() {
            return m_identifier;
        }

        /**
         * @return the value previously used in query parameters (for Space Versioning) to denote a specific link type.
         * {@code null} for FIXED_VERSION.
         * @since 5.1
         */
        public String getLegacyIdentifier() {
            return m_legacyIdentifier;
        }

        /**
         * @return a permanent identifier for this link type
         * @since 5.2
         */
        @Override
        public String toString() {
            return switch (this) {
                case LATEST_STATE -> "LATEST_STATE";
                case LATEST_VERSION -> "LATEST_VERSION";
                case FIXED_VERSION -> "FIXED_VERSION";
            };
        }

        /**
         * @param s permanent identifier as returned by {@link #toString()}
         * @return the link type for the given string representation, or {@link Optional#empty()} if the string does
         *         not match any identifier.
         * @since 5.2
         */
        public static Optional<LinkType> fromString(final String s) {
            return switch (s) {
                case "LATEST_STATE" -> Optional.of(LATEST_STATE);
                case "LATEST_VERSION" -> Optional.of(LATEST_VERSION);
                case "FIXED_VERSION" -> Optional.of(FIXED_VERSION);
                default -> Optional.empty();
            };
        }
    }

    /**
     * Pair of the resolved {@link NodeContainerTemplate} and the corresponding {@link MetaNodeTemplateInformation}. The
     * node container template is <code>null</code> iff no update is available. The template information is used to
     * check which timestamp refers to the stored update-check-result.
     *
     * Allows for creating a new result record when a new template is available.
     *
     * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
     */
    record TemplateUpdateCheckResult(NodeContainerTemplate template, MetaNodeTemplateInformation info) {}

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TemplateUpdateUtil.class);

    /**
     * Resolves the localDir from the template information's source URI,
     * then passes it on to {@linkplain TemplateUpdateUtil#loadMetaNodeTemplateInternal}.
     *
     * @param meta template
     * @param loadHelper
     * @param loadResult
     * @return referenced NodeContainerTemplate
     * @throws IOException
     * @throws UnsupportedWorkflowVersionException
     * @throws CanceledExecutionException
     */
    public static NodeContainerTemplate loadMetaNodeTemplate(final NodeContainerTemplate meta,
        final WorkflowLoadHelper loadHelper, final LoadResult loadResult)
        throws IOException, UnsupportedWorkflowVersionException, CanceledExecutionException {
        var sourceURI = meta.getTemplateInformation().getSourceURI();
        // Context is used by ResolverUtil
        NodeContext.pushContext((NodeContainer)meta);
        try {
            return loadMetaNodeTemplateInternal(sourceURI, loadHelper, loadResult,
                ResolverUtil.resolveURItoLocalOrTempFile(sourceURI));
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Resolves the specified URI to a localDir and passes the sourceURI on to the internal template loader. It's
     * important that the caller ensures a {@link NodeContext} to be set for the current thread.
     *
     * @param sourceURI
     * @param loadHelper
     * @param loadResult
     * @return referenced NodeContainerTemplate
     * @throws IOException
     * @throws UnsupportedWorkflowVersionException
     * @throws CanceledExecutionException
     */
    public static NodeContainerTemplate loadMetaNodeTemplate(final URI sourceURI, final WorkflowLoadHelper loadHelper,
        final LoadResult loadResult)
        throws IOException, UnsupportedWorkflowVersionException, CanceledExecutionException {
        return loadMetaNodeTemplateInternal(sourceURI, loadHelper, loadResult,
            ResolverUtil.resolveURItoLocalOrTempFile(sourceURI));
    }

    /**
     * Resolves the localDir from the template information's source URI but with the condition
     * that the URI must either be local or outdated,
     * then passes it on to {@linkplain TemplateUpdateUtil#loadMetaNodeTemplateInternal}.
     *
     * @param meta template
     * @param loadHelper
     * @param loadResult
     * @return referenced NodeContainerTemplate
     * @throws IOException
     * @throws UnsupportedWorkflowVersionException
     * @throws CanceledExecutionException
     */
    private static NodeContainerTemplate loadMetaNodeTemplateIfLocalOrOutdatedOrVersioned(
            final NodeContainerTemplate meta, final WorkflowLoadHelper loadHelper, final LoadResult loadResult)
            throws IOException, UnsupportedWorkflowVersionException, CanceledExecutionException {
        final var linkInfo = meta.getTemplateInformation();
        final var sourceURI = linkInfo.getSourceURI();

        NodeContext.pushContext((NodeContainer)meta);
        try {
            /**
             * "ifModifiedSince" parameter is used to make an HTTP pre-check on whether an update is available to reduce
             * unnecessary downloads. Update is available only when the server version is *newer* than the local
             * timestamp. This will not fetch updates from an older, restored template snapshot on KS4.
             *
             * In case the template URI has a specific version set, we do not want to use the cutoff date
             * (since there are no changes to versioned content but we still want the content when changing
             * the version of the template.)
             */
            // ignore modified since for versioned content
            final var cutoffDate = URLResolverUtil.parseVersion(sourceURI.getQuery())
                .filter(ItemVersion::isVersioned).isPresent() ? null
                    : Optional.ofNullable(linkInfo.getTimestampInstant()) //
                        .map(instant -> instant.atZone(ZoneOffset.UTC)).orElse(null);
            final var localDir = ResolverUtil.resolveURItoLocalOrTempFileConditional(sourceURI,
                new NullProgressMonitor(), cutoffDate).orElse(null);
            if (localDir == null) {
                return null;
            }
            return loadMetaNodeTemplateInternal(sourceURI, loadHelper, loadResult, localDir);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * Uses the source URI to fetch the template. If the localDir looks like a zipped file that has been downloaded, it
     * will unzip the contents and load that template. Otherwise, the URI is local and the template at that location
     * will be loaded.
     *
     * @param sourceURI the template referenced source URI
     * @param loadHelper
     * @param loadResult
     * @param loadOnlyIfLocalOrOutdated if <code>true</code> it will only load the template if a local template is
     *            referenced or the referenced template is newer than this one (i.e. this template is outdated)
     * @return the referenced template instance or <code>null</code> if <code>loadOnlyIfLocalOrOutdated</code> is set to
     *         <code>true</code> and there is no update for the template available
     * @throws IOException
     * @throws UnsupportedWorkflowVersionException
     * @throws CanceledExecutionException
     */
    private static NodeContainerTemplate loadMetaNodeTemplateInternal(final URI sourceURI,
        final WorkflowLoadHelper loadHelper, final LoadResult loadResult, File localDir)
        throws IOException, UnsupportedWorkflowVersionException, CanceledExecutionException {
        var tempParent = WorkflowManager.lazyInitTemplateWorkflowRoot();
        MetaNodeLinkUpdateResult loadResultChild;
        try {
            if (localDir.isFile()) {
                // looks like a zipped metanode downloaded from a 4.4+ server
                var unzipped = FileUtil.createTempDir("metanode-template");
                FileUtil.unzip(localDir, unzipped);
                localDir = unzipped.listFiles()[0];
            }
            TemplateNodeContainerPersistor loadPersistor = loadHelper.createTemplateLoadPersistor(localDir, sourceURI);
            loadResultChild = new MetaNodeLinkUpdateResult("Template from " + sourceURI.toString());
            tempParent.load(loadPersistor, loadResultChild, new ExecutionMonitor(), false);
        } catch (InvalidSettingsException e) {
            throw new IOException("Unable to read template: " + e.getMessage(), e);
        }
        NodeContainerTemplate linkResult = loadResultChild.getLoadedInstance();
        MetaNodeTemplateInformation templInfo = linkResult.getTemplateInformation();
        var sourceRole = templInfo.getRole();
        if (sourceRole != Role.Link) {
            // "Template" field in the workflow settings should have changed to "Link" during preLoadNodeContainer
            // (this is due to the template information link uri set above), otherwise throw an exception
            throw new IOException(
                "The source of the linked instance does not represent a template but is of role " + sourceRole);
        }
        loadResult.addChildError(loadResultChild);
        return linkResult;
    }

    /**
     * Iterates through a collection of NodeContainerTemplates and retrieves their update status
     * by trying to load the template. Also uses a cache with the visitedTemplateMap from which already
     * visited templates' links are retrieved.
     *
     * @param nodesToCheck NodeContainerTemplates to check
     * @param loadHelper
     * @param loadResult
     * @param visitedTemplateMap cache of visited template nodes
     * @return map from NodeIDs to their corresponding UpdateStatus
     * @throws IOException if the execution was cancelled or the workflow could not be loaded
     */
    public static Map<NodeID, UpdateStatus> fillNodeUpdateStates(
        final Collection<NodeContainerTemplate> nodesToCheck, final WorkflowLoadHelper loadHelper,
        final LoadResult loadResult, final Map<URI, TemplateUpdateCheckResult> visitedTemplateMap) throws IOException {
        Map<NodeID, UpdateStatus> nodeIdToUpdateStatus = new LinkedHashMap<>();

        for (NodeContainerTemplate linkedMeta : nodesToCheck) {
            MetaNodeTemplateInformation linkInfo = linkedMeta.getTemplateInformation();
            final var uri = linkInfo.getSourceURI();
            NodeContainerTemplate tempLink = null;
            // if not visited yet OR we visited a newer version: visit new template and try to load it
            if (!visitedTemplateMap.containsKey(uri)
                || visitedTemplateMap.get(uri).info().isNewerOrNotEqualThan(linkInfo)) {
                try {
                    final var templateLoadResult = new LoadResult("Template to " + uri);
                    tempLink =
                        loadMetaNodeTemplateIfLocalOrOutdatedOrVersioned(linkedMeta, loadHelper, templateLoadResult);
                    loadResult.addChildError(templateLoadResult);
                    visitedTemplateMap.put(uri, new TemplateUpdateCheckResult(tempLink, linkInfo));
                } catch (IOException | UnsupportedWorkflowVersionException e) {
                    /**
                     * Remark: we introduced the {@link ResourceAccessException} as an exception for non-resolvable
                     * resources (here: templates). However, we still want to catch IOExceptions here since there can
                     * also occur standard IOExceptions on the way: unzipping the downloaded template, the template
                     * having an inconsistent TemplateInformation#Role, and now, a ResourceAccessException from fetching
                     * the template.
                     */
                    var uriString = Optional.of(uri) //
                            .filter(u -> KnimeUrlType.getType(u).isPresent()) // only KNIME URLs allowed
                            .flatMap(u -> ResolverUtil.toDescription(uri, null)) //
                            .map(KNIMEURIDescription::toDisplayString) //
                            .orElseGet(uri::toString);
                    var verb = e instanceof ResourceAccessException ? "accessed" : "read";
                    LOGGER.warn(String.format(
                        "Could not load template for \"%s\" - it links to \"%s\" but it could not be %s: %s", //
                        linkedMeta.getNameWithID(), uriString, verb, e.getMessage()), e);
                    nodeIdToUpdateStatus.put(linkedMeta.getID(), UpdateStatus.Error);
                    continue;
                } catch (CanceledExecutionException e) {
                    throw new IOException("Could not load template, the execution was cancelled", e);
                }
            } else {
                // retrieve already visited template link
                tempLink = visitedTemplateMap.get(uri).template();
            }

            nodeIdToUpdateStatus.put(linkedMeta.getID(), fillUpdateStatus(linkedMeta, tempLink));
        }
        return nodeIdToUpdateStatus;
    }

    /**
     * Returns whether a template has updates, checks if the name and the template information timestamp are different.
     *
     * @param thisContainer the local Metanode/Component
     * @param remoteContainer the remote Metanode/Component
     * @return an {@link UpdateStatus}
     * @throws URIException
     */
    private static UpdateStatus fillUpdateStatus(final NodeContainerTemplate thisContainer,
        final NodeContainerTemplate remoteContainer) throws URIException {
        String thisName = URIUtil.decode(thisContainer.getName());
        String remoteName = remoteContainer != null ? URIUtil.decode(remoteContainer.getName()) : null;
        // the template might be null which also means that there is _no_ update available
        // (see TemplateUpdateUtil#loadMetaNodeTemplateIfLocalOrOutdate)
        return remoteContainer != null && (!Objects.equals(remoteName, thisName)
            || remoteContainer.getTemplateInformation().isNewerOrNotEqualThan(thisContainer.getTemplateInformation()))
                ? UpdateStatus.HasUpdate : UpdateStatus.UpToDate;
    }

    /**
     * Special update state extractor that keeps track of parent update states. When going through accumulated nodeID
     * and their update status, if an error is detected, that error state will NOT be propagated if the node's parent
     * already has a "normal" HasUpdate status.
     *
     * @param updateStatesPerNode map from NodeID to UpdateStatus
     * @return is update available?
     * @throws IOException thrown when there exists a failed update from a node
     */
    public static boolean extractTemplateHasUpdate(final Map<NodeID, UpdateStatus> updateStatesPerNode)
        throws IOException {
        if (updateStatesPerNode == null) {
            return false;
        }

        var totalStatus = UpdateStatus.UpToDate;
        List<NodeID> updateableIds = new LinkedList<>();
        var erroneousNode = "";

        /* Reduces the map of update state to a single state. An error state will dominate the result
         * iff no parent update is present. Parent updates are kept track of in updateableIds. */
        for (Map.Entry<NodeID, UpdateStatus> entry : updateStatesPerNode.entrySet()) {
            var nodeId = entry.getKey();
            var status = entry.getValue();

            if (status == UpdateStatus.HasUpdate) {
                updateableIds.add(nodeId);
                // an accumulated error will always cause the reducedUpdateStatus to be erroneous
                if (totalStatus != UpdateStatus.Error) {
                    totalStatus = UpdateStatus.HasUpdate;
                }
            // fixes bug AP-18224
            } else if (status == UpdateStatus.Error
                && updateableIds.stream().allMatch(updatedId -> !nodeId.hasPrefix(updatedId))) {
                erroneousNode = String.valueOf(nodeId);
                // propagate the error only if no node's parent has an update available
                totalStatus = UpdateStatus.Error;
            }
        }
        // in case of an error, throw an IOException
        if (totalStatus == UpdateStatus.Error) {
            throw new IOException("Unable to read template: \"" + erroneousNode + "\"");
        }
        return totalStatus == UpdateStatus.HasUpdate;
    }

    /**
     * Shifts all input connections to a metanode one down so that, after update, they are correctly linked to a
     * component's input ports.
     * @param inConns The original connections
     * @return A new copy with adjusted ports.
     */
    static Set<ConnectionContainer> shiftInputsOneDown(final Set<ConnectionContainer> inConns) {
        final LinkedHashSet<ConnectionContainer> shiftedConnections = new LinkedHashSet<>();
        for (ConnectionContainer conn : inConns) {
            shiftedConnections.add(new ConnectionContainer( //
                conn.getSource(), conn.getSourcePort(), //
                conn.getDest(), conn.getDestPort() + 1, //
                conn.getType(), conn.isFlowVariablePortConnection()));
        }
        return shiftedConnections;
    }

    /**
     * Shifts all output connections from a metanode one down so that, after update, they are correctly linked from a
     * component's output ports.
     * @param outConns The original connections
     * @return A new copy with adjusted ports.
     */
    static Set<ConnectionContainer> shiftOutputsOneDown(final Set<ConnectionContainer> outConns) {
        final LinkedHashSet<ConnectionContainer> shiftedConnections = new LinkedHashSet<>();
        for (ConnectionContainer conn : outConns) {
            shiftedConnections.add(new ConnectionContainer( //
                conn.getSource(), conn.getSourcePort() + 1, //
                conn.getDest(), conn.getDestPort(), //
                conn.getType(), conn.isFlowVariablePortConnection()));
        }
        return shiftedConnections;
    }

    /**
     * Hides implicit constructor.
     */
    private TemplateUpdateUtil() {
    }
}
