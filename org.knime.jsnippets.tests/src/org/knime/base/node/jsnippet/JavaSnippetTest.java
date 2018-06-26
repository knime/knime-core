package org.knime.base.node.jsnippet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import javax.swing.text.Document;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.base.node.jsnippet.expression.Abort;
import org.knime.base.node.jsnippet.expression.AbstractJSnippet;
import org.knime.base.node.jsnippet.type.ConverterUtil;
import org.knime.base.node.jsnippet.util.JavaSnippetFields;
import org.knime.base.node.jsnippet.util.JavaSnippetSettings;
import org.knime.base.node.jsnippet.util.field.InCol;
import org.knime.base.node.jsnippet.util.field.OutCol;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

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
 *   20.02.2018 (Jonathan Hale): created
 */

/**
 * Test for {@link JavaSnippet}.
 *
 * @author Jonathan Hale
 */
public class JavaSnippetTest {

    JavaSnippet snippet;

    /** Create a JavaSnippet instance */
    @Before
    public void before() {
         snippet = new JavaSnippet();
    }

    /** Close the JavaSnippet instance */
    @After
    public void after() {
         snippet.close();
    }

    /**
     * Test execution of a simple java snippet.
     * @throws Exception
     */
    @Test
    public void testSimpleSnippet() throws Exception {
        final JavaSnippetSettings settings = new JavaSnippetSettings("throw new Abort(\"success\");");
        snippet.setSettings(settings);

        final AbstractJSnippet s = snippet.createSnippetInstance();
        assertNotNull(s);

        try {
            s.snippet();
            fail("Expected exception to be thrown by snippet");
        } catch (Abort e) {
            if(!e.getMessage().equals("success")) {
                throw e;
            }
        }
    }

    /**
     * Test encoding.
     * @throws Exception
     */
    @Test
    public void testEncoding() throws Exception {
        final JavaSnippetSettings settings = new JavaSnippetSettings("outString = \"äüö\";");

        final OutCol outCol = new OutCol();
        outCol.setJavaName("outString");
        outCol.setConverterFactory(ConverterUtil.getConverterFactory(String.class, StringCell.TYPE).get());
        settings.getJavaSnippetFields().getOutColFields().add(outCol);
        snippet.setSettings(settings);

        final AbstractJSnippet s = snippet.createSnippetInstance();
        assertNotNull(s);

        s.snippet();

        final Field outStringField = s.getClass().getField("outString");
        final String string = (String)outStringField.get(s);

        assertEquals(string, "äüö");
    }

    /**
     * Test compiling with additional eclipse/osgi bundles.
     * @throws Exception
     */
    @Test
    public void testAdditionalBundles() throws Exception {
        final JavaSnippetSettings settings = new JavaSnippetSettings("new Complex(1.0, 1.0);");
        settings.setBundles(new String[]{"org.apache.commons.math3"});
        settings.setScriptImports("import org.apache.commons.math3.complex.Complex;");
        snippet.setSettings(settings);

        final AbstractJSnippet s = snippet.createSnippetInstance();
        assertNotNull(s);
        s.snippet();
    }

    /**
     * Test code generation
     * @throws Exception
     */
    @Test
    public void testCodeGeneration() throws Exception {
        final JavaSnippetSettings settings = new JavaSnippetSettings("// code here");
        settings.setScriptImports("import java.lang.String;");

        final JavaSnippetFields fields = new JavaSnippetFields();

        final InCol inCol = new InCol();
        inCol.setJavaName("m_inCol");
        inCol.setKnimeName("inCol");
        inCol.setConverterFactory(IntCell.TYPE,
            ConverterUtil.getConverterFactory(IntCell.TYPE, Integer.class).get());
        fields.getInColFields().add(inCol);

        final OutCol outCol = new OutCol();
        outCol.setJavaName("m_outCol");
        outCol.setKnimeName("outCol");
        outCol.setConverterFactory(ConverterUtil.getConverterFactory(String.class, StringCell.TYPE).get());
        fields.getOutColFields().add(outCol);

        settings.setJavaSnippetFields(fields);
        snippet.setSettings(settings);
        snippet.getSettings(); // this updates the settings, i.e. the generated code

        final Document doc = snippet.getDocument();
        assertEquals(
            "// system imports\n" +
            "import org.knime.base.node.jsnippet.expression.AbstractJSnippet;\n" +
            "import org.knime.base.node.jsnippet.expression.Abort;\n" +
            "import org.knime.base.node.jsnippet.expression.Cell;\n" +
            "import org.knime.base.node.jsnippet.expression.ColumnException;\n" +
            "import org.knime.base.node.jsnippet.expression.TypeException;\n" +
            "import static org.knime.base.node.jsnippet.expression.Type.*;\n" +
            "import java.util.Date;\n" +
            "import java.util.Calendar;\n" +
            "import org.w3c.dom.Document;\n\n\n" +

            "import java.lang.String;\n" +
            "// system variables\n" +
            "public class JSnippet extends AbstractJSnippet {\n" +

            "  // Fields for input columns\n" +
            "  /** Input column: \"inCol\" */\n" +
            "  public Integer m_inCol;\n\n" +

            "  // Fields for output columns\n" +
            "  /** Output column: \"outCol\" */\n" +
            "  public String m_outCol;\n\n" +

            "// Your custom variables:\n\n" +
            "// expression start\n" +
            "    public void snippet() throws TypeException, ColumnException, Abort {\n" +
            "// Enter your code here:\n\n" +
            "\t\t// code here\n\n\n" +
            "// expression end\n" +
            "    }\n" +
            "}\n", doc.getText(0, doc.getLength()));
    }
}
