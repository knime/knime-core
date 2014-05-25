/*
 * ------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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

    private final String m_namePattern;

    private final String m_pathPattern;

    private final Collection<File> m_testRootDirs = new ArrayList<File>();

    /**
     * Registry for all testing workflows. Note that only of the first two arguments should be non-null.
     *
     * @param testNamePattern the pattern test names are matched against (regular expression). If <code>null</code> (or
     *            empty) all tests are run.
     * @param testPathPattern the pattern workflow paths are matched against (regular expression). If <code>null</code>
     *            or empty all tests are run.
     * @param testRootDir a directory that is recursively searched for workflows
     */
    public TestflowCollector(final String testNamePattern, final String testPathPattern, final File testRootDir) {
        this(testNamePattern, testPathPattern, Collections.singleton(testRootDir));
    }

    /**
     * Registry for all testing workflows. Note that only of the first two arguments should be non-null.
     *
     * @param testNamePattern the pattern workflow names are matched against (regular expression). If <code>null</code>
     *            or empty all tests are run.
     * @param testPathPattern the pattern workflow paths are matched against (regular expression). If <code>null</code>
     *            or empty all tests are run.
     * @param testRootDirs a collection of directories that are recursively searched for workflows
     */
    public TestflowCollector(final String testNamePattern, final String testPathPattern,
        final Collection<File> testRootDirs) {
        if (testRootDirs == null) {
            throw new IllegalArgumentException("Root dir for tests must not be null");
        }
        for (File dir : testRootDirs) {
            if (!dir.isDirectory()) {
                throw new IllegalArgumentException("Root dir '" + dir + "' for tests must be an existing directory");
            }
        }

        if ((testNamePattern == null) || testNamePattern.isEmpty()) {
            m_namePattern = ".*";
        } else {
            m_namePattern = testNamePattern;
        }
        if ((testPathPattern == null) || testPathPattern.isEmpty()) {
            m_pathPattern = ".*";
        } else {
            m_pathPattern = testPathPattern.replace('/', File.separatorChar); // fix path separators under Windows
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
            String workflowName = currentDir.getName();
            String workflowPath = currentDir.getAbsolutePath().substring(rootDir.getAbsolutePath().length());
            if (workflowName.matches(m_namePattern) && workflowPath.matches(m_pathPattern)) {
                WorkflowTestSuite testCase =
                    new WorkflowTestSuite(workflowFile.getParentFile(), rootDir, runConfiguration, null);
                tests.add(testCase);
            } else {
                logger.info("Skipping testcase '" + workflowPath + "' (doesn't match name pattern '" + m_namePattern
                    + "'" + " and/or path pattern '" + m_pathPattern + "').");
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
                String workflowName = zipList[i].getName();
                String workflowPath = zipList[i].getAbsolutePath().substring(rootDir.getAbsolutePath().length());

                if (workflowName.matches(m_namePattern) && workflowPath.matches(workflowPath)) {
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
