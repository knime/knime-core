package org.knime.base.node.preproc.pmml.missingval.compute;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.base.node.preproc.pmml.missingval.utils.MissingValueNodeDescriptionHelper;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.xml.sax.SAXException;

/**
 * <code>NodeFactory</code> for the "CompiledModelReader" Node.
 *
 * @author Alexander Fillbrunn
 * @noreference This class is not intended to be referenced by clients.
 */
public class MissingValueHandlerNodeFactory
        extends NodeFactory<MissingValueHandlerNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MissingValueHandlerNodeModel createNodeModel() {
        return new MissingValueHandlerNodeModel();
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
    public NodeView<MissingValueHandlerNodeModel> createNodeView(final int viewIndex,
            final MissingValueHandlerNodeModel nodeModel) {
        return null;
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
        return new MissingValueHandlerNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        NodeDescription createNodeDescription = super.createNodeDescription();
        return MissingValueNodeDescriptionHelper.createNodeDescription(createNodeDescription);
    }
}

