// resolver.java - A simple command-line test tool for the resolver

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xml.resolver.apps;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Vector;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.helpers.Debug;
import org.apache.xml.resolver.tools.CatalogResolver;

/**
 * A simple command-line resolver.
 *
 * <p>This class implements a simple command-line resolver. It takes
 * some parameters and passes them through the resolver, printing the
 * result.
 * </p>
 *
 * <p>Usage: resolver [options] keyword</p>
 *
 * <p>Where options are:</p>
 *
 * <dl>
 * <dt><code>-c</code> <em>catalogfile</em></dt>
 * <dd>Load a particular catalog file.</dd>
 * <dt><code>-n</code> <em>name</em></dt>
 * <dd>Sets the name.</dd>
 * <dt><code>-p</code> <em>publicId</em></dt>
 * <dd>Sets the public identifier.</dd>
 * <dt><code>-s</code> <em>systemId</em></dt>
 * <dd>Sets the system identifier.</dd>
 * <dt><code>-a</code></dt>
 * <dd>Absolute system URI.</dd>
 * <dt><code>-u</code> <em>uri</em></dt>
 * <dd>Sets the URI.</dd>
 * <dt><code>-d</code> <em>integer</em></dt>
 * <dd>Set the debug level.</dd>
 * </dl>
 *
 * <p>And keyword is one of: doctype, document, entity, notation, public,
 * system, or uri.</p>
 *
 * <p>The process ends with error-level 1, if there errors.</p>
 *
 * @see org.apache.xml.resolver.tools.ResolvingParser
 *
 * @author Norman Walsh
 * <a href="mailto:Norman.Walsh@Sun.COM">Norman.Walsh@Sun.COM</a>
 *
 * @version 1.0 */
public class resolver {
  private static Debug debug = CatalogManager.getStaticManager().debug;

