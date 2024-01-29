package org.knime.core.node.workflow;

import static org.hamcrest.MatcherAssert.assertThat;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.testing.util.URIToFileResolveTestUtil;
import org.mockito.Mockito;

/**
 * Verifies that all component updates are found. The issue of AP-20451
 * describes the case where components of the same kind (same remote URI) were
 * not checked when the first component was up-to-date. A component/metanode
 * template is not only characterized by its URI, but also the timestamp (which
 * we use as version). Since we cache update statuses per template kind, it was
 * the case that we overlooked an out-of-date component because of the checking
 * order.
 * 
 * This test checks that despite the first checked component being up-to-date,
 * the second one is still checked and the update is found.
 *
 * @author Leon Wenzler, KNIME GmbH, Konstanz, Germany
 */
public class BugAP20451_NotAllComponentUpdatesFound extends WorkflowTestCase {

    private URIToFileResolve m_origResolveService;

    private NodeContainerTemplate m_upToDateComponent, m_outOfDateComponent;

    /**
     * Initialize all node IDs
     * 
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // setup resolve mock to mock remote template
        URIToFileResolve resolveMock = Mockito.mock(URIToFileResolve.class);
        m_origResolveService = URIToFileResolveTestUtil.replaceURIToFileResolveService(resolveMock);

        loadAndSetWorkflow(new File(getDefaultWorkflowDirectory(), "mainWorkflow"));
        final var templates = getManager().getNodeContainers().stream()//
                .map(NodeContainerTemplate.class::cast)//
                .sorted((o1, o2) -> o1.getID().compareTo(o2.getID()))//
                .toList();
        if (templates.size() < 2) {
            throw new IllegalStateException("Not all node container templates could be loaded from workflow");
        }
        m_upToDateComponent = templates.get(0);
        m_outOfDateComponent = templates.get(1);

        // *no* update is available for A (#1)
        final var updateTemplateDir = new File(getDefaultWorkflowDirectory(), "A");
        final var ifModified1 = m_upToDateComponent.getTemplateInformation().getTimestamp().toZonedDateTime();
        when(resolveMock.resolveToLocalOrTempFileConditional(any(), any(), eq(ifModified1)))
                .thenReturn(Optional.empty());
        // an update is available for A (#2)
        final var ifModified2 = m_outOfDateComponent.getTemplateInformation().getTimestamp().toZonedDateTime();
        when(resolveMock.resolveToLocalOrTempFileConditional(any(), any(), eq(ifModified2)))
                .thenReturn(Optional.of(updateTemplateDir));
    }

    @Test
    public void testAllUpdatesAreFound() throws Exception {
        // order is important: the first up-to-date status should not influence the
        // second update
        checkForUpdateInOrder(orderedMap(//
                m_upToDateComponent, UpdateStatus.UpToDate, //
                m_outOfDateComponent, UpdateStatus.HasUpdate));
        checkForUpdateInOrder(orderedMap(//
                m_outOfDateComponent, UpdateStatus.HasUpdate, //
                m_upToDateComponent, UpdateStatus.UpToDate));
    }

	@After
	public void resetURIToFileResolveService() {
		URIToFileResolveTestUtil.replaceURIToFileResolveService(m_origResolveService);
	}

    /**
     * Checks for updates in the order of the map given, containing expected update
     * statuses.
     * 
     * @param expectedUpdateStatuses map from NCT to expected status
     * @throws IOException if something went wrong
     */
    private void checkForUpdateInOrder(final Map<NodeContainerTemplate, UpdateStatus> expectedUpdateStatuses)
            throws IOException {
        final var actualUpdateStatuses = TemplateUpdateUtil.fillNodeUpdateStates(expectedUpdateStatuses.keySet(),
                new WorkflowLoadHelper(true, getManager().getContextV2()), new LoadResult("ignored"), new HashMap<>());

        for (final var template : expectedUpdateStatuses.keySet()) {
            final var expected = expectedUpdateStatuses.get(template);
            final var actual = actualUpdateStatuses.get(template.getID());
            assertThat(String.format("Component '%s' should be %s, but is actually %s", template.getNameWithID(),
                    expected, actual), actual == expected);
        }
    }

    /**
     * {@link Map#of(Object, Object, Object, Object))} does not respect insert
     * order, hence this method.
     * 
     * @param <K>    key type
     * @param <V>    value type
     * @param key1
     * @param value1
     * @param key2
     * @param value2
     * @return ordered map with two key-value entries
     */
    private static <K, V> Map<K, V> orderedMap(K key1, V value1, K key2, V value2) {
        final Map<K, V> map = new LinkedHashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }
    
}
