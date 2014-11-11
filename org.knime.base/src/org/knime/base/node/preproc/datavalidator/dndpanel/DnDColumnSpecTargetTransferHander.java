package org.knime.base.node.preproc.datavalidator.dndpanel;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import org.knime.core.data.DataColumnSpec;

/**
 * TransferHandler to handle of DnD actions containing {@link DataColumnSpec}s (see also
 * {@link DnDColumnSpecTransferable}) as dropping target.
 *
 * @author Marcel Hanser
 */
@SuppressWarnings("serial")
public final class DnDColumnSpecTargetTransferHander extends TransferHandler {

    private final DnDDropListener m_droppingListener;

    /**
     * @param droppingListener listener called on drag and drop events containing {@link DnDColumnSpecTransferable}
     */
    public DnDColumnSpecTargetTransferHander(final DnDDropListener droppingListener) {
        super();
        m_droppingListener = droppingListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canImport(final TransferSupport support) {
        boolean dataFlavorSupported = support.isDataFlavorSupported(DnDColumnSpecTransferable.DATA_COLUMN_SPEC_FLAVOR);

        if (!dataFlavorSupported) {
            return false;
        }

        List<DataColumnSpec> extractColumnSpecs =
            DnDColumnSpecTransferable.extractColumnSpecs(support.getTransferable());
        if (extractColumnSpecs != null) {
            return m_droppingListener.isDropable(extractColumnSpecs);
        }
        return true;
    }

    @Override
    public boolean importData(final TransferHandler.TransferSupport support) {
        if (!support.isDrop()) {
            return false;
        }

        // Check for column spec flavor
        if (!support.isDataFlavorSupported(DnDColumnSpecTransferable.DATA_COLUMN_SPEC_FLAVOR)) {
            return false;
        }

        List<DataColumnSpec> extractColumnSpecs =
            DnDColumnSpecTransferable.extractColumnSpecs(support.getTransferable());

        if (m_droppingListener.isDropable(extractColumnSpecs)) {
            if (m_droppingListener.update(extractColumnSpecs)) {
                //                            DragAndDropConfigurationSubPanel.this.repaint();
                return true;
            }
        }
        return false;
    }

    @Override
    public int getSourceActions(final JComponent c) {
        return COPY;
    }
}