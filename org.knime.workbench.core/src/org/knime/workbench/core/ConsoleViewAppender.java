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
 * -------------------------------------------------------------------
 *
 * History
 *   2005/04/19 (georg): created
 */
package org.knime.workbench.core;

import java.io.IOException;
import java.io.Writer;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.knime.core.node.NodeLogger;

/**
 * This is an implementation of a writer which logs to the ConsoleView inside
 * Eclipse.
 *
 * @author Florian Georg, University of Konstanz
 */
public final class ConsoleViewAppender extends Writer {
    /** Name for the console view. * */
    public static final String CONSOLE_NAME = "KNIME Console";

    /** Color: fatal error. * */
    public static final Color COLOR_FATAL_ERROR =
            new Color(Display.getDefault(), 0x80, 0, 0);

    /** Color: error. * */
    public static final Color COLOR_ERROR =
            new Color(Display.getDefault(), 0xFF, 0, 0);

    /** Color: warning. * */
    public static final Color COLOR_WARN =
            new Color(Display.getDefault(), 0, 0, 0xFF);

    /** Color: info. * */
    public static final Color COLOR_INFO =
            new Color(Display.getDefault(), 0, 0x80, 0);

    /** Color: debug. * */
    public static final Color COLOR_DEBUG =
            new Color(Display.getDefault(), 0x80, 0x80, 0x80);

    private final Color m_color;

    /** Appender: fatal error. */
    public static final ConsoleViewAppender FATAL_ERROR_APPENDER =
            new ConsoleViewAppender(COLOR_FATAL_ERROR, "Fatal Error",
                    NodeLogger.LEVEL.FATAL);

    /** Appender: error. * */
    public static final ConsoleViewAppender ERROR_APPENDER =
            new ConsoleViewAppender(COLOR_ERROR, "Error",
                    NodeLogger.LEVEL.ERROR);

    /** Appender: info. */
    public static final ConsoleViewAppender WARN_APPENDER =
            new ConsoleViewAppender(COLOR_WARN, "Warn", NodeLogger.LEVEL.WARN);

    /** Appender: warn. */
    public static final ConsoleViewAppender INFO_APPENDER =
            new ConsoleViewAppender(COLOR_INFO, "Info", NodeLogger.LEVEL.INFO);

    /** Appender: debug. */
    public static final ConsoleViewAppender DEBUG_APPENDER =
            new ConsoleViewAppender(COLOR_DEBUG, "Debug",
                    NodeLogger.LEVEL.DEBUG);

    private final String m_name;

    private final NodeLogger.LEVEL m_level;

    private final MessageConsoleStream m_out;



    /**
     * Initializes the ConsoleView appender.S.
     *
     */
    private ConsoleViewAppender(final Color color, final String name,
            final NodeLogger.LEVEL level) {
        m_color = color;
        m_name = name;
        m_level = level;

        MessageConsole console = findConsole(CONSOLE_NAME);
        m_out = console.newMessageStream();
    }

    /**
     * @return The name for this writer.
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return Logging level.
     */
    public NodeLogger.LEVEL getLevel() {
        return m_level;
    }

    /**
     * Looks up the console view that is responsible for the given event, does
     * not activate the view.
     *
     * @param consoleName The name of the console to look up
     */
    private MessageConsole findConsole(final String consoleName) {
        ConsolePlugin plugin = ConsolePlugin.getDefault();
        IConsoleManager conMan = plugin.getConsoleManager();
        IConsole[] existing = conMan.getConsoles();
        for (int i = 0; i < existing.length; i++) {
            if (consoleName.equals(existing[i].getName())) {
                return (MessageConsole)existing[i];
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(consoleName, null);
        conMan.addConsoles(new IConsole[]{myConsole});
        return myConsole;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(final char[] cbuf, final int off, final int len)
            throws IOException {
        // make new string here as the caller reuses the char[]
        final String str = new String(cbuf, off, len);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                // activateConsole();
                m_out.setColor(m_color);
                m_out.print(str);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        // nothing to do here
    }
}
