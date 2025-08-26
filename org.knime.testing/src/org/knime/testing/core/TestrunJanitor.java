/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   30.07.2014 (thor): created
 */
package org.knime.testing.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.FlowVariable;


/**
 * Interface for a class that executes certain operation before and after all testflows are executed.
 *
 * <b>This class is not part of the API and my change without notice!</b>
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 2.11
 */
public abstract class TestrunJanitor {

    /**
     * Returns a list of workflow variables that should be injected into the workflow. The number and values may be
     * changed by {@link #before()} and {@link #after()}.
     *
     * @return a (possibly empty) list with flow variables
     */
    public abstract List<FlowVariable> getFlowVariables();

    /**
     * This method is executed by the Testflow configuration node when it is executed. Implementations can e.g.
     * set up databases.
     *
     * @throws Exception in an exception occurs
     */
    public abstract void before() throws Exception;

    /**
     * This method is executed when the Testflow configuration node is disposed, i.e. either deleted or when the
     * workflow is closed. Implementations can e.g. delete databases.
     *
     * @throws Exception in an exception occurs
     */
    public abstract void after() throws Exception;

    /**
     * Returns a descriptive name for this janitor.
     *
     * @return a name, never <code>null</code>
     */
    public abstract String getName();

    /**
     * Returns a unique ID for this janitor. The ID should never change after a janitor has been released. Users of
     * janitors should use this ID to reference them.
     *
     * @return a unique ID, never <code>null</code>
     */
    public abstract String getID();

    /**
     * Returns a short description about what this janitor does.
     *
     * @return a description, never null
     */
    public abstract String getDescription();

    /**
     * Called by the framework before {@link #before()} is invoked and allows the janitor to extract credentials.
     *
     * @param provider credentials provider
     */
    public void injectCredentials(final CredentialsProvider provider) {
        // may be overwritten if credentials are needed
    }

    /**
     * Called by the framework before {@link #before()} is invoked and allows the janitor to extract flow variables.
     *
     * @param flowVariables the map of flow variables
     */
    public void injectFlowVariables(final Map<String, FlowVariable> flowVariables) {
        // may be overwritten if flow variables are needed
    }

    /**
     * Returns a list with all registered testrun janitors. Each call will create new instances.
     *
     * @return a (possibly empty) collection with testrun janitors
     */
    public static Collection<TestrunJanitor> getJanitors() {
        List<TestrunJanitor> janitors = new ArrayList<>();
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint("org.knime.testing.TestrunJanitor");

        for (IExtension ext : point.getExtensions()) {
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (IConfigurationElement janitorElement : elements) {
                try {
                    janitors.add((TestrunJanitor)janitorElement.createExecutableExtension("class"));
                } catch (CoreException ex) {
                    NodeLogger.getLogger(TestrunJanitor.class).error(
                        "Could not create testrun janitor " + janitorElement.getAttribute("class")
                            + " from plug-in " + janitorElement.getNamespaceIdentifier() + ": " + ex.getMessage(),
                        ex);
                }
            }
        }

        return janitors;
    }
}
