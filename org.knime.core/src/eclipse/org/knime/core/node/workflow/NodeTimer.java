/*
 * ------------------------------------------------------------------------
 *
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
 *   Mar 11, 2015 (Berthold): created
 */
package org.knime.core.node.workflow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

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

/**
 * Holds execution timing information about a specific node.
 * @noreference This class is not intended to be referenced by clients.
 * @author Michael Berthold
 */
public final class NodeTimer {

    private final NodeContainer m_parent;
    private long m_startTime;
    private long m_lastExecutionDuration;
    private long m_executionDurationSinceReset;
    private long m_executionDurationOverall;
    private int m_numberOfExecutionsSinceReset;
    private int m_numberOfExecutionsOverall;

    public static final class GlobalNodeStats {

        private static final NodeLogger LOGGER = NodeLogger.getLogger(GlobalNodeStats.class);

        private class NodeStats {
            long executionTime = 0;
            int executionCount = 0;
            int creationCount = 0;
            String likelySuccessor = "n/a";
        }
        private LinkedHashMap<String, NodeStats> m_globalNodeStats = new LinkedHashMap<String, NodeStats>();

        private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        private String m_created = DATE_FORMAT.format(new Date());
        private long m_avgUpTime = 0;
        private long m_currentInstanceLaunchTime = System.currentTimeMillis();
        private int m_launches = 0;
        private int m_crashes = 0;
        private long m_timeOfLastSave = System.currentTimeMillis() - SAVEINTERVAL + 1000*60;
        private static final long SAVEINTERVAL = 15*60*1000;  // save no more than every 15mins
        private static final String FILENAME = "nodeusage.json";

        private static final boolean DISABLE_GLOBAL_TIMER = Boolean.getBoolean("knime.globaltimer.disable");

        GlobalNodeStats() {
            if (DISABLE_GLOBAL_TIMER) {
                LOGGER.debug("Global Timer disabled due to system property");
                return;
            }
            readFromFile();
            Thread hook = new Thread() {
                @Override
                public void run() {
                    writeToFile(true);
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);
        }

        void addExecutionTime(final String cname, final long exectime) {
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
                ns.executionCount++;
                if (System.currentTimeMillis() > m_timeOfLastSave + SAVEINTERVAL) {
                    asyncWriteToFile();
                    m_timeOfLastSave = System.currentTimeMillis();
                }
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
                if (System.currentTimeMillis() > m_timeOfLastSave + SAVEINTERVAL) {
                    asyncWriteToFile();
                    m_timeOfLastSave = System.currentTimeMillis();
                }
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
                if ((ns.likelySuccessor.equals("n/a")) | (Math.random() >= .5)) {
                    ns.likelySuccessor = NodeTimer.getCanonicalName(dest);
                }
                if (System.currentTimeMillis() > m_timeOfLastSave + SAVEINTERVAL) {
                    asyncWriteToFile();
                    m_timeOfLastSave = System.currentTimeMillis();
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
            // TODO: double check that we can not possibly run into a deadlock via the ExecutionContext?!
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
        public long getAvgUpTime() {
            return (m_avgUpTime * m_launches + (System.currentTimeMillis() - m_currentInstanceLaunchTime)) / (m_launches + 1);
        }
        public int getNrLaunches() {
            return m_launches + 1;
        }
        public int getNrCrashes() {
            return m_crashes;
        }

        private void writeToFile(final boolean properShutdown) {
            try {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("version", KNIMEConstants.VERSION);
                job.add("created", m_created);
                JsonObjectBuilder job2 = Json.createObjectBuilder();
                synchronized (this) {
                    for (String cname : m_globalNodeStats.keySet()) {
                        JsonObjectBuilder job3 = Json.createObjectBuilder();
                        NodeStats ns = m_globalNodeStats.get(cname);
                        if (ns != null) {
                            job3.add("nrexecs", ns.executionCount);
                            job3.add("exectime", ns.executionTime);
                            job3.add("nrcreated", ns.creationCount);
                            job3.add("successor", ns.likelySuccessor);
                            job2.add(cname, job3);
                        }
                    }
                }
                job.add("nodestats", job2);
                job.add("uptime", getAvgUpTime());
                job.add("launches", getNrLaunches());
                job.add("crashes", getNrCrashes());
                job.add("properlyShutDown", properShutdown);
                JsonObject jo = job.build();
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                Map<String, Boolean> cfg = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                try (JsonWriter jw = Json.createWriterFactory(cfg).createWriter(new FileOutputStream(propfile))) {
                    jw.write(jo);
                }
                LOGGER.debug("Successfully wrote node usage stats to file: " + propfile.getCanonicalPath());
            } catch (IOException ioe) {
                LOGGER.warn("Failed to write node usage stats to file.", ioe);
            }
        }

        private void asyncWriteToFile() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeToFile(false);
                }
            }, "KNIME-Node-Usage-Writer").start();
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
                for (String key : jo.keySet()) {
                    switch (key) {
                        case "version":
                            // ignored (for now)
                            break;
                        case "created":
                            m_created = jo.getString(key);
                            break;
                        case "nodestats":
                            JsonObject jo2 = jo.getJsonObject(key);
                            for (String key2 : jo2.keySet()) {
                                // key represents name of NodeModel
                                JsonObject job3 = jo2.getJsonObject(key2);
                                int execCount = job3.getInt("nrexecs", 0);
                                JsonNumber num = job3.getJsonNumber("exectime");
                                Long time = num == null ? 0 : num.longValue();
                                int creationCount = job3.getInt("nrcreated", 0);
                                String successor = job3.getString("successor", "");
                                NodeStats ns = new NodeStats();
                                ns.creationCount = execCount;
                                ns.executionTime = time;
                                ns.creationCount = creationCount;
                                ns.likelySuccessor = successor;
                                m_globalNodeStats.put(key2, ns);
                            }
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
                LOGGER.warn("Failed reading node usage file", e);
            }
        }
    }
    public static final GlobalNodeStats GLOBAL_TIMER = new GlobalNodeStats();

    private static String getCanonicalName(final NodeContainer nc) {
        String cname = "NodeContainer";
        if (nc instanceof NativeNodeContainer) {
            cname = ((NativeNodeContainer)nc).getNodeModel().getClass().getName();
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

    private void initialize() {
        m_startTime = -1;
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
            GLOBAL_TIMER.addExecutionTime(cname, m_lastExecutionDuration);
        }
        m_startTime = -1;
    }

}
