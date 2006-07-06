/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   31.05.2005 (Florian Georg): created
 */
package de.unikn.knime.workbench.editor2.figures;

import java.util.List;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.RelativeLocator;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

import de.unikn.knime.core.node.NodeFactory.NodeType;
import de.unikn.knime.workbench.editor2.ImageRepository;

/**
 * Figure displaying a <code>NodeContainer</code> in a workflow. This serves
 * as a container for <code>NodeInPortFigure</code>s and
 * <code>NodeOutPortFigure</code>s
 * 
 * This figure is composed of the following sub-figures:
 * <ul>
 * <li><code>ContentFigure</code> - hosts the child visuals (port figures)
 * and the center icon</li>
 * <li><code>StatusFigure</code> - contains description text and some color
 * codes</li>
 * </ul>
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeContainerFigure extends RectangleFigure {

    /** absolute width of this figure. * */
    public static final int WIDTH = 80;

    /** absolute height of this figure. * */
    public static final int HEIGHT = 48;

    /** Red traffic light. * */
    public static final Image RED = ImageRepository
            .getImage("icons/ampel_red.png");

    /** Yellow traffic light. * */
    public static final Image YELLOW = ImageRepository
            .getImage("icons/ampel_yellow.png");

    /** Green traffic light. * */
    public static final Image GREEN = ImageRepository
            .getImage("icons/ampel_green.png");

    /** Info sign. * */
    public static final Image INFO_SIGN = ImageRepository
            .getImage("icons/roundInfo.jpg");

    /** Warning sign. * */
    public static final Image WARNING_SIGN = ImageRepository
            .getImage("icons/warning.gif");

    /** Warning sign. * */
    public static final Image WARNING_SIGN_TINY = ImageRepository
            .getImage("icons/warningTiny.gif");

    /** Error sign. * */
    public static final Image ERROR_SIGN = ImageRepository
            .getImage("icons/error.jpg");

    /** State: Node not configured. * */
    public static final int STATE_NOT_CONFIGURED = 0;

    /** State: Node ready, but idle. * */
    public static final int STATE_READY = 1;

    /** State: Node has been scheduled for execution * */
    // public static final int STATE_QUEUED = 2;
    /** State: Node is currently being executed. * */
    public static final int STATE_EXECUTING = 3;

    /** State: Node was executed sucessfully. * */
    public static final int STATE_EXECUTED = 4;

    /** State: Warning. * */
    public static final int STATE_WARNING = 5;

    /** State: Error. * */
    public static final int STATE_ERROR = 6;

    private static final Font FONT_NORMAL;

    private static final Font FONT_USER_NAME;

    private static final Font FONT_EXECUTING;

    private static final Font FONT_EXECUTED;

    static {
        // I (Bernd) had problem using the hardcoded font "Arial" -
        // this static block derives the fonts from the system font.
        Display current = Display.getCurrent();
        Font systemFont = current.getSystemFont();
        FontData[] systemFontData = systemFont.getFontData();
        String name = "Arial"; // fallback
        int height = 9;
        if (systemFontData.length >= 1) {
            name = systemFontData[0].getName();
            height = systemFontData[0].getHeight();
        }
        FONT_NORMAL = new Font(current, name, height, SWT.NORMAL);
        FONT_USER_NAME = new Font(current, name, height, SWT.ITALIC);
        FONT_EXECUTING = new Font(current, name, height, SWT.ITALIC);
        FONT_EXECUTED = new Font(current, name, height, SWT.BOLD);
    }

    /** tooltip for displaying the full heading. * */
    private NewToolTipFigure m_headingTooltip;

    /** content pane, contains the port visuals and the icon. * */
    private ContentFigure m_contentFigure;

    /** contains the the "traffic light". * */
    private StatusFigure m_statusFigure;

    /** contains the the warning/error sign. * */
    private InfoWarnErrorPanel m_infoWarnErrorPanel;

    /**
     * The node name. E.g File Reader
     */
    private Label m_heading;

    /**
     * The user specified node name. E.g. Molecule Data 4
     */
    private Label m_name;

    /**
     * Tooltip for displaying the user description. This tooltip is displayed
     * with the user name
     */
    private NewToolTipFigure m_nameTooltip;

    /**
     * An optional user description.
     */
    private String m_description;

    /**
     * Creates a new node figure.
     */
    public NodeContainerFigure() {

        m_description = null;

        setOpaque(false);
        setFill(false);
        setOutline(false);

        // no border
        // setBorder(SimpleEtchedBorder.singleton);

        // add sub-figures
        ToolbarLayout layout = new ToolbarLayout(false);
        layout.setSpacing(1);
        layout.setStretchMinorAxis(true);
        layout.setVertical(true);
        setLayoutManager(layout);

        // Heading (Label)
        m_heading = new Label();
        m_headingTooltip = new NewToolTipFigure("");
        m_heading.setToolTip(m_headingTooltip);
        m_heading.setFont(FONT_NORMAL);

        // Name (Label)
        m_name = new Label();
        m_nameTooltip = new NewToolTipFigure("");
        m_name.setFont(FONT_USER_NAME);

        // register the figure as listener for click events
        // in this case a text field enable the user to change the specific
        // node name
        // m_name.addMouseListener(this);
        //        
        // m_nameTextField = new Text();

        // Content (Ports and icon)
        m_contentFigure = new ContentFigure();

        // Status: traffic light
        m_statusFigure = new StatusFigure();

        // Additional status (warning/error sign)
        m_infoWarnErrorPanel = new InfoWarnErrorPanel();

        add(m_heading);
        add(m_contentFigure);
        add(m_infoWarnErrorPanel);
        add(m_statusFigure);
    }

    /**
     * Sets the icon.
     * 
     * @param icon the icon
     */
    public void setIcon(final Image icon) {
        m_contentFigure.setIcon(icon);
    }

    /**
     * Sets the type.
     * 
     * @param type the type
     */
    public void setType(final NodeType type) {
        m_contentFigure.setType(type);

    }

    /**
     * Sets the text of the heading label.
     * 
     * @param text The text to set.
     */
    public void setLabelText(final String text) {
        m_heading.setText(text);
        m_headingTooltip.setText(text);
    }

    /**
     * Sets the user name of the figure.
     * 
     * @param name The name to set.
     */
    public void setCustomName(final String name) {

        if (name == null || name.trim().equals("")) {

            try {
                m_name.setText("");
                remove(m_name);
            } catch (IllegalArgumentException iae) {
                // do nothing
            }
            return;
        }

        // if the name is not already set
        // set it
        m_name.setText(name);
        if (!(m_name.getParent() == this)) {
            add(m_name);
        }

        // if the tooltip (description) contains
        // content, set it

        String toolTipText = m_name.getText();
        if (m_description != null && !m_description.trim().equals("")) {
            toolTipText = toolTipText + ":\n\n Description:\n" + m_description;
        }

        m_nameTooltip.setText(toolTipText);
        m_name.setToolTip(m_nameTooltip);

    }

    /**
     * Sets the description for this node as the name's tooltip.
     * 
     * @param description the description to set as tooltip
     */
    public void setCustomDescription(final String description) {

        if (description == null || description.trim().equals("")) {

            // if there is no description, reset the description
            // and invoke the set name method once more to
            // adjust the tooltip text
            m_description = null;
            setCustomName(m_name.getText());
            return;
        }

        m_description = description;

        String toolTipText = m_name.getText();

        toolTipText = toolTipText + ":\n\n Description:\n" + m_description;

        m_nameTooltip.setText(toolTipText);
        m_name.setToolTip(m_nameTooltip);

    }

    /**
     * Sets the state of the node, which shall be reflected in the UI.
     * 
     * @param state The state to set
     * @param message The message for the state
     */
    public void setState(final int state, final String message) {

        switch (state) {
        case STATE_NOT_CONFIGURED:
            m_heading.setFont(FONT_NORMAL);
            m_heading.setEnabled(false);
            m_statusFigure.setIcon(RED);
            break;
        case STATE_READY:
            m_heading.setFont(FONT_NORMAL);
            m_heading.setEnabled(true);
            m_statusFigure.setIcon(YELLOW);

            // m_contentFigure.removeWarningSign();
            break;
        // case STATE_QUEUED:
        // m_statusFigure.setIcon(YELLOW);
        // m_statusFigure.setEnabled(false);
        // break;
        case STATE_EXECUTING:
            m_heading.setFont(FONT_EXECUTING);
            m_heading.setEnabled(true);
            m_statusFigure.setIcon(YELLOW);
            m_heading.setFont(FONT_EXECUTING);
            // m_contentFigure.removeWarningSign();
            break;
        case STATE_EXECUTED:
            m_heading.setFont(FONT_EXECUTED);
            m_heading.setEnabled(true);
            m_statusFigure.setIcon(GREEN);
            break;
        case STATE_WARNING:
            m_heading.setFont(FONT_NORMAL);
            m_heading.setEnabled(true);
            m_infoWarnErrorPanel.setWarning(message);
            // m_contentFigure.setWarning(message, WarnErrorToolTip.WARNING);
            break;
        case STATE_ERROR:
            m_heading.setFont(FONT_NORMAL);
            m_heading.setEnabled(true);
            m_infoWarnErrorPanel.setError(message);
            // m_contentFigure.setWarning(message, WarnErrorToolTip.ERROR);
            break;
        default:
            m_heading.setFont(FONT_NORMAL);
            m_heading.setEnabled(false);
            m_statusFigure.setIcon(RED);
            break;
        }

        m_heading.repaint();
        m_statusFigure.repaint();

    }

    /**
     * 
     * @see org.eclipse.draw2d.IFigure#getMinimumSize(int, int)
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        return new Dimension(Math
                .max(WIDTH, m_heading.getPreferredSize().width), super
                .getPreferredSize(-1, -1).height);
    }

    /**
     * @see org.eclipse.draw2d.IFigure#getBackgroundColor()
     */
    @Override
    public Color getBackgroundColor() {
        return ColorConstants.white;
    }

    /**
     * @see org.eclipse.draw2d.IFigure#getForegroundColor()
     */
    @Override
    public Color getForegroundColor() {
        return ColorConstants.white;
    }

    /**
     * @return The content figure (= content pane for port visuals)
     */
    public IFigure getContentFigure() {
        return m_contentFigure;
    }

    /**
     * Removes all currently set messages.
     */
    public void removeMessages() {
        m_infoWarnErrorPanel.removeAll();
    }

    /**
     * We need to set the color before invoking super.
     * 
     * @see org.eclipse.draw2d.Shape#fillShape(org.eclipse.draw2d.Graphics)
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.fillShape(graphics);
    }

    /**
     * Subfigure, hosts the in/out port figures and the icon.
     * 
     * @author Florian Georg, University of Konstanz
     */
    private class ContentFigure extends Figure {

        private Label m_iconFigure;

        private static final String BACKGROUND_OTHER = "icons/node/"
                + "background_other.png";

        private static final String BACKGROUND_SOURCE = "icons/node/"
                + "background_source.png";
        
        private static final String BACKGROUND_SINK = "icons/node/"
            + "background_sink.png";

        private static final String BACKGROUND_LEARNER = "icons/node/"
                + "background_learner.png";
        
        private static final String BACKGROUND_PREDICTOR = "icons/node/"
            + "background_precictor.png";

        private static final String BACKGROUND_TRANSFORMER = "icons/node/"
                + "background_transformer.png";
        
        private static final String BACKGROUND_MANIPULATOR = "icons/node/"
            + "background_manipulator.png";
        
        private static final String BACKGROUND_META = "icons/node/"
            + "background_meta.png";

        private static final String BACKGROUND_VIEWER = "icons/node/"
                + "background_viewer.png";
        
        private static final String BACKGROUND_UNKNOWN = "icons/node/"
            + "background_unknown.png";

        private Label m_backgroundIcon;

        /**
         * The base icon without overlays.
         * 
         * Is used once the overlay has to be undone
         */
        private Image m_baseIcon;

        /**
         * Creates a new content figure.
         * 
         */
        public ContentFigure() {
            // delegating layout, children provide a Locator as constraint
            DelegatingLayout layout = new DelegatingLayout();
            setLayoutManager(layout);
            setOpaque(false);

            // setLayoutManager(new BorderLayout());

            // the "frame", that indicates the node type
            m_backgroundIcon = new Label();
            // m_backgroundIcon.setIconAlignment(PositionConstants.CENTER);

            // create a label that shows the nodes' icon
            m_iconFigure = new Label();
            // m_iconFigure.setBorder(new RaisedBorder(2, 2, 2, 2));
            m_iconFigure.setOpaque(false);

            // center the icon figure
            add(m_backgroundIcon);
            m_backgroundIcon.setLayoutManager(new BorderLayout());
            m_backgroundIcon.add(m_iconFigure, BorderLayout.CENTER);
            setConstraint(m_backgroundIcon, new RelativeLocator(this, 0.5, 0.5));

        }

        /**
         * This determines the background image according to the "type" of the
         * node as stored in the repository model.
         * 
         * @param type The Type
         * @return Image that should be uses as background for this node
         */
        private Image getBackgroundForType(final NodeType type) {

            String str = null;
            if (type == null) {
                str = BACKGROUND_UNKNOWN;
            } else if (type.equals(NodeType.Source)) {
                str = BACKGROUND_SOURCE;
            } else if (type.equals(NodeType.Sink)) {
                str = BACKGROUND_SINK;
            } else if (type.equals(NodeType.Transformer)) {
                str = BACKGROUND_TRANSFORMER;
            } else if (type.equals(NodeType.Manipulator)) {
                str = BACKGROUND_MANIPULATOR;
            } else if (type.equals(NodeType.Learner)) {
                str = BACKGROUND_LEARNER;
            } else if (type.equals(NodeType.Predictor)) {
                str = BACKGROUND_PREDICTOR;
            } else if (type.equals(NodeType.Meta)) {
                str = BACKGROUND_META;
            } else if (type.equals(NodeType.Other)) {
                str = BACKGROUND_OTHER;
            } else if (type.equals(NodeType.Visualizer)) {
                str = BACKGROUND_VIEWER;
            } else {
                str = BACKGROUND_UNKNOWN;
            }
            Image img = ImageRepository.getImage(str);

            return img == null ? ImageRepository.getImage(BACKGROUND_OTHER)
                    : img;
        }

        /**
         * Sets a type specific background image.
         * 
         * @param type The node type, results in a different background
         * @see de.unikn.knime.workbench.repository.model.NodeTemplate
         */
        void setType(final NodeType type) {

            m_backgroundIcon.setIcon(getBackgroundForType(type));
        }

        /**
         * Overlays the warning sign to indicate a problem or info.
         * 
         * @param message the message to display
         * @param type the concrete promblem type to display (info/warn/error)
         */
        public void setWarning(final String message, final int type) {

            // overlay the image with the warning sign
            Image icon = m_baseIcon;

            OverlayImage overlayImage = new OverlayImage(icon,
                    WARNING_SIGN_TINY);

            // parameters are only dummy values. not needed!
            overlayImage.drawCompositeImage(0, 0);
            m_iconFigure.setIcon(overlayImage.createImage());

            m_iconFigure.setToolTip(new WarnErrorToolTip(message, type));

        }

        /**
         * Removes the warning sign.
         */
        public void removeWarningSign() {
            m_iconFigure.setIcon(m_baseIcon);
            m_iconFigure.setToolTip(null);
        }

        /**
         * Sets the icon for the node (now provided from the factory class).
         * 
         * @param icon Image to display as icon
         */
        void setIcon(final Image icon) {

            if (m_baseIcon == null) {
                m_baseIcon = icon;
            }

            m_iconFigure.setIcon(icon);
            // m_iconFigure.setIconAlignment(PositionConstants.CENTER);
            // m_iconFigure.setIconDimension(new Dimension(16, 16));

            m_iconFigure.revalidate();
        }

        /**
         * 
         * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return new Dimension(-1, HEIGHT);
        }

    }

    /**
     * Subfigure containing the information/warning/error signs. The panel can
     * display any combination of the signs and also provides functionality to
     * set tool tips containing one or more messages for each category
     * (info/warning/error).
     * 
     * @author Christoph Sieb, University of Konstanz
     */
    private class InfoWarnErrorPanel extends Figure {

        /**
         * The info figure.
         */
        private InfoWarnErrorFigure m_infoFigure;

        /**
         * The warning figure.
         */
        private InfoWarnErrorFigure m_warningFigure;

        /**
         * The error figure.
         */
        private InfoWarnErrorFigure m_errorFigure;

        /**
         * Constructor for a new <code>SignPanel</code>.
         */
        public InfoWarnErrorPanel() {

            // a flow layout is used to arrange all signs in
            // a line

            FlowLayout layout = new FlowLayout(true);
            layout.setMajorAlignment(FlowLayout.ALIGN_CENTER);
            layout.setMinorAlignment(FlowLayout.ALIGN_CENTER);
            layout.setMajorSpacing(3);
            setLayoutManager(layout);

            // create the info/warning/error figures
            m_infoFigure = new InfoWarnErrorFigure();
            m_infoFigure.setIcon(INFO_SIGN);

            m_warningFigure = new InfoWarnErrorFigure();
            m_warningFigure.setIcon(WARNING_SIGN);

            m_errorFigure = new InfoWarnErrorFigure();
            m_errorFigure.setIcon(ERROR_SIGN);

            setVisible(true);
            repaint();
        }

        /**
         * Removes a info sign.
         */
        public void removeInfoSign() {
            remove(m_infoFigure);
        }

        /**
         * Sets a new warning message.
         * 
         * @param message the message to set
         */
        public void setWarning(final String message) {

            // as the warning sign should always be before the error sign
            // but after the info sign, it must be checked in the case there
            // is only one sign so far at which position to insert the
            // warning sign
            List<Figure> children = getChildren();

            // check if there is already a warning sign
            boolean alreadyInserted = false;
            for (Figure child : children) {
                if (child == m_warningFigure) {
                    alreadyInserted = true;
                }
            }

            if (!alreadyInserted) {
                // in case of 0 children the sign can simply be inserted
                if (children.size() == 0) {
                    add(m_warningFigure);
                } else if (children.size() == 2) {
                    // in case of 2 children, the warning sign must be at idx 1
                    add(m_warningFigure, 1);
                } else {
                    // else there is exact 1 child
                    Figure figure = children.get(0);
                    // in case of the error sign, the warning sign has to be
                    // inserted before the error sign
                    if (figure == m_errorFigure) {
                        add(m_warningFigure, 0);
                    } else {
                        // else append at the end (after the info sign)
                        add(m_warningFigure);
                    }
                }
            }

            if (message != null && !message.trim().equals("")) {
                m_warningFigure.setToolTip(message, WarnErrorToolTip.WARNING);
            } else {
                m_warningFigure.setToolTip("Warning occured: no details.",
                        WarnErrorToolTip.WARNING);
            }

            repaint();
        }

        /**
         * Removes a warning sign.
         */
        public void removeWarningSign() {
            remove(m_warningFigure);
        }

        /**
         * Sets a new error message.
         * 
         * @param message the message to set
         */
        public void setError(final String message) {

            // the error sign is always the last sign.
            add(m_errorFigure);

            if (message != null && !message.trim().equals("")) {
                m_errorFigure.setToolTip(message, WarnErrorToolTip.ERROR);
            } else {
                m_errorFigure.setToolTip("Error occured: no details.",
                        WarnErrorToolTip.ERROR);
            }

            repaint();
        }

        /**
         * Removes a error sign.
         */
        public void removeErrorSign() {
            remove(m_errorFigure);
        }

    }

    /**
     * Subfigure, contains the "traffic light".
     * 
     * @author Florian Georg, University of Konstanz
     */
    private class StatusFigure extends Figure {

        private Label m_label;

        /**
         * Creates a new bottom figure.
         * 
         */
        public StatusFigure() {

            ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_label = new Label();

            // m_label.setIconAlignment(PositionConstants.CENTER);
            add(m_label);
            setOpaque(false);
            setIcon(RED);
            // setIcon(null);

        }

        /**
         * Sets the icon to display.
         * 
         * @param icon The icon (traffic light) to set
         */
        void setIcon(final Image icon) {
            m_label.setIcon(icon);
            revalidate();
        }

        /**
         * 
         * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return super.getPreferredSize(WIDTH,
                    m_label.getPreferredSize().height);
        }

    }

    /**
     * Subfigure, contains the warning error signs.
     * 
     * @author Christoph Sieb, University of Konstanz
     */
    private class InfoWarnErrorFigure extends Figure {

        private Label m_label;

        /**
         * Creates a new bottom figure.
         */
        public InfoWarnErrorFigure() {

            ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_label = new Label();

            // m_label.setIconAlignment(PositionConstants.CENTER);
            add(m_label);
            setOpaque(false);
        }

        /**
         * Sets the icon to display.
         * 
         * @param icon The icon (traffic light) to set
         */
        public void setIcon(final Image icon) {
            m_label.setIcon(icon);
            revalidate();
        }

        /**
         * Switches to warning mode.
         * 
         * @param message the message to set for the tool tip
         */
        void setAsWarning(final String message) {
            m_label.setIcon(WARNING_SIGN);
            this.setToolTip(message, WarnErrorToolTip.WARNING);
            revalidate();
        }

        /**
         * Switches to error mode.
         * 
         * @param message the message to set for the tool tip
         */
        void setAsError(final String message) {
            m_label.setIcon(ERROR_SIGN);
            this.setToolTip(message, WarnErrorToolTip.ERROR);
            revalidate();
        }

        /**
         * Switche off warning or error.
         */
        void switchOff() {
            m_label.setIcon(null);
            m_label.setToolTip(null);
            revalidate();
        }

        /**
         * Sets tool tip message.
         * 
         * @param message The status message for the tool tip
         */
        private void setToolTip(final String message, final int type) {
            m_label.setToolTip(new WarnErrorToolTip(message, type));
            revalidate();
        }

        /**
         * @see org.eclipse.draw2d.IFigure#getPreferredSize(int, int)
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return super.getPreferredSize(WIDTH,
                    m_label.getPreferredSize().height);
        }

    }

    private class OverlayImage extends CompositeImageDescriptor {

        private Image m_baseImage;

        private Image m_overlayImage;

        /**
         * Creates an overlay image descriptor.
         * 
         * @param baseImage the background base image
         * @param overlayImage the image to overlay
         */
        public OverlayImage(final Image baseImage, final Image overlayImage) {
            m_baseImage = baseImage;
            m_overlayImage = overlayImage;

            getImageData();
        }

        @Override
        protected void drawCompositeImage(final int width, final int height) {
            // To draw a composite image, the base image should be
            // drawn first (first layer) and then the overlay image
            // (second layer)

            // Draw the base image using the base image's image data
            drawImage(m_baseImage.getImageData(), 2, 2);

            // Method to create the overlay image data
            // Get the image data from the Image store or by other means
            ImageData overlayImageData = m_overlayImage.getImageData();

            // Overlaying the icon in the top left corner i.e. x and y
            // coordinates are both zero
            int xValue = 0;
            int yValue = 0;
            drawImage(overlayImageData, xValue, yValue);
        }

        @Override
        protected Point getSize() {
            return new Point(18, 18);
        }

    }

    /**
     * @return the rectangle of the name label.
     */
    public Rectangle getNameLabelRectangle() {
        return m_name.getClientArea().getCopy();
    }

    /**
     * @return the user node name of this figure
     */
    public String getCustomName() {

        return m_name.getText();
    }
}
