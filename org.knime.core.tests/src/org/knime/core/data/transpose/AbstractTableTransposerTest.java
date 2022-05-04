package org.knime.core.data.transpose;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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

    private static final DataRow transposedABCDEF = new DefaultRow("Alpha", "A", "B", "C", "D", "E", "F");
    private static final DataRow transposed123456 = new DefaultRow("Omega", "1", "2", "3", "4", "5", "6");

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

        container.addRowToTable(new DefaultRow("Row0", "A", "1"));
        container.addRowToTable(new DefaultRow("Row1", "B", "2"));
        container.addRowToTable(new DefaultRow("Row2", "C", "3"));
        container.addRowToTable(new DefaultRow("Row3", "D", "4"));
        container.addRowToTable(new DefaultRow("Row4", "E", "5"));
        container.addRowToTable(new DefaultRow("Row5", "F", "6"));
        container.close();
        testTable = container.getTable();

        MemoryAlertSystemTest.forceGC();
    }

    @Test
    void testMinChunks() {
        assertThrows(IllegalArgumentException.class, () -> new FixedChunksTransposer(testTable, m_exec, 0));
    }

    /**
     * Test transposing the table with different fixed chunk sizes.
     * @param chunkSize number of columns to transpose at once
     * @throws CanceledExecutionException
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void testFixedChunksTransposer(final int chunkSize) throws CanceledExecutionException {
        var transposer = new FixedChunksTransposer(testTable, m_exec, chunkSize);
        transposer.transpose();
        var resultTable = transposer.getTransposedTable();
        List<DataRow> result = StreamSupport.stream(resultTable.spliterator(), false).collect(Collectors.toList());

        assertThat(dataRowsEqual(result.get(0), transposedABCDEF)).isTrue();
        assertThat(dataRowsEqual(result.get(1), transposed123456)).isTrue();
    }

    /**
     * Test transposing the table with chunk sizes that react to available memory.
     * Transpose result must be independent of what the alert system does.
     * @param alertSystemMock provider for the memory low condition
     * @throws CanceledExecutionException
     */
    @ParameterizedTest
    @MethodSource("memoryAlertSystemMocks")
    void testMemoryAwareTransposer(final BooleanSupplier alertSystemMock) throws CanceledExecutionException {
        var transposer = new MemoryAwareTransposer(testTable, m_exec, alertSystemMock);
        transposer.transpose();
        var resultTable = transposer.getTransposedTable();
        List<DataRow> result = StreamSupport.stream(resultTable.spliterator(), false).collect(Collectors.toList());

        assertThat(dataRowsEqual(result.get(0), transposedABCDEF)).isTrue();
        assertThat(dataRowsEqual(result.get(1), transposed123456)).isTrue();
    }

    /** Provides the memory alert system stubs that simulate different low memory scenarios. */
    private static Stream<BooleanSupplier> memoryAlertSystemMocks() {
        return Stream.of(
            // never throw an alert
            () -> false,
            // always throw an alert
            () -> true,
            // throws an alert: false, true, false, ...
            new BooleanSupplier() {
                long i = 0;
                @Override public boolean getAsBoolean() {
                    return (i++) % 2 != 0;
                }
            },
            // throws an alert: true, false, true, ...
            new BooleanSupplier() {
                long i = 0;
                @Override public boolean getAsBoolean() {
                    return (i++) % 2 == 0;
                }
            });
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
