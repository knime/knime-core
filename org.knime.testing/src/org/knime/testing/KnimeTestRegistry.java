/* -------------------------------------------------------------------
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
 *   23.05.2006 (Pluto): created
 */
package org.knime.testing;

import java.io.File;
import java.io.FileFilter;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.testing.core.KnimeTestCase;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeTestRegistry extends TestSuite {

    private static NodeLogger m_logger =
            NodeLogger.getLogger(KnimeTestRegistry.class);

    private static Collection<KnimeTestCase> m_registry;

    /**
     * pattern (regexp) used to select testcases to run 
     */
    private static String m_pattern = null;
    
    /**
     * 
     * @return all registered testcases.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        for (KnimeTestCase test : m_registry) {
            m_logger.debug("adding: " + test.getName());
            suite.addTest(test);
        }
        return suite;
    }

    /**
     * Filters workflow.knime files
     * 
     * @author ritmeier, University of Konstanz
     */
    private static class WorkflowFileFilter implements FileFilter {

        /**
         * Filters workflow.knime files
         * 
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(File pathname) {
            return pathname.getAbsolutePath().endsWith(
                    WorkflowManager.WORKFLOW_FILE);
        }
    }

    private static class DirectoryFilter implements FileFilter {

        /**
         * @see java.io.FileFilter#accept(java.io.File)
         */
        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }

    }

    static {
        m_registry = new ArrayList<KnimeTestCase>();
        m_pattern = System.getProperty("testcase");
        if ((m_pattern == null) || (m_pattern.length() < 1)) {
            m_pattern = JOptionPane.showInputDialog(null, 
            "Enter name (regular expression) of testcase(s) to run: \n"
                    + "(Cancel runs all.)");
        }
        try {
            
            // the workflows to run are at the root dir of the project
            final String TEST_REPOSITORY_DIR = "testWorkflows";
            final String PROJECT_DIR = "org.knime.testing";
            
            File testWorkflowsDir =
                new File(KnimeTestRegistry.class.getResource(".").toURI());
            // go down the dir tree until we are at the src root
            while (!testWorkflowsDir.getName().equalsIgnoreCase(PROJECT_DIR)) {
                testWorkflowsDir = testWorkflowsDir.getParentFile();
                assert testWorkflowsDir != null : "Couldn't find the project "
                    + "dir '" + PROJECT_DIR 
                    + "' (on my search of the test repository. Did you "
                    + "rename the project?!?";
            }
            testWorkflowsDir = new File(testWorkflowsDir, TEST_REPOSITORY_DIR);
            
            if (!testWorkflowsDir.isDirectory()) {
                throw new IllegalStateException(testWorkflowsDir
                        .getAbsolutePath()
                        + " is no directory");
            }
            searchDirectory(testWorkflowsDir);
            // Collection c = new ArrayList<KnimeTestCase>(m_registry);
            // System.out.println(c.toString());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Searches in the directory and subdirectories for workflow.knime files and
     * adds them to m_registry
     * 
     * @param dir - the basedir for the search
     */
    private static void searchDirectory(File dir) {

        File[] workflowFile = dir.listFiles(new WorkflowFileFilter());

        if (workflowFile != null && workflowFile.length == 1) {
            String name = workflowFile[0].getParentFile().getName();
            if ((m_pattern == null) || (m_pattern.length() < 1)
                    || name.matches(m_pattern)) {
                KnimeTestCase testCase = new KnimeTestCase(workflowFile[0]);
                testCase.setName(name);
                m_registry.add(testCase);
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

}
