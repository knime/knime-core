package org.knime.core.node;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.internal.NodeDescriptionUtil;

public class NodeDescriptionUtilTest {

    @Test
    public void testStripXmlFragment() {
        assertThat(NodeDescriptionUtil.stripXmlFragment("<frag>Text</frag>"), is("Text"));
        assertThat(NodeDescriptionUtil.stripXmlFragment("<frag att='Bla' >Text</frag>"), is("Text"));
        assertThat(NodeDescriptionUtil.stripXmlFragment("<fragText</frag>"), is(""));
        assertThat(NodeDescriptionUtil.stripXmlFragment("<fragText</frag"), is(""));
        assertThat(NodeDescriptionUtil.stripXmlFragment("frag>Text/frag>"), is(""));
    }

    @Test
    public void testNormalizeWhitespace() {
        Assert.assertEquals("text text",
            NodeDescriptionUtil.normalizeWhitespace("    \t  \n   \r\n  text   text    \n   "));
    }

}