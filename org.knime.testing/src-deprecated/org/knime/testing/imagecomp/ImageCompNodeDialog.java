package org.knime.testing.imagecomp;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;


/**
 * Dialog for the image comparator node.
 * @author Iris Adä, University of Konstanz
 *
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} instead
 */
@Deprecated
public class ImageCompNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Default constructor.
     */
    public ImageCompNodeDialog() {
        addDialogComponent(new DialogComponentNumber(getAllowanceModel(), "% missmatched pixel allowed", 1));
    }

    /**
     *  @return the SettingsModel for the percentage of bits allowed to be not matching.
     */
    public static SettingsModelDoubleBounded getAllowanceModel() {
        return new SettingsModelDoubleBounded("CFG_ALLOWANCE", 0.0, 0.0, 100.0);
    }
}
