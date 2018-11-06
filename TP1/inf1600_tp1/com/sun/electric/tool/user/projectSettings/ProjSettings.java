/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjSettings.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.user.projectSettings;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.URL;
import java.net.URLConnection;
import java.io.*;
import java.util.Stack;
import java.util.HashSet;
import java.util.Set;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Setting;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.OpenFile;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Jul 25, 2006
 * Time: 5:18:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class ProjSettings {

    private static ProjSettingsNode settings = new ProjSettingsNode();
    private static File lastProjectSettingsFile = null;
    private static final String entry = "e";
    private static final boolean READER_RESETS_DEFAULTS = true;
    private static final boolean WRITER_WRITES_DEFAULTS = true;

    public static ProjSettingsNode getSettings() {
        return settings;
    }

    public static void writeSettings(File file) {
        write(file.getPath(), getSettings());
    }

    public static File getLastProjectSettingsFile() {
        return lastProjectSettingsFile;
    }

    /**
     * Read project settings and apply them
     * @param file the file to read
     * @param allowOverride true to allow overriding current settings,
     * false to disallow and warn if different.
     */
    public static void readSettings(File file, boolean allowOverride) {
        if (lastProjectSettingsFile == null) allowOverride = true;

        ReadResult result = read(file.getPath(), allowOverride);
        if (result == ReadResult.ERROR) return;

        if (result == ReadResult.CONFLICTS && lastProjectSettingsFile != null) {
            String message = "Warning: Project Settings conflicts;" + (allowOverride ? "" : " ignoring new settings.") + " See messages window";
            Job.getUserInterface().showInformationMessage(message, "Project Settings Conflict");
            System.out.println("Project Setting conflicts found: "+lastProjectSettingsFile.getPath()+" vs "+file.getPath());
        }
        lastProjectSettingsFile = file;
    }

    /**
     * Write settings to disk.  Pops up a dialog to prompt for file location
     */
    public static void exportSettings() {
/*
        int ret = JOptionPane.showConfirmDialog(TopLevel.getCurrentJFrame(), "Changes to Project Settings may affect all users\n\nContinue Anyway?",
                "Warning!", JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.NO_OPTION) return;
*/
        File outputFile = new File(FileType.LIBRARYFORMATS.getGroupPath(), "projsettings.xml");
        String ofile = OpenFile.chooseOutputFile(FileType.XML, "Export Project Settings", outputFile.getPath());
        if (ofile == null) return;
        outputFile = new File(ofile);

        writeSettings(outputFile);
    }

    public static void importSettings() {
        String ifile = OpenFile.chooseInputFile(FileType.XML, "Import Project Settings", false, FileType.LIBRARYFORMATS.getGroupPath(), false);
        if (ifile == null) return;
        new ImportSettingsJob(ifile);
    }

    /**
     * Class to read a library in a new thread.
     * For a non-interactive script, use ReadLibrary job = new ReadLibrary(filename, format).
     */
    public static class ImportSettingsJob extends Job {
        private String fileName;
        
        public ImportSettingsJob(String fileName) {
            super("Import Settings", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.fileName = fileName;
            startJob();
        }
        
        public boolean doIt() throws JobException {
            readSettings(new File(fileName), true);
            return true;
        }
    }

    // ----------------------------- Private -------------------------------

    private static void write(String file, ProjSettingsNode node) {
        File ofile = new File(file);
        Writer wr = new Writer(ofile);
        if (wr.write("ProjectSettings", node)) {
            System.out.println("Wrote Project Settings to "+file);
            lastProjectSettingsFile = ofile; 
        }
        wr.close();
    }

    // return false if error reading file
    private static ReadResult read(String file, boolean allowOverride) {
        Reader rd = new Reader(TextUtils.makeURLToFile(file));
        ReadResult r = rd.read(allowOverride);
        System.out.println("Read Project Settings from "+file);
        return r;
    }

    // ------------------------- ProjSettings Writer ----------------------------

    private static class Writer {
        private File file;
        PrintWriter out;
        private int indent;
        private static final int indentVal = 4;
        Set<Object> visited = new HashSet<Object>();

        private Writer(File file) {
            this.file = file;
        }

        private boolean write(String nodeName, ProjSettingsNode node) {
            out = new PrintWriter(new BufferedOutputStream(System.out));
            if (file != null) {
                try {
                    out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
                } catch (java.io.IOException e) {
                    System.out.println("Error opening "+file+" for write: "+e.getMessage());
                    file = null;
                    return false;
                }
            }
            writeNode(nodeName, node);
            return true;
        }


        private void prIndent(String msg) {
            for (int i=0; i<indent*indentVal; i++) { out.print(" "); }
            out.println(msg);
        }

        private void writeNode(String name, ProjSettingsNode node) {
            if (visited.contains(node)) {
                // already wrote this object out, must be recursion.
                System.out.println("ERROR: recursive loop in Project Settings; "+
                        "ignoring second instantiation of ProjSettingsNode \""+name+"\".");
                return;
            }
            visited.add(node);

            String classDef = "";
/*
            if (node.getClass() != ProjSettingsNode.class) {
                classDef = " class=\""+node.getClass().getName()+"\">";
            }
*/

            if (node.getKeys().size() == 0) {
                // if nothing in node, skip
                //prIndent("<node key=\""+name+"\""+classDef+"/>");
            } else {
                prIndent("<node key=\""+name+"\""+classDef+">");
                indent++;
                for (String key : node.getKeys()) {
                    writeSetting(key, node.get(key));
                }
                indent--;
                prIndent("</node>");
            }
        }

        private void writeSetting(String key, Object value) {
            if (value instanceof ProjSettingsNode) {
                ProjSettingsNode node = (ProjSettingsNode)value;
                writeNode(key, node);
            } else if (value instanceof Setting) {
                Setting setting = (Setting)value;
                Object val = setting.getValue();
                if (WRITER_WRITES_DEFAULTS || !setting.getFactoryValue().equals(val))
                    writeValue(key, val);
            } else if (value instanceof ProjSettingsNode.UninitializedPref) {
                ProjSettingsNode.UninitializedPref pref = (ProjSettingsNode.UninitializedPref)value;
                writeValue(key, pref.value);
            } else {
                System.out.println("Ignoring unsupported class "+value.getClass().getName()+" for key "+key+
                        " in project settings");
            }
        }

        private void writeValue(String key, Object value) {
            if (value instanceof Integer) {
                prIndent("<"+entry+" key=\""+key+"\"\t int=\""+value.toString()+"\" />");
            } else if (value instanceof Double) {
                prIndent("<"+entry+" key=\""+key+"\"\t double=\""+value.toString()+"\" />");
            } else if (value instanceof Long) {
                prIndent("<"+entry+" key=\""+key+"\"\t long=\""+value.toString()+"\" />");
            } else if (value instanceof Boolean) {
                prIndent("<"+entry+" key=\""+key+"\"\t boolean=\""+value.toString()+"\" />");
            } else {
                prIndent("<"+entry+" key=\""+key+"\"\t string=\""+value+"\" />");
            }
        }

        private void close() {
            if (file != null) out.close();
        }
    }

    // ------------------------- ProjSettings Reader ----------------------------

    private enum ReadResult { OK, CONFLICTS, ERROR };

    private static class Reader extends DefaultHandler {
        private URL url;
        private Stack<ProjSettingsNode> context;
        private Locator locator;
        private boolean allowOverride;
        private boolean differencesFound;
        private HashMap<Setting,Object> settings = new HashMap<Setting,Object>();

        private static final boolean DEBUG = false;

        private Reader(URL url) {
            this.url = url;
            this.context = new Stack<ProjSettingsNode>();
            this.locator = null;
            allowOverride = true;
            differencesFound = false;
        }

        private ReadResult read(boolean allowOverride) {
            this.allowOverride = allowOverride;
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setValidating(true);
                factory.setNamespaceAware(true);
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();
                factory.newSAXParser().parse(is, this);
                if (READER_RESETS_DEFAULTS) 
                    commit(ProjSettings.getSettings());
                else
                    commit0();
                if (differencesFound) return ReadResult.CONFLICTS;
                return ReadResult.OK;
            } catch (IOException e) {
                System.out.println("Error reading file "+url.toString()+": "+e.getMessage());
            } catch (SAXParseException e) {
                System.out.println("Error parsing file "+url.toString()+" on line "+e.getLineNumber()+
                        ", column "+e.getColumnNumber()+": "+e.getMessage());
            } catch (ParserConfigurationException e) {
                System.out.println("Configuration error reading file "+url.toString()+": "+e.getMessage());
            } catch (SAXException e) {
                System.out.println("Exception reading file "+url.toString()+": "+e.getMessage());
            }
            return ReadResult.ERROR;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {

            // get the name of the key
            String key = attributes.getValue("key");
            if (key == null) {
                throw parseException("Entry "+qName+" is missing \"key\" attribute");
            }

            // see if this is a project settings node
            if (qName.equals("node")) {
                if (context.isEmpty()) {
                    // first node is root node
                    context.push(ProjSettings.getSettings());
                } else {
                    ProjSettingsNode node = context.peek().getNode(key);
                    if (node == null)
                        System.out.println("Error: No Project Settings Node named "+key+" in Electric");
                    else
                        context.push(node);
                }
            } else if (qName.equals(entry)) {

                // must be a key-value pair
                if (context.isEmpty()) {
                    throw parseException("No node for key-value pair "+qName);
                }
                ProjSettingsNode currentNode = context.peek();
                String valueStr = null;
                Object value = null;
                Setting setting = currentNode.getValue(key);
                try {
                    if ((valueStr = attributes.getValue("string")) != null)
                        value = valueStr;
                    else if ((valueStr = attributes.getValue("int")) != null)
                        value = Integer.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("double")) != null)
                        value = Double.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("boolean")) != null)
                        value = Boolean.valueOf(valueStr);
                    else if ((valueStr = attributes.getValue("long")) != null)
                        value = Long.valueOf(valueStr);
                    else
                        System.out.println("Error: Unsupported value for key "+key+", at line "+locator.getLineNumber()+": must be string, int, double, boolean, or long");
                } catch (NumberFormatException e) {
                    System.out.println("Error converting "+valueStr+" to a number at line "+locator.getLineNumber()+": "+e.getMessage());
                }
                if (value != null) {
                    if (setting != null) {
                        settings.put(setting, value);
                    } else if (allowOverride)
                        currentNode.put(key, new ProjSettingsNode.UninitializedPref(value));
                }
            }

            if (DEBUG) {
                System.out.print("Read "+qName+", Attributes: ");
                for (int i=0; i<attributes.getLength(); i++) {
                    System.out.print("["+attributes.getQName(i)+"="+attributes.getValue(i)+"], ");
                }
                System.out.println();
            }
        }

        private void commit(ProjSettingsNode node) {
            for (Object v: node.data.values()) {
                if (v instanceof Setting) {
                    Setting setting = (Setting)v;
                    Object value = settings.get(setting);
                    if (value == null)
                        value = setting.getFactoryValue();
                    prDiff(setting, setting.getValue(), value, allowOverride);
                    if (allowOverride) setting.set(value);
                } else if (v instanceof ProjSettingsNode) {
                    commit((ProjSettingsNode)v);
                }
            }
        }
        
        private boolean commit0() {
            for (Map.Entry<Setting,Object> e: settings.entrySet()) {
                Setting setting = e.getKey();
                Object value = e.getValue();
                prDiff(setting, setting.getValue(), value, allowOverride);
                if (allowOverride) setting.set(value);
            }
            return differencesFound;
            
        }
        
        private void prDiff(Setting setting, Object prefVal, Object xmlVal, boolean allowOverride) {
            if (!ProjSettingsNode.equal(xmlVal, setting)) {
                differencesFound = true;
                if (allowOverride)
                    System.out.println("Warning: Setting \""+setting.getPrefName()+"\" set to "+xmlVal+", overrides current val of "+prefVal);
                else
                    System.out.println("Warning: Setting \""+setting.getPrefName()+"\" retains current val of "+prefVal+", while ignoring projectsettings.xml value of "+xmlVal);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")) {
                // pop node context
                if (context.isEmpty()) {
                    throw parseException("Empty context, too many closing </> brackets");
                }
                context.pop();
            }
            if (DEBUG) {
                System.out.println("End "+qName);
            }
        }

        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
        private SAXParseException parseException(String msg) {
            return new SAXParseException(msg, locator);
        }
    }

    public static String describeContext(Stack<String> context) {
        StringBuffer buf = new StringBuffer();
        boolean first = true;
        for (String name : context) {
            if (first)
                first = false;
            else
                buf.append(".");
            buf.append(name);
        }
        if (buf.length() == 0) return "RootContext";
        return buf.toString();
    }

    // ------------------------------ Testing -------------------------------

    public static void main(String args[]) {
        test();
    }

    public static void test() {
        write("/tmp/projsettings.xml", getSettings());
        read("/tmp/projsettings.xml", false);
    }

/*
    public static void test2() {
        ProjSettingsNode node = new ProjSettingsNode();
        node.putInteger("an int", 1);
        node.putString("a string", "this is a string");
        ProjSettingsNode node2 = new ProjSettingsNode();
        node.putNode("node2", node2);
        node2.putDouble("a double", 43.56);
        node2.putBoolean("a boolean", true);
        ProjSettingsNode node3 = new TestExtendNode();
        node2.putNode("extended node", node3);
        node3.putLong("a long", 302030049);
        node3.putString("a string", "some string");

        write("/tmp/projsettings.xml", node);
        ProjSettingsNode readNode = read("/tmp/projsettings.xml");
        if (node.printDifferences(readNode)) {
            System.out.println("Node read does not match node written");
        } else {
            System.out.println("Node read matches node written");
        }
    }
*/

    protected static class TestExtendNode extends ProjSettingsNode {

    }
}
