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
 *   Sep 12, 2018 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.api.impl;

import org.knime.core.api.impl.exec.DefaultExecutor;
import org.knime.core.api.impl.workflow.DefaultNodeFactory;
import org.knime.core.api.impl.workflow.DefaultParametersValues;
import org.knime.core.api.impl.workflow.DefaultSingleNode;
import org.knime.core.api.impl.workflow.DefaultWorkflow;
import org.knime.core.api.impl.workflow.function.DefaultParameters;
import org.knime.core.api.impl.workflow.function.DefaultParametrizedFunction;
import org.knime.core.api.workflow.SingleNode;
import org.knime.core.api.workflow.Workflow;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowManager;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class Test {

    public void test() {

//        final StatefulWorkflow sW = null;
//
//        final WorkflowEditor wE = new WorkflowEditor(sW);
//
//        wE.addConnection(null);
//
//        wE.addNode(NodeRegistry.getInstance().getNodeFactory(NodeFactory.class).createNode());
//
//        final ExecEnv eE = ExecEnvRegistry.getInstance().createExecEnv(ExecEnvFactory.class);
//
//        StateManipulator<WorkflowFunction> m = null;
//
//        WorkflowExecutor
//
//
//        Executor<StatefulWorkflow> e = eE.apply(sW);
//
//        e.execute();
//

        //create workflow and add node
        NodeFactory<NodeModel> nf = null;
        DefaultNodeFactory fac = new DefaultNodeFactory(nf);
        SingleNode<DefaultNodeFactory> n = new DefaultSingleNode(fac);
        WorkflowManager wfm = null;
        Workflow w = new DefaultWorkflow(null);
        w.addNode(n);

        //try to configure a node - if individual node configuration is allowed
        DefaultParameters params = n.getFactory().createParameters();
        //create a dialog for the given parameters and return the actual values
        //possibly load the values from somewhere first
        DefaultParametersValues values = null;
        //configure the node with these values by somehow attaching a new state to the node, i.e. a configured state


        //try to execute a node
        // get execution environment
        // get an executor - i.e. one that allows individual node execution
        DefaultParametrizedFunction fct = n.getFactory().createFunction();
        fct.setValues(values);
        //let some configurator call specify on the function with the inspecs -> needs to be done for multiple nodes
        fct.specify(null);
        DefaultExecutor exec = new DefaultExecutor(fct);
        // execute the provided function by the factory -> will add states to the workflow
        exec.execute();




    }

}
