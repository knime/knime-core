/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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
package org.knime.testing.core.ng;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;

/**
 * This class collects all testflows in a list of root directories and creates a {@link WorkflowTestSuite} for each
 * found workflow.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class TestflowCollector {
    private static NodeLogger logger = NodeLogger.getLogger(TestflowCollector.class);

    private final String m_pattern;

    private final Collection<File> m_testRootDirs = new ArrayList<File>();

    /**
     * Registry for all testing workflows.
     *
     * @param testNamePattern the pattern test names are matched against (regular expression). If <code>null</code> (or
     *            empty) all tests are run.
     * @param testRootDir a directory that is recursively searched for workflows
     */
    public TestflowCollector(final String testNamePattern, final File testRootDir) {
        this(testNamePattern, Collections.singleton(testRootDir));
    }

    /**
     * Registry for all testing workflows.
     *
     * @param testNamePattern the pattern test names are matched against (regular expression). If <code>null</code> or
     *            empty all tests are run.
     * @param testRootDirs a collection of directories that are recursively searched for workflows
     */
    public TestflowCollector(final String testNamePattern, final Collection<File> testRootDirs) {
        if (testRootDirs == null) {
            throw new IllegalArgumentException("Root dir for tests must not be null");
        }
        for (File dir : testRootDirs) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Root dir '" + dir + "' for tests must be an existing directory");
            }
        }

        if ((testNamePattern == null) || (testNamePattern.length() == 0)) {
            m_pattern = ".*";
        } else {
            m_pattern = testNamePattern;
        }

        m_testRootDirs.addAll(testRootDirs);
    }

    /**
     * Recursively collects all testflows in the directory given in the constructor and creates a
     * {@link WorkflowTestSuite} for each found testflow.
     *
     * @param runConfiguration configuration how the testflows should be run
     * @return all found test cases.
     * @throws IOException if an I/O error occurs
     */
    public Collection<WorkflowTestSuite> collectTestCases(final TestrunConfiguration runConfiguration)
            throws IOException {
        Collection<WorkflowTestSuite> workflowTests = new ArrayList<WorkflowTestSuite>();
        Collection<File> rootDirSnapshot = new ArrayList<File>(m_testRootDirs);
        // m_testRootDirs may be changed during search for zipped workflows
        for (File dir : rootDirSnapshot) {
            searchDirectory(dir, dir, workflowTests, runConfiguration);
        }

        return workflowTests;
    }

    /**
     * Searches in the directory and subdirectories for workflow.knime files..
     *
     * @param currentDir the current directory for the search
     * @param rootDir the root directory of all workflows
     * @param tests a collection into which the found testflows are added
     * @param runConfiguration configuration how the testflows should be run
     * @throws IOException if an I/O error occurs
     */
    private void searchDirectory(final File currentDir, final File rootDir, final Collection<WorkflowTestSuite> tests,
                                 final TestrunConfiguration runConfiguration) throws IOException {
        File workflowFile = new File(currentDir, WorkflowPersistor.WORKFLOW_FILE);

        if (workflowFile.exists()) {
            String name = currentDir.getName();
            if (name.matches(m_pattern)) {
                WorkflowTestSuite testCase =
                        new WorkflowTestSuite(workflowFile.getParentFile(), rootDir, runConfiguration, null);
                tests.add(testCase);
            } else {
                logger.info("Skipping testcase '" + name + "' (doesn't match pattern '" + m_pattern + "').");
            }
        } else {
            // recursively search directories
            File[] dirList = currentDir.listFiles(DIRECTORY_FILTER);
            if (dirList == null) {
                logger.error("I/O error accessing '" + currentDir + "'. Does it exist?!?");
                return;
            }
            for (int i = 0; i < dirList.length; i++) {
                searchDirectory(dirList[i], rootDir, tests, runConfiguration);
            }

            // check for zipped workflow(group)s
            File[] zipList = currentDir.listFiles(ZIP_FILTER);
            if (zipList == null) {
                logger.error("I/O error accessing '" + currentDir + "'. Does it exist?!?");
                return;
            }
            for (int i = 0; i < zipList.length; i++) {
                String name = zipList[i].getName();
                if (name.matches(m_pattern)) {
                    File tempDir = FileUtil.createTempDir("UnzippedTestflows");
                    m_testRootDirs.add(tempDir);
                    FileUtil.unzip(zipList[i], tempDir);
                    searchDirectory(tempDir, rootDir, tests, runConfiguration);
                }
            }
        }
    }

    private static final FileFilter DIRECTORY_FILTER = new FileFilter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File pathname) {
            return pathname.isDirectory();
        }
    };

    private static final FileFilter ZIP_FILTER = new FileFilter() {
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accept(final File pathname) {
            return pathname.getName().endsWith(".zip");
        }
    };
}
