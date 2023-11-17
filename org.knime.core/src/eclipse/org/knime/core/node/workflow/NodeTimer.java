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
 *   Mar 11, 2015 (Berthold): created
 */
package org.knime.core.node.workflow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.data.container.BufferedTableBackend;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.EclipseUtil;
import org.knime.core.util.JsonUtil;
import org.knime.core.util.MutableInteger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.prefs.Preferences;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.stream.JsonGenerator;

/**
 * Holds execution timing information about a specific node. It also defines a static global
 * member that is used to collect node stats for this KNIME instance.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author Michael Berthold
 * @since 2.12
 */
public final class NodeTimer {

    /* For now we use the default store address always. */
    private static final String SERVER_ADDRESS = "https://stats.knime.com/store/rest";
            /*"http://localhost:8080/com.knime.store.server/rest"*/

    /** Preference constant: send anonymous usage statistics to KNIME, yes or no. */
    public static final String PREF_KEY_SEND_ANONYMOUS_STATISTICS = "knime.sendAnonymousStatistics";
    /** Preference constant: if KNIME already asked the user to transmit usage statistics, yes or no. */
    public static final String PREF_KEY_ASKED_ABOUT_STATISTICS = "knime.askedToSendStatistics";

    private final NodeContainer m_parent;
    private long m_startTime;
    private long m_lastStartTime;
    private long m_lastExecutionDuration;
    private long m_executionDurationSinceReset;
    private long m_executionDurationOverall;
    private int m_numberOfExecutionsSinceReset;
    private int m_numberOfExecutionsOverall;

    /**
     * Container holding stats for the entire instance and all nodes that have been used/timed.
     */
    public static final class GlobalNodeStats {

        /**Placeholder to use for {@link GlobalNodeStats#setLastUsedPerspective(String)} whenever the user switches
         * to the classic (Java) UI.
         * @since 5.0
         * @see #setLastUsedPerspective(String)
         */
        public static final String CLASSIC_PERSPECTIVE_PLACEHOLDER = "none (classic)";

        /**
         * The type of workflow.
         * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
         * @since 4.5
         */
        public enum WorkflowType {
            /**Local workflow opened with the eclipse based editor.*/
            LOCAL,
            /**Remote workflow opened with the eclipse based editor but not via job view.*/
            REMOTE;
        }

        /**
         * The different ways a new node can be created.
         * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
         * @since 5.0
         */
        public enum NodeCreationType {
            /**Inserted via classic (Java) UI (any source - node repo, hub, favorites, etc.)*/
            JAVA_UI,
            /**Inserted via modern (Web) UI (any source - node repo, hub, quick insertion, etc.)*/
            WEB_UI,
            /**Inserted via drag and drop from KNIME Hub in classic (Java) UI.*/
            JAVA_UI_HUB,
            /**Inserted via drag and drop from KNIME Hub in modern (Web) UI.*/
            WEB_UI_HUB,
            /**Inserted via quick insertion function in modern (Web) UI.*/
            WEB_UI_QUICK_INSERTION_RECOMMENDED;
        }

        private static final String N_A = "n/a";

        private static final NodeLogger LOGGER = NodeLogger.getLogger(GlobalNodeStats.class);

        private class NodeStats {
            long executionTime = 0;
            int executionCount = 0;
            int failureCount = 0;
            int creationCount = 0;
            String likelySuccessor = N_A;
            String nodeName;

            private NodeStats(final String name) {
                nodeName = name;
            }
        }
        private LinkedHashMap<NodeKey, NodeStats> m_globalNodeStats = new LinkedHashMap<>();

        private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of("UTC"));
        private String m_created = DATE_FORMAT.format(Instant.now());
        private long m_avgUpTime = 0;
        private long m_currentInstanceLaunchTime = System.currentTimeMillis();
        private int m_workflowsOpened = 0;
        private int m_remoteWorkflowsOpened = 0;
        //Reported since version 4.5
        private int m_columnarStorageWorkflowsOpened = 0;
        // Reported since 4.6 -- number of times the user switched to the Web-UI perspective.
        private int m_webUIPerspectiveSwitches = 0;
        //Reported since 5.0 -- number of times the user switched to the Java-UI (classic) perspective.
        private int m_javaUIPerspectiveSwitches = 0;
        //Reported since 5.0 -- PLACEHOLDER is default since this field is not initialized if we are in the classic UI
        private String m_lastUsedPerspective = CLASSIC_PERSPECTIVE_PLACEHOLDER;
        //Reported since 5.0 -- Stores the number of nodes added via different means in KNIME UI
        private LinkedHashMap<NodeCreationType, MutableInteger> m_nodesCreatedVia = new LinkedHashMap<>();

