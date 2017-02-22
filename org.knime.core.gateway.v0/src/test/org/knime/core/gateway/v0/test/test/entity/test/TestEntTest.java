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
package org.knime.core.gateway.v0.test.test.entity.test;

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
import org.knime.core.gateway.v0.test.entity.TestEnt;
import org.knime.core.gateway.v0.test.entity.builder.TestEntBuilder;
import org.knime.core.gateway.v0.test.workflow.entity.test.XYEntTest;
import org.knime.core.gateway.v0.workflow.entity.XYEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.XYEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class TestEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        TestEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static TestEnt createEnt(final List<Object> valueList) {
        TestEntBuilder builder = EntityBuilderManager.builder(TestEntBuilder.class);
		builder.setXY(XYEntTest.createEnt((List<Object>) valueList.get(0)));
		List<XYEnt> list1 = new ArrayList<>();
		List<Object> subList1 = (List<Object>) valueList.get(1);
		for(int i = 0; i < subList1.size(); i++) {
			list1.add(XYEntTest.createEnt((List<Object>) subList1.get(i)));
		}
		builder.setXYList(list1);
		builder.setOther((String) valueList.get(2));
		List<String> list3 = new ArrayList<>();
		List<Object> subList3 = (List<Object>) valueList.get(3);
		for(int i = 0; i < subList3.size(); i++) {
			list3.add((String) subList3.get(i));
		}
		builder.setPrimitiveList(list3);
		Map<String, XYEnt> map4 = new HashMap<>();
		Map<String, Object> subMap4 = (Map<String, Object>) valueList.get(4);
		for(String key : subMap4.keySet()) {
			map4.put(key, XYEntTest.createEnt((List<Object>) subMap4.get(key)));
		}
		builder.setXYMap(map4);
		Map<Integer, String> map5 = new HashMap<>();
		Map<Integer, Object> subMap5 = (Map<Integer, Object>) valueList.get(5);
		for(Integer key : subMap5.keySet()) {
			map5.put(key, (String) subMap5.get(key));
		}
		builder.setPrimitiveMap(map5);
        return builder.build();
    }

    public static void testEnt(final TestEnt ent, final List<Object> valueList) {
		XYEntTest.testEnt(ent.getXY(), (List<Object>) valueList.get(0));
		List<Object> subValueList1 = (List<Object>) valueList.get(1);
		List<XYEnt> subList1 =  ent.getXYList();
		for(int i = 0; i < subList1.size(); i++) {
			XYEntTest.testEnt(subList1.get(i), (List<Object>) subValueList1.get(i));
		}
		assertEquals(ent.getOther(), (String) valueList.get(2));
		List<Object> subValueList3 = (List<Object>) valueList.get(3);
		List<String> subList3 =  ent.getPrimitiveList();
		for(int i = 0; i < subList3.size(); i++) {
			assertEquals(subList3.get(i), subValueList3.get(i));
		}
		Map<String, List<Object>> subValueMap4 = (Map<String, List<Object>>) valueList.get(4);
		Map<String, XYEnt> subMap4 =  ent.getXYMap();
		for(String key : subMap4.keySet()) {
			XYEntTest.testEnt(subMap4.get(key), (List<Object>) subValueMap4.get(key));
		}
		Map<Integer, List<Object>> subValueMap5 = (Map<Integer, List<Object>>) valueList.get(5);
		Map<Integer, String> subMap5 =  ent.getPrimitiveMap();
		for(Integer key : subMap5.keySet()) {
			assertEquals(subMap5.get(key), subValueMap5.get(key));
		}
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		valueList.add(XYEntTest.createValueList());

 		List<List<Object>> subList2 = new ArrayList<>();
		subList2.add(XYEntTest.createValueList());
		subList2.add(XYEntTest.createValueList());
		subList2.add(XYEntTest.createValueList());
		subList2.add(XYEntTest.createValueList());
		subList2.add(XYEntTest.createValueList());
 		valueList.add(subList2);

 		valueList.add("gtJkR");	

 		List<Object> subList4 = new ArrayList<>();
		subList4.add("XJwSg");
		subList4.add("E9EpK");
		subList4.add("LomSd");
		subList4.add("FJUr2");
		subList4.add("hvMNZ");
 		valueList.add(subList4);

 		Map<String, List<Object>> subMap5 = new HashMap<>();
 		subMap5.put("679vu", XYEntTest.createValueList());
 		subMap5.put("ia7Je", XYEntTest.createValueList());
 		subMap5.put("C6zTP", XYEntTest.createValueList());
 		subMap5.put("Rk3ls", XYEntTest.createValueList());
 		subMap5.put("MZ4vH", XYEntTest.createValueList());
 		valueList.add(subMap5);

		Map<Integer, Object> subMap6 = new HashMap<>();
		subMap6.put(-2012428734, "BlgtE");
		subMap6.put(-2008954433, "SrUk1");
		subMap6.put(-190720476, "Url7Y");
		subMap6.put(-1119926908, "2CR5k");
		subMap6.put(-1078136943, "X0BZU");
 		valueList.add(subMap6);

        return valueList;
    }

}
