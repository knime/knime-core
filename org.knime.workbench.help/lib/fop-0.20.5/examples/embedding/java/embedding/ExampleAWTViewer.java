/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package embedding;

//Java
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

//JAXP
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXResult;

//Avalon
import org.apache.avalon.framework.ExceptionUtil;
import org.apache.avalon.framework.logger.ConsoleLogger;

//FOP
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.render.awt.AWTRenderer;
import org.apache.fop.viewer.PreviewDialog;
import org.apache.fop.viewer.SecureResourceBundle;
import org.apache.fop.viewer.Translator;
import org.apache.fop.viewer.UserMessage;

/**
 * This class demonstrates the use of the AWT Viewer.
 */
public class ExampleAWTViewer {

    public static final String TRANSLATION_PATH =
        "/org/apache/fop/viewer/resources/";

    protected PreviewDialog createPreviewDialog(
                AWTRenderer renderer,
                Translator res) {
        PreviewDialog frame = new PreviewDialog(renderer, res);
        frame.validate();
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent we) {
                    System.exit(0);
                }
            });

        // center window
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        if (frameSize.height > screenSize.height)
            frameSize.height = screenSize.height;
        if (frameSize.width > screenSize.width)
            frameSize.width = screenSize.width;
        frame.setLocation((screenSize.width - frameSize.width) / 2,
                          (screenSize.height - frameSize.height) / 2);
        frame.setVisible(true);
        return frame;
    }

    private SecureResourceBundle getResourceBundle(String path) throws IOException {
        URL url = getClass().getResource(path);
        if (url == null) {
            // if the given resource file not found, the english resource uses as default
            path = path.substring(0, path.lastIndexOf(".")) + ".en";
            url = getClass().getResource(path);
        }
        return new SecureResourceBundle(url.openStream());
    }

    public void viewFO(File fo)
                throws IOException, FOPException, TransformerException {

        //Setup l18n
        String language = System.getProperty("user.language");
        Translator translator = getResourceBundle(
            TRANSLATION_PATH + "resources." + language);
        translator.setMissingEmphasized(false);

        UserMessage.setTranslator(getResourceBundle(
            TRANSLATION_PATH + "messages." + language));

        //Setup renderer
        AWTRenderer renderer = new AWTRenderer(translator);

        //Create preview dialog (target for the AWTRenderer)
        PreviewDialog frame = createPreviewDialog(renderer, translator);
        renderer.setProgressListener(frame);
        renderer.setComponent(frame);

        //Setup Driver
        Driver driver = new Driver();
        driver.setLogger(new ConsoleLogger(ConsoleLogger.LEVEL_INFO));
        driver.setRenderer(renderer);

        try {
            // build FO tree: time
            frame.progress(translator.getString("Build FO tree") + " ...");

            //Load XSL-FO file (you can also do an XSL transformation here)
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            Source src = new StreamSource(fo);
            Result res = new SAXResult(driver.getContentHandler());
            transformer.transform(src, res);

            //Show page
            frame.progress(translator.getString("Show"));
            frame.showPage();

        } catch (Exception e) {
            frame.reportException(e);
            if (e instanceof FOPException) {
                throw (FOPException)e;
            }
            throw new FOPException(e);
        }
    }


    public static void main(String[] args) {
        try {
            System.out.println("FOP ExampleAWTViewer\n");
            System.out.println("Preparing...");

            //Setup directories
            File baseDir = new File(".");
            File outDir = new File(baseDir, "out");
            outDir.mkdirs();

            //Setup input and output files
            File fofile = new File(baseDir, "xml/fo/helloworld.fo");

            System.out.println("Input: XSL-FO (" + fofile + ")");
            System.out.println("Output: AWT Viewer");
            System.out.println();
            System.out.println("Starting AWT Viewer...");

            ExampleAWTViewer app = new ExampleAWTViewer();
            app.viewFO(fofile);

            System.out.println("Success!");
        } catch (Exception e) {
            System.err.println(ExceptionUtil.printStackTrace(e));
            System.exit(-1);
        }
    }
}
