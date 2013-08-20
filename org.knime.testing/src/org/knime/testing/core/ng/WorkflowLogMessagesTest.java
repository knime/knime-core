/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   19.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.LevelRangeFilter;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Testcase that checks for expected log messages and reported unexpected ERRORs and FATALs. An appender to the root
 * logger is installed when an instance of this class is created. All subsequent log messages are recorded and analyzed
 * while the test is {@link #run(TestResult)}. The appender is unregistered after the test has run once. Therefore it
 * cannot be re-used.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class WorkflowLogMessagesTest extends WorkflowTest {
    private WorkflowManager m_manager;

    private TestflowConfiguration m_flowConfiguration;

    private final List<LoggingEvent> m_logEvents = new ArrayList<LoggingEvent>();

    private final AppenderSkeleton m_logAppender = new AppenderSkeleton() {
        {
            LevelRangeFilter filter = new LevelRangeFilter();
            filter.setLevelMin(Level.DEBUG);
            filter.setLevelMax(Level.FATAL);
            addFilter(filter);
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }

        @Override
        public void close() {
            m_logEvents.clear();
        }

        @Override
        protected void append(final LoggingEvent event) {
            m_logEvents.add(event);
        }
    };

    WorkflowLogMessagesTest(final String workflowName) {
        super(workflowName);
        Logger.getRootLogger().addAppender(m_logAppender);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        try {
            m_flowConfiguration = new TestflowConfiguration(m_manager);
            checkLogMessages(result);
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
            m_logEvents.clear();
            Logger.getRootLogger().removeAppender(m_logAppender);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "log messages (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(final AtomicReference<WorkflowManager> managerRef) {
        m_manager = managerRef.get();
    }

    private void checkLogMessages(final TestResult result) {
        Map<Level, List<Pattern>> map = new HashMap<Level, List<Pattern>>();
        map.put(Level.ERROR, new ArrayList<Pattern>(m_flowConfiguration.getRequiredErrors()));
        map.put(Level.WARN, new ArrayList<Pattern>(m_flowConfiguration.getRequiredWarnings()));
        map.put(Level.INFO, new ArrayList<Pattern>(m_flowConfiguration.getRequiredInfos()));
        map.put(Level.DEBUG, new ArrayList<Pattern>(m_flowConfiguration.getRequiredDebugs()));

        for (LoggingEvent logEvent : m_logEvents) {
            String message = logEvent.getRenderedMessage();

            boolean expected = false;
            List<Pattern> currentList = map.get(logEvent.getLevel());
            if (currentList != null) {
                Iterator<Pattern> it = m_flowConfiguration.getRequiredErrors().iterator();
                while (it.hasNext()) {
                    Pattern p = it.next();
                    if (p.matcher(message).matches()) {
                        it.remove();
                        expected = true;
                        break;
                    }
                }
            }

            if (!expected && logEvent.getLevel().isGreaterOrEqual(Level.ERROR)) {
                result.addFailure(this, new AssertionFailedError("Unexpected " + logEvent.getLevel() + " logged: "
                        + logEvent.getRenderedMessage()));
            }
        }
    }
}
