package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.workflow.def.BaseNodeDef;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.PortTypeDef;
import org.knime.core.workflow.def.impl.BaseNodeDefBuilder;
import org.knime.core.workflow.def.impl.FallibleMetaNodeDef;
import org.knime.core.workflow.def.impl.FalliblePortDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.loader.LoadException;
import org.knime.core.workflow.loader.LoadExceptionTree;

/**
 * Test that the convenience getters of Fallible*Def return the same thing as via access through
 * {@link LoadExceptionTree#getSuppliers()} for various constellations.
 *
 * <pre>
 *  - Single instance: see {@link SingleConvenienceGettersTest}
 *  - Collection attribute
 *    - Make sure it doesn't matter whether setting collection elements in bulk (set*) or individually (addTo, putTo)
 *
 *    - List
 *      - convenience setter
 *        - Def type
 *          - null default value
 *          - clean default value (no load exceptions)
 *          - faulty default value (with load exceptions)
 *        - primitive type
 *          - null default value
 *          - actual default value
 *    - Map
 *
 *
 * </pre>
 *
 * collection types).
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class CollectionConvenienceGettersTest {

    /**
     * Build two metanodes with identical contents, but using different builder methods (addToInPorts and setInPorts).
     *
     * Some of the inports are decorated with {@link LoadException}s that should later be retrievable via
     * {@link LoadExceptionTree#getChildrenWithLoadExceptions()}.
     *
     */
    private static class MetanodeExample {

        // ok
        static final BaseNodeDef BASE_NODE = new BaseNodeDefBuilder().setId(1).build();

        // ok
        static final PortTypeDef SOME_TYPE = new PortTypeDefBuilder().setName("some type name").build();

        // ok
        static final PortDef DEFAULT_PORT = new PortDefBuilder().build();

        // ok
        static final PortDef CORRECT_PORT =
            new PortDefBuilder().setIndex(0).setName("port name").setPortType(SOME_TYPE).build();

        // has exceptions on loading name
        static final FalliblePortDef WITH_EXCEPTION_PORT = new PortDefBuilder()//
            .setIndex(0)//
            .setName(() -> {
                throw new IllegalStateException("Cannot load name for port 1");
            }, "default port name")//
            .build();

        // has exceptions on loading index and port type
        static final FalliblePortDef WITH_EXCEPTION_PORT2 = new PortDefBuilder()//
            .setIndex(() -> {
                throw new InvalidSettingsException("Cannot load index for port 2");
            }, 0)
            // this will overwrite the first attempt: only the last exception will be visible on the def
            .setIndex(() -> {
                throw new InvalidSettingsException("Still cannot load index for port 2");
            }, 1)//
            .setPortType(() -> {
                throw new IOException("Cannot load type for port 2");
            }, SOME_TYPE).build();

        // metanode has no load exceptions, but the added ports have
        static FallibleMetaNodeDef withInPortErrorsList = new MetaNodeDefBuilder()//
            .setInPorts(() -> List.of(CORRECT_PORT, WITH_EXCEPTION_PORT, CORRECT_PORT, WITH_EXCEPTION_PORT2), List.of())
            .setBaseNode(BASE_NODE).build();

        // metanode has no load exceptions, but the added ports have
        static FallibleMetaNodeDef withInPortErrorsIndividual = new MetaNodeDefBuilder()//
            .addToInPorts(() -> CORRECT_PORT, DEFAULT_PORT)//
            .addToInPorts(WITH_EXCEPTION_PORT)//
            .addToInPorts(CORRECT_PORT)//
            .addToInPorts(() -> WITH_EXCEPTION_PORT2, DEFAULT_PORT)//
            .setBaseNode(BASE_NODE).build();

        // metanode has no load exceptions, but the added ports have
        static FallibleMetaNodeDef withInPortErrorsMixed = new MetaNodeDefBuilder()//
            .addToInPorts(CORRECT_PORT)//
            .setInPorts(() -> List.of(WITH_EXCEPTION_PORT2, CORRECT_PORT), List.of())//
            .addToInPorts(WITH_EXCEPTION_PORT)//
            .setBaseNode(BASE_NODE).build();
    }

    /**
     * Test that the port exceptions collected during construction of a metanode are the same, independent of whether
     * the bulk set method (setInPorts) is used or the convenience methods (addTo).
     *
     * This requires that added lists of defs are correctly unpacked and checked for {@link LoadException}s that are
     * then linked to the containing def - for later traversal of the def tree.
     */
    @Test
    public void testCollectException() {

        // all three should have the same exception structure
        var testCases = List.of(MetanodeExample.withInPortErrorsList, MetanodeExample.withInPortErrorsIndividual,
            MetanodeExample.withInPortErrorsMixed);

        // irrespective of how the object was constructed, assert the expected exception structure
        for (FallibleMetaNodeDef metaNodeWithErrors : testCases) {

            // metanode indicates load problems are present
            assertThat(metaNodeWithErrors.hasExceptions()).isTrue();
            //
            assertThat(metaNodeWithErrors.hasExceptions(MetaNodeDef.Attribute.IN_PORTS_ELEMENTS)).isTrue();

            // access via LoadExceptionSupplier interfaces yields the same as convenience getters
            assertThat(metaNodeWithErrors.getSuppliers().get(MetaNodeDef.Attribute.IN_PORTS_ELEMENTS))
                .hasSameElementsAs(metaNodeWithErrors.getFaultyInPorts());
            // flattened getter returns the same as convenience getter
            // (because there's only one key in the map, IN_PORTS_ELEMENTS)
            assertThat(metaNodeWithErrors.getFlattenedLoadExceptions())
                .containsOnly(MetanodeExample.WITH_EXCEPTION_PORT, MetanodeExample.WITH_EXCEPTION_PORT2);

            // no exception on the collection container, only on the elements
            assertThat(metaNodeWithErrors.getInPortsException()).isEmpty();

            // TODO how to obtain the exceptions associated to the individual elements?
//            assertThat(metaNodeWithErrors.getInPortsExceptions()).isEmpty();

            // check the structure of the map with the exceptions
            var loadExceptions = metaNodeWithErrors.getSuppliers();
            assertThat(loadExceptions).isNotEmpty();

            // check that there's a list of exception suppliers listed under IN_PORTS:
            assertThat(loadExceptions).containsOnlyKeys(MetaNodeDef.Attribute.IN_PORTS_ELEMENTS);
            // the list references two faulty defs: WITH_EXCEPTION_PORT and WITH_EXCEPTION_PORT2
            assertThat(loadExceptions.get(MetaNodeDef.Attribute.IN_PORTS_ELEMENTS)).size().isEqualTo(2);

            // convenience getter
            assertThat(metaNodeWithErrors.getFaultyInPorts())//
                .isEqualTo(loadExceptions.get(MetaNodeDef.Attribute.IN_PORTS_ELEMENTS));

            List<FalliblePortDef> faultyInPorts = metaNodeWithErrors.getFaultyInPorts();
            assertThat(metaNodeWithErrors.getFlattenedLoadExceptions())
                .containsOnly(MetanodeExample.WITH_EXCEPTION_PORT, MetanodeExample.WITH_EXCEPTION_PORT2);

            {
                // the first has one exception on name
                FalliblePortDef withExceptionPort = faultyInPorts.get(0);

                // only one key in the map, that's name
                assertEquals(Set.of(PortDef.Attribute.NAME), Set.copyOf(withExceptionPort.getSuppliers().keySet()));

                // only one exception associated to it...
                assertThat(withExceptionPort.getSuppliers().get(PortDef.Attribute.NAME).size()).isOne();
                // and the convenience getter
                assertThat(withExceptionPort.getNameException()).isPresent();

                // looks like this
                // exceptions have no semantic equals built-in.
                exceptionEquals(
                    new LoadException(PortDef.Attribute.NAME, new IllegalStateException("Cannot load name for port 1")),
                    withExceptionPort.getNameException().get());
            }

            {
                // the second has an exception on index and port type
                FalliblePortDef withExceptionPort = faultyInPorts.get(1);

                assertThat(withExceptionPort.getSuppliers())//
                    .containsOnlyKeys(PortDef.Attribute.INDEX, PortDef.Attribute.PORT_TYPE);

                // one exception associated to index
                assertThat(withExceptionPort.getSuppliers().get(PortDef.Attribute.INDEX).size()).isOne();
                assertThat(withExceptionPort.getIndexException()).isPresent();

                exceptionEquals(
                    new LoadException(PortDef.Attribute.INDEX,
                        new InvalidSettingsException("Still cannot load index for port 2")),
                    withExceptionPort.getIndexException().get());

                // one exception associated to port type
                assertThat(withExceptionPort.getSuppliers().get(PortDef.Attribute.PORT_TYPE).size()).isOne();
                assertThat(withExceptionPort.getFaultyPortType()).isPresent();

                var falliblePortTypeDef = withExceptionPort.getFaultyPortType().get();
                var actual = falliblePortTypeDef.getLoadException().get();
                exceptionEquals(
                    new LoadException(PortDef.Attribute.PORT_TYPE, new IOException("Cannot load type for port 2")),
                    actual);
            }
        }
    }

    private static void exceptionEquals(final Exception expected, final Exception actual) {
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
    }
}
