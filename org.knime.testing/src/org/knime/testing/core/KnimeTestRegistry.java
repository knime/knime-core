/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2011
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
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class KnimeTestRegistry {
    private static NodeLogger m_logger = NodeLogger
            .getLogger(KnimeTestRegistry.class);

    /**
     * pattern (regular expression) used to select test cases to run
     */
    private final String m_pattern;

    private final File m_testRootDir;

    private final File m_saveRootDir;

    /**
     * Constructor. Isn't it?
     *
     * @param testNamePattern the pattern test names are matched against
     *            (regular expression). If null (or empty) all tests are run.
     */
    public KnimeTestRegistry(final String testNamePattern,
            final File testRootDir, final File saveRoot) {
        if (testRootDir == null) {
            throw new NullPointerException("Root dir for tests can't be null");
        }
        if (!testRootDir.isDirectory() || !testRootDir.exists()) {
            throw new IllegalArgumentException("Root dir for tests must"
                    + " be an existing directory");
        }

        if ((testNamePattern != null) && (testNamePattern.length() == 0)) {
            m_pattern = ".*";
        } else {
            m_pattern = testNamePattern;
        }

        m_testRootDir = testRootDir;
        String saveSubDir = "savedTestFlows_";
        Calendar now = Calendar.getInstance();

        saveSubDir +=
                now.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.US);
        if (now.get(Calendar.DATE) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.DATE);
        saveSubDir += "_";
        if (now.get(Calendar.HOUR_OF_DAY) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.HOUR_OF_DAY);
        saveSubDir += "_";
        if (now.get(Calendar.MINUTE) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.MINUTE);
        saveSubDir += "_";
        if (now.get(Calendar.SECOND) < 10) {
            saveSubDir += "0";
        }
        saveSubDir += now.get(Calendar.SECOND);

        if (saveRoot != null) {
            // if specified is must be a existing dir
            if (!saveRoot.exists() || !saveRoot.isDirectory()) {
                throw new IllegalArgumentException("Location to save workflows"
                        + " must be na existing directory");
            }
            m_saveRootDir = new File(saveRoot, saveSubDir);
        } else {
            m_saveRootDir = null;
        }
    }

    /**
     * @param factory a factory for creating workflow tests
     * @return all registered test cases.
     */
    public Test collectTestCases(final WorkflowTestFactory factory) {
        Collection<WorkflowTest> workflowTests = new ArrayList<WorkflowTest>();
        searchDirectory(m_testRootDir, workflowTests, factory);

        if ((m_pattern != null) && (workflowTests.size() == 0)) {
            System.out.println("Found no matching tests. "
                    + "Thank you very much.");
        }

        TestSuite suite = new TestSuite();
        for (WorkflowTest test : workflowTests) {
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
     * @param saveRoot - the baseDir executed flows will be saved to (or null).
     */
    private void searchDirectory(final File dir,
            final Collection<WorkflowTest> tests,
            final WorkflowTestFactory factory) {
        if (m_pattern == null) {
            // null pattern matches nothing!
            return;
        }

        File workflowFile = new File(dir, WorkflowPersistor.WORKFLOW_FILE);

        if (workflowFile.exists()) {
            String name = dir.getName();
            if (name.matches(m_pattern)) {
                File saveLoc = createSaveLocation(workflowFile);
                WorkflowTest testCase =
                        factory.createTestcase(workflowFile, saveLoc);
                tests.add(testCase);
            } else {
                m_logger.info("Skipping testcase '" + name + "' (doesn't match"
                        + "pattern '" + m_pattern + "').");
            }

        } else {
            dir.listFiles();
            File[] fileList = dir.listFiles(new DirectoryFilter());
            if (fileList == null) {
                m_logger.error("I/O error accessing '" + dir
                        + "'. Does it exist?!?");
                return;
            }
            for (int i = 0; i < fileList.length; i++) {
                searchDirectory(fileList[i], tests, factory);
            }

        }
    }

    private File createSaveLocation(final File workflowDir) {
        if (m_saveRootDir == null) {
            // null indicates "do not save"
            return null;
        }

        File testName = workflowDir.getParentFile();
        // path from root to current flow dir
        String postfix =
                testName.getAbsolutePath().substring(
                        m_testRootDir.getAbsolutePath().length());
        if (postfix.isEmpty()) {
            // seems the test was in the testRoot dir
            postfix = testName.getName();
        }
        File save = new File(m_saveRootDir, postfix);
        return save;

    }

    private class DirectoryFilter implements FileFilter {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }
    }
}
