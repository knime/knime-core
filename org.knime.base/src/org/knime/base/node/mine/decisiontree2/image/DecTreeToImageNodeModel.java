/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   10.11.2011 (hofer): created
 */
package org.knime.base.node.mine.decisiontree2.image;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.imageio.ImageIO;

import org.knime.base.node.mine.decisiontree2.PMMLDecisionTreeTranslator;
import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.decisiontree2.model.DecisionTreeNode;
import org.knime.base.node.mine.decisiontree2.view.DecTreeGraphView;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.image.ImageContent;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.core.node.port.image.ImagePortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * The NodeModel of the Decision Tree to Image node.
 *
 * @author Heiko Hofer
 */
public class DecTreeToImageNodeModel extends NodeModel {
    private static final String DEC_TREE_FILE_NAME = "DecTree.bin";
    private static final String IMAGE_FILE_NAME = "Image.bin";
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecTreeToImageNodeModel.class);

    private DecisionTree m_decTree;
    private DecTreeToImageNodeSettings m_settings;
    private ImageContent m_imageContent;

    /**
     * Create a new instance.
     */
    public DecTreeToImageNodeModel() {
        super(new PortType[] {PMMLPortObject.TYPE,
                new PortType(BufferedDataTable.class, true)
                },
                new PortType[] {ImagePortObject.TYPE});
        m_settings = new DecTreeToImageNodeSettings();
    }

    /**
     * @return internal tree structure or <code>null</code> if it does not
     *         exist
     */
    protected DecisionTree getDecisionTree() {
        return m_decTree;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final PortObjectSpec[] configure(final PortObjectSpec[] inPOSpecs)
    throws InvalidSettingsException {
        return new PortObjectSpec[] {
                new ImagePortObjectSpec(PNGImageContent.TYPE)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PortObject[] execute(final PortObject[] inPorts,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setMessage("Decision Tree Predictor: Loading predictor...");
        PMMLPortObject port = (PMMLPortObject)inPorts[0];

        List<Node> models = port.getPMMLValue().getModels(
                PMMLModelType.TreeModel);
        if (models.isEmpty()) {
            String msg = "Decision Tree evaluation failed: "
                   + "No tree model found.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        PMMLDecisionTreeTranslator trans = new PMMLDecisionTreeTranslator();
        port.initializeModelTranslator(trans);
        m_decTree = trans.getDecisionTree();

        m_decTree.resetColorInformation();

        String colorColumn = null;
        if (null != inPorts[1]) {
            BufferedDataTable inData = (BufferedDataTable)inPorts[1];
            // get column with color information
            for (DataColumnSpec s : inData.getDataTableSpec()) {
                if (s.getColorHandler() != null) {
                    colorColumn = s.getName();
                    break;
                }
            }
            if (null != m_decTree) {
                m_decTree.setColorColumn(colorColumn);
            }

            for (DataRow thisRow : inData) {
                m_decTree.addCoveredColor(thisRow, inData
                        .getDataTableSpec());
            }

        }
        // create PNG via streamed string
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        int width = m_settings.getWidth();
        int height = m_settings.getHeight();

        GraphicsEnvironment env =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        BufferedImage image = null;
        if (env.isHeadlessInstance()) {
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
        } else {
            // create compatible image for better performance
            GraphicsConfiguration gfxConf = env.getDefaultScreenDevice().
                getDefaultConfiguration();
            //image = gfxConf.createCompatibleImage(width, height);
            // with binary transparency
            image = gfxConf.createCompatibleImage(width, height,
                    Transparency.BITMASK);
            // with transparency
            // image = gfxConf.createCompatibleImage(width, height,
            //         Transparency.TRANSLUCENT);
        }
        Graphics2D g = (Graphics2D)image.getGraphics();
        DecisionTreeNode root = null != getDecisionTree() ?
                getDecisionTree().getRootNode() : null;
        DecTreeGraphView graph = new DecTreeToImageGraphView(root, colorColumn,
                m_settings);
        // draw graph
        graph.getView().paint(g);

        // write png
        ImageIO.write(image, "png", os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        m_imageContent = new PNGImageContent(is);
        ImagePortObjectSpec outSpec = new ImagePortObjectSpec(
                PNGImageContent.TYPE);

        // return image object
        PortObject po = new ImagePortObject(m_imageContent, outSpec);
        return new PortObject[]{po};
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        DecTreeToImageNodeSettings s = new DecTreeToImageNodeSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_decTree = null;
        m_imageContent = null;
    }

    /**
     * Load internals.
     *
     * @param nodeInternDir The intern node directory to load tree from.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #loadInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {

        // read the decision tree
        File internalsFile = new File(nodeInternDir, DEC_TREE_FILE_NAME);
        if (!internalsFile.exists()) {
            // file to load internals from not available
            setWarningMessage("Internal model could not be loaded.");
            return;
        }

        BufferedInputStream in2 =
                new BufferedInputStream(new GZIPInputStream(
                        new FileInputStream(internalsFile)));

        ModelContentRO binModel = ModelContent.loadFromXML(in2);

        try {
            m_decTree = new DecisionTree(binModel);
        } catch (InvalidSettingsException ise) {
            LOGGER.warn("Model (internals) could not be loaded.", ise);
            setWarningMessage("Internal model could not be loaded.");
        }
        exec.setProgress(0.5);

        // read image content
        File f = new File(nodeInternDir, IMAGE_FILE_NAME);
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(f));
        try {
            m_imageContent = new PNGImageContent(in);
        } catch (Exception e) {
            in.close();
            LOGGER.warn("Model (internals) could not be loaded.", e);
            setWarningMessage("Internal model could not be loaded.");
        }
        in.close();
        exec.setProgress(1.0);
    }

    /**
     * Save internals.
     *
     * @param nodeInternDir The intern node directory to save table to.
     * @param exec Used to report progress or cancel saving.
     * @throws IOException Always, since this method has not been implemented
     *             yet.
     * @see org.knime.core.node.NodeModel
     *      #saveInternals(java.io.File,ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        // write the tree as pred params
        ModelContent model = new ModelContent(DEC_TREE_FILE_NAME);
        m_decTree.saveToPredictorParams(model, true);

        File internalsFile = new File(nodeInternDir, DEC_TREE_FILE_NAME);
        BufferedOutputStream out2 =
                new BufferedOutputStream(new GZIPOutputStream(
                        new FileOutputStream(internalsFile)));

        model.saveToXML(out2);
        out2.close();
        exec.setProgress(0.5);

        // write the image
        File f = new File(nodeInternDir, IMAGE_FILE_NAME);
        ObjectOutputStream out
                    = new ObjectOutputStream(new FileOutputStream(f));
        m_imageContent.save(out);
        out.close();
        exec.setProgress(1.0);
    }

    /**
     * Get the image that will be send to the output.
     *
     * @return the image or null if the node has not been executed
     */
    public ImageContent getImage() {
        return m_imageContent;
    }

    /**
     * A short description of the image return by getImage().
     * @return a short description of the image
     */
    protected String getImageDescription() {
        return "Image (PNG, " + m_settings.getWidth() + "x"
               + m_settings.getHeight() + ")";
    }

}
