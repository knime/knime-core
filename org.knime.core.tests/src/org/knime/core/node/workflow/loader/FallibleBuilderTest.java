package org.knime.core.node.workflow.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
import org.knime.core.workflow.loader.LoadExceptionSupplier;

/**
 * Test basic functionality of builders that collect exceptions in their setters. This includes accessing exceptions of
 * nested objects.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FallibleBuilderTest {

    /**
     * Build two metanodes with identical contents, but using different builder methods (addToInPorts and setInPorts).
     *
     * Some of the inports are decorated with {@link LoadException}s that should later be retrievable via
     * {@link LoadExceptionSupplier#getChildrenWithLoadExceptions()}.
     *
     */
    private static class MetanodeExample {

        // ok
        static final BaseNodeDef BASE_NODE = new BaseNodeDefBuilder()//
            .build();

        // ok
        static final PortTypeDef SOME_TYPE = new PortTypeDefBuilder()//
            .setName("some type name")//
            .build();

        // ok
        static final PortDef DEFAULT_PORT = new PortDefBuilder().build();

        // ok
        static final PortDef CORRECT_PORT =
            new PortDefBuilder().setIndex(0).setName("port name").setPortType(SOME_TYPE).build();

        // has exceptions on loading name
        static final PortDef WITH_EXCEPTION_PORT = new PortDefBuilder()//
            .setIndex(0)//
            .setName(() -> {
                throw new IllegalStateException("Cannot load name for port 1");
            }, "default port name")//
            .build();

        // has exceptions on loading index and port type
        static final PortDef WITH_EXCEPTION_PORT2 = new PortDefBuilder()//
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

    public void testStuff() throws Exception {

        var lesList = MetanodeExample.withInPortErrorsList;
        var lesIndividual = MetanodeExample.withInPortErrorsIndividual;

        FallibleMetaNodeDef def = MetaNodeLoader.load(null, null, null); //

        // non-recursive
        if (!def.getSupplierExceptions().isEmpty()) {
            Map<MetaNodeDef.Attribute, List<LoadException>> problems = def.getSupplierExceptions();

            if (problems.get(MetaNodeDef.Attribute.IN_PORTS) != null) {
                // logging
                // load result add error
            }

        }

        // recursive
        if (!def.getChildrenWithLoadExceptions().isEmpty()) {

            for (var child : def.getChildrenWithLoadExceptions().values()) {

            }

        }

    }

    /**
     * Test that the exceptions collected during construction of {@link #withInPortErrorsList} and
     * {@link #withInPortErrorsIndividual} are the same.
     *
     * This requires that added lists of defs are correctly unpacked and checked for {@link LoadException}s that are
     * then linked to the containing def - for later traversal of the def tree.
     *
     * This requires the convenience adders (e.g., addToInPorts) to work correctly, compared to the aggregate fallible
     * setters (setInPorts).
     *
     * @throws Exception
     */
    @Test
    public void testCollectException() throws Exception {

        // both should have the same exception structure
        var testCases = List.of(MetanodeExample.withInPortErrorsList, MetanodeExample.withInPortErrorsIndividual,
            MetanodeExample.withInPortErrorsMixed);

        for (FallibleMetaNodeDef metaNodeWithErrors : testCases) {

            // since the fallible supplier () -> WITH_EXCEPTION_PORT doesn't throw an
            // exception the object itself hasn't had loading exceptions
            assertThat(metaNodeWithErrors.getSupplierExceptions()).isEmpty();

            var children = metaNodeWithErrors.getChildrenWithLoadExceptions();
            assertThat(children).isNotEmpty();

            // check that there's a list of exception suppliers listed under IN_PORTS:
            assertThat(children).containsOnlyKeys(MetaNodeDef.Attribute.IN_PORTS);
            // the list references two faulty defs: WITH_EXCEPTION_PORT and WITH_EXCEPTION_PORT2
            assertThat(children.get(MetaNodeDef.Attribute.IN_PORTS)).size().isEqualTo(2);

            // convenience getter
            assertThat(metaNodeWithErrors.getInPortsWithException())//
                .isEqualTo(children.get(MetaNodeDef.Attribute.IN_PORTS));

            List<FalliblePortDef> inportExceptions = metaNodeWithErrors.getInPortsWithException();

            {
                // the first has one exception on name
                FalliblePortDef withExceptionPort = inportExceptions.get(0);

                // only one key in the map, that's name
                assertEquals(Set.of(PortDef.Attribute.NAME),
                    Set.copyOf(withExceptionPort.getSupplierExceptions().keySet()));

                // only one exception associated to it...
                assertThat(withExceptionPort.getSupplierExceptions().get(PortDef.Attribute.NAME).size()).isOne();
                // and the convenience getter
                assertThat(withExceptionPort.getNameException()).isPresent();

                // looks like this
                // exceptions have no semantic equals built-in.
                exceptionEquals(new LoadException(new IllegalStateException("Cannot load name for port 1")),
                    withExceptionPort.getNameException().get());
            }

            {
                // the second has an exception on index and port type
                LoadExceptionSupplier<PortDef.Attribute> withExceptionPort = //
                    inportExceptions.get(1);

                assertEquals(Set.of(PortDef.Attribute.INDEX, PortDef.Attribute.PORT_TYPE),
                    Set.copyOf(withExceptionPort.getSupplierExceptions().keySet()));

                // one exception associated to index
                assertThat(withExceptionPort.getSupplierExceptions().get(PortDef.Attribute.INDEX).size()).isOne();
                exceptionEquals(new LoadException(new InvalidSettingsException("Still cannot load index for port 2")),
                    withExceptionPort.getSupplierExceptions().get(PortDef.Attribute.INDEX).get(0));

                assertThat(withExceptionPort.getSupplierExceptions().get(PortDef.Attribute.PORT_TYPE).size()).isOne();
                exceptionEquals(new LoadException(new IOException("Cannot load type for port 2")),
                    withExceptionPort.getSupplierExceptions().get(PortDef.Attribute.PORT_TYPE).get(0));
            }
        }
    }

    private static void exceptionEquals(final Exception expected, final Exception actual) {
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
    }
}
