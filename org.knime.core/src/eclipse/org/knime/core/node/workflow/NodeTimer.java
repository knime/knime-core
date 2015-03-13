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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

import com.google.common.util.concurrent.AtomicLongMap;

/** Holds execution timing information about a specific node.
 *
 * @author Michael Berthold
 */
public class NodeTimer {

    private final NodeContainer m_parent;
    private long m_startTime;
    private long m_lastExecutionDuration;
    private long m_executionDurationSinceReset;
    private long m_executionDurationOverall;
    private int m_numberOfExecutionsSinceReset;
    private int m_numberOfExecutionsOverall;

    public static class GlobalNodeTimer {
        private static final NodeLogger LOGGER = NodeLogger.getLogger(GlobalNodeTimer.class);
        private AtomicLongMap m_exectimes = AtomicLongMap.create();
        private AtomicLongMap m_execcounts = AtomicLongMap.create();
        private long m_startTime= -1;
        private long m_avgUpTime = 0;
        private long m_currentInstanceLaunchTime = System.currentTimeMillis();
        private int m_launches = 0;
        private int m_crashes = 0;
        private long m_timeOfLastSave = System.currentTimeMillis() - SAVEINTERVAL + 1000*60;
        private static final long SAVEINTERVAL = 15*60*1000;  // save no more than every 15mins
        private static final String FILENAME = "nodeusage.log";
        GlobalNodeTimer() {
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
            m_exectimes.addAndGet(cname, exectime);
            m_execcounts.incrementAndGet(cname);
            m_startTime = System.currentTimeMillis();
            if (System.currentTimeMillis() > m_timeOfLastSave + SAVEINTERVAL) {
                asyncWriteToFile();
                m_timeOfLastSave = System.currentTimeMillis();
            }
        }
        public Set<String> getNodeNames() {
            return m_exectimes.asMap().keySet();
        }
        public long getExecutionCount(final String cname) {
            return m_execcounts.get(cname);
        }
        public long getExecutionTime(final String cname) {
            return m_exectimes.get(cname);
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
        public void writeToFile(final boolean properShutdown) {
            try {
                JsonObjectBuilder job = Json.createObjectBuilder();
                job.add("_version", "1.0");
                JsonObjectBuilder job2 = Json.createObjectBuilder();
                for (String cname : getNodeNames()) {
                    JsonObjectBuilder job3 = Json.createObjectBuilder();
                    job3.add("count", getExecutionCount(cname));
                    job3.add("time", getExecutionTime(cname));
                    job2.add(cname, job3);
                }
                job.add("_nodestats", job2);
                job.add("_uptime", getAvgUpTime());
                job.add("_launches", getNrLaunches());
                job.add("_crashes", getNrCrashes());
                job.add("_properlyShutDown", properShutdown);
                JsonObject jo = job.build();
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                FileOutputStream fos = new FileOutputStream(propfile);
                Map<String, Boolean> cfg = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
                JsonWriter jw = Json.createWriterFactory(cfg).createWriter(fos);
                jw.write(jo);
                jw.close();
                LOGGER.debug("Successfully wrote node usage stats to file: " + propfile.getCanonicalPath());
            } catch (IOException ioe) {
                LOGGER.debug("Failed to write node usage stats to file.");
                // TODO: report or just skip?
            }
        }
        public void asyncWriteToFile() {
            new Thread() {
                @Override
                public void run() {
                    writeToFile(false);
                }
            }.start();
        }
        public void readFromFile() {
            try {
                File propfile = new File(KNIMEConstants.getKNIMEHomeDir(), FILENAME);
                FileInputStream fis = new FileInputStream(propfile);
                JsonReader jr = Json.createReader(fis);
                JsonObject jo = jr.readObject();
                jr.close();
                for (String key : jo.keySet()) {
                    switch (key) {
                        case "_version":
                            String v = jo.getString(key);
                            break;
                        case "_nodestats":
                            JsonObject jo2 = jo.getJsonObject(key);
                            for (String key2 : jo2.keySet()) {
                                // key represents name of NodeModel
                                JsonObject job3 = jo2.getJsonObject(key2);
                                Long count = job3.getJsonNumber("count").longValue();
                                Long time = job3.getJsonNumber("time").longValue();
                                m_execcounts.put(key2, count);
                                m_exectimes.put(key2, time);
                            }
                            break;
                        case "_uptime":
                            m_avgUpTime = jo.getJsonNumber(key).longValue();
                            break;
                        case "_launches":
                            m_launches = jo.getInt(key);
                            break;
                        case "_crashes":
                            m_crashes = jo.getInt(key);
                            break;
                        case "_properlyShutDown":
                            if (!jo.getBoolean(key)) {
                                m_crashes++;
                            }
                        default:
                            // TODO: complain?
                    }
                }
                LOGGER.debug("Successfully read node usage stats from file: " + propfile.getCanonicalPath());
            } catch (Exception e) {
                // TODO: should we do more than just log this?
                LOGGER.debug("Failed reading node usage file");
            }
        }
    }
    public static final GlobalNodeTimer m_globalTimer = new GlobalNodeTimer();

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

    public void initialize() {
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
            String cname = "NodeContainer";
            if (m_parent instanceof NativeNodeContainer) {
                cname = ((NativeNodeContainer)m_parent).getNodeModel().getClass().getName();
            }
            m_globalTimer.addExecutionTime(cname, m_lastExecutionDuration);
        }
        m_startTime = -1;
    }

}
