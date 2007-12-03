/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   23.05.2006 (Pluto): created // aka Fabian Dill!
 *   02.08.2007 (ohl): not a JUnit test anymore
 */
package org.knime.testing.core;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeTestRegistry {

    /**
     * name of the file containing the testowner's email address.
     */
    public static final String OWNER_FILE = "owner";

    /* ---------- end of static stuff ---------------------- */

    private static NodeLogger m_logger =
            NodeLogger.getLogger(KnimeTestRegistry.class);

    private Collection<KnimeTestCase> m_registry;

    /**
     * pattern (regular expression) used to select test cases to run
     */
    private final String m_pattern;

    /**
     * Constructor. Isn't it?
     * 
     * @param testNamePattern the pattern test names are matched against
     *            (regular expression). If null (or empty) all tests are run.
     */
    public KnimeTestRegistry(final String testNamePattern) {
        m_registry = new ArrayList<KnimeTestCase>();

        if ((testNamePattern != null) && (testNamePattern.length() == 0)) {
            m_pattern = ".*";
        } else {
            m_pattern = testNamePattern;
        }
    }

    /**
     * @param testRootDir the dir to start the search in
     * @return all registered test cases.
     */
    public Test collectTestCases(final File testRootDir) {

        searchDirectory(testRootDir);

        if ((m_pattern != null) && (m_registry.size() == 0)) {
            System.out.println("Found no matching tests. "
                    + "Thank you very much.");
        }

        TestSuite suite = new TestSuite();
        for (KnimeTestCase test : m_registry) {
            m_logger.debug("adding: " + test.getName());
            suite.addTest(test);
        }
        return suite;
    }

    /**
     * Searches in the directory and subdirectories for workflow.knime files and
     * adds them to m_registry
     * 
     * @param dir - the basedir for the search
     */
    private void searchDirectory(File dir) {

        if (m_pattern == null) {
            // null pattern matches nothing!
            return;
        }

        File workflowFile = new File(dir, WorkflowManager.WORKFLOW_FILE);

        if (workflowFile.exists()) {
            String name = dir.getName();
            if (name.matches(m_pattern)) {
                KnimeTestCase testCase = new KnimeTestCase(workflowFile);
                testCase.setName(name);
                m_registry.add(testCase);
                File ownerFile = new File(dir, OWNER_FILE);
                if (!ownerFile.exists()) {
                    m_logger.error("Test '" + name
                            + "' is going to fail due to missing"
                            + " owner file (add a file called '" + OWNER_FILE
                            + "' with the email address of the test owner in "
                            + "the dir of the workflow file)!!");
                }
            } else {
                m_logger.info("Skipping testcase '" + name + "' (doesn't match"
                        + "pattern '" + m_pattern + "').");
            }

        } else {
            dir.listFiles();
            File[] fileList = dir.listFiles(new DirectoryFilter());
            for (int i = 0; i < fileList.length; i++) {
                searchDirectory(fileList[i]);
            }

        }
    }

    private class DirectoryFilter implements FileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }

    }

}
