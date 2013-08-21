/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror.JUnitTestRunnerMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;

/**
 * This application runs all Unit tests it can find. It collects all classes by querying implementations of
 * {@link org.knime.testing.core.AbstractTestcaseCollector} that are registered at the extension point.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class UnittestRunnerApplication implements IApplication {
    private volatile boolean m_stopped;

    private File m_destDir;

    @Override
    public Object start(final IApplicationContext context) throws Exception {
        PlatformUI.createDisplay(); // create a display because some tests may need it
        context.applicationRunning();
        Object args = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_destDir == null)) {
            printUsage();
            return EXIT_OK;
        }
        if (!m_destDir.isDirectory() && !m_destDir.mkdirs()) {
            throw new IOException("Could not create destination directory '" + m_destDir + "'");
        }

        Collection<Class<?>> allTests = AllJUnitTests.getAllJunitTests();
        int maxNameLength = 0;
        for (Class<?> testClass : allTests) {
            maxNameLength = Math.max(maxNameLength, testClass.getName().length());
        }

        // run the tests
        for (Class<?> testClass : allTests) {
            if (m_stopped) {
                System.err.println("Tests aborted");
                break;
            }

            System.out.printf("=> Running %-" + maxNameLength + "s ...", testClass.getName());
            JUnitTest junitTest = new JUnitTest(testClass.getName());
            final JUnitTestRunner runner =
                    new JUnitTestRunner(junitTest, false, false, false, testClass.getClassLoader());
            XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
            OutputStream out = new FileOutputStream(new File(m_destDir, testClass.getName() + ".xml"));
            formatter.setOutput(out);
            runner.addFormatter(formatter);

            Writer stdout = new Writer() {
                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    runner.handleOutput(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            };
            Writer stderr = new Writer() {

                @Override
                public void write(final char[] cbuf, final int off, final int len) throws IOException {
                    runner.handleErrorOutput(new String(cbuf, off, len));
                }

                @Override
                public void flush() throws IOException {
                }

                @Override
                public void close() throws IOException {
                }
            };

            NodeLogger.addWriter(stdout, LEVEL.DEBUG, LEVEL.FATAL);
            NodeLogger.addWriter(stderr, LEVEL.ERROR, LEVEL.FATAL);

            PrintStream oldOut = System.out;
            PrintStream oldErr = System.err;
            try {
                System.setOut(new PrintStream(new WriterOutputStream(stdout), false, "UTF-8"));
                System.setErr(new PrintStream(new WriterOutputStream(stderr), false, "UTF-8"));
                runner.run();
                System.out.flush();
                System.err.flush();
            } finally {
                System.setOut(oldOut);
                System.setErr(oldErr);
            }
            NodeLogger.removeWriter(stderr);
            NodeLogger.removeWriter(stdout);

            out.close();

            switch (runner.getRetCode()) {
                case JUnitTestRunnerMirror.SUCCESS:
                    System.out.println("OK");
                    break;
                case JUnitTestRunnerMirror.FAILURES:
                    System.out.println("FAILURE");
                    break;
                case JUnitTestRunnerMirror.ERRORS:
                    System.out.println("ERROR");
                    break;
                default:
                    System.out.println("UNKNOWN");
                    break;
            }
        }

        return EXIT_OK;
    }

    @Override
    public void stop() {
        m_stopped = true;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything went smooth, false if the application
     * must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line arguments, false, if an error message was
     *         printed and the application must exit.
     */
    private boolean extractCommandLineArgs(final Object args) {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments. Was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '" + args.toString() + "'");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {

            // "-destDir" specifies the destination directory
            if ((stringArgs[i] != null) && stringArgs[i].equals("-xmlResultDir")) {
                if (m_destDir != null) {
                    System.err.println("Multiple -xmlResultDir arguments are not allowed");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null) || (stringArgs[i].length() == 0)) {
                    System.err.println("Missing <dir_name> for option -xmlResultDir.");
                    printUsage();
                    return false;
                }
                m_destDir = new File(stringArgs[i++]);
                continue;
            }

            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -xmlResultDir <dir_name>: specifies the directory into which the test results are "
                + "written.");
    }

    private static class WriterOutputStream extends OutputStream {
        private final Writer m_writer;

        public WriterOutputStream(final Writer writer) {
            m_writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int b) throws IOException {
            m_writer.write(b);
        }
    }
}