        private int m_workflowsImported = 0;
        private int m_workflowsExported = 0;
        private int m_launches = 0;
        private int m_crashes = 0;
        private long m_timeOfLastSave = System.currentTimeMillis() - SAVEINTERVAL + 1000*60;
        private long m_timeOfLastSend = m_timeOfLastSave;
        private static final long SAVEINTERVAL = 15*60*1000;  // save no more than every 15mins
        private static final long SENDINTERVAL = 24*60*60*1000; // only send every 24h
        static final String FILENAME = "nodeusage_3.0.json";

        private static final boolean DISABLE_GLOBAL_TIMER = Boolean.getBoolean("knime.globaltimer.disable");

        GlobalNodeStats() {
            if (DISABLE_GLOBAL_TIMER) {
                LOGGER.debug("Global Timer disabled due to system property");
                return;
            }
            //load the global stats
            readFromFile();
        }

        private void addExecutionTime(final NodeContainer nc, final boolean success, final long exectime) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            var nodeKey = NodeKey.get(nc);
            // synchronized to avoid conflicts and parallel file writes
            synchronized (this) {
                NodeStats ns = m_globalNodeStats.get(nodeKey);
                if (ns == null) {
                    ns = new NodeStats(nc.getName());
                    m_globalNodeStats.put(nodeKey, ns);
                }
                ns.executionTime += exectime;
                if (success) {
                    ns.executionCount++;
                } else {
                    ns.failureCount++;
                }
                processStatChanges();
            }
        }

        public void addNodeCreation(final NodeContainer nc) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            // synchronized to avoid conflicts and parallel file writes
            synchronized (this) {
                NodeStats ns = m_globalNodeStats.get(NodeKey.get(nc));
                if (ns == null) {
                    ns = new NodeStats(nc.getName());
                    m_globalNodeStats.put(NodeKey.get(nc), ns);
                }
                ns.creationCount++;
                processStatChanges();
            }
        }

        public void addConnectionCreation(final NodeContainer source, final NodeContainer dest) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            // synchronized to avoid conflicts and parallel file writes
            synchronized (this) {
                NodeStats ns = m_globalNodeStats.get(NodeKey.get(source));
                if (ns == null) {
                    ns = new NodeStats(source.getName());
                    m_globalNodeStats.put(NodeKey.get(source), ns);
                }
                // remember the newly connected successor with a 50:50 chance
                // (statistics over many thousands of users will provide real info)
                if ((ns.likelySuccessor.equals(N_A)) | (Math.random() >= .5)) {
                    ns.likelySuccessor = NodeKey.get(dest).id();
                }
                processStatChanges();
            }
        }

        /**
         * Called whenever a workflow is opened independent of its source e.g. local repository, server repository
         * or KNIME Hub. This includes also workflows opened via the remote job view. The type is
         * used to distinguish between workflows opened from a local or remote repository.
         *
         *
         * @param wfm the optional {@link WorkflowManager}. Can be <code>null</code>.
         * @param type the type of workflow e.g. local or remote
         * @since 4.5
         */
        public void incWorkflowOpening(final WorkflowManager wfm, final WorkflowType type) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_workflowsOpened++;

            if (GlobalNodeStats.WorkflowType.REMOTE == type) {
                m_remoteWorkflowsOpened++;
            }

            if (usesColumnarStorageBackend(wfm)) {
                m_columnarStorageWorkflowsOpened++;
            }
        }

        /**
         * Checks if the workflow manager is available and if so checks if the columnar storage {@link TableBackend}
         * is used.
         *
         * @param wfm the optional {@link WorkflowManager}
         * @return <code>true</code> if the columnar storage backend is used otherwise <code>false</code>
         */
        private static boolean usesColumnarStorageBackend(final WorkflowManager wfm) {
            if (wfm != null) {
                final TableBackend tableBackend =
                    wfm.getTableBackendSettings().map(WorkflowTableBackendSettings::getTableBackend)
                        .orElse(TableBackendRegistry.getInstance().getDefaultBackendForNewWorkflows());
                //if it is not the BufferedTableBackend it can only be the FastTableBackend
                return !(tableBackend instanceof BufferedTableBackend);
            }
            //workflow manager is not present in remote job view
            return false;
        }

        /**
         * Called by KNIME AP when the user imports a workflow.
         * @since 4.0
         */
        public void incWorkflowImport() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_workflowsImported++;
        }

        /**
         * Called by KNIME AP when the user exports a workflow.
         * @since 4.0
         */
        public void incWorkflowExport() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_workflowsExported++;
        }

        /**
         * Called by KNIME AP when the user switches to the Web-UI perspective
         * @since 4.6
         */
        public void incWebUIPerspectiveSwitch() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_webUIPerspectiveSwitches++;
        }

        /**
         * Called by KNIME AP when the user switches to the Java-UI (classic) perspective
         * @since 5.0
         */
        public void incJavaUIPerspectiveSwitch() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_javaUIPerspectiveSwitches++;
        }

        /**
         * Called by KNIME AP when the user switches the perspective (e.g. all nodes/restricted).
         * @param perspective the name of the perspective
         * @since 5.0
         * @see NodeTimer.GlobalNodeStats#CLASSIC_PERSPECTIVE_PLACEHOLDER
         */
        public void setLastUsedPerspective(final String perspective) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_lastUsedPerspective = perspective;
        }

        /**
         * Called by KNIME AP when the created a node by dragging it from the Java (classic) node repository
         * @param type the {@link NodeCreationType} e.g. via Hub, (classic/modern) node repository or quick insertion
         * @since 5.0
         */
        public void incNodeCreatedVia(final NodeCreationType type) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_nodesCreatedVia.computeIfAbsent(type, t -> new MutableInteger(0)).inc();
        }

        private void processStatChanges() {
            if (System.currentTimeMillis() > m_timeOfLastSave + SAVEINTERVAL) {
                asyncWriteToFile(false);
                m_timeOfLastSave = System.currentTimeMillis();
                if (System.currentTimeMillis() > m_timeOfLastSend + SENDINTERVAL) {
                    asyncSendToServer(false);
                    m_timeOfLastSend = System.currentTimeMillis();
                }
            }
        }

        public DataTableSpec getGlobalStatsSpecs() {
            DataTableSpecCreator dtsc = new DataTableSpecCreator();
            DataColumnSpec[] colSpecs = new DataColumnSpec[] {
                new DataColumnSpecCreator("Name", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Aggregate Execution Time", LongCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Overall Nr of Executions", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Overall Nr of Creations", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Likely Successor", StringCell.TYPE).createSpec()
            };
            dtsc.addColumns(colSpecs);
            return dtsc.createSpec();
        }
        public synchronized BufferedDataTable getGlobalStatsTable(final ExecutionContext exec) {
            // TODO: double check that we cannot possibly run into a deadlock via the ExecutionContext?!
            //  (if so: copy data first...)
            BufferedDataContainer result = exec.createDataContainer(getGlobalStatsSpecs());
            int rowcount = 0;
            for (var entry : m_globalNodeStats.entrySet()) {
                var nodeKey = entry.getKey();
                var ns = entry.getValue();
                if (ns != null) {
                    DataRow row = new DefaultRow(
                        new RowKey("Row " + rowcount++),
                        new StringCell(nodeKey.id()),
                        new LongCell(ns.executionTime),
                        new IntCell(ns.executionCount),
                        new IntCell(ns.creationCount),
                        new StringCell(ns.likelySuccessor)
                            );
                    result.addRowToTable(row);
                } else {
                    DataRow row = new DefaultRow(
                        new RowKey("Row " + rowcount++),
                        DataType.getMissingCell(),
                        DataType.getMissingCell(),
                        DataType.getMissingCell(),
                        DataType.getMissingCell(),
                        DataType.getMissingCell()
                            );
                    result.addRowToTable(row);
                }
            }
            result.close();
            return result.getTable();
        }
        /**
         * @return the average up time of this KNIME instance
         */
        public long getAvgUpTime() {
            return (m_avgUpTime * m_launches + getCurrentInstanceUpTime()) / (m_launches + 1);
        }
        /**
         * @return the time since the last launch
         * @since 3.0
         */
        public long getCurrentInstanceUpTime() {
            return System.currentTimeMillis() - m_currentInstanceLaunchTime;
        }
        /**
         * @return the number of KNIME launches
         */
        public int getNrLaunches() {
            return m_launches + 1;
        }
        /**
         * @return the number of KNIME crashes
         */
        public int getNrCrashes() {
            return m_crashes;
        }

        private JsonObject constructJSONObject(final boolean properShutdown) {
            JsonObjectBuilder job = JsonUtil.getProvider().createObjectBuilder();
            job.add("version", KNIMEConstants.VERSION);
            job.add("created", m_created);
            JsonObjectBuilder job2 = JsonUtil.getProvider().createObjectBuilder();
            synchronized (this) {
                    JsonArrayBuilder jab = JsonUtil.getProvider().createArrayBuilder();
                    for (var entry : m_globalNodeStats.entrySet()) {
                        var nodeKey = entry.getKey();
                        if (!nodeKey.type().equals(NativeNodeContainer.class)) {
                            continue;
                        }
                        JsonObjectBuilder job3 = JsonUtil.getProvider().createObjectBuilder();
                        var ns = entry.getValue();
                        if (ns != null) {
                            job3.add("id", nodeKey.id()); // NodeFactory.getFactoryId
                            job3.add("nodename", ns.nodeName);
                            job3.add("nrexecs", ns.executionCount);
                            job3.add("nrfails", ns.failureCount);
                            job3.add("exectime", ns.executionTime);
                            job3.add("nrcreated", ns.creationCount);
                            job3.add("successor", ns.likelySuccessor);
                            jab.add(job3);
                        }
                    }
                    job2.add("nodes", jab);

                    // metanodes
                    JsonObjectBuilder jobMeta = JsonUtil.getProvider().createObjectBuilder();
                    NodeStats ns = m_globalNodeStats.get(NodeKey.get(WorkflowManager.class));
                    if (ns != null) {
                        jobMeta.add("nodename", ns.nodeName);
                        jobMeta.add("nrexecs", ns.executionCount);
                        jobMeta.add("nrfails", ns.failureCount);
                        jobMeta.add("exectime", ns.executionTime);
                        jobMeta.add("nrcreated", ns.creationCount);
                    }
                    job2.add("metaNodes", jobMeta);

                    // sub nodes
                    JsonObjectBuilder jobSub = JsonUtil.getProvider().createObjectBuilder();
                    ns = m_globalNodeStats.get(NodeKey.get(SubNodeContainer.class));
                    if (ns != null) {
                        jobSub.add("nodename", ns.nodeName);
                        jobSub.add("nrexecs", ns.executionCount);
                        jobSub.add("nrfails", ns.failureCount);
                        jobSub.add("exectime", ns.executionTime);
                        jobSub.add("nrcreated", ns.creationCount);
                    }
                    job2.add("wrappedNodes", jobSub);

                    //created information
                    final JsonObjectBuilder jobNodesCreated = JsonUtil.getProvider().createObjectBuilder();
                    for (Entry<NodeCreationType, MutableInteger> e : m_nodesCreatedVia.entrySet()) {
                        jobNodesCreated.add(e.getKey().name(), e.getValue().intValue());
                    }
                    job2.add("createdVia", jobNodesCreated);
            }
            job.add("nodestats", job2);
            job.add("uptime", getAvgUpTime());
            job.add("workflowsOpened", m_workflowsOpened);
            job.add("remoteWorkflowsOpened", m_remoteWorkflowsOpened);
            job.add("columnarStorageWorkflowsOpened", m_columnarStorageWorkflowsOpened);
            job.add("workflowsImported", m_workflowsImported);
            job.add("workflowsExported", m_workflowsExported);
            job.add("webUIPerspectiveSwitches", m_webUIPerspectiveSwitches);
            job.add("javaUIPerspectiveSwitches", m_javaUIPerspectiveSwitches);
            job.add("lastUsedPerspective", m_lastUsedPerspective);
            job.add("launches", getNrLaunches());
            job.add("lastApplicationID", getApplicationID()); // batch, standard KNIME AP, ...
            job.add("timeSinceLastStart", getCurrentInstanceUpTime());
            job.add("crashes", m_crashes);
            job.add("properlyShutDown", properShutdown);
            JsonObject jo = job.build();
            return jo;
        }

        private static String getApplicationID() {
            if (!StringUtils.isEmpty(System.getProperty("eclipse.application"))) {
                return System.getProperty("eclipse.application");
            }

            Bundle myself = FrameworkUtil.getBundle(NodeTimer.class);
            if (myself != null) {
                BundleContext ctx = myself.getBundleContext();
                ServiceReference<ApplicationHandle> ser = ctx.getServiceReference(ApplicationHandle.class);
                if (ser != null) {
                    try {
                        ApplicationHandle appHandle = ctx.getService(ser);
                        return appHandle.getInstanceId();
                    } finally {
                        ctx.ungetService(ser);
                    }
                }
            }

            return "<unknown>";
        }

        synchronized void writeToFile(final boolean properShutdown) {
            try {
                JsonObject jo = constructJSONObject(properShutdown);
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                Map<String, Boolean> cfg = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                try (FileOutputStream outStream = new FileOutputStream(propfile);
                        JsonWriter jw = JsonUtil.getProvider().createWriterFactory(cfg).createWriter(outStream)) {
                    jw.write(jo);
                }
                LOGGER.debug("Successfully wrote node usage stats to file: " + propfile.getCanonicalPath());
            } catch (IOException ioe) {
                LOGGER.warn("Failed to write node usage stats to file.", ioe);
            }
        }


        private void sendToServer(final boolean properShutdown) {
            // Send statistics based on user preferences. If there are no prefs available,  don't send them in a
            // full KNIME AP desktop application (because the user was prompted and the default value, false,
            // was confirmed); all other applications (e.g. batch) will send stats - user can suppress that by providing
            // a workspace containing preferences
            final Preferences preferences = InstanceScope.INSTANCE.getNode("org.knime.workbench.core");
            boolean defaultSendStats;
            if ("org.knime.product.KNIME_APPLICATION".equals(getApplicationID())) {
                // running in UI mode -- the user was prompted if he wants to share (and if so, it will have value
                // 'true' in the preferences)
                defaultSendStats = false;
            } else { // non-ui mode
                // running in batch (or server or ... anything that likely doesn't have a UI) and the user was not
                // prompted if he wants to share. We assume 'true' for stats submission unless he's using a workspace
                // from which we know it prompted the 'do you want to share' question
                defaultSendStats = true;
                boolean hasPromptedToSendStats = preferences.getBoolean(PREF_KEY_ASKED_ABOUT_STATISTICS, false);
                if (hasPromptedToSendStats) {
                    // the value is stored in the preferences if it deviates from the store default (which is false)
                    defaultSendStats = false;
                }
            }

            boolean sendStatistics = preferences.getBoolean(PREF_KEY_SEND_ANONYMOUS_STATISTICS, defaultSendStats)
                    && !EclipseUtil.isRunFromSDK();
            if (!sendStatistics) {
                LOGGER.debug("Sending of usage stats disabled.");
                return;
            }

            PostMethod method = null;
            try {
                JsonObject jo = constructJSONObject(properShutdown);
                Map<String, Boolean> cfg = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                //we don't need to compress here since gzip is done on the web server
                try (JsonWriter jw = JsonUtil.getProvider().createWriterFactory(cfg).createWriter(bos)) {
                    jw.write(jo);
                }

                byte[] bytes = bos.toByteArray();
                String knid = URLEncoder.encode(KNIMEConstants.getKNID(), "UTF-8");
                HttpClient requestClient = new HttpClient();
                requestClient.getParams().setAuthenticationPreemptive(true);
                org.apache.commons.httpclient.Credentials usageCredentials =
                    new UsernamePasswordCredentials("knime-usage-user", "knime");
                requestClient.getState().setCredentials(AuthScope.ANY, usageCredentials);
                //use the new v2 end point for session based files
                String uri = SERVER_ADDRESS + "/usage/v2/" + knid;
                method = new PostMethod(uri);
                RequestEntity entity = new ByteArrayRequestEntity(bytes);
                method.setRequestEntity(entity);
                int response = requestClient.executeMethod(method);
                if (response != HttpStatus.SC_OK) {
                    String responseReason = HttpStatus.getStatusText(response);
                    String responseString = responseReason == null ?
                        Integer.toString(response) : (response + " - " + responseReason);
                    throw new HttpException("Server returned HTTP code " + responseString);
                }
                //reset all session counts
                resetSessionCounts();
                //write new file after resetting the session counts to read the current global counts next time
                writeToFile(properShutdown);
                LOGGER.debug("Successfully sent node usage stats to server");
            } catch (HttpException ex) {
                LOGGER.warn("Node usage file failed to send  because of a protocol exception.", ex);
            } catch (IOException ex) {
                LOGGER.debug("Node usage file did not send. Not logging additional information, "
                    + "since this is commonly due to limited internet connection: "
                    + ex.getClass().getName() + " - " + ex.getMessage());
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        }

        private Thread asyncSendToServer(final boolean properShutdown) {
            if (!EclipseUtil.determineServerUsage()) {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendToServer(properShutdown);
                    }
                }, "KNIME-Node-Usage-Sender");
                t.start();
                return t;
            }
            return null;
        }

        private Thread asyncWriteToFile(final boolean properShutdown) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeToFile(properShutdown);
                }
            }, "KNIME-Node-Usage-Writer");
            t.start();
            return t;
        }

        /**
         * This method can be called when the application properly shuts down.
         * It forces an out-of-interval write and send of the usage data with the
         * shutdown flag set.
         */
        public void performShutdown() {
            Thread writeThread = asyncWriteToFile(true);
            int threadTimeoutsInMS = 5000;
            try {
                writeThread.join(threadTimeoutsInMS);
            } catch (InterruptedException ex) {
                return;
            }

            if (!writeThread.isAlive()) {
                // don't send when the file has not properly written yet
                Thread sendThread = asyncSendToServer(true);
                if (sendThread != null) {
                    try {
                        sendThread.join(threadTimeoutsInMS);
                    } catch (InterruptedException ex) { /* silently ignore to not block shutdown */ }
                }
            }
        }

        private void readFromFile() {
            try {
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                if (!propfile.exists()) {
                    LOGGER.debug("Node usage file does not exist. Starting counts from scratch.");
                    return;
                }
                JsonObject jo;
                try (JsonReader jr = JsonUtil.getProvider().createReader(new FileInputStream(propfile))) {
                    jo = jr.readObject();
                }
                String version = "0.0.0";
                for (String key : jo.keySet()) {
                    switch (key) {
                        case "version":
                            version = jo.getString(key);
                            if (compareVersionString(version, "3.0.1") < 0) {
                                // ignore file created before 3.0.1, as the structure has changed
                                LOGGER.debug("Ignoring usage file content, because version was before 3.0.1. Starting counts from scratch.");
                                resetAllCounts();
                                return;
                            }
                            break;
                        case "created":
                            m_created = jo.getString(key);
                            break;
                        case "nodestats":
                            JsonObject jo2 = jo.getJsonObject(key);

                            // regular nodes
                            JsonArray jab = jo2.getJsonArray("nodes");
                            if (jab == null) {
                                // secondary check for changed structure
                                LOGGER.debug("Ignoring usage file content, because of missing 'nodes' field. Starting counts from scratch.");
                                resetAllCounts();
                                return;
                            }
                            for (int curNode = 0; curNode < jab.size(); curNode++) {
                                JsonObject job3 = jab.getJsonObject(curNode);
                                String nodeID = job3.getString("id");
                                String nodeName;
                                if (job3.containsKey("nodename")) {
                                    nodeName = job3.getString("nodename");
                                } else {
                                    nodeName = getNodeNameFromLegacyNodeID(nodeID);
                                }
                                int execCount = job3.getInt("nrexecs", 0);
                                int failCount = job3.getInt("nrfails", 0);
                                JsonNumber num = job3.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = job3.getInt("nrcreated", 0);
                                String successor = job3.getString("successor", "");
                                NodeStats ns = new NodeStats(nodeName);
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                ns.likelySuccessor = successor;
                                m_globalNodeStats.put(new NodeKey(NativeNodeContainer.class, nodeID), ns);
                            }

                            // metanodes
                            JsonObject jobMeta = jo2.getJsonObject("metaNodes");
                            if (!jobMeta.isEmpty()) {
                                String nodeName;
                                if (jobMeta.containsKey("nodename")) {
                                    nodeName = jobMeta.getString("nodename");
                                } else {
                                    nodeName = "MetaNode";
                                }
                                int execCount = jobMeta.getInt("nrexecs", 0);
                                int failCount = jobMeta.getInt("nrfails", 0);
                                JsonNumber num = jobMeta.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = jobMeta.getInt("nrcreated", 0);
                                NodeStats ns = new NodeStats(nodeName);
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                m_globalNodeStats.put(NodeKey.get(NodeContainer.class), ns);
                            }

                            JsonObject jubSub = jo2.getJsonObject("wrappedNodes");
                            if (!jubSub.isEmpty()) {
                                String nodeName;
                                if (jubSub.containsKey("nodename")) {
                                    nodeName = jubSub.getString("nodename");
                                } else {
                                    nodeName = "Component";
                                }
                                int execCount = jubSub.getInt("nrexecs", 0);
                                int failCount = jubSub.getInt("nrfails", 0);
                                JsonNumber num = jubSub.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = jubSub.getInt("nrcreated", 0);
                                NodeStats ns = new NodeStats(nodeName);
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                m_globalNodeStats.put(new NodeKey(SubNodeContainer.class), ns);
                            }

                            //created information
                            final JsonObject joNodesCreated = jo2.getJsonObject("createdVia");
                            if (joNodesCreated != null) {
                                for(String nodeKey : joNodesCreated.keySet()) {
                                    m_nodesCreatedVia.put(NodeCreationType.valueOf(nodeKey),
                                        new MutableInteger(joNodesCreated.getInt(nodeKey)));
                                }
                            }
                            break;
                        case "workflowsOpened":
                            m_workflowsOpened = jo.getInt(key);
                            break;
                        case "remoteWorkflowsOpened":
                            m_remoteWorkflowsOpened = jo.getInt(key);
                            break;
                        case "columnarStorageWorkflowsOpened":
                            m_columnarStorageWorkflowsOpened = jo.getInt(key);
                            break;
                        case "workflowsImported":
                            m_workflowsImported = jo.getInt(key);
                            break;
                        case "workflowsExported":
                            m_workflowsExported = jo.getInt(key);
                            break;
                        case "webUIPerspectiveSwitches":
                            m_webUIPerspectiveSwitches = jo.getInt(key);
                            break;
                        case "javaUIPerspectiveSwitches":
                            m_javaUIPerspectiveSwitches = jo.getInt(key);
                            break;
                        case "lastUsedPerspective":
                            m_lastUsedPerspective = jo.getString(key);
                            break;
                        case "uptime":
                            m_avgUpTime = jo.getJsonNumber(key).longValue();
                            break;
                        case "launches":
                            m_launches = jo.getInt(key);
                            break;
                        case "crashes":
                            m_crashes = jo.getInt(key);
                            break;
                        case "properlyShutDown":
                            if (!jo.getBoolean(key)) {
                                m_crashes++;
                            }
                        default:
                            // TODO: complain?
                    }
                }
                LOGGER.debug("Successfully read node usage stats from file: " + propfile.getCanonicalPath());
                if (compareVersionString(version, "5.1.0") < 0) {
                    // reset session counts for versions before 5.1.0, to start with a fresh session
                    LOGGER.debug("Resetting session count, because version was before 5.1.0. "
                            + "Starting session counts from scratch.");
                    resetSessionCounts();
                }
            } catch (Exception e) {
                LOGGER.warn("Failed reading node usage file. Starting counts from scratch.", e);
                resetAllCounts();
            }
        }

        /** Parses the given node id and returns the node name if it is a node id created by a KNIME AP version
         * prior 5.2.
         * @param nodeID the node id to parse
         * @return the node name or 'unknown' if the id format doesn't match the old id format
         * e.g. factoryclassname#nodename
         */
        private static String getNodeNameFromLegacyNodeID(final String nodeID) {
            if (StringUtils.isAllBlank(nodeID)) {
                return "unknwon";
            }
            //this looks for the previous used NODE_NAME_SEP '#'
            int i = nodeID.lastIndexOf('#');
            if (i < 0) {
                return "unknwon";
            }
            return nodeID.substring(i + 1);
        }

        private void resetSessionCounts() {
            m_globalNodeStats = new LinkedHashMap<>();
            m_nodesCreatedVia = new LinkedHashMap<>();
            m_workflowsOpened = 0;
            m_remoteWorkflowsOpened = 0;
            m_columnarStorageWorkflowsOpened = 0;
            m_workflowsImported = 0;
            m_workflowsExported = 0;
            m_webUIPerspectiveSwitches = 0;
            m_javaUIPerspectiveSwitches = 0;
            m_lastUsedPerspective = CLASSIC_PERSPECTIVE_PLACEHOLDER;
            m_crashes = 0;
        }

        void resetAllCounts() {
            m_created = DATE_FORMAT.format(Instant.now());
            m_globalNodeStats = new LinkedHashMap<>();
            m_nodesCreatedVia = new LinkedHashMap<>();
            m_workflowsOpened = 0;
            m_remoteWorkflowsOpened = 0;
            m_columnarStorageWorkflowsOpened = 0;
            m_workflowsImported = 0;
            m_workflowsExported = 0;
            m_webUIPerspectiveSwitches = 0;
            m_javaUIPerspectiveSwitches = 0;
            m_lastUsedPerspective = CLASSIC_PERSPECTIVE_PLACEHOLDER;
            m_crashes = 0;
            m_launches = 0;
            m_avgUpTime = 0;
        }

        private Integer compareVersionString(final String str1, final String str2)
        {
            Version v1 = new Version(str1);
            Version v2 = new Version(str2);
            return Integer.signum(v1.compareTo(v2));
        }

    }


    public static final GlobalNodeStats GLOBAL_TIMER = new GlobalNodeStats();

    NodeTimer(final NodeContainer parent) {
        m_parent = parent;
        initialize();
    }

    public long getLastExecutionDuration() {
        return m_lastExecutionDuration;
    }

    public long getExecutionDurationSinceReset() {
        return m_executionDurationSinceReset;
    }

    public long getExecutionDurationSinceStart() {
        return m_executionDurationOverall;
    }

    public int getNrExecsSinceReset() {
        return m_numberOfExecutionsSinceReset;
    }

    public int getNrExecsSinceStart() {
        return m_numberOfExecutionsOverall;
    }

    /**
     * @return time when node has been started the last time (format is the same as returned by
     *         {@link System#currentTimeMillis()}), -1 if node hasn't been started, yet
     */
    public long getStartTime() {
        if (m_startTime < 0) {
            //if node execution has been finished
            //(or never been started)
            return m_lastStartTime;
        } else {
            //if node is executing atm
            return m_startTime;
        }
    }

    private void initialize() {
        m_startTime = -1;
        m_lastStartTime = -1;
        m_lastExecutionDuration = -1;
        m_executionDurationSinceReset = 0;
        m_numberOfExecutionsSinceReset = 0;
        m_numberOfExecutionsOverall = 0;
        m_executionDurationOverall = 0;
    }

    public void resetNode() {
        m_numberOfExecutionsSinceReset = 0;
        m_executionDurationSinceReset = 0;
    }

    public void startExec() {
        m_startTime = System.currentTimeMillis();
    }

    public void endExec(final boolean success) {
        long currentTime = System.currentTimeMillis();
        if (m_startTime > 0) {
            // only do this if startExec() was called before (which it should...)
            m_lastExecutionDuration = currentTime - m_startTime;
            m_executionDurationSinceReset += m_lastExecutionDuration;
            m_executionDurationOverall += m_lastExecutionDuration;
            m_numberOfExecutionsOverall++;
            m_numberOfExecutionsSinceReset++;
            GLOBAL_TIMER.addExecutionTime(m_parent, success, m_lastExecutionDuration);
        }
        m_lastStartTime = m_startTime;
        m_startTime = -1;
    }

    private record NodeKey(Class<? extends NodeContainer> type, String id) {

        NodeKey(final Class<? extends NodeContainer> type) {
            this(type, SubNodeContainer.class.isAssignableFrom(type) ? type.getName() : "NodeContainer");
            assert !NativeNodeContainer.class.equals(type);
        }

        static NodeKey get(final NodeContainer nc) {
            if (nc instanceof NativeNodeContainer nnc) {
                return new NodeKey(nnc.getClass(), nnc.getNode().getFactory().getFactoryId());
            } else {
                return new NodeKey(nc.getClass());
            }
        }

        static NodeKey get(final Class<? extends NodeContainer> type) {
            return new NodeKey(type);
        }

    }

}
