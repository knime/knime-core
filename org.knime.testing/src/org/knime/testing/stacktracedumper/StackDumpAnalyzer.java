/*
 * ------------------------------------------------------------------
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
 * History
 *   30.01.2008 (ohl): created
 */
package org.knime.testing.stacktracedumper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFileChooser;

import org.knime.core.node.NodeLogger;

/**
 * This class goes together with the {@link StacktraceDumper}. It analyzes the
 * file the dumper class created and writes the result into an HTML file (see
 * {@link #writeToHTML(File)}). <br />
 * The generated HTML file shows the call tree for each thread with the
 * percentage of the "time" spent in sub methods. The time spent is really the
 * number of stack traces (in percent of all stack traces for this
 * thread/method) in which this sub method occurred above the method. It is not
 * the real CPU time spent.<br />
 * The provided main method pops open a file chooser, analyzes the selected file
 * and write the result to a new file with &quot;.html&quot; appended to the
 * selected file name
 *
 * @author ohl, University of Konstanz
 */
public class StackDumpAnalyzer {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(StackDumpAnalyzer.class);

    private static final String CRLF = "\r\n";

    private final BufferedReader m_reader;

    /**
     * The names of the threads seen in the dump with the methods they have been
     * executing.
     */
    private final HashMap<String, StackElementCount> m_threads =
            new HashMap<String, StackElementCount>();

    private final HashSet<StackElementCount> m_allMethods =
            new HashSet<StackElementCount>();

