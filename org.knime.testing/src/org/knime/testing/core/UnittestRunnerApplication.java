/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2010
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
package org.knime.testing.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.knime.DummyClass;

/**
 * This application runs all Unit tests it can find. It collects all classes in
 * the same classpath entry as {@link DummyClass}. This can either be a
 * directory (if started from within the IDE) or a JAR file (if started from an
 * Eclipse installation).
 *
 *
 * @author Thorsten Meinl, University of Konstanz
 *
 */
@SuppressWarnings("restriction")
public class UnittestRunnerApplication implements IApplication {
    private boolean m_stopped;

    private File m_destDir;

    @Override
    public Object start(final IApplicationContext context) throws Exception {
        context.applicationRunning();
        Object args =
                context.getArguments()
                        .get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_destDir == null)) {
            printUsage();
            return EXIT_OK;
        }
        m_destDir.mkdirs();

        // find the correct classpath entry
        BaseClassLoader cl = (BaseClassLoader)getClass().getClassLoader();
        ClasspathManager cpm = cl.getClasspathManager();
        String dummyClassPath =
                DummyClass.class.getName().replace(".", "/") + ".class";

        BundleEntry be = cpm.findLocalEntry(dummyClassPath);
        URL localUrl = be.getLocalURL();

        // collect all classes inside the director or jar file
        List<String> classNames;
        if ("file".equals(localUrl.getProtocol())) {
            String path = localUrl.getPath();
            path = path.replaceFirst(Pattern.quote(dummyClassPath) + "$", "");
            classNames = collectInDirectory(new File(path), "");
        } else if ("jar".equals(localUrl.getProtocol())) {
            String path =
                    localUrl.getPath().replaceFirst("^file:", "")
                            .replaceFirst("\\!.+$", "");
            classNames = collectInJar(new JarFile(path));
        } else {
            throw new IllegalStateException("Cannot read from protocol '"
                    + localUrl.getProtocol() + "'");
        }

        classNames.remove(DummyClass.class.getName());

        // run the tests
        for (String className : classNames) {
            if (m_stopped) {
                System.err.println("Tests aborted");
                break;
            }

            System.out.println("======= Running " + className + " =======");
            JUnitTest junitTest = new JUnitTest(className);
            JUnitTestRunner runner =
                    new JUnitTestRunner(junitTest, false, false, false, this
                            .getClass().getClassLoader());
            XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
            OutputStream out =
                    new FileOutputStream(
                            new File(m_destDir, className + ".xml"));
            formatter.setOutput(out);
            runner.addFormatter(formatter);
            runner.run();
            out.close();
        }

        return EXIT_OK;
    }

    @Override
    public synchronized void stop() {
        m_stopped = true;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything
     * went smooth, false if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line
     *         arguments, false, if an error message was printed and the
     *         application must exit.
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
            System.err.println("Unable to read application's arguments."
                    + " (was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '"
                    + args.toString() + "')");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {

            // "-destDir" specifies the destination directory
            if ((stringArgs[i] != null) && stringArgs[i].equals("-destDir")) {
                if (m_destDir != null) {
                    System.err.println("You can't specify multiple -destDir "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err
                            .println("Missing <dir_name> for option -destDir.");
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

        System.err.println("    -outputDir <dir_name>: specifies the"
                + " directory into which the test results are written.");
    }

    /**
     * Recursively Collects and returns all classes (excluding inner classes)
     * that are in the specified directory.
     *
     * @param directory the directory
     * @param packageName the package name, initially the empty string
     * @return a list with class names
     */
    private List<String> collectInDirectory(final File directory,
            final String packageName) {
        List<String> classNames = new ArrayList<String>();

        for (File f : directory.listFiles()) {
            if (f.isDirectory()) {
                classNames.addAll(collectInDirectory(f,
                        packageName + f.getName() + "."));
            } else if (f.getName().endsWith(".class")
                    && !f.getName().contains("$")) {
                String s =
                        packageName + f.getName().replaceFirst("\\.class$", "");
                classNames.add(s);
            }
        }

        return classNames;
    }

    /**
     * Collects and returns all classes inside the given JAR file (excluding
     * inner classes).
     *
     * @param jar the jar file
     * @return a list with class names
     * @throws IOException if an I/O error occurs
     */
    private List<String> collectInJar(final JarFile jar) throws IOException {
        List<String> classNames = new ArrayList<String>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            String s = e.getName();
            if (s.endsWith(".class") && !s.contains("$")) {
                s = s.replaceFirst("\\.class$", "");
                classNames.add(s.replace('/', '.'));
            }
        }
        jar.close();
        return classNames;
    }
}
