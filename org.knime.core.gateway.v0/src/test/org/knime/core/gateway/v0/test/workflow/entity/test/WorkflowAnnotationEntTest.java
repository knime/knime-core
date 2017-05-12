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
import org.knime.core.gateway.v0.workflow.entity.AnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.BoundsEnt;
import org.knime.core.gateway.v0.workflow.entity.StyleRangeEnt;
import org.knime.core.gateway.v0.workflow.entity.WorkflowAnnotationEnt;
import org.knime.core.gateway.v0.workflow.entity.builder.AnnotationEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.BoundsEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.StyleRangeEntBuilder;
import org.knime.core.gateway.v0.workflow.entity.builder.WorkflowAnnotationEntBuilder;

/**
 *
 * @author Martin Horn, University of Konstanz
 */
// AUTO-GENERATED CODE; DO NOT MODIFY
public class WorkflowAnnotationEntTest {

    private static Random RAND = new Random();

    @Test
    public void test() {
        List<Object> valueList = createValueList();
        WorkflowAnnotationEnt ent = createEnt(valueList);
        testEnt(ent, valueList);
    }

    public static WorkflowAnnotationEnt createEnt(final List<Object> valueList) {
        WorkflowAnnotationEntBuilder builder = EntityBuilderManager.builder(WorkflowAnnotationEntBuilder.class);
		builder.setText((String) valueList.get(0));
		builder.setBackgroundColor((int) valueList.get(1));
		builder.setBounds(BoundsEntTest.createEnt((List<Object>) valueList.get(2)));
		builder.setTextAlignment((String) valueList.get(3));
		builder.setBorderSize((int) valueList.get(4));
		builder.setBorderColor((int) valueList.get(5));
		builder.setDefaultFontSize((int) valueList.get(6));
		builder.setVersion((int) valueList.get(7));
		List<StyleRangeEnt> list8 = new ArrayList<>();
		List<Object> subList8 = (List<Object>) valueList.get(8);
		for(int i = 0; i < subList8.size(); i++) {
			list8.add(StyleRangeEntTest.createEnt((List<Object>) subList8.get(i)));
		}
		builder.setStyleRanges(list8);
        return builder.build();
    }

    public static void testEnt(final WorkflowAnnotationEnt ent, final List<Object> valueList) {
		assertEquals(ent.getText(), (String) valueList.get(0));
		assertEquals(ent.getBackgroundColor(), (int) valueList.get(1));
		BoundsEntTest.testEnt(ent.getBounds(), (List<Object>) valueList.get(2));
		assertEquals(ent.getTextAlignment(), (String) valueList.get(3));
		assertEquals(ent.getBorderSize(), (int) valueList.get(4));
		assertEquals(ent.getBorderColor(), (int) valueList.get(5));
		assertEquals(ent.getDefaultFontSize(), (int) valueList.get(6));
		assertEquals(ent.getVersion(), (int) valueList.get(7));
		List<Object> subValueList8 = (List<Object>) valueList.get(8);
		List<StyleRangeEnt> subList8 =  ent.getStyleRanges();
		for(int i = 0; i < subList8.size(); i++) {
			StyleRangeEntTest.testEnt(subList8.get(i), (List<Object>) subValueList8.get(i));
		}
    }

    public static List<Object> createValueList() {
        List<Object> valueList = new ArrayList<Object>();
 		valueList.add("lzCuG");

 		valueList.add(-1154715079);

 		valueList.add(BoundsEntTest.createValueList());

 		valueList.add("5VLnL");

 		valueList.add(-1157408321);

 		valueList.add(-1156254074);

 		valueList.add(-1156638823);

 		valueList.add(-1158562568);

 		List<List<Object>> subList9 = new ArrayList<>();
		subList9.add(StyleRangeEntTest.createValueList());
		subList9.add(StyleRangeEntTest.createValueList());
		subList9.add(StyleRangeEntTest.createValueList());
		subList9.add(StyleRangeEntTest.createValueList());
		subList9.add(StyleRangeEntTest.createValueList());
 		valueList.add(subList9);

        return valueList;
    }

}