    /**
     * Analyzes a stack trace dump file and ... who knows...
     *
     * @param dumpFile the file to analyze. Must be created with the
     *            {@link StacktraceDumper}.
     * @throws IOException if it can't read from the specified file.
     * @see StacktraceDumper
     */
    public StackDumpAnalyzer(final String dumpFile) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(dumpFile));
        } catch (IOException ioe) {
            LOGGER.error("Can't open dump file. Not analyzing stack traces.");
            m_reader = null;
            return;
        }

        m_reader = reader;

        // read stack dumps while there are
        boolean result = true;
        while (result) {
            result = readOneStackDump();
        }

    }

    /**
     * Reads one stacktrace dump (with all threads) and stores it. Returns false
     * if the file ended before a full dump was read in.
     *
     * @return true if it read successfully another dump - false if the file
     *         ended before
     * @throws IOException
     */
    private boolean readOneStackDump() throws IOException {

        String startLine =
                getLineContaining(StacktraceDumper.DUMPSTART, m_reader);
        if (startLine == null) {
            return false;
        }

        while (true) {
            String threadHdr = m_reader.readLine();
            if (threadHdr.contains(StacktraceDumper.DUMPEND)) {
                // done with the dump - we can return
                return true;
            }

            String threadName = extractThreadName(threadHdr);
            StackElementCount threadMethods = m_threads.get(threadName);
            if (threadMethods == null) {
                threadMethods = createStackElementCount(threadName, null, "");
                m_threads.put(threadName, threadMethods);
            }

            // count how often we see this thread
            threadMethods.incrCount();

            boolean result = readOneThreadDump(threadMethods);

            if (!result) {
                return false;
            }

        }

    }

    /**
     * Reads one stack trace for one thread. Adds the first method to the
     * thread's methods passed in.
     *
     * @param threadMethods the methods the thread - to which the dump belongs
     *            to - calls. Will be modified, if the first method in the trace
     *            is not in the set yet.
     * @throws IOException if an IOErr occurred.
     */
    private boolean readOneThreadDump(final StackElementCount threadMethods)
            throws IOException {

        StackElementCount element = null;
        StackElementCount caller = threadMethods;

        while (true) {
            String line = m_reader.readLine();
            if (line == null) {
                return false;
            }
            if (line.contains(StacktraceDumper.THREADEND)) {
                return true;
            }

            /**
             * 0 = class name<br>
             * 1 = file name<br>
             * 2 = line number<br>
             * 3 = method name<br>
             */
            String[] section = line.split("\\|");
            if (section.length != 4) {
                LOGGER.error("Invalid line: " + line);
                return false;
            }

            String methodID = section[0] + "#" + section[3] + "@" + section[2];
            String calledMethodName = section[0] + "#" + section[3];

            element =
                    caller.getOrCreateSubMethodElement(methodID,
                            calledMethodName, section[2]);
            element.incrCount();

            caller = element;
        }

    }

    private String extractThreadName(final String line) {
        int startIdx = StacktraceDumper.THREADSTART.length();
        int endIdx = line.length() - StacktraceDumper.LINEENDTAG.length();

        return line.substring(startIdx, endIdx);
    }

    /**
     * Reads lines from the reader and discards them, until it reads a line
     * containing the passed key. This line is returned then.
     *
     * @param key the string that must be contained in the line returned
     * @param reader to read the lines from
     * @return the next line in the reader that contains the argument. All lines
     *         before that line are read and discarded.
     *
     * @throws IOException if it can't read from the reader
     */
    private String getLineContaining(final String key,
            final BufferedReader reader) throws IOException {

        String result;
        while ((result = reader.readLine()) != null) {

            if (result.indexOf(key) >= 0) {
                break;
            }
        }
        return result;

    }

    /**
     * Creates a new stack element object. Only create objects for methods and
     * callers that don't exist already.
     *
     * @param methodName the method on the stack
     * @param caller the one calling this method
     * @param callerLineNumber the line number where this method is called
     * @return a new element with count zero.
     */
    private StackElementCount createStackElementCount(final String methodName,
            final StackElementCount caller, final String callerLineNumber) {

        StackElementCount result =
                new StackElementCount(methodName, caller, callerLineNumber);

        if (caller != null) {
            // only non-thread elements are added to "allMethods"
            m_allMethods.add(result);
        }
        return result;
    }

    /**
     * Counts the occurrences of one method in all stack traces. Keeps the
     * counts of all method called by this method (if any). The difference of
     * all counts of all sub methods and the count of this is the self "time" of
     * this method.<br>
     * NOTE: Instances of this class should be created only through the factory
     * method above.
     *
     * @author ohl, University of Konstanz
     */
    class StackElementCount {

        /*
         * the name of the method this element is representing
         */
        private final String m_methodName;

        /*
         * number of times this method is on the stack
         */
        private int m_count;

        /*
         * the sub methods that were called from this method - if there is none
         * this method spent all the time itself...
         */
        private final HashMap<String, StackElementCount> m_subMethods =
                new HashMap<String, StackElementCount>();

        private final StackElementCount m_caller;

        private final String m_callerLineNumber;

        /**
         * DO NOT instantiate this class directly! Use the factory method above.
         * <p>
         * Creates a stack occurrence counter for the specified method.
         *
         * @param methodName name of the method.
         * @param caller the guy calling us. Can be <code>null</code> if this
         *            is the first element on the stack.
         * @param callerLineNumber line number in the caller where this method
         *            is called
         */
        StackElementCount(final String methodName,
                final StackElementCount caller, final String callerLineNumber) {
            m_methodName = methodName;
            m_caller = caller;
            m_callerLineNumber = callerLineNumber;
        }

        /**
         * @return the methodName
         */
        public String getMethodName() {
            return m_methodName;
        }

        /**
         * @return the number of times this method appeared on a stack
         */
        public int getCount() {
            return m_count;
        }

        /**
         * Increments the count of this method by one and returns the new value.
         *
         * @return the new incremented count
         */
        public int incrCount() {
            return ++m_count;
        }

        /**
         * Returns the guy that called us.
         *
         * @return the method calling this
         */
        public StackElementCount getCaller() {
            return m_caller;
        }

        /**
         * Returns the line number from where this was called in the caller
         * method.
         *
         * @return the string representation of the line number where this was
         *         called in the caller method
         */
        public String getCallerLineNumber() {
            return m_callerLineNumber;
        }

        /**
         * Creates a new stack element called by this with a new method ID
         * (typically the className#methodName#lineNumber).
         *
         * @param methodID the method ID for the element to return (a unique ID
         *            for the caller to identify the method - typically full
         *            class name plus method name plus the line number of the
         *            caller)
         * @param methodName the name (package+class+method)
         * @param lineNumber the line number where this calls the method
         * @return the stack element for the specified method ID
         */
        public StackElementCount getOrCreateSubMethodElement(
                final String methodID, final String methodName,
                final String lineNumber) {
            StackElementCount result = m_subMethods.get(methodID);
            if (result == null) {
                result = createStackElementCount(methodName, this, lineNumber);
                m_subMethods.put(methodID, result);
            }
            return result;
        }

        /**
         * Returns the sub methods called by this method.
         *
         * @return the sub methods called by this method. DO NOT modify it!
         */
        public Collection<StackElementCount> getSubMethods() {
            return m_subMethods.values();
        }

    }

    /**
     * Writes extracted info into a text file. The generated HTML file shows the
     * call tree for each thread with the percentage of the "time" spent in sub
     * methods. The time spent is really the number of stack traces (in percent
     * of all stack traces for this thread or method) in which this sub method
     * occurred above the method. It is not the real CPU time spent.
     *
     * @param output the output file
     * @throws IOException if it occurs.
     */
    public void writeToHTML(final File output) throws IOException {

        Writer w = new BufferedWriter(new FileWriter(output));
        w.write("<html>" + CRLF);
        w.write("<body>" + CRLF);
        writeAnchor(w, "Top");

        writeThreadTable(w);

        for (StackElementCount method : m_allMethods) {
            w.write("<br>" + CRLF + "<br>" + CRLF);
            writeAnchor(w, Integer.toString(System.identityHashCode(method)));
            writeElementTable(w, method);
        }

        w.write("</body>" + CRLF);
        w.write("</html>" + CRLF);
        w.close();
    }

    private void writeThreadTable(final Writer w) throws IOException {

        w.write("<table "
                + "style=\"text-align: left; width: 850px; height: 144px;\""
                + "border=\"1\" cellpadding=\"2\" cellspacing=\"2\">" + CRLF);
        w.write("<tbody>" + CRLF);

        for (Map.Entry<String, StackElementCount> e : m_threads.entrySet()) {

            String thread = e.getKey();
            StackElementCount methods = e.getValue();

//            /*
//             * count how many stack traces we got for this thread (a thread
//             * spends all its time in its methods called)
//             */
//            int count = 0;
//            for (StackElementCount m : methods.getSubMethods()) {
//                count += m.getCount();
//            }
            // first line contains thread name plus 2 empty cells
            w.write("<tr>");
            w.write("<td>" + thread + "</td>" + CRLF);

            // all following lines contain an empty cell plus called method name
            // plus the percentage of calls to this method
            boolean firstRow = true;
            Set<StackElementCount> sortedMethods =
                    sortByCount(methods.getSubMethods());
            for (StackElementCount method : sortedMethods) {

                if (!firstRow) {
                    // only the first line has the first cell filled w/ thread
                    w.write("<tr>" + CRLF);
                    w.write("<td></td>" + CRLF);
                }
                firstRow = false;
                w.write("<td>" + "<a href=\"#"
                        + System.identityHashCode(method) + "\">"
                        + method.getMethodName() + "</a>" + "</td>" + CRLF);
                w.write("<td>" + ((int)Math.round(method.getCount() * 1000.0
                        / methods.getCount()) / 10.0) + "%</td>" + CRLF);
                w.write("</tr>" + CRLF);
            }

        }

        w.write("</tbody>" + CRLF);
        w.write("</table>" + CRLF);

    }

    private void writeElementTable(final Writer w,
            final StackElementCount method) throws IOException {
        // Header for this method
        w.write("Method <b>" + method.getMethodName() + "</b> ");
        if (method.getCaller().getCallerLineNumber().isEmpty()) {
            // was called by a thread
            w.write("<br>(called by thread <a href=\"#Top\">"
                    + method.getCaller().getMethodName() + "</a>");
        } else {
            w.write("<br>(called by <a href=\"#"
                    + System.identityHashCode(method.getCaller()) + "\">"
                    + method.getCaller().getMethodName() + "</a>, line "
                    + method.getCallerLineNumber());
        }
        w.write(")<br><br>" + CRLF);
        // table of all sub methods
        w.write("<table " + "style=\"text-align: left;\""
                + "border=\"1\" cellpadding=\"2\" cellspacing=\"2\">" + CRLF);
        w.write("<tbody>" + CRLF);
        // first row is self time - if any
        int subCount = 0;
        for (StackElementCount sub : method.getSubMethods()) {
            subCount += sub.getCount();
        }
        // can't spend more time in sub methods than time spend...
        assert subCount <= method.getCount();
        if (subCount < method.getCount()) {
            // the difference is self time
            int selfTime = method.getCount() - subCount;
            w.write("<tr>" + CRLF);
            w.write("<td>time all by itself</td>" + CRLF);
            w.write("<td> - </td>" + CRLF);
            w.write("<td>" + ((int)Math.round(method.getCount() * 1000.0
                    / selfTime) / 10.0) + "%</td>" + CRLF);
            w.write("</tr>" + CRLF);
        }
        // now all real sub methods
        Collection<StackElementCount> sortedMethods =
                sortByCount(method.getSubMethods());
        for (StackElementCount sub : sortedMethods) {
            w.write("<tr>" + CRLF);
            w.write("<td>" + "<a href=\"#" + System.identityHashCode(sub)
                    + "\">" + sub.getMethodName() + "</a>" + "</td>" + CRLF);
            w.write("<td>line " + sub.getCallerLineNumber() + "</td>" + CRLF);
            w.write("<td>"
                    + ((int)Math.round(method.getCount() * 1000.0
                            / sub.getCount()) / 10.0) + "%</td>" + CRLF);
            w.write("</tr>" + CRLF);
        }

        w.write("</tbody>" + CRLF);
        w.write("</table>" + CRLF);
    }

    private void writeAnchor(final Writer w, final String anchorName)
            throws IOException {
        w.write("<a name=\"" + anchorName + "\"></a>");
    }

    /**
     * Sorts the passed set of elements by their call count.
     *
     * @param methods the set to sort.
     * @return a new set with the sorted elements of the argument set. The first
     *         element is the one with the highest count.
     */
    private SortedSet<StackElementCount> sortByCount(
            final Collection<StackElementCount> methods) {
        SortedSet<StackElementCount> sorted =
                new TreeSet<StackElementCount>(
                        new Comparator<StackElementCount>() {
                            @Override
                            public int compare(final StackElementCount o1,
                                    final StackElementCount o2) {
                                if (o1.getCount() == o2.getCount()) {
                                    return 0;
                                }
                                if (o1.getCount() < o2.getCount()) {
                                    return -1;
                                }
                                return 1;
                            }
                        });
        sorted.addAll(methods);
        return sorted;
    }

    /**
     * State of Main.
     *
     * @param args the arguments to main.
     * @throws IOException if it does.
     */
    public static void main(final String[] args) throws IOException {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogTitle("Select Stacktrace Dump File");

        if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            String inFile = fc.getSelectedFile().getAbsolutePath();
            File outFile = new File(inFile + ".html");
            new StackDumpAnalyzer(inFile).writeToHTML(outFile);
            System.out.println("Written result to " + outFile);
        }

    }
}
