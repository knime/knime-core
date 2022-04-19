package org.knime.core.data.transpose;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.memory.MemoryAlertSystemTest;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.exec.dataexchange.in.PortObjectInNodeFactory;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowTableBackendSettingsTestUtils;

/**
 * Test class for the {@link AbstractTableTransposer}.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class AbstractTableTransposerTest {

    private WorkflowManager m_manager;

    private ExecutionContext m_exec;

    private BufferedDataTable testTable;

    /**
     * @throws java.lang.Exception
     */
    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() throws Exception {
        m_manager = WorkflowManager.ROOT.createAndAddProject(
            getClass().getSimpleName(), new WorkflowCreationHelper());
        WorkflowTableBackendSettingsTestUtils.forceOldTableBackendOnto(m_manager);
        var nodeID = m_manager.createAndAddNode(new PortObjectInNodeFactory());
        var nodeContainer = m_manager.getNodeContainer(nodeID, NativeNodeContainer.class, true);
        m_exec = nodeContainer.createExecutionContext();
        NodeContext.pushContext(nodeContainer);

        DataColumnSpec[] colSpecs =
            new DataColumnSpec[]{new DataColumnSpecCreator("Alpha", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Omega", StringCell.TYPE).createSpec()};

        DataTableSpec spec = new DataTableSpec(colSpecs);
        final BufferedDataContainer container = m_exec.createDataContainer(spec);

        container.addRowToTable(new DefaultRow("Row0", "A", "X"));
        container.addRowToTable(new DefaultRow("Row1", "B", "Y"));
        container.addRowToTable(new DefaultRow("Row2", "C", "Z"));
        container.close();
        testTable = container.getTable();

        MemoryAlertSystemTest.forceGC();
    }

    @Test
    void testNoAlert() throws CanceledExecutionException {
        var transposer = new FixedChunksTransposer(testTable, m_exec, 1000);
        transposer.transpose();
        var resultTable = transposer.getTransposedTable();
        List<DataRow> result = StreamSupport.stream(resultTable.spliterator(), false).collect(Collectors.toList());

        assertThat(dataRowsEqual(result.get(0), new DefaultRow("Alpha", "A", "B", "C"))).isTrue();
        assertThat(dataRowsEqual(result.get(1), new DefaultRow("Omega", "X", "Y", "Z"))).isTrue();
    }

    @Test
    void testAlwaysAlert() throws CanceledExecutionException {
        var transposer = new MemoryAwareTransposer(testTable, m_exec, () -> true);
        transposer.transpose();
        var resultTable = transposer.getTransposedTable();
        List<DataRow> result = StreamSupport.stream(resultTable.spliterator(), false).collect(Collectors.toList());

        assertThat(dataRowsEqual(result.get(0), new DefaultRow("Alpha", "A", "B", "C"))).isTrue();
        assertThat(dataRowsEqual(result.get(1), new DefaultRow("Omega", "X", "Y", "Z"))).isTrue();
    }

    @Test
    void testAlertEverySecondTime() throws CanceledExecutionException {
        var alertEverySecondTime = new BooleanSupplier() {
            long i = 0;
            @Override public boolean getAsBoolean() {
                return (i++)%2 == 0;
            }
        };
        var transposer = new MemoryAwareTransposer(testTable, m_exec, alertEverySecondTime);
        transposer.transpose();
        var resultTable = transposer.getTransposedTable();
        List<DataRow> result = StreamSupport.stream(resultTable.spliterator(), false).collect(Collectors.toList());

        assertThat(dataRowsEqual(result.get(0), new DefaultRow("Alpha", "A", "B", "C"))).isTrue();
        assertThat(dataRowsEqual(result.get(1), new DefaultRow("Omega", "X", "Y", "Z"))).isTrue();
    }

    @AfterEach
    void destroyWorkflowManager() {
        m_exec = null;
        m_manager.getParent().removeProject(m_manager.getID());
        m_manager = null;
    }

    private static boolean dataRowsEqual(final DataRow r1, final DataRow r2){
        return r1.getKey().equals(r2.getKey())
            && r1.stream().collect(Collectors.toList()).equals(r2.stream().collect(Collectors.toList()));
    }

}
