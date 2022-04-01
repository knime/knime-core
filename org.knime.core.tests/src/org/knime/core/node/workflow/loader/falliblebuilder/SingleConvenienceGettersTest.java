package org.knime.core.node.workflow.loader.falliblebuilder;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.knime.core.workflow.def.ConnectionDef;
import org.knime.core.workflow.def.MetaNodeDef;
import org.knime.core.workflow.def.NodeAnnotationDef;
import org.knime.core.workflow.def.impl.BaseNodeDefBuilder;
import org.knime.core.workflow.def.impl.FallibleBaseNodeDef;
import org.knime.core.workflow.def.impl.FallibleMetaNodeDef;
import org.knime.core.workflow.def.impl.FallibleNodeAnnotationDef;
import org.knime.core.workflow.def.impl.MetaNodeDefBuilder;
import org.knime.core.workflow.def.impl.NodeAnnotationDefBuilder;
import org.knime.core.workflow.loader.ListLoadExceptionSupplierAdapter;
import org.knime.core.workflow.loader.LoadException;
import org.knime.core.workflow.loader.LoadExceptionTree;

/**
 * Test that the convenience getters of Fallible*Def return the same thing as via access through
 * {@link LoadExceptionTree#getSuppliers()} for various constellations.
 *  <pre>
 *  - Single instance attribute
 *    - Def type
 *      - null default value
 *      - clean default value (no load exceptions)
 *      - faulty default value (with load exceptions)
 *    - primitive type
 *      - null default value
 *      - actual default value
 *  - Collection attribute: see {@link CollectionConvenienceGettersTest}
 *  </pre>
 *
 * collection types).
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class SingleConvenienceGettersTest {

    /**
     * Cannot retrieve a Fallible*Def instance via getFaulty* for a null default, only get*Exception.
     */
    @Test
    public void testSingleDefNullDefault() {
        FallibleBaseNodeDef faultyBaseNode = new BaseNodeDefBuilder().setId(0).setAnnotation(() -> {
            throw new Exception();
        }, null).build();

        // default is used
        assertThat(faultyBaseNode.getAnnotation()).isNull();
        // but a load exception is stored
        assertThat(faultyBaseNode.getAnnotationException()).containsInstanceOf(LoadException.class);
        // however, null can't be wrapped into a fallible def
        assertThat(faultyBaseNode.getFaultyAnnotation()).isEmpty();
    }

    /**
     * Test that the convenience getters of Fallible*Def return the same thing as via access through
     * {@link LoadExceptionTree#getSuppliers()} for a default with no load exceptions.
     */
    @Test
    public void testSingleDefCleanDefault() {
        // single def with clean non-null default
        FallibleNodeAnnotationDef defaultAnnotation = new NodeAnnotationDefBuilder().build();
        FallibleBaseNodeDef faultyBaseNode = new BaseNodeDefBuilder().setId(0).setAnnotation(() -> {
            throw new IOException();
        }, defaultAnnotation).build();

        // default is used
        assertThat(faultyBaseNode.getAnnotation()).isEqualTo(defaultAnnotation);
        // but a load exception is stored
        assertThat(faultyBaseNode.getAnnotationException()).containsInstanceOf(LoadException.class);
        // which can also be accessed via convenience wrapper
        assertThat(faultyBaseNode.getFaultyAnnotation()).isPresent();

        // and the convenience wrapped object behaves as expected
        FallibleNodeAnnotationDef faultyAnnotation = faultyBaseNode.getFaultyAnnotation().get();
        // contains the same content
        assertThat(faultyAnnotation).isEqualTo(defaultAnnotation);
        // but also the load exception
        assertThat(faultyAnnotation.hasExceptions()).isTrue();
        assertThat(faultyAnnotation.getSuppliers()).isEmpty(); // only exception present, no children
        // and the load exception wraps the IOException from above
        assertThat(faultyAnnotation.getLoadException()).containsInstanceOf(LoadException.class);
        assertThat(faultyAnnotation.getLoadException().get().getCause()).isInstanceOf(IOException.class);
    }

    /**
     * Test that the convenience getters of Fallible*Def return the same thing as via access through
     * {@link LoadExceptionTree#getSuppliers()} for a default with load exceptions.
     */
    @Test
    public void testSingleDefFaultyDefault() {
        // single def with default that has load exceptions
        FallibleNodeAnnotationDef faultyDefaultAnnotation = new NodeAnnotationDefBuilder()//
            .setData(() -> {
                throw new RuntimeException();
            }, null).build();
        FallibleBaseNodeDef faultyBaseNode = new BaseNodeDefBuilder().setId(0).setAnnotation(() -> {
            throw new IOException();
        }, faultyDefaultAnnotation).build();

        // default is used
        assertThat(faultyBaseNode.getAnnotation()).isEqualTo(faultyDefaultAnnotation);
        // but a load exception is stored - > LES for complex types ; LE for primitve types
        // TODO rename for complex and primitives
        assertThat(faultyBaseNode.getAnnotationExceptionSupplier()).containsInstanceOf(LoadException.class);
        // which can also be accessed via convenience wrapper
        assertThat(faultyBaseNode.getFaultyAnnotation()).isPresent();

        // WorkflowDef implementation
        // including default values that have been added due to LoadException
        workflow.getConnections();
        // gives access to the children and their LoadExceptions
        ListLoadExceptionSupplierAdapter lles = workflow.getConnectionsExceptionSupplier(); // returns ListLoadExceptionSupplierAdapter
        lles.getList(); // get the data
        Map<Integer, LoadExceptionTree<ConnectionDef.Attribute>> children = lles.getExceptionalChildren();

        // give FallibleConnectionDef to inspect exceptions
        workflow.getFaultyConnections();
        // getConnections() but filtered to those elements without LoadExceptions
        workflow.getCleanConnections();

        // and the convenience wrapped object behaves as expected
        FallibleNodeAnnotationDef faultyAnnotation = faultyBaseNode.getFaultyAnnotation().get();
        // contains the same content
        assertThat(faultyAnnotation).isEqualTo(faultyDefaultAnnotation);
        // but has exceptions
        assertThat(faultyAnnotation.hasExceptions()).isTrue();

        // the load exception that caused the usage of the default
        assertThat(faultyAnnotation.getLoadException()).containsInstanceOf(LoadException.class);
        assertThat(faultyAnnotation.getLoadException().get().getCause()).isInstanceOf(IOException.class);
        // and the previously present exceptions
        assertThat(faultyAnnotation.getSuppliers()).containsOnlyKeys(NodeAnnotationDef.Attribute.DATA);
        assertThat(faultyAnnotation.getDataException().get().getCause()).isInstanceOf(RuntimeException.class);
    }

    /**
     * Test that the convenience getters of Fallible*Def return the same thing as via access through
     * {@link LoadExceptionTree#getSuppliers()} for a null default for a non-def type.
     */
    @Test
    public void testSinglePrimitiveNullDefault() {
        // single non-def
        FallibleBaseNodeDef faultyBaseNode = new BaseNodeDefBuilder().setId(15).setCustomDescription(() -> {
            throw new IllegalStateException();
        }, null).build();

        // default is used
        assertThat(faultyBaseNode.getCustomDescription()).isNull();
        // but a load exception is stored
        assertThat(faultyBaseNode.getCustomDescriptionException()).containsInstanceOf(LoadException.class);

        // check propagation to containing def
        FallibleMetaNodeDef metanode = new MetaNodeDefBuilder().setBaseNode(faultyBaseNode).build();
        // via map
        assertThat(metanode.getSuppliers().get(MetaNodeDef.Attribute.BASE_NODE).size()).isOne();
        // same as LoadExceptionSupplier convenience
        assertThat(metanode.hasExceptions(MetaNodeDef.Attribute.BASE_NODE)).isTrue();
        // same as convenience getter
        assertThat(metanode.getFaultyBaseNode()).isPresent();

        // check access
        // via map
        var faultyList = metanode.getSuppliers().get(MetaNodeDef.Attribute.BASE_NODE);
        // yields the same as convenience getter
        assertThat(faultyList).containsOnly(metanode.getFaultyBaseNode().get());
    }

    /**
     * Test that the convenience getters of Fallible*Def return the same thing as via access through
     * {@link LoadExceptionTree#getSuppliers()} for a non-null default for a non-def type.
     */
    @Test
    public void testSinglePrimitiveActualDefault() {
        // single non-def
        FallibleBaseNodeDef faultyBaseNode = new BaseNodeDefBuilder().setId(() -> {
            throw new Exception();
        }, 13).build();

        // default is used
        assertThat(faultyBaseNode.getId()).isEqualTo(13);
        // but a load exception is stored
        assertThat(faultyBaseNode.getIdException()).containsInstanceOf(LoadException.class);

        // check propagation to containing def
        FallibleMetaNodeDef metanode = new MetaNodeDefBuilder().setBaseNode(faultyBaseNode).build();
        // via map
        assertThat(metanode.getSuppliers().get(MetaNodeDef.Attribute.BASE_NODE).size()).isOne();
        // same as LoadExceptionSupplier convenience
        assertThat(metanode.hasExceptions(MetaNodeDef.Attribute.BASE_NODE)).isTrue();
        // same as convenience getter
        assertThat(metanode.getFaultyBaseNode()).isPresent();

        // check access
        // via map
        var faultyList = metanode.getSuppliers().get(MetaNodeDef.Attribute.BASE_NODE);
        // yields the same as convenience getter
        assertThat(faultyList).containsOnly(metanode.getFaultyBaseNode().get());
    }

}
