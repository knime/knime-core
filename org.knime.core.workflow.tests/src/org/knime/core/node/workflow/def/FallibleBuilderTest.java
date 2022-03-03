package org.knime.core.node.workflow.def;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.PortDef;
import org.knime.core.workflow.def.PortTypeDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.PortDefBuilder;
import org.knime.core.workflow.def.impl.PortTypeDefBuilder;
import org.knime.core.workflow.loader.LoadException;
import org.knime.core.workflow.loader.LoadExceptionSupplier;

/**
 * 
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class FallibleBuilderTest {

    /**
     * Build two metanodes with identical contents, but using different builder
     * methods (addToInPorts and setInPorts).
     * 
     * Some of the inports are decorated with {@link LoadException}s that should
     * later be retrievable via
     * {@link LoadExceptionSupplier#getChildLoadExceptionSuppliers()}.
     * 
     */
    private static class Example1 {

	// ok
	static final PortTypeDef SOME_TYPE = new PortTypeDefBuilder()//
		.setName("some type name")//
		.build();

	// ok
	static final PortDef DEFAULT_PORT = new PortDefBuilder().build();

	// ok
	static final PortDef CORRECT_PORT = new PortDefBuilder().setIndex(0).setName("port name").setPortType(SOME_TYPE)
		.build();

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
		}, 0)//
		.setIndex(() -> {
		    throw new InvalidSettingsException("Still cannot load index for port 2");
		}, 1)//
		.setPortType(() -> {
		    throw new IOException("Cannot load type for port 2");
		}, SOME_TYPE).build();

	// metanode has no load exceptions, but the added ports have
	static MetaNodeDef withInPortErrorsList = new MetaNodeDefBuilder()//
		.setNodeType("MetaNode")//
		.setInPorts(() -> List.of(CORRECT_PORT, WITH_EXCEPTION_PORT, CORRECT_PORT, WITH_EXCEPTION_PORT2),
			List.of())
		.build();

	// metanode has no load exceptions, but the added ports have
	static MetaNodeDef withInPortErrorsIndividual = new MetaNodeDefBuilder()//
		.setNodeType("MetaNode")//
		.addToInPorts(() -> CORRECT_PORT, DEFAULT_PORT)//
		.addToInPorts(() -> WITH_EXCEPTION_PORT, DEFAULT_PORT)//
		.addToInPorts(() -> CORRECT_PORT, DEFAULT_PORT)//
		.addToInPorts(() -> WITH_EXCEPTION_PORT2, DEFAULT_PORT)//
		.build();
    }

    /**
     * Test that the exceptions collected during construction of
     * {@link #withInPortErrorsList} and {@link #withInPortErrorsIndividual} are the
     * same.
     * 
     * This requires that added lists of defs are correctly unpacked and checked for
     * {@link LoadException}s that are then linked to the containing def - for later
     * traversal of the def tree.
     * 
     * This requires the convenience adders (e.g., addToInPorts) to work correctly,
     * compared to the aggregate fallible setters (setInPorts).
     * 
     * @throws Exception
     */
    @Test
    public void testCollectException() throws Exception {

	assertThat("Object has not been decorated with exceptions.",
		Example1.withInPortErrorsList instanceof LoadExceptionSupplier);

	assertThat("Object has not been decorated with exceptions.",
		Example1.withInPortErrorsIndividual instanceof LoadExceptionSupplier);

	var lesList = (LoadExceptionSupplier<?>) Example1.withInPortErrorsList;
	var lesIndividual = (LoadExceptionSupplier<?>) Example1.withInPortErrorsIndividual;

	// both should have the same exception structure
	for (LoadExceptionSupplier<?> metaNodeWithErrors : List.of(lesList, lesIndividual)) {

	    // since the fallible supplier () -> WITH_EXCEPTION_PORT doesn't throw an
	    // exception
	    assertTrue("Metanode itself must not have errors.", metaNodeWithErrors.getLoadExceptions().isEmpty());

	    var children = metaNodeWithErrors.getChildLoadExceptionSuppliers();
	    assertTrue("Metanode must have links to child exceptions.", !children.isEmpty());

	    // check that there are two exception suppliers are linked, WITH_EXCEPTION_PORT
	    // and WITH_EXCEPTION_PORT2
	    assertEquals(2, children.size());

	    {
		// the first has one exception on name
		LoadExceptionSupplier<PortDef.Attribute> withExceptionPort = //
			(LoadExceptionSupplier<PortDef.Attribute>) children.get(0);

		// only one key in the map, that's name
		assertEquals(Set.of(PortDef.Attribute.NAME),
			Set.copyOf(withExceptionPort.getLoadExceptions().keySet()));
		// only one exception associated to it...
		assertEquals(1, withExceptionPort.getLoadExceptions().get(PortDef.Attribute.NAME).size());
		// looks like this
		// exceptions have no semantic equals built-in.
		exceptionEquals(new LoadException(new IllegalStateException("Cannot load name for port 1")),
			withExceptionPort.getLoadExceptions().get(PortDef.Attribute.NAME).get(0));
	    }

	    {
		// the second has an exception on index and port type
		LoadExceptionSupplier<PortDef.Attribute> withExceptionPort = //
			(LoadExceptionSupplier<PortDef.Attribute>) children.get(1);

		assertEquals(Set.of(PortDef.Attribute.INDEX, PortDef.Attribute.PORT_TYPE),
			Set.copyOf(withExceptionPort.getLoadExceptions().keySet()));

		// two exceptions associated to index
		assertEquals(2, withExceptionPort.getLoadExceptions().get(PortDef.Attribute.INDEX).size());
		exceptionEquals(new LoadException(new InvalidSettingsException("Cannot load index for port 2")),
			withExceptionPort.getLoadExceptions().get(PortDef.Attribute.INDEX).get(0));
		exceptionEquals(new LoadException(new InvalidSettingsException("Still cannot load index for port 2")),
			withExceptionPort.getLoadExceptions().get(PortDef.Attribute.INDEX).get(1));

		assertEquals(1, withExceptionPort.getLoadExceptions().get(PortDef.Attribute.PORT_TYPE).size());
		exceptionEquals(new LoadException(new IOException("Cannot load type for port 2")),
			withExceptionPort.getLoadExceptions().get(PortDef.Attribute.PORT_TYPE).get(0));

	    }

	}

    }

    private static void exceptionEquals(Exception expected, Exception actual) {
	assertEquals(expected.getMessage(), actual.getMessage());
	assertEquals(expected.getCause().getMessage(), actual.getCause().getMessage());
    }
}
