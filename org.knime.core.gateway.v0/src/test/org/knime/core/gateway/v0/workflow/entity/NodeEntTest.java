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
package org.knime.core.gateway.v0.workflow.entity;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.knime.core.gateway.entities.EntityBuilderManager;
import org.knime.core.gateway.v0.workflow.entity.*;
import org.knime.core.gateway.v0.workflow.entity.builder.*;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class NodeEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        NodeEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    static NodeEnt createEnt(final List<Object> valueList) {
        NodeEntBuilder builder = EntityBuilderManager.builder(NodeEntBuilder.class);
			builder.setParent(EntityIDTest.createEnt((List<Object>) valueList.get(0)));
				builder.setJobManager(JobManagerEntTest.createEnt((List<Object>) valueList.get(1)));
				builder.setNodeMessage(NodeMessageEntTest.createEnt((List<Object>) valueList.get(2)));
				List<NodeInPortEnt> list3 = new ArrayList<>();
		List<Object> subList3 = (List<Object>) valueList.get(3);
		for(int i = 0; i < subList3.size(); i++) {
			list3.add(NodeInPortEntTest.createEnt((List<Object>) subList3.get(i)));
		}
		builder.setInPorts(list3);
				List<NodeOutPortEnt> list4 = new ArrayList<>();
		List<Object> subList4 = (List<Object>) valueList.get(4);
		for(int i = 0; i < subList4.size(); i++) {
			list4.add(NodeOutPortEntTest.createEnt((List<Object>) subList4.get(i)));
		}
		builder.setOutPorts(list4);
				builder.setName((String) valueList.get(5));
				builder.setNodeID((String) valueList.get(6));
				builder.setNodeType((String) valueList.get(7));
				builder.setBounds(BoundsEntTest.createEnt((List<Object>) valueList.get(8)));
				builder.setIsDeletable((boolean) valueList.get(9));
				builder.setNodeState((String) valueList.get(10));
				builder.setHasDialog((boolean) valueList.get(11));
				builder.setNodeAnnotation(NodeAnnotationEntTest.createEnt((List<Object>) valueList.get(12)));
	        return builder.build();
    }

    static void testEnt(final NodeEnt ent, final List<Object> valueList) {
			EntityIDTest.testEnt(ent.getParent(), (List<Object>) valueList.get(0));
				JobManagerEntTest.testEnt(ent.getJobManager(), (List<Object>) valueList.get(1));
				NodeMessageEntTest.testEnt(ent.getNodeMessage(), (List<Object>) valueList.get(2));
				List<Object> subValueList3 = (List<Object>) valueList.get(3);
		List<NodeInPortEnt> subList3 =  ent.getInPorts();
		for(int i = 0; i < subList3.size(); i++) {
			NodeInPortEntTest.testEnt(subList3.get(i), (List<Object>) subValueList3.get(i));
		}
				List<Object> subValueList4 = (List<Object>) valueList.get(4);
		List<NodeOutPortEnt> subList4 =  ent.getOutPorts();
		for(int i = 0; i < subList4.size(); i++) {
			NodeOutPortEntTest.testEnt(subList4.get(i), (List<Object>) subValueList4.get(i));
		}
				assertEquals(ent.getName(), (String) valueList.get(5));
				assertEquals(ent.getNodeID(), (String) valueList.get(6));
				assertEquals(ent.getNodeType(), (String) valueList.get(7));
				BoundsEntTest.testEnt(ent.getBounds(), (List<Object>) valueList.get(8));
				assertEquals(ent.getIsDeletable(), (boolean) valueList.get(9));
				assertEquals(ent.getNodeState(), (String) valueList.get(10));
				assertEquals(ent.getHasDialog(), (boolean) valueList.get(11));
				NodeAnnotationEntTest.testEnt(ent.getNodeAnnotation(), (List<Object>) valueList.get(12));
	    }

    static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
  	 		valueList.add(EntityIDTest.createValueList());
 	  	 		valueList.add(JobManagerEntTest.createValueList());
 	  	 		valueList.add(NodeMessageEntTest.createValueList());
 	  	 		//TODO list of primitives?
 		List<List<Object>> subList4 = new ArrayList<>();
 		for(int i = 0; i < 5; i++) {
 			subList4.add(NodeInPortEntTest.createValueList());	
 		}
 		valueList.add(subList4);
 	  	 		//TODO list of primitives?
 		List<List<Object>> subList5 = new ArrayList<>();
 		for(int i = 0; i < 5; i++) {
 			subList5.add(NodeOutPortEntTest.createValueList());	
 		}
 		valueList.add(subList5);
 	  	 		valueList.add("ogLyl");	
 	  	 		valueList.add("OtcXJ");	
 	  	 		valueList.add("TD1GB");	
 	  	 		valueList.add(BoundsEntTest.createValueList());
 	  	 		valueList.add(false);	
 	  	 		valueList.add("gZB6r");	
 	  	 		valueList.add(true);	
 	  	 		valueList.add(NodeAnnotationEntTest.createValueList());
 	         return valueList;
    }

}
