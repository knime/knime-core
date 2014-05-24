/*
 * ------------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 *
 * Created on 05.06.2013 by thor
 */
package org.knime.testing.core.ng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;
import org.knime.testing.core.AbstractTestcaseCollector;

/**
 * A JUnit4-style Test suite that collects all JUnit tests registered via the extension point.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
@RunWith(AllTests.class)
public class AllJUnitTests {
    private static final String EXT_POINT_ID = "org.knime.testing.TestcaseCollector";

    private static final String EXT_POINT_ATTR_DF = "TestcaseCollector";

    /**
     * This is called via the JUnit framework in order to collect all testcases.
     *
     * @return a test suite with all testcases
     *
     * @throws Exception if something goes wrong
     */
    public static TestSuite suite() throws Exception {
        TestSuite suite = new TestSuite();

        for (Class<?> testClass : getAllJunitTests()) {
            suite.addTest(new JUnit4TestAdapter(testClass));
        }

        return suite;
    }

    /**
     * Returns a collection with class names of all JUnit tests.
     *
     * @return a collection with class names
     * @throws CoreException if a testcase collector cannot be created via its plug-in
     * @throws IOException if an I/O error occurs while searching for testcase collectors
     * @throws ClassNotFoundException if a testcase collector class cannot be found
     */
    public static Collection<Class<?>> getAllJunitTests() throws CoreException, IOException, ClassNotFoundException {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
        if (point == null) {
            throw new IllegalStateException("ACTIVATION ERROR: " + " --> Invalid extension point: " + EXT_POINT_ID);
        }

        List<Class<?>> allTests = new ArrayList<Class<?>>(512);
        for (IConfigurationElement elem : point.getConfigurationElements()) {
            String collectorName = elem.getAttribute(EXT_POINT_ATTR_DF);
            String decl = elem.getDeclaringExtension().getUniqueIdentifier();

            if (collectorName == null || collectorName.isEmpty()) {
                throw new IllegalStateException("The extension '" + decl + "' doesn't provide the required attribute '"
                    + EXT_POINT_ATTR_DF + "', ignoring it");
            }

            AbstractTestcaseCollector collector =
                (AbstractTestcaseCollector)elem.createExecutableExtension(EXT_POINT_ATTR_DF);

            allTests.addAll(collector.getUnittestsClasses());
        }
        return allTests;
    }
}
