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
import org.knime.core.gateway.v0.workflow.entity.AnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.NodeAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.AnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.NodeAnnotationEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
public class NodeAnnotationEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        NodeAnnotationEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static NodeAnnotationEnt createEnt(final List<Object> valueList) {
        NodeAnnotationEntBuilder builder = EntityBuilderManager.builder(NodeAnnotationEntBuilder.class);
		builder.setNode((String) valueList.get(0));
		builder.setText((String) valueList.get(1));
		builder.setBackgroundColor((int) valueList.get(2));
		builder.setX((int) valueList.get(3));
		builder.setY((int) valueList.get(4));
		builder.setWidth((int) valueList.get(5));
		builder.setHeight((int) valueList.get(6));
		builder.setTextAlignment((String) valueList.get(7));
		builder.setBorderSize((int) valueList.get(8));
		builder.setBorderColor((int) valueList.get(9));
		builder.setDefaultFontSize((int) valueList.get(10));
		builder.setVersion((int) valueList.get(11));
        return builder.build();
    }

    public static void testEnt(final NodeAnnotationEnt ent, final List<Object> valueList) {
		assertEquals(ent.getNode(), (String) valueList.get(0));
		assertEquals(ent.getText(), (String) valueList.get(1));
		assertEquals(ent.getBackgroundColor(), (int) valueList.get(2));
		assertEquals(ent.getX(), (int) valueList.get(3));
		assertEquals(ent.getY(), (int) valueList.get(4));
		assertEquals(ent.getWidth(), (int) valueList.get(5));
		assertEquals(ent.getHeight(), (int) valueList.get(6));
		assertEquals(ent.getTextAlignment(), (String) valueList.get(7));
		assertEquals(ent.getBorderSize(), (int) valueList.get(8));
		assertEquals(ent.getBorderColor(), (int) valueList.get(9));
		assertEquals(ent.getDefaultFontSize(), (int) valueList.get(10));
		assertEquals(ent.getVersion(), (int) valueList.get(11));
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		valueList.add("69mp5");	
 		valueList.add("ie8Dn");	
 		valueList.add(1828398059);	
 		valueList.add(-1598296581);	
 		valueList.add(1056710543);	
 		valueList.add(-217889110);	
 		valueList.add(1124434551);	
 		valueList.add("KUKm2");	
 		valueList.add(-58915974);	
 		valueList.add(447067024);	
 		valueList.add(-112132399);	
 		valueList.add(-1440650204);	
        return valueList;
    }

}
