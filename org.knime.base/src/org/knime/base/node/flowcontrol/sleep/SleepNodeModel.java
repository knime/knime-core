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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.flowcontrol.sleep;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Calendar;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.util.FileUtil;

/**
 * A simple breakpoint node which allows to halt execution when a certain condition on the input table is fulfilled
 * (such as is-empty, is-inactive, is-active, ...).
 *
 * @author M. Berthold, University of Konstanz
 */
public class SleepNodeModel extends NodeModel {

    /**
     * Wait for, wait to or wait for file.
     */
    public static final String CFGKEY_WAITOPTION = "wait_option";

    /**
     * Hours to wait for.
     */
    static final String CFGKEY_FORHOURS = "for_hours";

    /**
     * Minutes to wait for.
     */
    static final String CFGKEY_FORMINUTES = "for_minutes";

    /**
     * Seconds to wait for.
     */
    static final String CFGKEY_FORSECONDS = "for_seconds";

    /**
     * Hours to wait to.
     */
    public static final String CFGKEY_TOHOURS = "to_hours";

    /**
     * Minutes to wait to.
     */
    public static final String CFGKEY_TOMINUTES = "to_min";

    /**
     * Seconds to wait to.
     */
    public static final String CFGKEY_TOSECONDS = "to_seconds";

    /**
     * Path to file to wait for.
     */
    public static final String CFGKEY_FILEPATH = "path_to_file";

    /**
     * File event to observe.
     */
    public static final String CFGKEY_FILESTATUS = "file_status_to_observe";

    private int m_toHours = 0;

    private int m_toMin = 0;

    private int m_toSec = 0;

    private long m_waittime = 0;

    private int m_selection;

    private String m_fileStatus;

    private String m_filePath;

    private int m_forHours;

    private int m_forMin;

    private int m_forSec;

    /**
     * One input, one output.
     */
    protected SleepNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_waittime = m_forSec * 1000 // seconds to milliseconds
            + m_forMin * 60 * 1000 // minutes to milliseconds
            + m_forHours * 60 * 60 * 1000; // hours to milliseconds
        return inSpecs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {

        if (m_selection == 0) {
            // wait for
            exec.setMessage("Waiting for " + (m_waittime / 1000) + " seconds");
            waitFor(m_waittime);
        } else if (m_selection == 1) {
            // wait to
            Calendar c = Calendar.getInstance();
            c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), m_toHours, m_toMin, m_toSec);
            if (c.getTimeInMillis() - System.currentTimeMillis() <= 0) {
                c.add(Calendar.DAY_OF_YEAR, 1);
            }
            exec.setMessage("Waiting until " + c.getTime());
            final long sleepTime = c.getTimeInMillis() - System.currentTimeMillis();
            waitFor(sleepTime);
        } else if (m_selection == 2) {
            WatchService w = FileSystems.getDefault().newWatchService();
            Path p = FileUtil.resolveToPath(FileUtil.toURL(m_filePath));
            if (p == null) {
                throw new IllegalArgumentException("File location '" + m_filePath + "' is not a local file.");
            }

            exec.setMessage("Waiting for file '" + p + "'");
            Path fileName = p.subpath(p.getNameCount() - 1, p.getNameCount());
            Path parent = p.getParent();
            Kind<Path> e = null;
            if (m_fileStatus.equals("Creation")) {
                e = StandardWatchEventKinds.ENTRY_CREATE;
            } else if (m_fileStatus.equals("Modification")) {
                e = StandardWatchEventKinds.ENTRY_MODIFY;
            } else if (m_fileStatus.equals("Deletion")) {
                e = StandardWatchEventKinds.ENTRY_DELETE;
            } else {
                throw new RuntimeException("Selected watchservice event is not available. Selected watchservice : "
                    + m_fileStatus);
            }
            parent.register(w, e);

            boolean keepLooking = true;

            while (keepLooking) {
                // watch file until the event appears
                WatchKey key;
                // wait for a key to be available
                key = w.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (fileName.equals(event.context())) {
                        keepLooking = false;
                    }
                }

                // reset key
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        }

        if (inData[0] == null) {
            return new PortObject[]{FlowVariablePortObject.INSTANCE};
        } else {
            return inData;
        }
    }

    private static void waitFor(final long delay) throws ExecutionException {
        Callable<Void> c = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Thread.sleep(delay);
                return null;
            }
        };

        KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        int selection = settings.getInt(CFGKEY_WAITOPTION);
        if (!(0 <= selection && selection <= 2)) {
            throw new InvalidSettingsException("Selected wait option is not available. Please reconfigure.");
        }

        int h = 0;
        int m = 0;
        int s = 0;
        if (selection == 0) {
            h = settings.getInt(CFGKEY_FORHOURS);
            m = settings.getInt(CFGKEY_FORMINUTES);
            s = settings.getInt(CFGKEY_FORSECONDS);
        } else if (selection == 1) {
            h = settings.getInt(CFGKEY_TOHOURS);
            m = settings.getInt(CFGKEY_TOMINUTES);
            s = settings.getInt(CFGKEY_TOSECONDS);
        }

        if (!(0 <= h) && !(h <= 23)) {
            throw new InvalidSettingsException("Number of hours must be between 0 and 23. Hours = " + h + ".");
        } else if (!(0 <= m) && !(m <= 59)) {
            throw new InvalidSettingsException("Number of minutes must be between 0 and 59. Minutes = " + m + ".");
        } else if (!(0 <= s) && !(s <= 59)) {
            throw new InvalidSettingsException("Number of seconds must be between 0 and 59. Secondss = " + s + ".");
        }

        if (selection == 2) {
            SettingsModelString sms = new SettingsModelString(CFGKEY_FILESTATUS, null);
            sms.loadSettingsFrom(settings);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selection = settings.getInt(CFGKEY_WAITOPTION);

        if (m_selection == 0) {
            m_forHours = settings.getInt(CFGKEY_FORHOURS);
            m_forMin = settings.getInt(CFGKEY_FORMINUTES);
            m_forSec = settings.getInt(CFGKEY_FORSECONDS);
        } else if (m_selection == 1) {
            m_toHours = settings.getInt(CFGKEY_TOHOURS);
            m_toMin = settings.getInt(CFGKEY_TOMINUTES);
            m_toSec = settings.getInt(CFGKEY_TOSECONDS);
        } else if (m_selection == 2) {
            m_filePath = settings.getString(CFGKEY_FILEPATH);
            SettingsModelString sms = new SettingsModelString(CFGKEY_FILESTATUS, null);
            sms.loadSettingsFrom(settings);
            m_fileStatus = sms.getStringValue();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // ignore -> no view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(CFGKEY_WAITOPTION, m_selection);

        settings.addInt(CFGKEY_FORHOURS, m_forHours);
        settings.addInt(CFGKEY_FORMINUTES, m_forMin);
        settings.addInt(CFGKEY_FORSECONDS, m_forSec);

        settings.addInt(CFGKEY_TOHOURS, m_toHours);
        settings.addInt(CFGKEY_TOMINUTES, m_toMin);
        settings.addInt(CFGKEY_TOSECONDS, m_toSec);

        settings.addString(CFGKEY_FILEPATH, m_filePath);
        SettingsModelString sms = new SettingsModelString(CFGKEY_FILESTATUS, "Modification");
        sms.setStringValue(m_fileStatus);
        sms.saveSettingsTo(settings);
    }

}
