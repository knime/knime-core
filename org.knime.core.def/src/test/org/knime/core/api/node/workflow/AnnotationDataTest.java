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
 * History
 *   Sep 22, 2016 (hornm): created
 */
package org.knime.core.api.node.workflow;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.knime.core.def.node.workflow.AnnotationData;
import org.knime.core.def.node.workflow.AnnotationData.StyleRange;
import org.knime.core.def.node.workflow.AnnotationData.TextAlignment;

/**
 *
 * @author Martin Horn, KNIME.com
 */
public class AnnotationDataTest {

    @Test
    public void testBuilderAndGetter() {
        StyleRange styleRange = StyleRange.builder().build();
        AnnotationData annotationData = AnnotationData.builder()
                .setAlignment(TextAlignment.CENTER)
                .setBgColor(10)
                .setBorderColor(20)
                .setBorderSize(5)
                .setDefaultFontSize(3)
                .setDimension(50, 60, 100, 101)
                .setStyleRanges(new StyleRange[]{styleRange})
                .setText("text").build();

        assertEquals(annotationData.getAlignment(), TextAlignment.CENTER);
        assertEquals(annotationData.getBgColor(), 10);
        assertEquals(annotationData.getBorderColor(), 20);
        assertEquals(annotationData.getDefaultFontSize(), 3);
        assertEquals(annotationData.getWidth(), 100);
        assertEquals(annotationData.getHeight(), 101);
        assertEquals(annotationData.getX(), 50);
        assertEquals(annotationData.getY(), 60);
        assertArrayEquals(annotationData.getStyleRanges(), new StyleRange[]{styleRange});
        assertEquals(annotationData.getText(), "text");
    }

    @Test
    public void testCopyBuilder() {
        StyleRange styleRange = StyleRange.builder().build();
        AnnotationData annotationData = AnnotationData.builder()
                .setAlignment(TextAlignment.CENTER)
                .setBgColor(10)
                .setBorderColor(20)
                .setBorderSize(5)
                .setDefaultFontSize(3)
                .setDimension(50, 60, 100, 101)
                .setStyleRanges(new StyleRange[]{styleRange})
                .setText("text").build();

        //include bounds
        AnnotationData annotationData2 = AnnotationData.builder(annotationData, true).build();
        assertEquals(annotationData.getAlignment(), annotationData2.getAlignment());
        assertEquals(annotationData.getBgColor(), annotationData2.getBgColor());
        assertEquals(annotationData.getBorderColor(), annotationData2.getBorderColor());
        assertEquals(annotationData.getDefaultFontSize(), annotationData2.getDefaultFontSize());
        assertEquals(annotationData.getWidth(), annotationData2.getWidth());
        assertEquals(annotationData.getHeight(), annotationData2.getHeight());
        assertEquals(annotationData.getX(), annotationData2.getX());
        assertEquals(annotationData.getY(), annotationData2.getY());
        assertArrayEquals(annotationData.getStyleRanges(), annotationData2.getStyleRanges());
        assertEquals(annotationData.getText(), annotationData2.getText());

        //don't include bounds
        annotationData2 = AnnotationData.builder(annotationData, false).build();
        assertEquals(annotationData.getAlignment(), annotationData2.getAlignment());
        assertEquals(annotationData.getBgColor(), annotationData2.getBgColor());
        assertEquals(annotationData.getBorderColor(), annotationData2.getBorderColor());
        assertEquals(annotationData.getDefaultFontSize(), annotationData2.getDefaultFontSize());
        assertNotEquals(annotationData.getWidth(), annotationData2.getWidth());
        assertNotEquals(annotationData.getHeight(), annotationData2.getHeight());
        assertNotEquals(annotationData.getX(), annotationData2.getX());
        assertNotEquals(annotationData.getY(), annotationData2.getY());
        assertArrayEquals(annotationData.getStyleRanges(), annotationData2.getStyleRanges());
        assertEquals(annotationData.getText(), annotationData2.getText());


    }

    @Test
    public void testEqualsAndHashCode() {
        StyleRange styleRange = StyleRange.builder().build();
        AnnotationData annotationData = AnnotationData.builder()
                .setAlignment(TextAlignment.CENTER)
                .setBgColor(10)
                .setBorderColor(20)
                .setBorderSize(5)
                .setDefaultFontSize(3)
                .setDimension(50, 60, 100, 101)
                .setStyleRanges(new StyleRange[]{styleRange})
                .setText("text").build();

        StyleRange styleRange2 = StyleRange.builder().build();
        AnnotationData annotationData2 = AnnotationData.builder()
                .setAlignment(TextAlignment.CENTER)
                .setBgColor(10)
                .setBorderColor(20)
                .setBorderSize(5)
                .setDefaultFontSize(3)
                .setDimension(50, 60, 100, 101)
                .setStyleRanges(new StyleRange[]{styleRange})
                .setText("text").build();
        assertTrue(annotationData.equals(annotationData2));
        assertEquals(annotationData.hashCode(), annotationData2.hashCode());
    }

    @Test
    public void testShift() {
        StyleRange styleRange = StyleRange.builder().build();
        AnnotationData annotationData = AnnotationData.builder()
                .setAlignment(TextAlignment.CENTER)
                .setBgColor(10)
                .setBorderColor(20)
                .setBorderSize(5)
                .setDefaultFontSize(3)
                .setDimension(50, 60, 100, 101)
                .setStyleRanges(new StyleRange[]{styleRange})
                .setText("text")
                .shiftPosition(10, 11).build();

        assertEquals(annotationData.getX(), 60);
        assertEquals(annotationData.getY(), 71);
        assertEquals(annotationData.getWidth(), 100);
        assertEquals(annotationData.getHeight(), 101);
    }

    @Test
    public void testStyleRange() {
        StyleRange sr = StyleRange.builder()
                .setFgColor(10)
                .setFontName("test")
                .setFontSize(11)
                .setFontStyle(12)
                .setLength(13)
                .setStart(14).build();
        assertEquals(sr.getFgColor(), 10);
        assertEquals(sr.getFontName(), "test");
        assertEquals(sr.getFontSize(), 11);
        assertEquals(sr.getFontStyle(), 12);
        assertEquals(sr.getLength(), 13);
        assertEquals(sr.getStart(), 14);

        //equals and hashcode
        StyleRange sr2 = StyleRange.builder()
                .setFgColor(10)
                .setFontName("test")
                .setFontSize(11)
                .setFontStyle(12)
                .setLength(13)
                .setStart(14).build();
        assertTrue(sr.equals(sr2));
        assertEquals(sr.hashCode(), sr2.hashCode());
    }


}
