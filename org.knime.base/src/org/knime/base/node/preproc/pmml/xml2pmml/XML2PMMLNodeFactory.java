package org.knime.base.node.preproc.pmml.xml2pmml;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "XML2PMML" Node.
 *
 *
 * @author Alexander Fillbrunn
 */
public class XML2PMMLNodeFactory
        extends NodeFactory<XML2PMMLNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public XML2PMMLNodeModel createNodeModel() {
        return new XML2PMMLNodeModel();
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
    public NodeView<XML2PMMLNodeModel> createNodeView(final int viewIndex,
            final XML2PMMLNodeModel nodeModel) {
        return  null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new XML2PMMLNodeDialog();
    }

}

