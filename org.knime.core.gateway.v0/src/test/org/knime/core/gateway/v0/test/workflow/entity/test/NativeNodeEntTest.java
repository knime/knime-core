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
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.EntityID;
import org.knime.core.gateway.v0.workflow.entity.JobManagerEnt;
import org.knime.core.gateway.v0.workflow.entity.NativeNodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeFactoryIDEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeInPortEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeMessageEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeOutPortEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.EntityIDBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.JobManagerEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NativeNodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeFactoryIDEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeInPortEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeMessageEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeOutPortEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public class NativeNodeEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        NativeNodeEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static NativeNodeEnt createEnt(final List<Object> valueList) {
        NativeNodeEntBuilder builder = EntityBuilderManager.builder(NativeNodeEntBuilder.class);
		builder.setNodeFactoryID(NodeFactoryIDEntTest.createEnt((List<Object>) valueList.get(0)));
		builder.setParent(EntityIDTest.createEnt((List<Object>) valueList.get(1)));
		builder.setJobManager(JobManagerEntTest.createEnt((List<Object>) valueList.get(2)));
		builder.setNodeMessage(NodeMessageEntTest.createEnt((List<Object>) valueList.get(3)));
		List<NodeInPortEnt> list4 = new ArrayList<>();
		List<Object> subList4 = (List<Object>) valueList.get(4);
		for(int i = 0; i < subList4.size(); i++) {
			list4.add(NodeInPortEntTest.createEnt((List<Object>) subList4.get(i)));
		}
		builder.setInPorts(list4);
		List<NodeOutPortEnt> list5 = new ArrayList<>();
		List<Object> subList5 = (List<Object>) valueList.get(5);
		for(int i = 0; i < subList5.size(); i++) {
			list5.add(NodeOutPortEntTest.createEnt((List<Object>) subList5.get(i)));
		}
		builder.setOutPorts(list5);
		builder.setName((String) valueList.get(6));
		builder.setNodeID((String) valueList.get(7));
		builder.setNodeTypeID((String) valueList.get(8));
		builder.setNodeType((String) valueList.get(9));
		builder.setBounds(BoundsEntTest.createEnt((List<Object>) valueList.get(10)));
		builder.setIsDeletable((boolean) valueList.get(11));
		builder.setNodeState((String) valueList.get(12));
		builder.setHasDialog((boolean) valueList.get(13));
		builder.setNodeAnnotation(NodeAnnotationEntTest.createEnt((List<Object>) valueList.get(14)));
        return builder.build();
    }

    public static void testEnt(final NativeNodeEnt ent, final List<Object> valueList) {
		NodeFactoryIDEntTest.testEnt(ent.getNodeFactoryID(), (List<Object>) valueList.get(0));
		EntityIDTest.testEnt(ent.getParent(), (List<Object>) valueList.get(1));
		JobManagerEntTest.testEnt(ent.getJobManager(), (List<Object>) valueList.get(2));
		NodeMessageEntTest.testEnt(ent.getNodeMessage(), (List<Object>) valueList.get(3));
		List<Object> subValueList4 = (List<Object>) valueList.get(4);
		List<NodeInPortEnt> subList4 =  ent.getInPorts();
		for(int i = 0; i < subList4.size(); i++) {
			NodeInPortEntTest.testEnt(subList4.get(i), (List<Object>) subValueList4.get(i));
		}
		List<Object> subValueList5 = (List<Object>) valueList.get(5);
		List<NodeOutPortEnt> subList5 =  ent.getOutPorts();
		for(int i = 0; i < subList5.size(); i++) {
			NodeOutPortEntTest.testEnt(subList5.get(i), (List<Object>) subValueList5.get(i));
		}
		assertEquals(ent.getName(), (String) valueList.get(6));
		assertEquals(ent.getNodeID(), (String) valueList.get(7));
		assertEquals(ent.getNodeTypeID(), (String) valueList.get(8));
		assertEquals(ent.getNodeType(), (String) valueList.get(9));
		BoundsEntTest.testEnt(ent.getBounds(), (List<Object>) valueList.get(10));
		assertEquals(ent.getIsDeletable(), (boolean) valueList.get(11));
		assertEquals(ent.getNodeState(), (String) valueList.get(12));
		assertEquals(ent.getHasDialog(), (boolean) valueList.get(13));
		NodeAnnotationEntTest.testEnt(ent.getNodeAnnotation(), (List<Object>) valueList.get(14));
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		valueList.add(NodeFactoryIDEntTest.createValueList());

 		valueList.add(EntityIDTest.createValueList());

 		valueList.add(JobManagerEntTest.createValueList());

 		valueList.add(NodeMessageEntTest.createValueList());

 		List<List<Object>> subList5 = new ArrayList<>();
		subList5.add(NodeInPortEntTest.createValueList());
		subList5.add(NodeInPortEntTest.createValueList());
		subList5.add(NodeInPortEntTest.createValueList());
		subList5.add(NodeInPortEntTest.createValueList());
		subList5.add(NodeInPortEntTest.createValueList());
 		valueList.add(subList5);

 		List<List<Object>> subList6 = new ArrayList<>();
		subList6.add(NodeOutPortEntTest.createValueList());
		subList6.add(NodeOutPortEntTest.createValueList());
		subList6.add(NodeOutPortEntTest.createValueList());
		subList6.add(NodeOutPortEntTest.createValueList());
		subList6.add(NodeOutPortEntTest.createValueList());
 		valueList.add(subList6);

 		valueList.add("8TgLk");	

 		valueList.add("2qvUh");	

 		valueList.add("aDuFt");	

 		valueList.add("51Z8E");	

 		valueList.add(BoundsEntTest.createValueList());

 		valueList.add(false);	

 		valueList.add("lRcIV");	

 		valueList.add(true);	

 		valueList.add(NodeAnnotationEntTest.createValueList());

        return valueList;
    }

}
