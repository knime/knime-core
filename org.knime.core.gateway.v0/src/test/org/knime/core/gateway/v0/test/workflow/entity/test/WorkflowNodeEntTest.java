/*
 * ------------------------------------------------------------------------
 *
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
 */
package org.knime.core.gateway.v0.test.workflow.entity.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.knime.core.gateway.entities.EntityBuilderManager;
import java.util.List;
import java.util.Optional;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowNodeEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public class WorkflowNodeEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        WorkflowNodeEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static WorkflowNodeEnt createEnt(final List<Object> valueList) {
        WorkflowNodeEntBuilder builder = EntityBuilderManager.builder(WorkflowNodeEntBuilder.class);
		List<NodeOutPortEnt> list0 = new ArrayList<>();
		List<Object> subList0 = (List<Object>) valueList.get(0);
		for(int i = 0; i < subList0.size(); i++) {
			list0.add(NodeOutPortEntTest.createEnt((List<Object>) subList0.get(i)));
		}
		builder.setWorkflowIncomingPorts(list0);
		List<NodeInPortEnt> list1 = new ArrayList<>();
		List<Object> subList1 = (List<Object>) valueList.get(1);
		for(int i = 0; i < subList1.size(); i++) {
			list1.add(NodeInPortEntTest.createEnt((List<Object>) subList1.get(i)));
		}
		builder.setWorkflowOutgoingPorts(list1);
		builder.setParentNodeID((Optional<String>) valueList.get(2));
		builder.setRootWorkflowID((String) valueList.get(3));
		builder.setJobManager(Optional.of(JobManagerEntTest.createEnt((List<Object>) valueList.get(4))));
		builder.setNodeMessage(NodeMessageEntTest.createEnt((List<Object>) valueList.get(5)));
		List<NodeInPortEnt> list6 = new ArrayList<>();
		List<Object> subList6 = (List<Object>) valueList.get(6);
		for(int i = 0; i < subList6.size(); i++) {
			list6.add(NodeInPortEntTest.createEnt((List<Object>) subList6.get(i)));
		}
		builder.setInPorts(list6);
		List<NodeOutPortEnt> list7 = new ArrayList<>();
		List<Object> subList7 = (List<Object>) valueList.get(7);
		for(int i = 0; i < subList7.size(); i++) {
			list7.add(NodeOutPortEntTest.createEnt((List<Object>) subList7.get(i)));
		}
		builder.setOutPorts(list7);
		builder.setName((String) valueList.get(8));
		builder.setNodeID((String) valueList.get(9));
		builder.setNodeType((String) valueList.get(10));
		builder.setBounds(BoundsEntTest.createEnt((List<Object>) valueList.get(11)));
		builder.setIsDeletable((boolean) valueList.get(12));
		builder.setNodeState((String) valueList.get(13));
		builder.setHasDialog((boolean) valueList.get(14));
		builder.setNodeAnnotation(NodeAnnotationEntTest.createEnt((List<Object>) valueList.get(15)));
        return builder.build();
    }

    public static void testEnt(final WorkflowNodeEnt ent, final List<Object> valueList) {
		List<Object> subValueList0 = (List<Object>) valueList.get(0);
		List<NodeOutPortEnt> subList0 =  ent.getWorkflowIncomingPorts();
		for(int i = 0; i < subList0.size(); i++) {
			NodeOutPortEntTest.testEnt(subList0.get(i), (List<Object>) subValueList0.get(i));
		}
		List<Object> subValueList1 = (List<Object>) valueList.get(1);
		List<NodeInPortEnt> subList1 =  ent.getWorkflowOutgoingPorts();
		for(int i = 0; i < subList1.size(); i++) {
			NodeInPortEntTest.testEnt(subList1.get(i), (List<Object>) subValueList1.get(i));
		}
		assertEquals(ent.getParentNodeID().get(),((Optional<String>) valueList.get(2)).get());
		assertEquals(ent.getRootWorkflowID(), (String) valueList.get(3));
		JobManagerEntTest.testEnt(ent.getJobManager().get(), (List<Object>) valueList.get(4));
		NodeMessageEntTest.testEnt(ent.getNodeMessage(), (List<Object>) valueList.get(5));
		List<Object> subValueList6 = (List<Object>) valueList.get(6);
		List<NodeInPortEnt> subList6 =  ent.getInPorts();
		for(int i = 0; i < subList6.size(); i++) {
			NodeInPortEntTest.testEnt(subList6.get(i), (List<Object>) subValueList6.get(i));
		}
		List<Object> subValueList7 = (List<Object>) valueList.get(7);
		List<NodeOutPortEnt> subList7 =  ent.getOutPorts();
		for(int i = 0; i < subList7.size(); i++) {
			NodeOutPortEntTest.testEnt(subList7.get(i), (List<Object>) subValueList7.get(i));
		}
		assertEquals(ent.getName(), (String) valueList.get(8));
		assertEquals(ent.getNodeID(), (String) valueList.get(9));
		assertEquals(ent.getNodeType(), (String) valueList.get(10));
		BoundsEntTest.testEnt(ent.getBounds(), (List<Object>) valueList.get(11));
		assertEquals(ent.getIsDeletable(), (boolean) valueList.get(12));
		assertEquals(ent.getNodeState(), (String) valueList.get(13));
		assertEquals(ent.getHasDialog(), (boolean) valueList.get(14));
		NodeAnnotationEntTest.testEnt(ent.getNodeAnnotation(), (List<Object>) valueList.get(15));
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		List<List<Object>> subList1 = new ArrayList<>();
		subList1.add(NodeOutPortEntTest.createValueList());
		subList1.add(NodeOutPortEntTest.createValueList());
		subList1.add(NodeOutPortEntTest.createValueList());
		subList1.add(NodeOutPortEntTest.createValueList());
		subList1.add(NodeOutPortEntTest.createValueList());
 		valueList.add(subList1);

 		List<List<Object>> subList2 = new ArrayList<>();
		subList2.add(NodeInPortEntTest.createValueList());
		subList2.add(NodeInPortEntTest.createValueList());
		subList2.add(NodeInPortEntTest.createValueList());
		subList2.add(NodeInPortEntTest.createValueList());
		subList2.add(NodeInPortEntTest.createValueList());
 		valueList.add(subList2);

		valueList.add(Optional.of("OuJNz"));

 		valueList.add("5VLnL");

		valueList.add(JobManagerEntTest.createValueList());

 		valueList.add(NodeMessageEntTest.createValueList());

 		List<List<Object>> subList7 = new ArrayList<>();
		subList7.add(NodeInPortEntTest.createValueList());
		subList7.add(NodeInPortEntTest.createValueList());
		subList7.add(NodeInPortEntTest.createValueList());
		subList7.add(NodeInPortEntTest.createValueList());
		subList7.add(NodeInPortEntTest.createValueList());
 		valueList.add(subList7);

 		List<List<Object>> subList8 = new ArrayList<>();
		subList8.add(NodeOutPortEntTest.createValueList());
		subList8.add(NodeOutPortEntTest.createValueList());
		subList8.add(NodeOutPortEntTest.createValueList());
		subList8.add(NodeOutPortEntTest.createValueList());
		subList8.add(NodeOutPortEntTest.createValueList());
 		valueList.add(subList8);

 		valueList.add("fdY7G");

 		valueList.add("sXyEg");

 		valueList.add("2AUEB");

 		valueList.add(BoundsEntTest.createValueList());

 		valueList.add(true);

 		valueList.add("sUtIg");

 		valueList.add(true);

 		valueList.add(NodeAnnotationEntTest.createValueList());

        return valueList;
    }

}
