/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   31.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.figures;

import java.util.List;

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
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.workbench.editor2.ImageRepository;

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
 * <li><code>ProgressFigure</code> - displaying the execution progress</li>
 * </ul>
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerFigure extends RectangleFigure {
    /** absolute width of this figure. * */
    public static final int WIDTH = 80;

    /** absolute height of this figure. * */
    public static final int HEIGHT = 48;

    /** Red traffic light. * */
    public static final Image RED =
            ImageRepository.getImage("icons/ampel_red.png");

    /** Yellow traffic light. * */
    public static final Image YELLOW =
            ImageRepository.getImage("icons/ampel_yellow.png");

    /** Green traffic light. * */
    public static final Image GREEN =
            ImageRepository.getImage("icons/ampel_green.png");

    /** Info sign. * */
    public static final Image INFO_SIGN =
            ImageRepository.getImage("icons/roundInfo.jpg");

    /** Warning sign. * */
    public static final Image WARNING_SIGN =
            ImageRepository.getImage("icons/warning.gif");

    /** Error sign. * */
    public static final Image ERROR_SIGN =
            ImageRepository.getImage("icons/error.jpg");

    /** Delete sign. * */
    public static final Image DELETE_SIGN =
            ImageRepository.getImage("icons/delete.png");

    /** State: Node not configured. * */
    public static final int STATE_NOT_CONFIGURED = 0;

    /** State: Node ready, but idle. * */
    public static final int STATE_READY = 1;

    /**
     * State: The node is in the workflow manager queue wating for execution.
     */
    public static final int STATE_WAITING_FOR_EXEC = 2;

    /** State: Node is currently being executed. * */
    public static final int STATE_EXECUTING = 3;

    /** State: Node was executed sucessfully. * */
    public static final int STATE_EXECUTED = 4;

    /** State: Warning. * */
    public static final int STATE_WARNING = 5;

    /** State: Error. * */
    public static final int STATE_ERROR = 6;

    /** State: Node queued for execution (waiting). * */
    public static final int STATE_QUEUED = 7;

    private static final Font FONT_NORMAL;

    private static final Font FONT_SMALL;

    private static final Font FONT_USER_NAME;

//     private static final Font FONT_EXECUTING;

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

        // FONT_NORMAL = new Font(current, name, height, SWT.NORMAL);
        FONT_USER_NAME = new Font(current, name, height, SWT.NORMAL);
//        FONT_EXECUTING = new Font(current, name, height, SWT.ITALIC);
        FONT_EXECUTED = new Font(current, name, height, SWT.BOLD);
        FONT_NORMAL = FONT_EXECUTED;

        FONT_SMALL = new Font(current, name, 2, SWT.NORMAL);
    }

    /** tooltip for displaying the full heading. * */
    private final NewToolTipFigure m_headingTooltip;

    /** content pane, contains the port visuals and the icon. * */
    private final ContentFigure m_contentFigure;

    /** contains the the "traffic light". * */
    private final StatusFigure m_statusFigure;

    /** contains the the "progress bar". * */
    private ProgressFigure m_progressFigure;

    /** The background color to apply. */
    private final Color m_backgroundColor;

    /** contains the the warning/error sign. * */
    private final InfoWarnErrorPanel m_infoWarnErrorPanel;

    /**
     * The node name. E.g File Reader
     */
    private final Label m_heading;

    /**
     * The user specified node name. E.g. Molecule Data 4
     */
    private final Label m_name;

    /**
     * Tooltip for displaying the user description. This tooltip is displayed
     * with the user name
     */
    private final NewToolTipFigure m_nameTooltip;

    /**
     * An optional user description.
     */
    private String m_description;
    
    private Image m_jobExec;

    /**
     * Creates a new node figure.
     *
     * @param progressFigure the progress figure for this node
     */
    public NodeContainerFigure(final ProgressFigure progressFigure) {

        m_backgroundColor = ColorConstants.white;

        m_description = null;

        setOpaque(false);
        setFill(false);
        setOutline(false);

        // no border
        // setBorder(SimpleEtchedBorder.singleton);

        // add sub-figures
        ToolbarLayout layout = new ToolbarLayout(false);
        layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
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

        // progress bar
        if (progressFigure != null) {
            m_progressFigure = progressFigure;
        } else {
            m_progressFigure = new ProgressFigure();
        }
        m_progressFigure.setCurrentDisplay(Display.getCurrent());
        m_progressFigure.setOpaque(true);

        // Additional status (warning/error sign)
        m_infoWarnErrorPanel = new InfoWarnErrorPanel();

        add(m_heading);
        add(m_contentFigure);
        add(m_infoWarnErrorPanel);
        add(m_statusFigure);
    }

    /**
     * Returns the progress figure of this node figure. The figure is also a
     * progress listener and can be registered with node progress monitors. It
     * renders the progress then.
     *
     * @return the progress figure associated with this node container figure.
     */
    public ProgressFigure getProgressFigure() {
        return m_progressFigure;
    }

    public StatusFigure getStatusFigure() {
        return m_statusFigure;
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
    
    public void setJobExecutorIcon(final Image jobExecIcon) {
        m_jobExec = jobExecIcon;
        m_contentFigure.refreshJobManagerIcon();
    }

    /**
     * Sets the text of the heading label.
     *
     * @param text The text to set.
     */
    public void setLabelText(final String text) {
        m_heading.setText(wrapText(text));
        m_headingTooltip.setText(text);
    }

    public String wrapText(final String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        // wrap the text with line breaks if too long
        // we split just one time (i.e. two lines at most)
        if (text.trim().length() < 20) {
            return text.trim();
        }
        if (text.trim().indexOf(" ") < 0) {
            return text.trim();
        }
        int middle = text.length() / 2;
        // now go left and right to the next space
        // the closest space is used for a split
        int indexLeft = middle;
        int indexRight = middle + 1;
        for (; indexLeft >= 0 && indexRight < text.length(); indexLeft--, indexRight++) {
            if (text.charAt(indexLeft) == ' ') {
                StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexLeft, indexLeft + 1, "\n").toString();
            }
            if (text.charAt(indexRight) == ' ') {
                StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexRight, indexRight + 1, "\n").toString();
            }
        }

        return text;
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

            add(m_name, getChildren().size());
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

    private boolean isChild(final Figure figure) {

        for (Object contentFigure : getChildren()) {
            if (contentFigure == figure) {
                return true;
            }
        }

        return false;
    }

    /**
     * Replaces the status ampel with the progress bar. This is done once the
     * node is queued or executing.
     *
     * @param executing if true the progress bar displays a moving progress if
     *            fals an empty progress bar is set indicating the waiting
     *            situation.
     */
    private void setProgressBar(final boolean executing) {

        // remove both intergangable onse
        if (isChild(m_statusFigure)) {
            remove(m_statusFigure);
        }

        boolean alreadySet = false;
        // and set the progress bar
        if (!isChild(m_progressFigure)) {

            // reset the progress first
            m_progressFigure.reset();
            add(m_progressFigure, 3);
        } else {
            // if already set, remember this
            alreadySet = true;
        }

        if (executing) {

            // temporarily remember the execution state of the progress bar
            boolean barExecuting = m_progressFigure.isExecuting();
            m_progressFigure.setExecuting(true);
            m_progressFigure.setStateMessage("Executing");

            // if the progress bar was not set already
            // init it with an unknown progress first
            if (!alreadySet || !barExecuting) {
                m_progressFigure.activateUnknownProgress();
            }
        } else {
            m_progressFigure.setExecuting(false);
            m_progressFigure.setStateMessage("Queued");
        }
    }

    private void setStatusAmple() {

        // in every case reset the progress bar
        m_progressFigure.reset();

        // remove both intergangable onse
        if (isChild(m_progressFigure)) {
            remove(m_progressFigure);
            m_progressFigure.stopUnknownProgress();
            m_progressFigure.setExecuting(false);
        }

        // and set the progress bar
        if (!isChild(m_statusFigure)) {
            add(m_statusFigure, 3);
        }
    }

    /**
     * 
     * @param state new state of underlying node
     */
    public void setState(final NodeContainer.State state) {
        switch (state) {
        case IDLE:
            setStatusAmple();
            m_statusFigure.setIcon(RED);
            break;
        case CONFIGURED:
            setStatusAmple();
            m_statusFigure.setIcon(YELLOW);
            break;
        case EXECUTED:
            setStatusAmple();
            m_statusFigure.setIcon(GREEN);
            break;
        case EXECUTING:
        case EXECUTINGREMOTELY:
            setProgressBar(true);
            break;
        case MARKEDFOREXEC:
        case UNCONFIGURED_MARKEDFOREXEC:
        case QUEUED:
            setProgressBar(false);
            break;
        }
        m_statusFigure.repaint();
    }
    
    /**
     * 
     * @param msg the node message
     */
    public void setMessage(final NodeMessage msg) {
        if (msg == null || msg.getMessageType() == null) {
            removeMessages();
            NodeLogger.getLogger(NodeContainerFigure.class).warn(
                    "Received NULL message!");
        } else {
            switch (msg.getMessageType()) {
            case RESET:
                removeMessages();
                break;
            case WARNING:
                m_infoWarnErrorPanel.setWarning(msg.getMessage());
                break;
            case ERROR:
                m_infoWarnErrorPanel.setError(msg.getMessage());
                break;
            }
        }
        m_statusFigure.repaint();
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {
        // TODO: rewrite. We have to take into account:
        /*
         * WIDTH: max of 
         * m_contentFigure
         * m_heading
         * m_status figure
         * m_name
         * 
         * HEIGHT:
         * m_contentFigure
         * m_infoWarnErrorPanel constant height?
         * m_statusFigure
         * m_name
         * 
         */
        
        int prefWidth = Math.max(WIDTH, m_heading.getTextBounds().width);
        // add some offset, that the selection border is not directly at 
        // the label
        prefWidth += 10;
        
        int prefHeight = m_heading.getPreferredSize().height
                        + m_contentFigure.getPreferredSize().height
                        //+ m_infoWarnErrorPanel.getPreferredSize().height
                        // replace with a fixed size of 16? pixel
                        + m_progressFigure.getPreferredSize().height
                        + m_statusFigure.getPreferredSize().height
                        + m_name.getPreferredSize().height
                        // plus a fixed size for the info error warn panel
                        + 20;
                        
        return new Dimension(prefWidth, prefHeight);
        
        /*
        Rectangle parentBounds = getBounds();
        int prefWidth = Math.max(WIDTH, m_heading.getPreferredSize().width);
        if (parentBounds.width > 0) {
            prefWidth = parentBounds.width;
        }

        int widthOfHeading = m_heading.getPreferredSize().width;

        prefWidth = Math.max(WIDTH, widthOfHeading);

        int prefHeight = 110;
        /*
                m_heading.getPreferredSize().height
                        + m_contentFigure.getPreferredSize().height
                        + m_infoWarnErrorPanel.getPreferredSize().height
                        + m_statusFigure.getPreferredSize().height
                        + m_name.getPreferredSize().height + 5;
                        *
        return new Dimension(prefWidth, prefHeight);
        */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackgroundColor() {
        return m_backgroundColor;
    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.paintFigure(graphics);
    }

    /**
     * Subfigure, hosts the in/out port figures and the icon.
     *
     * @author Florian Georg, University of Konstanz
     */
    protected class ContentFigure extends Figure {
        private final Label m_iconFigure;

        private final Label m_deleteIcon;

        private static final String BACKGROUND_OTHER =
                "icons/node/" + "background_other.png";

        private static final String BACKGROUND_SOURCE =
                "icons/node/" + "background_source.png";

        private static final String BACKGROUND_SINK =
                "icons/node/" + "background_sink.png";

        private static final String BACKGROUND_LEARNER =
                "icons/node/" + "background_learner.png";

        private static final String BACKGROUND_PREDICTOR =
                "icons/node/" + "background_predictor.png";

        private static final String BACKGROUND_MANIPULATOR =
                "icons/node/" + "background_manipulator.png";

        private static final String BACKGROUND_META =
                "icons/node/" + "background_meta.png";

        private static final String BACKGROUND_VIEWER =
                "icons/node/" + "background_viewer.png";

        private static final String BACKGROUND_UNKNOWN =
                "icons/node/" + "background_unknown.png";
        
        private static final String BACKGROUND_LOOPER_START = 
                "icons/node/background_looper_start.png";
        
        private static final String BACKGROUND_LOOPER_END = 
            "icons/node/background_looper_end.png";

        private final Label m_backgroundIcon;

        /**
         * The base icon without overlays.
         *
         * Is used once the overlay has to be undone
         */
        private Image m_baseIcon;
        
        private Label m_jobExecutorLabel;

        /**
         * Creates a new content figure.
         *
         */
        public ContentFigure() {
            // delegating layout, children provide a Locator as constraint
            DelegatingLayout layout = new DelegatingLayout();
            setLayoutManager(layout);
            setOpaque(false);
            setFill(false);

            // setLayoutManager(new BorderLayout());

            // the "frame", that indicates the node type
            m_backgroundIcon = new Label();
            // m_backgroundIcon.setIconAlignment(PositionConstants.CENTER);

            // create a label that shows the nodes' icon
            m_iconFigure = new Label();
            // m_iconFigure.setBorder(new RaisedBorder(2, 2, 2, 2));
            m_iconFigure.setOpaque(false);

            // create the delete icon
            m_deleteIcon = new Label();
            m_deleteIcon.setOpaque(false);
            m_deleteIcon.setIcon(DELETE_SIGN);

            // center the icon figure
            add(m_backgroundIcon);
            m_backgroundIcon.setLayoutManager(new DelegatingLayout());
            m_backgroundIcon.add(m_iconFigure);
            m_backgroundIcon.setConstraint(m_iconFigure, 
                    new RelativeLocator(m_backgroundIcon, 0.5, 0.5));

            setConstraint(m_backgroundIcon, 
                    new RelativeLocator(this, 0.5, 0.5));
        }

        
        protected void refreshJobManagerIcon() {
            // do we have to remove it?
            if (m_jobExecutorLabel != null && m_jobExec == null) {
                m_backgroundIcon.remove(m_jobExecutorLabel);
                m_jobExecutorLabel = null;
                return;
            }
            // job executor icon
            m_jobExecutorLabel = new Label();
            m_jobExecutorLabel.setOpaque(false);
            m_jobExecutorLabel.setIcon(m_jobExec);
            m_backgroundIcon.add(m_jobExecutorLabel);
            m_backgroundIcon.setConstraint(m_jobExecutorLabel,
                    new RelativeLocator(m_backgroundIcon, 0.85, 0.9));
            repaint();
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
            } else if (type.equals(NodeType.LoopStart)) {
                str = BACKGROUND_LOOPER_START;
            } else if (type.equals(NodeType.LoopEnd)) {
                str = BACKGROUND_LOOPER_END;
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
         * @see org.knime.workbench.repository.model.NodeTemplate
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

            OverlayImage overlayImage =
                    new OverlayImage(icon, WARNING_SIGN);

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
        
        void setBackgroundIcon(Image icon) {
            m_backgroundIcon.setIcon(icon);
        }

        /**
         * {@inheritDoc}
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
        private final InfoWarnErrorFigure m_infoFigure;

        /**
         * The warning figure.
         */
        private final InfoWarnErrorFigure m_warningFigure;

        /**
         * The error figure.
         */
        private final InfoWarnErrorFigure m_errorFigure;

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

        private final Label m_label;

        /**
         * Creates a new bottom figure.
         *
         */
        public StatusFigure() {
            // status figure must have exact same dimensions as progress bar
            setBounds(new Rectangle(0,0, ProgressFigure.WIDTH, 
                    ProgressFigure.HEIGHT));
            ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(ToolbarLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_label = new Label();

            // the font is just set due to a bug in the getPreferedSize
            // method of a lable which accesses the font somewhere
            // if not set a nullpointer is thrown.
            // PO: Set a small font. The status image (as icon of the label) is
            // placed at the bottom of the label, which is too low, if the
            // font is bigger than the slot for the image.
            m_label.setFont(FONT_SMALL);

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
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return new Dimension(getBounds().width,
                    getBounds().height);
        }

    }

    /**
     * Subfigure, contains the warning error signs.
     *
     * @author Christoph Sieb, University of Konstanz
     */
    private class InfoWarnErrorFigure extends Figure {

        private final Label m_label;

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
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return super.getPreferredSize(WIDTH,
                    m_label.getPreferredSize().height);
        }

    }

    private class OverlayImage extends CompositeImageDescriptor {

        private final Image m_baseImage;

        private final Image m_overlayImage;

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

    /**
     * Marks this node parts figure. Used to hilite it from the rest of the
     * parts.
     *
     * @see NodeContainerFigure#unmark()
     */
    public void mark() {
        m_contentFigure.m_backgroundIcon.add(m_contentFigure.m_deleteIcon);
        m_contentFigure.m_backgroundIcon.setConstraint(
                m_contentFigure.m_deleteIcon, 
                new RelativeLocator(m_contentFigure.m_backgroundIcon,
                        0.5, 0.5));
    }

    /**
     * Resets the marked figure.
     *
     * @see NodeContainerFigure#mark()
     */
    public void unmark() {

        m_contentFigure.m_backgroundIcon.remove(m_contentFigure.m_deleteIcon);
    }

}
