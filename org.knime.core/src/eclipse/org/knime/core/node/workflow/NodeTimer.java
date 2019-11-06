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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.prefs.Preferences;

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
    private static final String SERVER_ADDRESS = "https://www.knime.com/store/rest";
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

        private static final String N_A = "n/a";
        private static final String NODE_NAME_SEP = "#";

        private static final NodeLogger LOGGER = NodeLogger.getLogger(GlobalNodeStats.class);

        private class NodeStats {
            long executionTime = 0;
            int executionCount = 0;
            int failureCount = 0;
            int creationCount = 0;
            String likelySuccessor = N_A;
        }
        private LinkedHashMap<String, NodeStats> m_globalNodeStats = new LinkedHashMap<String, NodeStats>();

        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        private String m_created = DATE_FORMAT.format(new Date());
        private long m_avgUpTime = 0;
        private long m_currentInstanceLaunchTime = System.currentTimeMillis();
        private int m_workflowsOpened = 0;
        private int m_remoteWorkflowsOpened = 0;
        private int m_workflowsImported = 0;
        private int m_workflowsExported = 0;
        private int m_launches = 0;
        private int m_crashes = 0;
        private long m_timeOfLastSave = System.currentTimeMillis() - SAVEINTERVAL + 1000*60;
        private long m_timeOfLastSend = m_timeOfLastSave;
        private static final long SAVEINTERVAL = 15*60*1000;  // save no more than every 15mins
        private static final long SENDINTERVAL = 24*60*60*1000; // only send every 24h
        private static final String FILENAME = "nodeusage_3.0.json";

        private static final boolean DISABLE_GLOBAL_TIMER = Boolean.getBoolean("knime.globaltimer.disable");

        GlobalNodeStats() {
            if (DISABLE_GLOBAL_TIMER) {
                LOGGER.debug("Global Timer disabled due to system property");
                return;
            }

            readFromFile();
        }

        void addExecutionTime(final String cname, final boolean success, final long exectime) {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            // synchronized to avoid conflicts and parallel file writes
            synchronized (this) {
                NodeStats ns = m_globalNodeStats.get(cname);
                if (ns == null) {
                    ns = new NodeStats();
                    m_globalNodeStats.put(cname, ns);
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
                NodeStats ns = m_globalNodeStats.get(NodeTimer.getCanonicalName(nc));
                if (ns == null) {
                    ns = new NodeStats();
                    m_globalNodeStats.put(NodeTimer.getCanonicalName(nc), ns);
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
                NodeStats ns = m_globalNodeStats.get(NodeTimer.getCanonicalName(source));
                if (ns == null) {
                    ns = new NodeStats();
                    m_globalNodeStats.put(NodeTimer.getCanonicalName(source), ns);
                }
                // remember the newly connected successor with a 50:50 chance
                // (statistics over many thousands of users will provide real info)
                if ((ns.likelySuccessor.equals(N_A)) | (Math.random() >= .5)) {
                    ns.likelySuccessor = NodeTimer.getCanonicalName(dest);
                }
                processStatChanges();
            }
        }

        /** Called when a workflow is opened.
         * @since 3.2 */
        public void incWorkflowOpening() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_workflowsOpened++;
        }

        /** Called when a remote workflow is opened.
         * @since 3.2 */
        public void incRemoteWorkflowOpening() {
            if (DISABLE_GLOBAL_TIMER) {
                return;
            }
            m_remoteWorkflowsOpened++;
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
            for (String cname : m_globalNodeStats.keySet()) {
                NodeStats ns = m_globalNodeStats.get(cname);
                if (ns != null) {
                    DataRow row = new DefaultRow(
                        new RowKey("Row " + rowcount++),
                        new StringCell(cname),
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
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add("version", KNIMEConstants.VERSION);
            job.add("created", m_created);
            JsonObjectBuilder job2 = Json.createObjectBuilder();
            synchronized (this) {
                    JsonArrayBuilder jab = Json.createArrayBuilder();
                    for (String cname : m_globalNodeStats.keySet()) {
                        if (!cname.matches(".+" + NODE_NAME_SEP + ".+")) {
                            // don't consider subnodes, metanodes or other weird things
                            continue;
                        }
                        JsonObjectBuilder job3 = Json.createObjectBuilder();
                        NodeStats ns = m_globalNodeStats.get(cname);
                        if (ns != null) {
                            job3.add("id", cname);
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
                    JsonObjectBuilder jobMeta = Json.createObjectBuilder();
                    NodeStats ns = m_globalNodeStats.get("NodeContainer");
                    if (ns != null) {
                        jobMeta.add("nrexecs", ns.executionCount);
                        jobMeta.add("nrfails", ns.failureCount);
                        jobMeta.add("exectime", ns.executionTime);
                        jobMeta.add("nrcreated", ns.creationCount);
                    }
                    job2.add("metaNodes", jobMeta);

                    // sub nodes
                    JsonObjectBuilder jobSub = Json.createObjectBuilder();
                    ns = m_globalNodeStats.get(SubNodeContainer.class.getName());
                    if (ns != null) {
                        jobSub.add("nrexecs", ns.executionCount);
                        jobSub.add("nrfails", ns.failureCount);
                        jobSub.add("exectime", ns.executionTime);
                        jobSub.add("nrcreated", ns.creationCount);
                    }
                    job2.add("wrappedNodes", jobSub);
            }
            job.add("nodestats", job2);
            job.add("uptime", getAvgUpTime());
            job.add("workflowsOpened", m_workflowsOpened);
            job.add("remoteWorkflowsOpened", m_remoteWorkflowsOpened);
            job.add("workflowsImported", m_workflowsImported);
            job.add("workflowsExported", m_workflowsExported);
            job.add("launches", getNrLaunches());
            job.add("lastApplicationID", getApplicationID()); // batch, standard KNIME AP, ...
            job.add("timeSinceLastStart", getCurrentInstanceUpTime());
            job.add("crashes", getNrCrashes());
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

        private void writeToFile(final boolean properShutdown) {
            try {
                JsonObject jo = constructJSONObject(properShutdown);
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                Map<String, Boolean> cfg = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                try (FileOutputStream outStream = new FileOutputStream(propfile);
                        JsonWriter jw = Json.createWriterFactory(cfg).createWriter(outStream)) {
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
                try (JsonWriter jw = Json.createWriterFactory(cfg).createWriter(bos)) {
                    jw.write(jo);
                }

                byte[] bytes = bos.toByteArray();
                String knimeID = URLEncoder.encode(KNIMEConstants.getKNIMEInstanceID(), "UTF-8");
                HttpClient requestClient = new HttpClient();
                requestClient.getParams().setAuthenticationPreemptive(true);
                org.apache.commons.httpclient.Credentials usageCredentials =
                    new UsernamePasswordCredentials("knime-usage-user", "knime");
                requestClient.getState().setCredentials(AuthScope.ANY, usageCredentials);
                String uri = SERVER_ADDRESS + "/usage/v1/" + knimeID;
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
                try (JsonReader jr = Json.createReader(new FileInputStream(propfile))) {
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
                                resetCounts();
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
                                resetCounts();
                                return;
                            }
                            for (int curNode = 0; curNode < jab.size(); curNode++) {
                                JsonObject job3 = jab.getJsonObject(curNode);
                                String nodeID = job3.getString("id");
                                int execCount = job3.getInt("nrexecs", 0);
                                int failCount = job3.getInt("nrfails", 0);
                                JsonNumber num = job3.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = job3.getInt("nrcreated", 0);
                                String successor = job3.getString("successor", "");
                                NodeStats ns = new NodeStats();
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                ns.likelySuccessor = successor;
                                m_globalNodeStats.put(nodeID, ns);
                            }

                            // metanodes
                            JsonObject jobMeta = jo2.getJsonObject("metaNodes");
                            if (!jobMeta.isEmpty()) {
                                int execCount = jobMeta.getInt("nrexecs", 0);
                                int failCount = jobMeta.getInt("nrfails", 0);
                                JsonNumber num = jobMeta.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = jobMeta.getInt("nrcreated", 0);
                                NodeStats ns = new NodeStats();
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                m_globalNodeStats.put("NodeContainer", ns);
                            }

                            JsonObject jubSub = jo2.getJsonObject("wrappedNodes");
                            if (!jubSub.isEmpty()) {
                                int execCount = jubSub.getInt("nrexecs", 0);
                                int failCount = jubSub.getInt("nrfails", 0);
                                JsonNumber num = jubSub.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = jubSub.getInt("nrcreated", 0);
                                NodeStats ns = new NodeStats();
                                ns.executionCount = execCount;
                                ns.failureCount = failCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                m_globalNodeStats.put(SubNodeContainer.class.getName(), ns);
                            }

                            break;
                        case "workflowsOpened":
                            m_workflowsOpened = jo.getInt(key);
                            break;
                        case "remoteWorkflowsOpened":
                            m_remoteWorkflowsOpened = jo.getInt(key);
                            break;
                        case "workflowsImported":
                            m_workflowsImported = jo.getInt(key);
                            break;
                        case "workflowsExported":
                            m_workflowsExported = jo.getInt(key);
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
            } catch (Exception e) {
                LOGGER.warn("Failed reading node usage file. Starting counts from scratch.", e);
                resetCounts();
            }
        }

        private void resetCounts() {
            m_created = DATE_FORMAT.format(new Date());
            m_avgUpTime = 0;
            m_launches = 0;
            m_crashes = 0;
            m_globalNodeStats = new LinkedHashMap<String, NodeTimer.GlobalNodeStats.NodeStats>();
        }

        private Integer compareVersionString(final String str1, final String str2)
        {
            Version v1 = new Version(str1);
            Version v2 = new Version(str2);
            return Integer.signum(v1.compareTo(v2));
        }
    }


    public static final GlobalNodeStats GLOBAL_TIMER = new GlobalNodeStats();

    private static String getCanonicalName(final NodeContainer nc) {
        String cname = "NodeContainer";
        if (nc instanceof NativeNodeContainer) {
            NativeNodeContainer node = (NativeNodeContainer)nc;
            //changed in 3.0.1: name consists of factory class name + node name
            String className = node.getNode().getFactory().getClass().getName();
            String nodeName = node.getName();
            cname = className + GlobalNodeStats.NODE_NAME_SEP + nodeName;
        } else if (nc instanceof SubNodeContainer) {
            cname = nc.getClass().getName();
        }
        return cname;
    }

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
        return m_lastStartTime;
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
            String cname = getCanonicalName(m_parent);
            GLOBAL_TIMER.addExecutionTime(cname, success, m_lastExecutionDuration);
        }
        m_lastStartTime = m_startTime;
        m_startTime = -1;
    }

}