  /** The main entry point */
  public static void main (String[] args)
    throws FileNotFoundException, IOException {

    int     debuglevel   = 0;
    Vector  catalogFiles = new Vector();
    int     resType      = 0;
    String  resTypeStr   = null;
    String  name         = null;
    String  publicId     = null;
    String  systemId     = null;
    String  uri          = null;
    boolean absoluteSystem = false;

    for (int i=0; i<args.length; i++) {
      if (args[i].equals("-c")) {
	++i;
	catalogFiles.add(args[i]);
	continue;
      }

      if (args[i].equals("-p")) {
	++i;
	publicId = args[i];
	continue;
      }

      if (args[i].equals("-s")) {
	++i;
	systemId = args[i];
	continue;
      }

      if (args[i].equals("-n")) {
	++i;
	name = args[i];
	continue;
      }

      if (args[i].equals("-u")) {
	++i;
	uri = args[i];
	continue;
      }

      if (args[i].equals("-a")) {
	absoluteSystem = true;
	continue;
      }

      if (args[i].equals("-d")) {
	++i;
	String debugstr = args[i];
	try {
	  debuglevel = Integer.parseInt(debugstr);
	  if (debuglevel > 0) {
	    debug.setDebug(debuglevel);
	  }
	} catch (Exception e) {
	  // nop
	}
	continue;
      }

      resTypeStr = args[i];
    }

    if (resTypeStr == null) {
      usage();
    }

    if (resTypeStr.equalsIgnoreCase("doctype")) {
      resType = Catalog.DOCTYPE;
      if (publicId == null && systemId == null) {
	System.out.println("DOCTYPE requires public or system identifier.");
	usage();
      }
    } else if (resTypeStr.equalsIgnoreCase("document")) {
      resType = Catalog.DOCUMENT;
    } else if (resTypeStr.equalsIgnoreCase("entity")) {
      resType = Catalog.ENTITY;
      if (publicId == null && systemId == null && name == null) {
	System.out.println("ENTITY requires name or public or system identifier.");
	usage();
      }
    } else if (resTypeStr.equalsIgnoreCase("notation")) {
      resType = Catalog.NOTATION;
      if (publicId == null && systemId == null && name == null) {
	System.out.println("NOTATION requires name or public or system identifier.");
	usage();
      }
    } else if (resTypeStr.equalsIgnoreCase("public")) {
      resType = Catalog.PUBLIC;
      if (publicId == null) {
	System.out.println("PUBLIC requires public identifier.");
	usage();
      }
    } else if (resTypeStr.equalsIgnoreCase("system")) {
      resType = Catalog.SYSTEM;
      if (systemId == null) {
	System.out.println("SYSTEM requires system identifier.");
	usage();
      }
    } else if (resTypeStr.equalsIgnoreCase("uri")) {
      resType = Catalog.URI;
      if (uri == null) {
	System.out.println("URI requires a uri.");
	usage();
      }
    } else {
      System.out.println(resTypeStr + " is not a recognized keyword.");
      usage();
    }

    if (absoluteSystem) {
      URL base = null;
      URL sysid = null;

      // Calculate the appropriate BASE URI
      try {
	// tack on a basename because URLs point to files not dirs
	String userdir = System.getProperty("user.dir");
	userdir.replace('\\', '/');
	base = new URL("file:///" + userdir + "/basename");
      } catch (MalformedURLException e) {
	String userdir = System.getProperty("user.dir");
	userdir.replace('\\', '/');
	debug.message(1, "Malformed URL on cwd", userdir);
	base = null;
      }

      try {
	sysid = new URL(base, systemId);
	systemId = sysid.toString();
      } catch (MalformedURLException e) {
	try {
	  sysid = new URL("file:///" + systemId);
	} catch (MalformedURLException e2) {
	  debug.message(1, "Malformed URL on system id", systemId);
	}
      }
    }

    CatalogResolver catalogResolver = new CatalogResolver();
    Catalog resolver = catalogResolver.getCatalog();

    for (int count = 0; count < catalogFiles.size(); count++) {
      String file = (String) catalogFiles.elementAt(count);
      resolver.parseCatalog(file);
    }
    String result = null;

    if (resType == Catalog.DOCTYPE) {
      System.out.println("Resolve DOCTYPE (name, publicid, systemid):");
      if (name != null) { System.out.println("       name: " + name); }
      if (publicId != null) { System.out.println("  public id: " + publicId); }
      if (systemId != null) { System.out.println("  system id: " + systemId); }
      if (uri != null) { System.out.println("        uri: " + uri); }
      result = resolver.resolveDoctype(name, publicId, systemId);
    } else if (resType == Catalog.DOCUMENT) {
      System.out.println("Resolve DOCUMENT ():");
      result = resolver.resolveDocument();
    } else if (resType == Catalog.ENTITY) {
      System.out.println("Resolve ENTITY (name, publicid, systemid):");
      if (name != null) { System.out.println("       name: " + name); }
      if (publicId != null) { System.out.println("  public id: " + publicId); }
      if (systemId != null) { System.out.println("  system id: " + systemId); }
      result = resolver.resolveEntity(name, publicId, systemId);
    } else if (resType == Catalog.NOTATION) {
      System.out.println("Resolve NOTATION (name, publicid, systemid):");
      if (name != null) { System.out.println("       name: " + name); }
      if (publicId != null) { System.out.println("  public id: " + publicId); }
      if (systemId != null) { System.out.println("  system id: " + systemId); }
      result = resolver.resolveNotation(name, publicId, systemId);
    } else if (resType == Catalog.PUBLIC) {
      System.out.println("Resolve PUBLIC (publicid, systemid):");
      if (publicId != null) { System.out.println("  public id: " + publicId); }
      if (systemId != null) { System.out.println("  system id: " + systemId); }
      result = resolver.resolvePublic(publicId, systemId);
    } else if (resType == Catalog.SYSTEM) {
      System.out.println("Resolve SYSTEM (systemid):");
      if (systemId != null) { System.out.println("  system id: " + systemId); }
      result = resolver.resolveSystem(systemId);
    } else if (resType == Catalog.URI) {
      System.out.println("Resolve URI (uri):");
      if (uri != null) { System.out.println("        uri: " + uri); }
      result = resolver.resolveURI(uri);
    } else {
      System.out.println("resType is wrong!? This can't happen!");
      usage();
    }

    System.out.println("Result: " + result);
  }

  public static void usage() {
    System.out.println("Usage: resolver [options] keyword");
    System.out.println("");
    System.out.println("Where:");
    System.out.println("");
    System.out.println("-c catalogfile  Loads a particular catalog file.");
    System.out.println("-n name         Sets the name.");
    System.out.println("-p publicId     Sets the public identifier.");
    System.out.println("-s systemId     Sets the system identifier.");
    System.out.println("-a              Makes the system URI absolute before resolution");
    System.out.println("-u uri          Sets the URI.");
    System.out.println("-d integer      Set the debug level.");
    System.out.println("keyword         Identifies the type of resolution to perform:");
    System.out.println("                doctype, document, entity, notation, public, system,");
    System.out.println("                or uri.");

    System.exit(1);
  }
}


