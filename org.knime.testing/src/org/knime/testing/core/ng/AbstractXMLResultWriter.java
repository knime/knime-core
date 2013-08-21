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
 *   20.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestFailure;
import junit.framework.TestListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Abstract base class for result writer that create JUnit compliant XML output. See the XSD in this package for details
 * about the format.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public abstract class AbstractXMLResultWriter implements TestListener {
    /**
     * Map with the start times of each test (in milliseconds since the epoch).
     */
    protected final Map<Test, Long> m_startTimes = new HashMap<Test, Long>();

    /**
     * Map with the end times of each test (in milliseconds since the epoch).
     */
    protected final Map<Test, Long> m_endTimes = new HashMap<Test, Long>();

    /**
     * Document builder for creating XML documents.
     */
    protected final DocumentBuilder m_docBuilder;

    /**
     * Transformer for serializing XML documents.
     */
    protected final Transformer m_serializer;

    private final DateFormat m_timestampFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    /**
     * Creates a new result writer.
     *
     * @throws ParserConfigurationException if the document builder cannot be created
     * @throws TransformerConfigurationException if the serializer cannot be created
     */
    public AbstractXMLResultWriter() throws ParserConfigurationException, TransformerConfigurationException {
        m_docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        m_serializer = TransformerFactory.newInstance().newTransformer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addError(final Test test, final Throwable t) {
        //  nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFailure(final Test test, final AssertionFailedError t) {
        // nothing to do here
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endTest(final Test test) {
        m_endTimes.put(test, System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startTest(final Test test) {
        m_startTimes.put(test, System.currentTimeMillis());
    }

    /**
     * Analyzes the the workflow test result and creates an XML <tt>&lt;testsuite></tt> element.
     *
     * @param result the result of a workflow test suite
     * @param doc the document from which the elements should be created
     * @return a new XML element containing the test suite
     */
    protected final Element createTestsuiteElement(final WorkflowTestResult result, final Document doc) {
        Element testSuite = doc.createElement("testsuite");

        testSuite.setAttribute("name", formatWorkflowName(result.getSuite().getName()));
        testSuite.setAttribute("tests", Integer.toString(result.runCount()));
        testSuite.setAttribute("failures", Integer.toString(result.failureCount()));
        testSuite.setAttribute("errors", Integer.toString(result.errorCount()));
        testSuite.setAttribute("time", Double.toString((m_endTimes.get(result.getSuite()) - m_startTimes.get(result
                .getSuite())) / 1000.0));
        testSuite.setAttribute("timestamp", m_timestampFormat.format(new Date(m_startTimes.get(result.getSuite()))));
        try {
            testSuite.setAttribute("hostname", InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException ex) {
            // TODO add log output
            testSuite.setAttribute("hostname", "<unknown>");
        }

        addTestcases(result, doc, testSuite);

        Element sysout = doc.createElement("system-out");
        testSuite.appendChild(sysout);
        sysout.appendChild(doc.createTextNode(result.getSystemOut()));

        Element syserr = doc.createElement("system-err");
        testSuite.appendChild(syserr);
        syserr.appendChild(doc.createTextNode(result.getSystemErr()));

        return testSuite;
    }

    private void addTestcases(final WorkflowTestResult result, final Document doc, final Element testSuite) {
        Map<Test, Element> testcases = new HashMap<Test, Element>();

        for (Test t : result.getAllTests()) {
            if ((t instanceof WorkflowTest) && !(t instanceof WorkflowTestSuite)) {
                Element tc = createTestcaseElement((WorkflowTest)t, doc);
                testSuite.appendChild(tc);
                testcases.put(t, tc);
            }
        }

        processIssues(result.failures(), "failure", doc, testSuite, testcases);
        processIssues(result.errors(), "failure", doc, testSuite, testcases);
    }

    private void processIssues(final Enumeration<TestFailure> issues, final String type, final Document doc,
                               final Element testSuite, final Map<Test, Element> testcases) {
        while (issues.hasMoreElements()) {
            TestFailure f = issues.nextElement();

            Element tc = testcases.get(f.failedTest());
            if (tc == null) {
                throw new IllegalStateException("Element for testcasse '" + f.failedTest() + "' not found");
            }

            Element failure = doc.createElement(type);
            tc.appendChild(failure);
            failure.setAttribute("message", f.exceptionMessage());
            failure.setAttribute("type", f.thrownException().getClass().getName());

            StringWriter buf = new StringWriter();
            f.thrownException().printStackTrace(new PrintWriter(buf));
            failure.appendChild(doc.createTextNode(buf.toString()));
        }
    }


    private Element createTestcaseElement(final WorkflowTest test, final Document doc) {
        Element tc = doc.createElement("testcase");
        tc.setAttribute("name", test.getName());
        tc.setAttribute("classname", formatWorkflowName(test.getWorkflowName()));
        tc.setAttribute("time", Double.toString((m_endTimes.get(test) - m_startTimes.get(test)) / 1000.0));
        return tc;
    }

    private static String formatWorkflowName(final String workflowName) {
        return workflowName.replace('/', '.').replace('\\', '.').replaceAll("[\\s\\(\\)]+", "_");
    }

    /**
     * This method is called before the first test suite is executed.
     */
    public abstract void startSuites();

    /**
     * This method is called after the last test suite has been executed.
     */
    public abstract void endSuites();

    /**
     * Writes out the given workflow test results.
     *
     * @param results a collection with test results
     * @throws TransformerException if an error occurs while writing the XML
     * @throws IOException if an I/O error occurs while writing the results
     */
    public abstract void writeResult(Collection<WorkflowTestResult> results) throws TransformerException, IOException;
}
