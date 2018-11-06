/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JELIB.java
 * Input/output tool: JELIB Library output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.ExportId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.ImmutablePortInst;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;

import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Class to write a library to disk in new Electric-Library format.
 */
public class JELIB extends Output {
    private boolean oldRevision;
    private Version version;
    /** Project settings. */                                    private HashMap<Setting,Object> projectSettings = new HashMap<Setting,Object>();
    private HashMap<Technology,Technology.SizeCorrector> sizeCorrectors = new HashMap<Technology,Technology.SizeCorrector>();
//    Snapshot snapshot;
//    private Map<LibId,URL> libFiles;
    
    JELIB() {
    }
    
    /**
     * Method to write a Library in Electric Library (.jelib) format.
     * @param libId the Library to be written.
     * @param libFiles new locations of lib files
     * @param oldRevision true to write library in format prior to "8.04l".
     * @return true on error.
     */
    protected boolean writeLib(Snapshot snapshot, LibId libId, Map<LibId,URL> libFiles, boolean oldRevision) {
        this.oldRevision = oldRevision;
        version = oldRevision ? Version.parseVersion("8.04k") : Version.getVersion();
//      this.libFiles = libFiles;
        writeTheLibrary(snapshot, libId);
        return false;
    }
    
    /**
     * Method to write the .jelib file.
     * @param lib the Library to write.
     */
    private void writeTheLibrary(Snapshot snapshot, LibId libId)
    {
        // gather all referenced objects
//        this.snapshot = snapshot;
        LibraryBackup libBackup = snapshot.getLib(libId);
        HashSet<LibId> usedLibs = new HashSet<LibId>();
        HashMap<CellId,BitSet> usedExports = new HashMap<CellId,BitSet>();
        TreeMap<CellName,CellBackup> sortedCells = new TreeMap<CellName,CellBackup>();
        for (CellBackup cellBackup: snapshot.cellBackups) {
            if (cellBackup == null || cellBackup.d.getLibId() != libId) continue;
            CellId cellId = cellBackup.d.cellId;
            sortedCells.put(cellId.cellName, cellBackup);
            int[] instCounts = cellBackup.getInstCounts();
            for (int i = 0; i < instCounts.length; i++) {
                int instCount = instCounts[i];
                if (instCount == 0) continue;
                CellUsage u = cellId.getUsageIn(i);
                usedLibs.add(u.protoId.libId);
            }
        }
        
        // write header information (library, version)
        printWriter.println("# header information:");
        printWriter.print("H" + convertString(libBackup.d.libId.libName) + "|" + version);
        printlnVars(libBackup.d);
        
        // write view information
        boolean viewHeaderPrinted = false;
        HashSet<View> usedViews = new HashSet<View>();
        for (CellBackup cellBackup: snapshot.cellBackups) {
            if (cellBackup == null) continue;
            if (cellBackup.d.getLibId() != libId && !usedExports.containsKey(cellBackup.d.cellId)) continue;
            usedViews.add(cellBackup.d.cellId.cellName.getView());
        }
        for(Iterator<View> it = View.getViews(); it.hasNext(); ) {
            View view = it.next();
            if (!usedViews.contains(view)) continue;
            if (!viewHeaderPrinted) {
                printWriter.println();
                printWriter.println("# Views:");
                viewHeaderPrinted = true;
            }
            printWriter.println("V" + convertString(view.getFullName()) + "|" + convertString(view.getAbbreviation()));
        }
        
        // write external library information
        writeExternalLibraryInfo(libId, usedLibs);
        
        // write tool information
        boolean toolHeaderPrinted = false;
        for(Iterator<Tool> it = Tool.getTools(); it.hasNext(); ) {
            Tool tool = it.next();
            List<Setting> settings = Setting.getSettings(tool.getProjectSettings());
            if (settings.size() == 0) continue;
            if (!toolHeaderPrinted) {
                printWriter.println();
                printWriter.println("# Tools:");
                toolHeaderPrinted = true;
            }
            printWriter.print("O" + convertString(tool.getName()));
            printlnSettings(settings);
        }
        
        // write technology information
        boolean technologyHeaderPrinted = false;
        for (Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); ) {
            Technology tech = it.next();
            List<Setting> settings = Setting.getSettings(tech.getProjectSettings());
            if (settings.size() == 0) continue;
            if (!technologyHeaderPrinted) {
                printWriter.println();
                printWriter.println("# Technologies:");
                technologyHeaderPrinted = true;
            }
            printWriter.print("T" + convertString(tech.getTechName()));
            printlnSettings(settings);
            
// 			for(Iterator<PrimitiveNode> nIt = tech.getNodes(); nIt.hasNext(); )
// 			{
// 				PrimitiveNode pn = nIt.next();
// 				if (!externalObjs.contains(pn)) continue;
            
// 				printWriter.println("D" + convertString(pn.getName()));
// 				for(Iterator<PrimitivePort> pIt = pn.getPorts(); pIt.hasNext(); )
// 				{
// 					PrimitivePort pp = pIt.next();
// 					if (!externalObjs.contains(pp)) continue;
// 					printWriter.println("P" + convertString(pp.getName()));
// 				}
// 			}
// 			for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
// 			{
// 				ArcProto ap = aIt.next();
// 				if (!externalObjs.contains(ap)) continue;
// 				printWriter.println("W" + convertString(ap.getName()));
// 			}
        }
//        printWriter.println();
        
