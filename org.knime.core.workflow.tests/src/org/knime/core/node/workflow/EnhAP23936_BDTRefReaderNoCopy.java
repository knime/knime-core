package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.knime.shared.workflow.def.NativeNodeDef;
import org.knime.shared.workflow.storage.util.PasswordRedactor;

/**
 * Tests that BufferedDataTable Reference Reader cannot be copied anymore.
 * The reason is that its referenced file is not copied with it and hence the pasted instances
 * is not functional.
 * 
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class EnhAP23936_BDTRefReaderNoCopy extends WorkflowTestCase {

    private WorkflowManager m_wfm;
    private NodeID m_bdtRefReader;
    private NodeID m_poRefReader;
    private NodeID m_rowFilter;
    private NodeID m_columnFilter;

    @Before
    public void setUp() throws Exception {
        final var baseID = loadAndSetWorkflow();
        m_wfm = getManager();
        // reference readers that should not be copied
        m_bdtRefReader = baseID.createChild(1);
        m_poRefReader = baseID.createChild(12);

        // normal nodes that should be copied
        m_rowFilter = baseID.createChild(2);
        m_columnFilter = baseID.createChild(3);
    }

    @Test
    public void testCopyViaDefs() {
        final var spec = WorkflowCopyContent.builder() //
                .setNodeIDs(m_bdtRefReader, m_poRefReader, m_rowFilter, m_columnFilter) //
                .setIncludeInOutConnections(false) //
                .build();
        final var wf = m_wfm.copyToDef(spec, PasswordRedactor.asNull()).getPayload();

        // copied only two nodes
        assertThat(wf.getNodes().size(), is(2));

        final var bdtReader = wf.getNodes().get("1");
        assertTrue("BufferedDataTable Reference Reader should not be copied", bdtReader == null);
        final var poReader = wf.getNodes().get("12");
        assertTrue("PortObject Reference Reader should not be copied", poReader == null);

        // the copied nodes are Row Filter and Column Filter
        final var rowFilter = (NativeNodeDef)wf.getNodes().get("2");
        assertThat(rowFilter.getNodeName(), is("Row Filter"));
        assertThat(rowFilter.getId(), is(2));

        final var columnFilter = (NativeNodeDef)wf.getNodes().get("3");
        assertThat(columnFilter.getNodeName(), is("Column Filter"));
        assertThat(columnFilter.getId(), is(3));
    }
}
