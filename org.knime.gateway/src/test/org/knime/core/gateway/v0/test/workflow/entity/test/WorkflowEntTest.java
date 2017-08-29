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
import org.knime.gateway.entities.EntityBuilderManager;
import java.util.List;
import java.util.Map;
import org.knime.gateway.v0.workflow.entity.ConnectionEnt;
import org.knime.gateway.v0.workflow.entity.MetaPortInfoEnt;
import org.knime.gateway.v0.workflow.entity.NodeEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowEnt;
import org.knime.gateway.v0.workflow.entity.WorkflowUIInfoEnt;
import org.knime.gateway.v0.workflow.entity.builder.ConnectionEntBuilder;
import org.knime.gateway.v0.workflow.entity.builder.MetaPortInfoEntBuilder;
import org.knime.gateway.v0.workflow.entity.builder.NodeEntBuilder;
import org.knime.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;
import org.knime.gateway.v0.workflow.entity.builder.WorkflowEntBuilder;
import org.knime.gateway.v0.workflow.entity.builder.WorkflowUIInfoEntBuilder;

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
		List<MetaPortInfoEnt> list2 = new ArrayList<>();
		List<Object> subList2 = (List<Object>) valueList.get(2);
		for(int i = 0; i < subList2.size(); i++) {
			list2.add(MetaPortInfoEntTest.createEnt((List<Object>) subList2.get(i)));
		}
		builder.setMetaInPortInfos(list2);
		List<MetaPortInfoEnt> list3 = new ArrayList<>();
		List<Object> subList3 = (List<Object>) valueList.get(3);
		for(int i = 0; i < subList3.size(); i++) {
			list3.add(MetaPortInfoEntTest.createEnt((List<Object>) subList3.get(i)));
		}
		builder.setMetaOutPortInfos(list3);
		List<WorkflowAnnotationEnt> list4 = new ArrayList<>();
		List<Object> subList4 = (List<Object>) valueList.get(4);
		for(int i = 0; i < subList4.size(); i++) {
			list4.add(WorkflowAnnotationEntTest.createEnt((List<Object>) subList4.get(i)));
		}
		builder.setWorkflowAnnotations(list4);
		builder.setWorkflowUIInfo(WorkflowUIInfoEntTest.createEnt((List<Object>) valueList.get(5)));
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
		List<MetaPortInfoEnt> subList2 =  ent.getMetaInPortInfos();
		for(int i = 0; i < subList2.size(); i++) {
			MetaPortInfoEntTest.testEnt(subList2.get(i), (List<Object>) subValueList2.get(i));
		}
		List<Object> subValueList3 = (List<Object>) valueList.get(3);
		List<MetaPortInfoEnt> subList3 =  ent.getMetaOutPortInfos();
		for(int i = 0; i < subList3.size(); i++) {
			MetaPortInfoEntTest.testEnt(subList3.get(i), (List<Object>) subValueList3.get(i));
		}
		List<Object> subValueList4 = (List<Object>) valueList.get(4);
		List<WorkflowAnnotationEnt> subList4 =  ent.getWorkflowAnnotations();
		for(int i = 0; i < subList4.size(); i++) {
			WorkflowAnnotationEntTest.testEnt(subList4.get(i), (List<Object>) subValueList4.get(i));
		}
		WorkflowUIInfoEntTest.testEnt(ent.getWorkflowUIInfo(), (List<Object>) valueList.get(5));
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		Map<String, List<Object>> subMap1 = new HashMap<>();
 		subMap1.put("lzCuG", NodeEntTest.createValueList());
 		subMap1.put("5KrGg", NodeEntTest.createValueList());
 		subMap1.put("OuJNz", NodeEntTest.createValueList());
 		subMap1.put("5VLnL", NodeEntTest.createValueList());
 		subMap1.put("YJQGG", NodeEntTest.createValueList());
 		valueList.add(subMap1);

 		List<List<Object>> subList2 = new ArrayList<>();
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
		subList2.add(ConnectionEntTest.createValueList());
 		valueList.add(subList2);

 		List<List<Object>> subList3 = new ArrayList<>();
		subList3.add(MetaPortInfoEntTest.createValueList());
		subList3.add(MetaPortInfoEntTest.createValueList());
		subList3.add(MetaPortInfoEntTest.createValueList());
		subList3.add(MetaPortInfoEntTest.createValueList());
		subList3.add(MetaPortInfoEntTest.createValueList());
 		valueList.add(subList3);

 		List<List<Object>> subList4 = new ArrayList<>();
		subList4.add(MetaPortInfoEntTest.createValueList());
		subList4.add(MetaPortInfoEntTest.createValueList());
		subList4.add(MetaPortInfoEntTest.createValueList());
		subList4.add(MetaPortInfoEntTest.createValueList());
		subList4.add(MetaPortInfoEntTest.createValueList());
 		valueList.add(subList4);

 		List<List<Object>> subList5 = new ArrayList<>();
		subList5.add(WorkflowAnnotationEntTest.createValueList());
		subList5.add(WorkflowAnnotationEntTest.createValueList());
		subList5.add(WorkflowAnnotationEntTest.createValueList());
		subList5.add(WorkflowAnnotationEntTest.createValueList());
		subList5.add(WorkflowAnnotationEntTest.createValueList());
 		valueList.add(subList5);

 		valueList.add(WorkflowUIInfoEntTest.createValueList());

        return valueList;
    }

}
