/* @(#)$$RCSfile$$ 
 * $$Revision$$ $$Date$$ $$Author$$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   2005/04/19 (georg): created
 */
package de.unikn.knime.workbench.editor2;

import java.io.IOException;
import java.io.Writer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import de.unikn.knime.core.node.NodeLogger;

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
    public static final Color COLOR_FATAL_ERROR = Display.getDefault()
            .getSystemColor(SWT.COLOR_DARK_RED);

    /** Color: error. * */
    public static final Color COLOR_ERROR = Display.getDefault()
            .getSystemColor(SWT.COLOR_RED);

    /** Color: warning. * */
    public static final Color COLOR_WARN = Display.getDefault().getSystemColor(
            SWT.COLOR_BLUE);

    /** Color: info. * */
    public static final Color COLOR_INFO = Display.getDefault().getSystemColor(
            SWT.COLOR_DARK_GREEN);

    /** Color: debug. * */
    public static final Color COLOR_DEBUG = Display.getDefault()
            .getSystemColor(SWT.COLOR_GRAY);
    
    private final Color m_color;

    /** Appender: fatal error. */
    public static final ConsoleViewAppender FATAL_ERROR_APPENDER
        = new ConsoleViewAppender(
                COLOR_FATAL_ERROR, "Fatal Error", NodeLogger.LEVEL.FATAL);

    /** Appender: error. * */
    public static final ConsoleViewAppender ERROR_APPENDER
        = new ConsoleViewAppender(COLOR_ERROR, "Error", NodeLogger.LEVEL.ERROR);
    
    /** Appender: info. */
    public static final ConsoleViewAppender WARN_APPENDER
        = new ConsoleViewAppender(COLOR_WARN, "Warn", NodeLogger.LEVEL.WARN);

    /** Appender: warn. */
    public static final ConsoleViewAppender INFO_APPENDER
        = new ConsoleViewAppender(COLOR_INFO, "Info", NodeLogger.LEVEL.INFO);

    /** Appender: debug. */
    public static final ConsoleViewAppender DEBUG_APPENDER
        = new ConsoleViewAppender(COLOR_DEBUG, "DEBUG", NodeLogger.LEVEL.DEBUG);

    private final String m_name;
    
    private final NodeLogger.LEVEL m_level;
    
    /**
     * Initializes the ConsoleView appender.S.
     * 
     */
    private ConsoleViewAppender(
            final Color color, final String name, 
            final NodeLogger.LEVEL level) {
        m_color = color;
        m_name  = name;
        m_level = level;
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
     * Appends a logging message to the console log. Color is selected based
     * upon the given logging level.
     * 
     * @param text text
     */
    public void append(final String text) {
        assert text == text;
        Display.getDefault().syncExec(new Runnable() {
            public void run() {

            }
        });

    }

    // /**
    // * Writes an event to the corresponding console log.
    // *
    // * @param event The event to log
    // * @param color Text color
    // */
    // private void writeToConsole(final LoggingEvent event, final Color color)
    // {
    // }

    // /**
    // * Activates (= brings to top) the console view that is responsible for
    // the
    // * given event
    // */
    // private void activateConsole() {
    // MessageConsole console = findConsole(CONSOLE_NAME);
    //
    // IWorkbenchPage page = PlatformUI.getWorkbench()
    // .getActiveWorkbenchWindow().getActivePage();
    // if (page == null) {
    // return; // workbench may not be opened yet, ignore !
    // // TODO: cache and display later !
    // }
    //
    // String id = IConsoleConstants.ID_CONSOLE_VIEW;
    // IConsoleView view;
    // try {
    // view = (IConsoleView) page.showView(id);
    // view.display(console);
    // } catch (PartInitException e) {
    // // TODO Auto-generated catch block
    //            e.printStackTrace();
    //        }
    //
    //    }

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
                return (MessageConsole) existing[i];
            }
        }
        // no console found, so create a new one
        MessageConsole myConsole = new MessageConsole(consoleName, null);
        conMan.addConsoles(new IConsole[] {myConsole});
        return myConsole;
    }

    /**
     * @see java.io.Writer#write(char[], int, int)
     */
    public void write(final char[] cbuf, final int off, final int len)
            throws IOException {
        Display.getDefault().syncExec(new Runnable() {
            public void run() {
                MessageConsole console = findConsole(CONSOLE_NAME);
                MessageConsoleStream out = console.newMessageStream();
                //activateConsole();
                out.setColor(m_color);
                out.print(new String(cbuf, off, len));
            }
        });

    }

    /**
     * @see java.io.Writer#flush()
     */
    public void flush() throws IOException {
        // TODO Auto-generated method stub

    }

    /**
     * @see java.io.Writer#close()
     */
    public void close() throws IOException {
        // TODO Auto-generated method stub

    }

}
