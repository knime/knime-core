/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   20.06.2012 (meinl): created
 */
package org.knime.workbench.core.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 * Central repository for all KNIME-related images. It stores images for nodes,
 * categories, and images used in the GUI. All images are stored in a central
 * {@link ImageRegistry} so that they get disposed automatically when the
 * plug-in is deactivated (which it probably never will). </ p>
 * The methods distinguish between IconImages and Images. IconImages are returned in a 16x16px size. Images as they are.
 * IconImages may be scaled if the file content is of different size. Both images (icon and "normal") are zoomed if
 * system zoom requires (high dpi) - but only if the corresponding SystemProperty is set. Icons can be provided in
 * different sizes in different files with different names. The size must be appended (like foo.png -> foo_24x24.png and
 * foo_32x32.png). Images are also scaled. If they are provided in different sizes their file names must be appended
 * with @1.5x and @2x.
 *
 * <b>This class is experimental API, please do not use it for now.</b>
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public final class ImageRepository {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ImageRepository.class);

    private static final ImageRegistry REGISTRY = KNIMECorePlugin.getDefault()
            .getImageRegistry();

    // appended to the key if the image is scaled to icon size
    private static final String ICONIFIED_KEY = "@:/icon";

    // appended to the key if the image is not scaled to sytem level zoom (highDPI)
    private static final String NOTSCALED_KEY = "@:/noscale";

    // used for the missing icon
    private static final String MISSING_ICON_KEY = "###MISSING_ICON###";

    /**
     * Enumeration for shared images.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @since 2.6
     */
    public enum SharedImages {
        /** the KNIME triangle. */
        KNIME("icons/knime.png"),

        /** edit icon when cursor moves over annotations. */
        AnnotationEditHover("icons/anno_edit.png"),
        /** move icon when cursor moves over annotation top left corner. */
        AnnotationMoveHover("icons/anno_move.png"),
        /** Small icon for export wizards. */
        ExportSmall("icons/knime_export16.png"),
        /** Big icon for export wizards. */
        ExportBig("icons/knime_export55.png"),
        /** Big icon for import wizards. */
        ImportBig("icons/knime_import55.png"),
        /** Big icon for new KNIME flow. */
        NewKnimeBig("icons/new_knime55.png"),
        /** The default node icon. */
        DefaultNodeIcon(NodeFactory.getDefaultIcon()),
        /** The default metanode icon. */
        DefaultMetaNodeIcon("icons/meta_nodes/metanode_template.png"),
        /** Disabled icon for metanodes. */
        MetanodeDisabled("icons/meta_nodes/metanode_template_disabled.png"),
        /** Icon for a metanode in the node repository or navigator. */
        MetanodeRepository("icons/meta_nodes/metanode_template_repository.png"),
        /** The default category icon. */
        DefaultCategoryIcon(NodeFactory.getDefaultIcon()),
        /** Icon with a lock. */
        Lock("icons/lockedstate.gif"),
        /** Icon for canceling node or workflow execution. */
        CancelExecution("icons/actions/cancel.gif"),
        /** Icon for configuring a node. */
        ConfigureNode("icons/actions/configure.gif"),
        /** Icon for executing a node or workflow. */
        Execute("icons/actions/execute.gif"),
        /** Icon for opening a node view. */
        OpenNodeView("icons/actions/openView.gif"),
        /** Icon for reseting a node or workflow. */
        Reset("icons/actions/reset.gif"),
        /** Icon for collapsing all levels in a tree. */
        CollapseAll("icons/collapseall.png"),
        /** Icon for expanding all levels in a tree. */
        ExpandAll("icons/expandall.png"),
        /** Icon for expanding one level in a tree. */
        Expand("icons/expand.png"),
        /** Icon for synching a tree with another selection. */
        Synch("icons/sync.png"),
        /** Icon for refreshing a component. */
        Refresh("icons/refresh.gif"),
        /** Icon for a history view. */
        History("icons/history.png"),
        /** Icon for delete. */
        Delete("icons/delete.png"),
        /** Icon for startup messages view. */
        StartupMessages("icons/startupMessages.png"),
        /** Icon showing a magnifier glass. */
        Search("icons/search.gif"),

        /** Icon for a folder. */
        Folder("icons/folder.png"),
        /** Icon for a file. */
        File("icons/any_file.png"),
        /** Icon for another file. */
        File2("icons/file.png"),
        /** Icon for a workflow node. */
        Node("icons/node.png"),
        /** Icon for knime project (neutral). */
        Workflow("icons/project_basic.png"),
        /** Icon for a metanode template. */
        MetaNodeTemplate("icons/meta_nodes/metanode_template_repository.png"),
        /** Icon for a workflow group. */
        WorkflowGroup("icons/wf_set.png"),
        /** Icon for a system folder. */
        SystemFolder("icons/system_folder.png"),
        /** Icon for a system flow. */
        SystemFlow("icons/system_flow.png"),


        /** Icons for diff view. */
        MetaNodeIcon("icons/metanode_icon.png"),
        /** meta node with ports. */
        MetaNodeDetailed("icons/metanode_detailed.png"),
        /** Icons for diff view. */
        NodeIcon("icons/node_icon.png"),
        /** node with ports and status. */
        NodeIconDetailed("icons/node_detailed.png"),
        /** Icons for diff view. */
        SubNodeIcon("icons/subnode_icon.png"),
        /** subnode with ports. */
        SubNodeDetailed("icons/subnode_detailed.png"),
        /** cross/delete icon for the search bar. */
        ButtonClear("icons/clear.png"),
        /** filter icon. */
        FunnelIcon("icons/filter.png"),
        /** hide equal nodes button. */
        ButtonHideEqualNodes("icons/hide_equals.png"),
        /** show additional nodes only button. */
        ButtonShowAdditionalNodesOnly("icons/show_add_only.png"),
        /** Icon for configured knime project. */
        WorkflowConfigured("icons/project_configured.png"),
        /** Icon for executing knime project. */
        WorkflowExecuting("icons/project_executing.png"),
        /** Icon for fully executed knime project. */
        WorkflowExecuted("icons/project_executed.png"),
        /** Icon for knime project with errors. */
        WorkflowError("icons/project_error.png"),
        /** Icon for a closed knime project. */
        WorkflowClosed("icons/project_basic.png"),
        /** Icon for a knime project with unknown state. */
        WorkflowUnknown("icons/knime_unknown.png"),
        /** Icon for a knime project with a red unknown state. */
        WorkflowUnknownRed("icons/knime_unknown_red.png"),

        /** Icon for the favorite nodes view. */
        FavoriteNodesFolder("icons/fav/folder_fav.png"),
        /** Icon for the most frequently used nodes category. */
        FavoriteNodesFrequentlyUsed("icons/fav/folder_freq.png"),
        /** Icon for the last used nodes category. */
        FavoriteNodesLastUsed("icons/fav/folder_last.png"),
        /** Icon with a green OK check mark. */
        Ok("icons/ok.png"),
        /** Icon for all kinds of warnings. */
        Warning("icons/warning.png"),
        /** Icon for information messages. */
        Info("icons/info.gif"),
        /** Icon icon in a round blue button. */
        InfoButton("icons/info.png"),
        /** Info icon in a little speech balloon. */
        InfoBalloon("icons/info_balloon.png"),
        /** Icon for all kinds of errors. */
        Error("icons/error.png"),
        /** busy cursor (hour glass). */
        Busy("icons/busy.png"),
        /** a question mark icon. */
        Help("icons/help.png"),
        /** ServerSpace Explorer Icon: server logo, 55px.*/
        ServerSpaceServerLogo("icons/server_space/server_logo_55.png"),
        /** ServerSpace Explorer Icon: server root. */
        ServerSpaceIcon("icons/server_space/explorer_server.png"),
        /** ServerSpace Explorer Icon: scheduled job. */
        ServerScheduledJob("icons/server_space/flow_scheduled.png"),
        /** ServerSpace Explorer Icon: scheduled periodic job. */
        ServerScheduledPeriodicJob("icons/server_space/flow_periodic.png"),
        /** ServerSpace Explorer Icon: configured job. */
        ServerJobConfigured("icons/server_space/running_job_confgd.png"),
        /** ServerSpace Explorer Icon: executing job. */
        ServerJobExecuting("icons/server_space/running_job_execting.png"),
        /** ServerSpace Explorer Icon: executed job. */
        ServerJobExecuted("icons/server_space/running_job_execed.png"),
        /** ServerSpace Explorer Icon: idle job. */
        ServerJobIdle("icons/server_space/running_job_idle.png"),
        /** ServerSpace Explorer Icon: Dialog icon - group permissions. */
        ServerDialogGroupPermissions("icons/server_space/grp_permission_55.png"),
        /** ServerSpace Explorer Icon: Dialog icon - Meta Info Edit. */
        ServerDialogEditMetaInfo("icons/server_space/meta_info_edit55.png"),
        /** ServerSpace Explorer Icon: Dialog icon - Permissions. */
        ServerDialogPermissions("icons/server_space/permission.png"),
        /** ServerSpace Explorer Icon: Dialog icon - upload workflow. */
        ServerDialogWorkflowUpload("icons/server_space/upload_wf55.png"),
        /** ServerSpace Explorer Menu Icon: edit meta info. */
        ServerEditMetaInfo("icons/server_space/meta_info_edit.png"),
        /** ServerSpace Explorer Menu Icon: show node messages. */
        ServerShowNodeMsg("icons/server_space/nodemsg.png"),
        /** ServerSpace Explorer Menu Icon: show schedule info. */
        ServerShowScheduleInfo("icons/server_space/schedinfo.png"),
        /** ServerSpace Explorer Menu Icon: upload workflow. */
        ServerUploadWorkflow("icons/server_space/upload_wf.png"),
        /** ServerSpace Explorer Menu Icon: edit meta info. */
        ServerPermissions("icons/server_space/key.png"),
        /** ServerSpace Explorer Menu Icon: create a snapshot. */
        ServerSnapshot("icons/server_space/snapshot.png"),
        /** ServerSpace Explorer Menu Icon: replace head with snapshot. */
        ServerReplaceHead("icons/server_space/replace_head.png"),
        /** ServerSpace Explorer Menu Icon: download something from the server. */
        ServerDownload("icons/server_space/download.png"),
        /** ServerSpace Explorer Icon: idle job. */
        TeamSpaceIcon("icons/team_space/explorer_teamspace.png"),
        /** LocalSpace Explorer Icon: server root. */
        LocalSpaceIcon("icons/workflow_projects.png"),
        /** WorkflowDiff: action icon. */
        WorkflowDiffIcon("icons/diff.png");

        private final URL m_url;

        private SharedImages(final String path) {
            m_url = FileLocator.find(KNIMECorePlugin.getDefault().getBundle(), new Path(path), null);
            if (m_url == null) {
                LOGGER.coding("Cannot find icon for '" + toString() + "' at " + path);
            }
        }

        private SharedImages(final URL url) {
            m_url = url;
        }

        /**
         * Returns the URL to the image.
         *
         * @return a URL
         */
        public URL getUrl() {
            return m_url;
        }
    }

    /**
     * Flags to decorate KNIME ServerSpace icons. Icon file names with the corresponding combinations of suffixes must
     * exist!
     */
    public enum ImgDecorator {
        /** Decorator for messages. */
        Message("_msg"),
        /** Decorator for outdated jobs (ignored right now). */
        Outdated("_out"),
        /** Decorator for orphaned jobs (ignored right now). */
        Orphaned("");

        private final String m_suffix;

        private ImgDecorator(final String suffix) {
            m_suffix = suffix;
        }

        /**
         * Returns the image filename suffix for this image decorator.
         *
         * @return a suffix
         */
        String getSuffix() {
            return m_suffix;
        }
    }

    /**
     *
     */
    private ImageRepository() {
        // don't instantiate. Utility class.
    }

    /**
     * Returns a shared image descriptor.
     *
     * @param image the image
     * @return an image
     */
    public static ImageDescriptor getImageDescriptor(final SharedImages image) {
        return getImageDescriptor(image.getUrl());
    }

    public static ImageDescriptor getImageDescriptor(final URL url) {
        final String key = url.toString();
        // make sure the image is in the registry
        getImage(url);
        return REGISTRY.getDescriptor(key);
    }

    /**
     * @see #getImage(URL).
     * @param image
     * @return an image.
     */
    public static Image getImage(final SharedImages image) {
        return getImage(image.getUrl());
    }

    public static ImageDescriptor getImageDescriptor(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getImageDescriptor(url);
    }

    public static Image getImage(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getImage(url);
    }

    /**
     * Returns an image from the specified location. If the system zoom factor (for high dpi) is set, it returns a
     * larger version (1.5 or 2 times). If a file with the corresponding name extension is provided it will take
     * the image from that file (without scaling). Eclipse naming convention is: 100% zoom file "basename.png", 150%
     * file: "basename@1.5x.png", 200% file: "basename@2x.png". If these extra files don't exist the base file is
     * scaled.
     *
     * @param resourceURL to the icon image
     * @return an image
     */
     public static Image getImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString();
        Image img = REGISTRY.get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the image
            img = new Image(Display.getDefault(), new KNIMEImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding("Unable to locate image " + resourceURL.toString() + ": " + e.getMessage(), e);
            return getMissingIcon();
        }
        REGISTRY.put(key, img);
        return img;
     }

    /**
     * Returns a shared image.
     *
     * @param image the image
     * @return an image
     */
    public static Image getUnscaledImage(final SharedImages image) {
        return getUnscaledImage(image.getUrl());
    }

    /**
     * @see #getUnscaledImage(SharedImages)
     *
     * @param image the image
     * @return an image descriptor
     */
    public static ImageDescriptor getUnscaledImageDescriptor(final SharedImages image) {
        final String key = image.getUrl().toString() + NOTSCALED_KEY;
        // make sure the image is in the registry
        getUnscaledImage(image.getUrl());
        return REGISTRY.getDescriptor(key);
    }

    /**
     * Returns an image from the specified location. If the system zoom factor (for high dpi) is set, it still returns
     * an image of original 100% size. Eclipse naming convention is for different zoom factors is totally ignored!
     *
     * @param resourceURL to the icon image
     * @return an image in its original size (no high DPI system zoom factor scaling)
     */
     public static Image getUnscaledImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString() + NOTSCALED_KEY;
        Image img = REGISTRY.get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the icon
            img = new Image(Display.getDefault(), new KNIMENonscalingImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding("Unable to locate image " + resourceURL.toString() + ": " + e.getMessage(), e);
            return getMissingIcon();
        }
        REGISTRY.put(key, img);
        return img;
     }

     /**
      * @see #getUnscaledImage(URL)
      * @param pluginID
      * @param path
      * @return
      */
     public static Image getUnscaledImage(final String pluginID, final String path) {
         if (path == null) {
             LOGGER.error("Null path passed to getImage (pluginID: " + pluginID + ")");
             return getMissingIcon();
         }
         URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
         if (url == null) {
             LOGGER.coding("Unable to locate image (pluginID: " + pluginID + ", img: " + path);
             return getMissingIcon();
         }
         return getUnscaledImage(url);
     }

     public static Image getUnscaledIconImage(final SharedImages image) {
         return getUnscaledIconImage(image.getUrl());
     }

     /**
      * Returns a 16x16px version of the provided image. Ignores the system zoom level for highDPI images.
      *
      * @param resourceURL to the icon image
      * @return a potentially scaled image
      */
      public static Image getUnscaledIconImage(final URL resourceURL) {
          if (resourceURL == null) {
              return null;
          }
         final String key = resourceURL.toString() + ICONIFIED_KEY + NOTSCALED_KEY;
         Image img = REGISTRY.get(key);
         if (img != null) {
             return img;
         }
         try {
             // the KNIME image provider ensures correct size of the icon
             img = new Image(Display.getDefault(), new KNIMENonscalingIconProvider(resourceURL));
         } catch (IOException e) {
             LOGGER.coding("Unable to locate node icon. Using default icon instead." + e.getMessage(), e);
             return getIconImage(SharedImages.DefaultNodeIcon);
         }
         REGISTRY.put(key, img);
         return img;
      }


     /**
      * Returns the 16x16 version of the specified image with the passed decorators.<p>
      * The current implementation requires each icon that may be requested with decorators to be present in the
      * corresponding icons directory (the decorators are not added programmatically!) with the corresponding name.
      *
      * @param image the image to return
      * @param decorators the decorators to add to the image
      * @return the image with the specified decorators
      */
     public static Image getIconImage(final SharedImages image, final ImgDecorator... decorators) {
         URL imgURL = image.getUrl();
         if (imgURL != null && decorators != null && decorators.length > 0) {
             String suffix = getRequestedDecoratorSuffixes(decorators);
             // in the path we have to insert the suffix in front of the extension (.jpg, .png, etc.)
             String path = imgURL.getPath();
             int dotIdx = path.lastIndexOf('.');
             if (dotIdx <= 0) {
                 path += suffix;
             } else {
                 path = path.substring(0, dotIdx) + suffix + path.substring(dotIdx);
             }

             try {
                 if (imgURL.getQuery() != null) {
                     path += '?' + imgURL.getQuery();
                 }
                 imgURL = new URL(imgURL.getProtocol(), imgURL.getHost(), imgURL.getPort(), path);
             } catch (MalformedURLException e) {
                 LOGGER.coding("Error while trying to construct URL for decorated image." + e.getMessage(), e);
                 return null;
             }
         }
         return getIconImage(imgURL);
     }

     // Computes an ordered list of image name suffixes for the requested decorators
     private static String getRequestedDecoratorSuffixes(final ImgDecorator... decorators) {
         StringBuilder suffix = new StringBuilder();
         // add the decorator suffixes in the order they are defined
         if (decorators != null) {
             for (ImgDecorator dec : ImgDecorator.values()) {
                 for (ImgDecorator flag : decorators) {
                     if (flag.equals(dec)) {
                         suffix.append(flag.getSuffix());
                         break; // add each decorator only once
                     }
                 }
             }
         }
         return suffix.toString();
     }

    /**
     * @see #getIconImage. Returns the image wrapped in a descriptor.
     * @param icon
     * @return the descriptor wrapping the iconified image
     */
    public static ImageDescriptor getIconDescriptor(final SharedImages icon) {
        final String key = icon.getUrl().toString() + ICONIFIED_KEY;
        // make sure the image is in the repository!
        getIconImage(icon.getUrl());
        return REGISTRY.getDescriptor(key);
    }
     /**
     * Returns a 16x16px version of a node icon. If no icon is specified by the node factory the default node icon is
     * returned. If the icon specified by the factory does not exist, the Eclipse default missing icon is returned. If the system
     * zoom factor (for high dpi) is set, it returns a 24x24px or 32x32px image accordingly. If a file with the corresponding name
     * extension is provided it will take the icon from that file (_24x24 or _32x32).
     *
     * @param nodeFactory a node factory
     * @return a potentially scaled image
     */
    public static Image getIconImage(final NodeFactory<? extends NodeModel> nodeFactory) {
        URL iconURL = nodeFactory.getIcon();
        if (iconURL == null) {
            return getIconImage(SharedImages.DefaultNodeIcon);
        }
        Image img = getIconImage(iconURL);
        return img;
    }

    /**
     * Returns a 16x16px version of the provided image. If the system zoom factor (for high dpi) is set, it returns a
     * 24x24px or 32x32px image accordingly. If a file with the corresponding name extension is provided it will take
     * the icon from that file (_24x24 or _32x32).
     *
     * @param resourceURL to the icon image
     * @return a potentially scaled image
     */
     public static Image getIconImage(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
        final String key = resourceURL.toString() + ICONIFIED_KEY;
        Image img = REGISTRY.get(key);
        if (img != null) {
            return img;
        }
        try {
            // the KNIME image provider ensures correct size of the icon
            img = new Image(Display.getDefault(), new KNIMEIconImageProvider(resourceURL));
        } catch (IOException e) {
            LOGGER.coding("Unable to locate node icon. Using default icon instead." + e.getMessage(), e);
//            return getIconImage(SharedImages.DefaultNodeIcon);
            return null;
        }
        REGISTRY.put(key, img);
        return img;
     }

     public static ImageDescriptor getIconDescriptor(final URL resourceURL) {
         if (resourceURL == null) {
             return null;
         }
         final String key = resourceURL.toString() + ICONIFIED_KEY;
         // make sure the image is in the registry
         getIconImage(resourceURL);
         return REGISTRY.getDescriptor(key);
     }

    /**
     * Returns an image for an "external" image. The image is given by the
     * plug-in an the path relative to the plug-in root.
     *
     * @param pluginID the plug-in's id
     * @param path the path of the image
     * @return an image or <code>null</code> if the image does not exist
     */
    public static Image getIconImage(final String pluginID, final String path) {
        if (path == null) {
            LOGGER.error("Null path passed to getImage (pluginID: " + pluginID + ")");
            return getMissingIcon();
        }
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getIconImage(url);
    }

    public static ImageDescriptor getIconDescriptor(final String pluginID, final String path) {
        URL url = FileLocator.find(Platform.getBundle(pluginID), new Path(path), null);
        if (url == null) {
            return null;
        }
        return getIconDescriptor(url);
    }

    /**
     * @return a red 16x16 icon
     */
    public static Image getMissingIcon() {
        Image img = REGISTRY.get(MISSING_ICON_KEY);
        if (img != null) {
            return img;
        }
        img = new Image(Display.getDefault(), KNIMEImageProvider.MISSING_IMAGE_DATA);
        REGISTRY.put(MISSING_ICON_KEY, img);
        return img;
    }
}
