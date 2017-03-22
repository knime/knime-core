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
import java.util.Map;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.MetaPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.EntityIDBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.MetaPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public class WorkflowEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        WorkflowEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static WorkflowEnt createEnt(final List<Object> valueList) {
        WorkflowEntBuilder builder = EntityBuilderManager.builder(WorkflowEntBuilder.class);
		Map<String, NodeEnt> map0 = new HashMap<>();
		Map<String, Object> subMap0 = (Map<String, Object>) valueList.get(0);
		for(String key : subMap0.keySet()) {
			map0.put(key, NodeEntTest.createEnt((List<Object>) subMap0.get(key)));
		}
		builder.setNodes(map0);
		List<ConnectionEnt> list1 = new ArrayList<>();
		List<Object> subList1 = (List<Object>) valueList.get(1);
		for(int i = 0; i < subList1.size(); i++) {
			list1.add(ConnectionEntTest.createEnt((List<Object>) subList1.get(i)));
		}
		builder.setConnections(list1);
		List<MetaPortEnt> list2 = new ArrayList<>();
		List<Object> subList2 = (List<Object>) valueList.get(2);
		for(int i = 0; i < subList2.size(); i++) {
			list2.add(MetaPortEntTest.createEnt((List<Object>) subList2.get(i)));
		}
		builder.setMetaInPorts(list2);
		List<MetaPortEnt> list3 = new ArrayList<>();
		List<Object> subList3 = (List<Object>) valueList.get(3);
		for(int i = 0; i < subList3.size(); i++) {
			list3.add(MetaPortEntTest.createEnt((List<Object>) subList3.get(i)));
		}
		builder.setMetaOutPorts(list3);
		builder.setParent(EntityIDTest.createEnt((List<Object>) valueList.get(4)));
		builder.setJobManager(JobManagerEntTest.createEnt((List<Object>) valueList.get(5)));
		builder.setNodeMessage(NodeMessageEntTest.createEnt((List<Object>) valueList.get(6)));
		List<NodeInPortEnt> list7 = new ArrayList<>();
		List<Object> subList7 = (List<Object>) valueList.get(7);
		for(int i = 0; i < subList7.size(); i++) {
			list7.add(NodeInPortEntTest.createEnt((List<Object>) subList7.get(i)));
		}
		builder.setInPorts(list7);
		List<NodeOutPortEnt> list8 = new ArrayList<>();
		List<Object> subList8 = (List<Object>) valueList.get(8);
		for(int i = 0; i < subList8.size(); i++) {
			list8.add(NodeOutPortEntTest.createEnt((List<Object>) subList8.get(i)));
		}
		builder.setOutPorts(list8);
		builder.setName((String) valueList.get(9));
		builder.setNodeID((String) valueList.get(10));
		builder.setNodeTypeID((String) valueList.get(11));
		builder.setNodeType((String) valueList.get(12));
		builder.setBounds(BoundsEntTest.createEnt((List<Object>) valueList.get(13)));
		builder.setIsDeletable((boolean) valueList.get(14));
		builder.setNodeState((String) valueList.get(15));
		builder.setHasDialog((boolean) valueList.get(16));
		builder.setNodeAnnotation(NodeAnnotationEntTest.createEnt((List<Object>) valueList.get(17)));
        return builder.build();
    }

    public static void testEnt(final WorkflowEnt ent, final List<Object> valueList) {
		Map<String, List<Object>> subValueMap0 = (Map<String, List<Object>>) valueList.get(0);
		Map<String, NodeEnt> subMap0 =  ent.getNodes();
		for(String key : subMap0.keySet()) {
			NodeEntTest.testEnt(subMap0.get(key), (List<Object>) subValueMap0.get(key));
		}
		List<Object> subValueList1 = (List<Object>) valueList.get(1);
		List<ConnectionEnt> subList1 =  ent.getConnections();
		for(int i = 0; i < subList1.size(); i++) {
			ConnectionEntTest.testEnt(subList1.get(i), (List<Object>) subValueList1.get(i));
		}
		List<Object> subValueList2 = (List<Object>) valueList.get(2);
		List<MetaPortEnt> subList2 =  ent.getMetaInPorts();
		for(int i = 0; i < subList2.size(); i++) {
			MetaPortEntTest.testEnt(subList2.get(i), (List<Object>) subValueList2.get(i));
		}
		List<Object> subValueList3 = (List<Object>) valueList.get(3);
		List<MetaPortEnt> subList3 =  ent.getMetaOutPorts();
		for(int i = 0; i < subList3.size(); i++) {
			MetaPortEntTest.testEnt(subList3.get(i), (List<Object>) subValueList3.get(i));
		}
		EntityIDTest.testEnt(ent.getParent(), (List<Object>) valueList.get(4));
		JobManagerEntTest.testEnt(ent.getJobManager(), (List<Object>) valueList.get(5));
		NodeMessageEntTest.testEnt(ent.getNodeMessage(), (List<Object>) valueList.get(6));
		List<Object> subValueList7 = (List<Object>) valueList.get(7);
		List<NodeInPortEnt> subList7 =  ent.getInPorts();
		for(int i = 0; i < subList7.size(); i++) {
			NodeInPortEntTest.testEnt(subList7.get(i), (List<Object>) subValueList7.get(i));
		}
		List<Object> subValueList8 = (List<Object>) valueList.get(8);
		List<NodeOutPortEnt> subList8 =  ent.getOutPorts();
		for(int i = 0; i < subList8.size(); i++) {
			NodeOutPortEntTest.testEnt(subList8.get(i), (List<Object>) subValueList8.get(i));
		}
		assertEquals(ent.getName(), (String) valueList.get(9));
		assertEquals(ent.getNodeID(), (String) valueList.get(10));
		assertEquals(ent.getNodeTypeID(), (String) valueList.get(11));
		assertEquals(ent.getNodeType(), (String) valueList.get(12));
		BoundsEntTest.testEnt(ent.getBounds(), (List<Object>) valueList.get(13));
		assertEquals(ent.getIsDeletable(), (boolean) valueList.get(14));
		assertEquals(ent.getNodeState(), (String) valueList.get(15));
		assertEquals(ent.getHasDialog(), (boolean) valueList.get(16));
		NodeAnnotationEntTest.testEnt(ent.getNodeAnnotation(), (List<Object>) valueList.get(17));
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		Map<String, List<Object>> subMap1 = new HashMap<>();
 		subMap1.put("fosqT", NodeEntTest.createValueList());
 		subMap1.put("EctMI", NodeEntTest.createValueList());
 		subMap1.put("hc9Ij", NodeEntTest.createValueList());
 		subMap1.put("lla3W", NodeEntTest.createValueList());
 		subMap1.put("nJ5zx", NodeEntTest.createValueList());
 		valueList.add(subMap1);

 		List<List<Object>> subList2 = new ArrayList<>();
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
 		valueList.add(subList2);

 		List<List<Object>> subList3 = new ArrayList<>();
		subList3.add(MetaPortEntTest.createValueList());
		subList3.add(MetaPortEntTest.createValueList());
		subList3.add(MetaPortEntTest.createValueList());
		subList3.add(MetaPortEntTest.createValueList());
		subList3.add(MetaPortEntTest.createValueList());
 		valueList.add(subList3);

 		List<List<Object>> subList4 = new ArrayList<>();
		subList4.add(MetaPortEntTest.createValueList());
		subList4.add(MetaPortEntTest.createValueList());
		subList4.add(MetaPortEntTest.createValueList());
		subList4.add(MetaPortEntTest.createValueList());
		subList4.add(MetaPortEntTest.createValueList());
 		valueList.add(subList4);

 		valueList.add(EntityIDTest.createValueList());

 		valueList.add(JobManagerEntTest.createValueList());

 		valueList.add(NodeMessageEntTest.createValueList());

 		List<List<Object>> subList8 = new ArrayList<>();
		subList8.add(NodeInPortEntTest.createValueList());
		subList8.add(NodeInPortEntTest.createValueList());
		subList8.add(NodeInPortEntTest.createValueList());
		subList8.add(NodeInPortEntTest.createValueList());
		subList8.add(NodeInPortEntTest.createValueList());
 		valueList.add(subList8);

 		List<List<Object>> subList9 = new ArrayList<>();
		subList9.add(NodeOutPortEntTest.createValueList());
		subList9.add(NodeOutPortEntTest.createValueList());
		subList9.add(NodeOutPortEntTest.createValueList());
		subList9.add(NodeOutPortEntTest.createValueList());
		subList9.add(NodeOutPortEntTest.createValueList());
 		valueList.add(subList9);

 		valueList.add("RKQe2");	

 		valueList.add("GjgY0");	

 		valueList.add("Wi6au");	

 		valueList.add("nV2t4");	

 		valueList.add(BoundsEntTest.createValueList());

 		valueList.add(true);	

 		valueList.add("VuCCg");	

 		valueList.add(true);	

 		valueList.add(NodeAnnotationEntTest.createValueList());

        return valueList;
    }

}
