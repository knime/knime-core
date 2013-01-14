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
 * Created on Nov 27, 2012 by wiswedel
 */
package org.knime.workbench.editor2.pervasive;

import java.net.URL;

import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.SingleNodeContainer;

/** A temporary class that returns some job manager icons until Pervasive changes their node execution job manager.
 *
 * @author wiswedel
 * @deprecated To be removed
 */
@Deprecated
public final class PervasiveJobExecutorHelper {

    private static final URL META_NODE_EDITOR_DR_ENABLED =
            PervasiveJobExecutorHelper.class.getResource("pdr-splash.png");

    private static final URL KRUNNER_NATIVE =
            PervasiveJobExecutorHelper.class.getResource("krunner_native.png");

    private static final URL KRUNNER_FORCED =
            PervasiveJobExecutorHelper.class.getResource("krunner_forced.png");

    private static final URL KRUNNER_DISABLED =
            PervasiveJobExecutorHelper.class.getResource("krunner_disabled.png");

    private PervasiveJobExecutorHelper() {
    }

    public static boolean isPervasiveJobManager(final NodeExecutionJobManager jobManager) {
        return jobManager != null && jobManager.getClass().getName().contains("pervasive");
    }

    public static URL getIconForWorkflow() {
        return META_NODE_EDITOR_DR_ENABLED;
    }

    public static URL getIconForChild(final NodeContainer child) {
        if ( child instanceof SingleNodeContainer ) {
            //first check to see if we are native streaming (if no we will never be forced)
            SingleNodeContainer snc = (SingleNodeContainer)child;
            NodeModel model = snc.getNodeModel();
            Class cl = model.getClass();
            if (cl.getName().contains("pervasive")) {
                return KRUNNER_NATIVE;
            }
            do {
                try {
                    cl.getDeclaredMethod("getInputPortRoles");
                    return KRUNNER_NATIVE;
                } catch (Exception e) {
                    // ignore, check superclass
                }
                cl = cl.getSuperclass();
            } while (!NodeModel.class.equals(cl));

            //force streaming is indicated by setting the same job manager again.
            //this will change (we will have a separate job manager), but not
            //until after KNIME 2.7 is out, at which point we will have our own implementation
            //of AbstractNodeExecutionJobManager
            if ( child.getJobManager() != null && child.getJobManager().getClass().getSimpleName().equals("KRunnerJobManager") ) {
                return KRUNNER_FORCED;
            }

            return KRUNNER_DISABLED;
        }
        else {
            //hmm, what to return for metanodes? they could be a mix of forced and non-forced, native and non-native..
            //ok, let's assume native for now...
            return KRUNNER_NATIVE;
        }
    }
}
