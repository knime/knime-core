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
 *   27.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import junit.framework.AssertionFailedError;
import junit.framework.TestResult;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.KNIMEConstants;

/**
 * Testcase that records all nodes in the loaded test workflows and compares them with the list
 * of all available nodes (by querying the extension point).
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
class UntestedNodesTest implements TestWithName {
    private final Set<String> m_testedNodes = new HashSet<String>();

    private final Pattern m_includePattern;

    UntestedNodesTest(final Pattern pattern) {
        m_includePattern = pattern;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int countTestCases() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final TestResult result) {
        result.startTest(this);

        try {
            checkTestedNodes(result);
        } catch (Throwable t) {
            result.addError(this, t);
        } finally {
            result.endTest(this);
        }
    }

    private void checkTestedNodes(final TestResult result) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.workbench.repository.nodes");
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : org.knime.workbench.repository.nodes");

        }

        Set<String> allAvailableNodes = new HashSet<String>();
        for (IExtension ext : point.getExtensions()) {
            for (IConfigurationElement e : ext.getConfigurationElements()) {
                allAvailableNodes.add(e.getAttribute("factory-class"));
            }
        }

        allAvailableNodes.removeAll(m_testedNodes);
        for (String factoryClassName : allAvailableNodes) {
            if (m_includePattern.matcher(factoryClassName).matches()) {
                result.addFailure(this, new AssertionFailedError("No testflow with " + factoryClassName + " found"));
            }
        }
    }

    /**
     * Adds the given set of factory class names to the nodes under test.
     *
     * @param set a set with factory class names
     */
    public void addNodesUnderTest(final Set<String> set) {
        m_testedNodes.addAll(set);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "untested nodes (assertions " + (KNIMEConstants.ASSERTIONS_ENABLED ? "on" : "off") + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSuiteName() {
        return getClass().getName();
    }
}