        // gather groups
        ArrayList<CellGroup> chronGroups = new ArrayList<CellGroup>();
        ArrayList<CellGroup> sortedGroups = new ArrayList<CellGroup>();
        for (CellBackup cellBackup: sortedCells.values()) {
            CellName cellName = cellBackup.d.cellId.cellName;
            
            int groupIndex = snapshot.cellGroups[cellBackup.d.cellId.cellIndex];
            while (groupIndex >= chronGroups.size()) chronGroups.add(null);
            CellGroup group = chronGroups.get(groupIndex);
            if (group == null) {
                group = new CellGroup();
                chronGroups.set(groupIndex, group);
                sortedGroups.add(group);
            }
            group.cellNames.add(cellName);
            if (cellName.getView() == View.SCHEMATIC && group.mainSchematics == null)
                group.mainSchematics = cellName;
        }
        
        // write cells
        for (CellBackup cellBackup: sortedCells.values()) {
            writeCell(cellBackup);
            //printWriter.println();
        }
        printWriter.println();
        
        // write groups in alphabetical order
        printWriter.println("# Groups:");
        for (CellGroup group: sortedGroups) {
            writeCellGroup(group);
        }
        
//		LinkedHashSet<Cell.CellGroup> groups = new LinkedHashSet<Cell.CellGroup>();
//		for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
//		{
//			Cell cell = cIt.next();
//			if (!groups.contains(cell.getCellGroup()))
//				groups.add(cell.getCellGroup());
//			cellNames.put(cell.getId(), convertString(cell.getCellName().toString()));
//		}
//
//		// write the cells of the database
//		for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
//		{
//			Cell cell = cIt.next();
//            writeCell(cell.backup());
//            printWriter.println();
//		}
//
//		// write groups in alphabetical order
//		printWriter.println("# Groups:");
//		for(Cell.CellGroup group : groups)
//		{
//			printWriter.print("G");
//
//			// if there is a main schematic cell, write that first
//			Cell main = group.getMainSchematics();
//			if (main != null)
//			{
//				printWriter.print(convertString(main.getCellName().toString()));
//			}
//
//			for(Iterator<Cell> cIt = group.getCells(); cIt.hasNext(); )
//			{
//				Cell cell = cIt.next();
//				if (cell == main) continue;
//
//				printWriter.print("|");
//				printWriter.print(cellNames.get(cell.getId()));
//			}
//			printWriter.println();
//      }
    }
    
    static class CellGroup {
        TreeSet<CellName> cellNames = new TreeSet<CellName>();
        CellName mainSchematics;
    }
    
    void writeCellGroup(CellGroup group) {
        printWriter.print("G");
        
        // if there is a main schematic cell, write that first
        if (group.mainSchematics != null)
            printWriter.print(convertString(group.mainSchematics.toString()));
        
        for(CellName cellName: group.cellNames) {
            if (cellName.equals(group.mainSchematics)) continue;
            printWriter.print("|" + cellName);
        }
        printWriter.println();
    }
    
    /**
     * Method to write a cell to the output file
     * @param cellBackup the cell to write
     */
    void writeCell(CellBackup cellBackup) {
        ImmutableCell d = cellBackup.d;
        LibId libId = d.getLibId();
        // write the Cell name
        printWriter.println();
        printWriter.println("# Cell " + d.cellId.cellName);
        printWriter.print("C" + convertString(d.cellId.cellName.toString()));
        if (!oldRevision) {
            printWriter.print("|");
            String cellGroupName = d.groupName.getName();
            if (!cellGroupName.equals(d.cellId.cellName.getName()))
                printWriter.print(convertString(cellGroupName));
        }
        printWriter.print("|" + convertString(d.tech.getTechName()));
        printWriter.print("|" + d.creationDate);
        printWriter.print("|" + cellBackup.revisionDate);
        StringBuilder cellBits = new StringBuilder();
        if ((d.flags & Cell.INCELLLIBRARY) != 0) cellBits.append("C");
        if ((d.flags & Cell.WANTNEXPAND) != 0 || d.cellId.cellName.getView() == View.ICON) cellBits.append("E");
        if ((d.flags & Cell.NPILOCKED) != 0) cellBits.append("I");
        if ((d.flags & Cell.NPLOCKED) != 0) cellBits.append("L");
        if ((d.flags & Cell.TECEDITCELL) != 0) cellBits.append("T");
        printWriter.print("|" + cellBits.toString());
        printlnVars(d);
        
        ArrayList<String> nodeNames = new ArrayList<String>();
        // write the nodes in this cell (sorted by node name)
        Name prevNodeName = null;
        int duplicate = 0;
        for (ImmutableNodeInst n: cellBackup.nodes) {
            NodeProtoId np = n.protoId;
            if (np instanceof CellId) {
                CellId subCellId = (CellId)np;
                String subCellName = subCellId.libId == libId ? subCellId.cellName.toString() : subCellId.toString();
                printWriter.print("I" + convertString(subCellName));
            } else {
                PrimitiveNode prim = (PrimitiveNode)np;
                if (d.tech == prim.getTechnology())
                    printWriter.print("N" + convertString(prim.getName()));
                else
                    printWriter.print("N" + convertString(prim.getFullName()));
            }
            String diskNodeName;
            if (n.name != prevNodeName) {
                prevNodeName = n.name;
                duplicate = 0;
                diskNodeName = convertString(n.name.toString());
            } else {
                duplicate++;
                diskNodeName = "\"" + convertQuotedString(n.name.toString()) + "\"" + duplicate;
            }
            int nodeId = n.nodeId;
            while (nodeId >= nodeNames.size()) nodeNames.add(null);
            nodeNames.set(nodeId, diskNodeName);
            printWriter.print("|" + diskNodeName + "|");
            if (!n.name.isTempname())
                printWriter.print(describeDescriptor(null, n.nameDescriptor));
            printWriter.print("|" + TextUtils.formatDouble(n.anchor.getX(), 0));
            printWriter.print("|" + TextUtils.formatDouble(n.anchor.getY(), 0));
            if (!(np instanceof CellId)) {
                EPoint size = getSizeCorrector(((PrimitiveNode)np).getTechnology()).getSizeToDisk(n);
                double lambdaWidth = size.getLambdaX();
                double lambdaHeight = size.getLambdaY();
                printWriter.print("|");
				if (lambdaWidth != 0)
					printWriter.print(TextUtils.formatDouble(lambdaWidth, 0));
                printWriter.print("|");
				if (lambdaHeight != 0)
					printWriter.print(TextUtils.formatDouble(lambdaHeight, 0));
//                double lambdaWidth = n.size.getLambdaX();
//                double lambdaHeight = n.size.getLambdaY();
//                if (NEW_REVISION && !oldRevision) {
//                    SizeOffset so = ((PrimitiveNode)np).getProtoSizeOffset();
//                    lambdaWidth = DBMath.round(lambdaWidth - so.getLowXOffset() - so.getHighXOffset());
//                    lambdaHeight = DBMath.round(lambdaHeight - so.getLowYOffset() - so.getHighYOffset());
//                }
//                printWriter.print("|" + TextUtils.formatDouble(lambdaWidth, 0));
//                printWriter.print("|" + TextUtils.formatDouble(lambdaHeight, 0));
            }
            printWriter.print('|');
            if (n.orient.isXMirrored()) printWriter.print('X');
            if (n.orient.isYMirrored()) printWriter.print('Y');
            int angle = n.orient.getAngle() % 3600;
            if (angle == 900 || angle == -2700) printWriter.print("R");
            else if (angle == 1800 || angle == -1800) printWriter.print("RR");
            else if (angle == 2700 || angle == -900) printWriter.print("RRR");
            else if (angle != 0) printWriter.print(angle);
            StringBuilder nodeBits = new StringBuilder();
            if (n.is(ImmutableNodeInst.HARD_SELECT)) nodeBits.append("A");
            if (n.is(ImmutableNodeInst.LOCKED)) nodeBits.append("L");
            if (n.is(ImmutableNodeInst.VIS_INSIDE)) nodeBits.append("V");
            int ts = n.techBits;
            if (ts != 0) nodeBits.append(ts);
            printWriter.print("|" + nodeBits.toString());
            if (np instanceof CellId) {
                String tdString = describeDescriptor(null, n.protoDescriptor);
                printWriter.print("|" + tdString);
            }
            printlnVars(n);
        }
        
        // write the arcs in this cell
        for (ImmutableArcInst a: cellBackup.arcs) {
            ArcProto ap = a.protoType;
            if (cellBackup.d.tech == ap.getTechnology())
                printWriter.print("A" + convertString(ap.getName()));
            else
                printWriter.print("A" + convertString(ap.getFullName()));
            printWriter.print("|" + convertString(a.name.toString()) + "|");
            if (!a.name.isTempname())
                printWriter.print(describeDescriptor(null, a.nameDescriptor));
            long arcWidth = getSizeCorrector(a.protoType.getTechnology()).getWidthToDisk(a);
            printWriter.print("|");
            if (arcWidth != 0)
                printWriter.print(TextUtils.formatDouble(DBMath.gridToLambda(arcWidth), 0));
            
            StringBuilder arcBits = new StringBuilder();
            
            if (a.is(ImmutableArcInst.HARD_SELECT)) arcBits.append("A");
            if (a.is(ImmutableArcInst.BODY_ARROWED)) arcBits.append("B");
            if (!a.is(ImmutableArcInst.FIXED_ANGLE)) arcBits.append("F");
            if (a.is(ImmutableArcInst.HEAD_NEGATED)) arcBits.append("G");
            if (!a.is(ImmutableArcInst.HEAD_EXTENDED)) arcBits.append("I");
            if (!a.is(ImmutableArcInst.TAIL_EXTENDED)) arcBits.append("J");
            if (a.is(ImmutableArcInst.TAIL_NEGATED)) arcBits.append("N");
            if (a.is(ImmutableArcInst.RIGID)) arcBits.append("R");
            if (a.is(ImmutableArcInst.SLIDABLE)) arcBits.append("S");
            if (a.is(ImmutableArcInst.HEAD_ARROWED)) arcBits.append("X");
            if (a.is(ImmutableArcInst.TAIL_ARROWED)) arcBits.append("Y");
            printWriter.print("|" + arcBits.toString() + a.getAngle());
            
            printWriter.print("|" + nodeNames.get(a.headNodeId) + "|" + getPortName(a.headPortId));
            printWriter.print("|" + TextUtils.formatDouble(a.headLocation.getX(), 0));
            printWriter.print("|" + TextUtils.formatDouble(a.headLocation.getY(), 0));
            
            printWriter.print("|" + nodeNames.get(a.tailNodeId) + "|" + getPortName(a.tailPortId));
            printWriter.print("|" + TextUtils.formatDouble(a.tailLocation.getX(), 0));
            printWriter.print("|" + TextUtils.formatDouble(a.tailLocation.getY(), 0));
            
            printlnVars(a);
        }
        
        // write the exports in this cell
        for (ImmutableExport e: cellBackup.exports) {
            printWriter.print("E" + convertString(e.exportId.externalId));
            if (!oldRevision) {
                printWriter.print("|");
                if (!e.name.toString().equals(e.exportId.externalId))
                    printWriter.print(convertString(e.name.toString()));
            }
            printWriter.print("|" + describeDescriptor(null, e.nameDescriptor));
            printWriter.print("|" + nodeNames.get(e.originalNodeId) + "|" + getPortName(e.originalPortId));
            printWriter.print("|" + e.characteristic.getShortName());
            if (e.alwaysDrawn) printWriter.print("/A");
            if (e.bodyOnly) printWriter.print("/B");
            printlnVars(e);
        }
        
        // write the end-of-cell marker
        printWriter.println("X");
    }
    
    void writeExternalLibraryInfo(LibId thisLib, Set<LibId> usedLibs) {
        // write external library information
        boolean libraryHeaderPrinted = false;
        TreeMap<String,LibId> sortedLibraries = new TreeMap<String,LibId>(TextUtils.STRING_NUMBER_ORDER);
        for (LibId libId: usedLibs)
            sortedLibraries.put(libId.libName, libId);
        for (LibId libId: sortedLibraries.values()) {
            if (libId == thisLib) continue;
            if (!libraryHeaderPrinted) {
                printWriter.println();
                printWriter.println("# External Libraries:");
                libraryHeaderPrinted = true;
            }
            String libFile = libId.libName;
            printWriter.println();
            printWriter.println("L" + convertString(libId.libName) + "|" + convertString(libFile));
        }
    }
    
    /**
     * Method to convert a variable to a string that describes its TextDescriptor
     * @param var the Variable being described (may be null).
     * @param td the TextDescriptor being described.
     * @return a String describing the variable/textdescriptor.
     * The string has these fields:
     *    Asize; for absolute size
     *    B if bold
     *    Cindex; if color index
     *    Dx for display position (2=bottom 8=top 4=left 6=right 7=upleft 9=upright 1=downleft 3=downright 5=centered 0=boxed)
     *    FfontName; if a nonstandard font
     *    Gsize; for relative (grid unit) size
     *    H if inherit
     *    I if italic
     *    L if underline
     *    N if name=value;
     *    Ol for language (J=Java L=Lisp T=TCL)
     *    P if parameter
     *    R/RR/RRR if rotated (90, 180, 270)
     *    T if interior
     *    Ux for units (R=resistance C=capacitance I=inductance A=current V=voltage D=distance T=time)
     *    Xoffset; for X offset
     *    Yoffset; for Y offset
     */
    private String describeDescriptor(Variable var, TextDescriptor td) {
        StringBuffer ret = new StringBuffer();
        boolean display = false;
        if (var == null || td.isDisplay()) display = true;
        
        if (display) {
            // write size
            TextDescriptor.Size size = td.getSize();
            if (size.isAbsolute()) ret.append("A" + (int)size.getSize() + ";");
            
            // write bold
            if (td.isBold()) ret.append("B");
            
            // write color
            int color = td.getColorIndex();
            if (color != 0)
                ret.append("C" + color + ";");
            
            // displayable: write display position
            ret.append("D");
            TextDescriptor.Position pos = td.getPos();
            if (pos == TextDescriptor.Position.UP) ret.append("8"); else
                if (pos == TextDescriptor.Position.DOWN) ret.append("2"); else
                    if (pos == TextDescriptor.Position.LEFT) ret.append("4"); else
                        if (pos == TextDescriptor.Position.RIGHT) ret.append("6"); else
                            if (pos == TextDescriptor.Position.UPLEFT) ret.append("7"); else
                                if (pos == TextDescriptor.Position.UPRIGHT) ret.append("9"); else
                                    if (pos == TextDescriptor.Position.DOWNLEFT) ret.append("1"); else
                                        if (pos == TextDescriptor.Position.DOWNRIGHT) ret.append("3"); else
                                            if (pos == TextDescriptor.Position.BOXED) ret.append("0"); else
                                                ret.append("5");
            
            // write font
            int font = td.getFace();
            if (font != 0) {
                TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(font);
                ret.append("F" + convertString(af.toString()) + ";");
            }
            
            if (!size.isAbsolute()) ret.append("G" + TextUtils.formatDouble(size.getSize()) + ";");
        }
        
        // write inherit
        if (td.isInherit()) ret.append("H");
        
        if (display) {
            // write italic
            if (td.isItalic()) ret.append("I");
            
            // write underline
            if (td.isUnderline()) ret.append("L");
            
            // write display type
            TextDescriptor.DispPos dispPos = td.getDispPart();
            if (dispPos == TextDescriptor.DispPos.NAMEVALUE) ret.append("N");
        }
        
        // write language
        if (var != null && td.isCode()) {
            Object value = var.getObject();
            if (value instanceof String || value instanceof String[]) {
                TextDescriptor.Code codeType = td.getCode();
                if (codeType == TextDescriptor.Code.JAVA) ret.append("OJ"); else
                    if (codeType == TextDescriptor.Code.SPICE) ret.append("OL"); else
                        if (codeType == TextDescriptor.Code.TCL) ret.append("OT");
            }
        }
        
        // write parameter
        if (var != null && td.isParam()) ret.append("P");
        
        if (display) {
            // write rotation
            TextDescriptor.Rotation rot = td.getRotation();
            if (rot == TextDescriptor.Rotation.ROT90) ret.append("R"); else
                if (rot == TextDescriptor.Rotation.ROT180) ret.append("RR"); else
                    if (rot == TextDescriptor.Rotation.ROT270) ret.append("RRR");
        }
        
        // write interior
        if (td.isInterior()) ret.append("T");
        
        // write units
        TextDescriptor.Unit unit = td.getUnit();
        if (unit == TextDescriptor.Unit.RESISTANCE) ret.append("UR"); else
            if (unit == TextDescriptor.Unit.CAPACITANCE) ret.append("UC"); else
                if (unit == TextDescriptor.Unit.INDUCTANCE) ret.append("UI"); else
                    if (unit == TextDescriptor.Unit.CURRENT) ret.append("UA"); else
                        if (unit == TextDescriptor.Unit.VOLTAGE) ret.append("UV"); else
                            if (unit == TextDescriptor.Unit.DISTANCE) ret.append("UD"); else
                                if (unit == TextDescriptor.Unit.TIME) ret.append("UT");
        
        if (display) {
            // write offset
            double offX = td.getXOff();
            if (offX != 0) ret.append("X" + TextUtils.formatDouble(offX, 0) + ";");
            double offY = td.getYOff();
            if (offY != 0) ret.append("Y" + TextUtils.formatDouble(offY, 0) + ";");
        }
        
        return ret.toString();
    }
    
    /**
     * Method to write the variables on an ImmutableElectricObject.
     * If object is ImmuatbleNodeInst write variables on its PortInsts also.
     */
    private void printlnVars(ImmutableElectricObject d) {
        // write the variables
        printVars(null, d);
        if (d instanceof ImmutableNodeInst) {
            ImmutableNodeInst nid = (ImmutableNodeInst)d;
//            for (Iterator<PortProtoId> it = nid.getPortsWithVariables(); it.hasNext(); ) {
//                PortProtoId portId = it.next();
//                ImmutablePortInst pid = nid.getPortInst(portId);
//                String portName;
//                if (portId instanceof ExportId) {
//                    ExportId exportId = (ExportId)portId;
//                    portName = getExportName(snapshot.getCell(exportId.parentId).getExport(exportId));
//                } else {
//                    portName = ((PrimitivePort)portId).getName();
//                }
//                printVars(portName, pid);
//            }
            if (nid.hasPortInstVariables()) {
                if (nid.protoId instanceof CellId) {
                    ArrayList<ExportId> exportsWithVariables = new ArrayList<ExportId>();
                    for (Iterator<PortProtoId> it = nid.getPortsWithVariables(); it.hasNext(); ) {
                        ExportId exportId = (ExportId)it.next();
                        exportsWithVariables.add(exportId);
                    }
                    Collections.sort(exportsWithVariables, EXPORTS_BY_EXTERNAL_ID);
                    for (ExportId exportId: exportsWithVariables)
                        printVars(exportId.externalId, nid.getPortInst(exportId));
                } else {
                    PrimitiveNode pn = (PrimitiveNode)nid.protoId;
                    for (int portIndex = 0; portIndex < pn.getNumPorts(); portIndex++) {
                        PrimitivePort pp = pn.getPort(portIndex);
                        ImmutablePortInst pid = nid.getPortInst(pp);
                        if (pid.getNumVariables() == 0) continue;
                        printVars(getPortName(pp), pid);
//                        printVars(pp.getName(), pid);
                    }
                }
            }
        }
        printWriter.println();
    }
    
    private static Comparator<ExportId> EXPORTS_BY_EXTERNAL_ID = new Comparator<ExportId>() {
        public int compare(ExportId e1, ExportId e2) {
            return TextUtils.STRING_NUMBER_ORDER.compare(e1.externalId, e2.externalId);
        }
    };
    
    /**
     * Method to write the variables on an object.
     */
    private void printVars(String portName, ImmutableElectricObject d) {
        // write the variables
        for(Iterator<Variable> it = d.getVariables(); it.hasNext(); ) {
            Variable var = it.next();
            Object varObj = var.getObject();
            String tdString = describeDescriptor(var, var.getTextDescriptor());
            printWriter.print("|" + convertVariableName(diskName(portName, var)) + "(" + tdString + ")");
            String pt = makeString(varObj);
            if (pt == null) pt = "";
            printWriter.print(pt);
        }
    }
    
    /**
     * Method to write the project settings on an object.
     */
    private void printlnSettings(List<Setting> settings) {
        for (Setting setting : settings) {
            Object value = setting.getValue();
            projectSettings.put(setting, value);
            printWriter.print("|" + convertVariableName(setting.getPrefName()) + "()" + makeString(value));
        }
        printWriter.println();
    }
    
    /**
     * Method to convert variable "var" to a string for printing in the text file.
     * returns zero on error
     */
    private String makeString(Object obj) {
        StringBuffer infstr = new StringBuffer();
        char type = getVarType(obj);
        infstr.append(type);
        if (obj instanceof Object[]) {
            Object [] objArray = (Object [])obj;
            int len = objArray.length;
            infstr.append('[');
            for(int i=0; i<len; i++) {
                Object oneObj = objArray[i];
                if (i != 0) infstr.append(',');
                makeStringVar(infstr, type, oneObj, true);
            }
            infstr.append(']');
        } else {
            makeStringVar(infstr, type, obj, false);
        }
        return infstr.toString();
    }
    
    /**
     * Method to make a string from the value in "addr" which has a type in
     * "type".
     */
    private void makeStringVar(StringBuffer infstr, char type, Object obj, boolean inArray) {
        if (obj == null) return;
        switch (type) {
            case 'B': infstr.append(((Boolean)obj).booleanValue() ? 'T' : 'F'); return;
            case 'C': infstr.append(convertString(((CellId)obj).toString(), inArray)); return;
            case 'D': infstr.append(((Double)obj).doubleValue()); return;
            case 'E': infstr.append(convertString(((ExportId)obj).toString(), inArray)); return;
            case 'F': infstr.append(((Float)obj).floatValue()); return;
            case 'G': infstr.append(((Long)obj).longValue()); return;
            case 'H': infstr.append(((Short)obj).shortValue()); return;
            case 'I': infstr.append(((Integer)obj).intValue()); return;
            case 'L': infstr.append(convertString(((LibId)obj).libName, inArray)); return;
            case 'O': infstr.append(convertString(((Tool)obj).getName(), inArray)); return;
            case 'P': infstr.append(convertString(((PrimitiveNode)obj).getFullName(), inArray)); return;
            case 'R': infstr.append(convertString(((ArcProto)obj).getFullName(), inArray)); return;
            case 'S': infstr.append(convertString((String)obj, inArray)); return;
            case 'T': infstr.append(convertString(((Technology)obj).getTechName(), inArray)); return;
            case 'V': {
                EPoint pt2 = (EPoint)obj;
                infstr.append(TextUtils.formatDouble(pt2.getX(), 0) + "/" + TextUtils.formatDouble(pt2.getY(), 0));
                return;
            }
            case 'Y': infstr.append(((Byte)obj).byteValue()); return;
        }
    }
    
    private String getPortName(PortProtoId portId) {
        if (portId instanceof PrimitivePort) {
            PrimitivePort pp = (PrimitivePort)portId;
            return pp.getParent().getNumPorts() > 1 ? pp.getName() : "";
        }
        ExportId exportId = (ExportId)portId;
        return convertString(exportId.externalId);
    }
    
    private Technology.SizeCorrector getSizeCorrector(Technology tech) {
        Technology.SizeCorrector corrector = sizeCorrectors.get(tech);
        if (corrector == null) {
            corrector = tech.getSizeCorrector(version, projectSettings, true, false);
            sizeCorrectors.put(tech, corrector);
        }
        return corrector;
    }
    
    /**
     * Method to make a char from the value in "addr" which has a type in
     * "type".
     */
    private char getVarType(Object obj) {
        if (obj instanceof String        || obj instanceof String [])        return 'S';
        
        if (obj instanceof Boolean       || obj instanceof Boolean [])       return 'B';
        if (obj instanceof CellId        || obj instanceof CellId [])        return 'C';
        if (obj instanceof Double        || obj instanceof Double [])        return 'D';
        if (obj instanceof ExportId      || obj instanceof ExportId [])      return 'E';
        if (obj instanceof Float         || obj instanceof Float [])         return 'F';
        if (obj instanceof Long          || obj instanceof Long [])          return 'G';
        if (obj instanceof Short         || obj instanceof Short [])         return 'H';
        if (obj instanceof Integer       || obj instanceof Integer [])       return 'I';
        if (obj instanceof LibId         || obj instanceof LibId [])         return 'L';
        if (obj instanceof Tool          || obj instanceof Tool [])          return 'O';
        if (obj instanceof PrimitiveNode || obj instanceof PrimitiveNode []) return 'P';
        if (obj instanceof ArcProto      || obj instanceof ArcProto [])      return 'R';
        if (obj instanceof Technology    || obj instanceof Technology [])    return 'T';
        if (obj instanceof EPoint        || obj instanceof EPoint [])        return 'V';
        if (obj instanceof Byte          || obj instanceof Byte [])          return 'Y';
        assert false : obj;
        return 'X';
    }
    
    /**
     * Method convert a string that is going to be quoted.
     * Inserts the quote character (^) before any quotation character (') or quote character (^) in the string.
     * Converts newlines to "^\n" (makeing the "\" and "n" separate characters).
     * @param str the string to convert.
     * @return the string with the appropriate quote characters.
     * If no conversion is necessary, the input string is returned.
     */
    private String convertQuotedString(String str) {
        StringBuffer infstr = new StringBuffer();
        int len = str.length();
        for(int i=0; i<len; i++) {
            char ch = str.charAt(i);
            if (ch == '\n') { infstr.append("\\n");   continue; }
            if (ch == '\r') { infstr.append("\\r");   continue; }
            if (ch == '"' || ch == '\\')
                infstr.append('\\');
            infstr.append(ch);
        }
        return infstr.toString();
    }
    
    /**
     * Method convert a string that is not going to be quoted.
     * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
     * Converts newlines to spaces.
     * @param str the string to convert.
     * @return the string with the appropriate quote characters.
     * If no conversion is necessary, the input string is returned.
     */
    String convertString(String str) {
        return convertString(str, (char)0, (char)0);
    }
    
    /**
     * Method convert a string that is not going to be quoted.
     * Inserts a quote character (^) before any separator (|), quotation character (") or quote character (^) in the string.
     * Converts newlines to spaces.
     * @param str the string to convert.
     * @return the string with the appropriate quote characters.
     * If no conversion is necessary, the input string is returned.
     */
    private String convertString(String str, char delim1, char delim2) {
        if (str.length() != 0 &&
                str.indexOf('\n') < 0 &&
                str.indexOf('\r') < 0 &&
                str.indexOf('\\') < 0 &&
                str.indexOf('"') < 0 &&
                str.indexOf('|') < 0 &&
                (delim1 == 0 || str.indexOf(delim1) < 0) &&
                (delim2 == 0 || str.indexOf(delim2) < 0))
            return str;
        return '"' + convertQuotedString(str) + '"';
    }
    
    /**
     * Method convert a string that is a variable name.
     * If string contains end-of-lines, backslashes, bars, quotation characters, open parenthesis,
     * encloses string in quotational characters.
     * @param str the string to convert.
     * @return the string with the appropriate quote characters.
     * If no conversion is necessary, the input string is returned.
     */
    private String convertVariableName(String str) {
        return convertString(str, '(', (char)0);
    }
    
    /**
     * Method convert a string that is a variable value.
     * If string contains end-of-lines, backslashes, bars, quotation characters, comma,
     * close bracket, returns string enclosed in quotational characters.
     * @param str the string to convert.
     * @param inArray true if string is element of array/
     * @return the string with the appropriate quote characters.
     * If no conversion is necessary, the input string is returned.
     */
    private String convertString(String str, boolean inArray) {
        return inArray ? convertString(str, ',', ']') : convertString(str, (char)0, (char)0);
    }
}
