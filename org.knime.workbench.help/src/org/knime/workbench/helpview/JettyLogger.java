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
 *   04.05.2011 (meinl): created
 */
package org.knime.workbench.helpview;

import org.knime.core.node.NodeLogger;
import org.mortbay.log.Logger;
import org.mortbay.util.DateCache;

/**
 * This class is not intended to be used externally!
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class JettyLogger implements Logger {
    private static DateCache dateCache;

    static {
        try {
            dateCache = new DateCache("yyyy-MM-dd HH:mm:ss");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static boolean debug =
            System.getProperty("DEBUG", null) != null;

    private final String m_name;

    private StringBuffer m_buffer = new StringBuffer();

    private boolean m_debugEnabled;

    private final NodeLogger m_logger;

    /** Creates a new logger. */
    public JettyLogger() {
        this("org.knime.workbench.help.jetty");
    }

    private JettyLogger(final String name) {
        m_name = name;
        m_logger = NodeLogger.getLogger(m_name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled() {
        return m_debugEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDebugEnabled(final boolean enabled) {
        m_debugEnabled = enabled;
    }

    @Override
    public void info(final String msg, final Object arg0, final Object arg1) {
        String d = dateCache.now();
        int ms = dateCache.lastMs();
        synchronized (m_buffer) {
            tag(d, ms, ":INFO:");
            format(msg, arg0, arg1);
            m_logger.info(m_buffer.toString());
        }
    }

    @Override
    public void debug(final String msg, final Throwable th) {
        if (debug) {
            String d = dateCache.now();
            int ms = dateCache.lastMs();
            synchronized (m_buffer) {
                tag(d, ms, ":DBUG:");
                format(msg);
                format(th);
                m_logger.debug(m_buffer.toString(), th);
            }
        }
    }

    @Override
    public void debug(final String msg, final Object arg0, final Object arg1) {
        if (debug) {
            String d = dateCache.now();
            int ms = dateCache.lastMs();
            synchronized (m_buffer) {
                tag(d, ms, ":DBUG:");
                format(msg, arg0, arg1);
                m_logger.debug(m_buffer.toString());
            }
        }
    }

    @Override
    public void warn(final String msg, final Object arg0, final Object arg1) {
        String d = dateCache.now();
        int ms = dateCache.lastMs();
        synchronized (m_buffer) {
            tag(d, ms, ":WARN:");
            format(msg, arg0, arg1);
            m_logger.warn(m_buffer.toString());
        }
    }

    @Override
    public void warn(final String msg, final Throwable th) {
        String d = dateCache.now();
        int ms = dateCache.lastMs();
        synchronized (m_buffer) {
            tag(d, ms, ":WARN:");
            format(msg);
            format(th);
            m_logger.warn(m_buffer.toString(), th);
        }
    }

    private void tag(final String d, final int ms, final String tag) {
        m_buffer.setLength(0);
        m_buffer.append(d);
        if (ms > 99) {
            m_buffer.append('.');
        } else if (ms > 9) {
            m_buffer.append(".0");
        } else {
            m_buffer.append(".00");
        }
        m_buffer.append(ms).append(tag).append(m_name).append(':');
    }

    @SuppressWarnings("null")
    private void format(final String msg, final Object arg0, final Object arg1) {
        int i0 = msg == null ? -1 : msg.indexOf("{}");
        int i1 = i0 < 0 ? -1 : msg.indexOf("{}", i0 + 2);

        if (i0 >= 0) {
            format(msg.substring(0, i0));
            format(String.valueOf(arg0 == null ? "null" : arg0));

            if (i1 >= 0) {
                format(msg.substring(i0 + 2, i1));
                format(String.valueOf(arg1 == null ? "null" : arg1));
                format(msg.substring(i1 + 2));
            } else {
                format(msg.substring(i0 + 2));
                if (arg1 != null) {
                    m_buffer.append(' ');
                    format(String.valueOf(arg1));
                }
            }
        } else {
            format(msg);
            if (arg0 != null) {
                m_buffer.append(' ');
                format(String.valueOf(arg0));
            }
            if (arg1 != null) {
                m_buffer.append(' ');
                format(String.valueOf(arg1));
            }
        }
    }

    private void format(final String msg) {
        if (msg == null) {
            m_buffer.append("null");
        } else {
            for (int i = 0; i < msg.length(); i++) {
                char c = msg.charAt(i);
                if (Character.isISOControl(c)) {
                    if (c == '\n') {
                        m_buffer.append('|');
                    } else if (c == '\r') {
                        m_buffer.append('<');
                    } else {
                        m_buffer.append('?');
                    }
                } else {
                    m_buffer.append(c);
                }
            }
        }
    }

    private void format(final Throwable th) {
        if (th == null) {
            m_buffer.append("null");
        } else {
            m_buffer.append('\n');
            format(th.toString());
            StackTraceElement[] elements = th.getStackTrace();
            for (int i = 0; elements != null && i < elements.length; i++) {
                m_buffer.append("\n\tat ");
                format(elements[i].toString());
            }
        }
    }

    @Override
    public String toString() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Logger getLogger(final String name) {
        if (((name == null) && (m_name == null))
                || ((name != null) && name.equals(m_name))) {
            return this;
        }
        return new JettyLogger(name);
    }
}
