package org.knime.base.node.preproc.pmml.missingval.apply;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "CompiledModelReader" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class MissingValueApplyNodeFactory
        extends NodeFactory<MissingValueApplyNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MissingValueApplyNodeModel createNodeModel() {
        return new MissingValueApplyNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MissingValueApplyNodeModel> createNodeView(final int viewIndex,
            final MissingValueApplyNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }

}

