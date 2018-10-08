package com.sun.electric.tool.generator.sclibrary;

import java.awt.Color;
import java.util.*;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.generator.layout.GateLayoutGenerator;
import com.sun.electric.tool.generator.layout.StdCellParams;
import com.sun.electric.tool.generator.layout.TechType;
//import com.sun.electric.plugins.sctiming.SCRun;
//import com.sun.electric.plugins.menus.PowerAnalysis;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Nov 15, 2006
 * Time: 3:39:44 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * Generate a standard cell library from purple and red libraries
 */
public class SCLibraryGen {

    private String purpleLibraryName = "purpleFour";
    private String redLibraryName = "redFour";
    private String scLibraryName = "sclib";
    private Library purpleLibrary;
    private Library redLibrary;
    private Library scLibrary;
    private List<StdCellSpec> scellSpecs = new ArrayList<StdCellSpec>();
    private PrimitiveNode pin = Generic.tech.invisiblePinNode;
    private Variable.Key sizeKey = Variable.findKey("ATTR_X");

    public static final Variable.Key STANDARDCELL = Variable.newKey("ATTR_StandardCell");

    private static final int blueColorIndex = EGraphics.makeIndex(Color.blue);

    public SCLibraryGen() {}

    private static class StdCellSpec {
        private String type;
        private double [] sizes;
        private StdCellSpec(String type, double [] sizes) {
            this.type = type;
            this.sizes = sizes;
        }
    }

    /* =======================================================
     * Settings
     * ======================================================= */

    /**
     * Set the names of the purple and red libraries. These must
     * be loaded when running the generation, and are used
     * as templates for the schematics and icons of standard cells.
     * @param purpleLibraryName
     * @param redLibraryName
     */
    public void setPurpleRedLibs(String purpleLibraryName, String redLibraryName) {
        this.purpleLibraryName = purpleLibraryName;
        this.redLibraryName = redLibraryName;
    }

    /**
     * Set the name of the output standard cell library.
     * Defaults to "sclib".
     * @param name
     */
    public void setOutputLibName(String name) {
        this.scLibraryName = name;
    }

    /**
     * Add command to generate the standard cell type
     * for the given space-separated list of sizes.
     * @param type
     * @param sizes
     */
    public void addStandardCell(String type, String sizes) {
        sizes = sizes.trim();
        if (sizes.equals("")) return;
        String [] ss = sizes.split("\\s+");
        double [] sss = new double [ss.length];
        for (int i=0; i<ss.length; i++) {
            sss[i] = Double.parseDouble(ss[i]);
        }
        scellSpecs.add(new StdCellSpec(type, sss));
    }

    /**
     * Generates the standard cell library
     * @param sc standard cell parameters
     * @return false on error, true otherwise
     */
    public boolean generate(StdCellParams sc) {
        // check for red and purple libraries
        purpleLibrary = Library.findLibrary(purpleLibraryName);
        if (purpleLibrary == null) {
            prErr("Purple library \""+purpleLibraryName+"\" is not loaded.");
            return false;
        }
        redLibrary = Library.findLibrary(redLibraryName);
        if (redLibrary == null) {
            prErr("Red library \""+redLibraryName+"\" is not loaded.");
            return false;
        }
        prMsg("Using purple library \""+purpleLibraryName+"\" and red library \""+redLibraryName+"\"");

        if (sc.getTechnology() == TechType.TSMC180)
            scLibraryName = "sclibTSMC180";
        else if (sc.getTechnology() == TechType.CMOS90)
            scLibraryName = "sclibCMOS90";
        scLibrary = Library.findLibrary(scLibraryName);
        if (scLibrary == null) {
            scLibrary = Library.newInstance(scLibraryName, null);
            prMsg("Created standard cell library "+scLibraryName);
        }
        prMsg("Using standard cell library "+scLibraryName);

        // dunno how to set standard cell params
        sc.enableNCC(purpleLibraryName);

        for (StdCellSpec stdcell : scellSpecs) {
            for (double d : stdcell.sizes) {

                String cellname = sc.sizedName(stdcell.type, d);
                cellname = cellname.substring(0, cellname.indexOf('{'));

                // generate layout first
                Cell laycell = scLibrary.findNodeProto(cellname+"{lay}");
                if (laycell == null) {
                    laycell = GateLayoutGenerator.generateCell(scLibrary, sc, stdcell.type, d);
                    if (laycell == null) {
                        prErr("Error creating layout cell "+stdcell.type+" of size "+d);
                        continue;
                    }
                }
                // generate icon next
                Cell iconcell = scLibrary.findNodeProto(cellname+"{ic}");
                if (iconcell == null) {
                    copyIconCell(stdcell.type, purpleLibrary, cellname, scLibrary, d);
                }

                // generate sch last
                Cell schcell = scLibrary.findNodeProto(cellname+"{sch}");
                if (schcell == null) {
                    copySchCell(stdcell.type, purpleLibrary, cellname, scLibrary, d);
                }

                schcell = scLibrary.findNodeProto(cellname+"{sch}");
                // mark schematic as standard cell
                markStandardCell(schcell);
            }
        }
        return true;
    }

    private boolean copyIconCell(String name, Library lib, String toName, Library toLib, double size) {
        // check if icon already exists
        Cell iconcell = toLib.findNodeProto(toName+"{ic}");
        Cell fromIconCell = lib.findNodeProto(name+"{ic}");
        if (iconcell == null && fromIconCell != null) {
            iconcell = Cell.copyNodeProto(fromIconCell, toLib, toName, false);
            if (iconcell == null) {
                prErr("Unable to copy purple cell "+fromIconCell.describe(false)+" to library "+toLib);
                return false;
            }
            // add size text
            NodeInst sizeni = NodeInst.makeInstance(pin, new EPoint(0,0),
                    0, 0, iconcell);
            sizeni.newVar(Artwork.ART_MESSAGE, new Double(size),
                    TextDescriptor.getAnnotationTextDescriptor().withColorIndex(blueColorIndex));

            // change all arcs to blue
            for (Iterator<ArcInst> it = iconcell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ai.newVar(Artwork.ART_COLOR, new Integer(blueColorIndex));
            }
            for (Iterator<NodeInst> it = iconcell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ni.newVar(Artwork.ART_COLOR, new Integer(blueColorIndex));
            }
        }
        return true;
    }

    private boolean copySchCell(String name, Library lib, String toName, Library toLib, double size) {
        // check if sch already exists
        Cell schcell = toLib.findNodeProto(toName+"{sch}");
        Cell fromSchCell = lib.findNodeProto(name+"{sch}");
        if (schcell == null && fromSchCell != null) {
            schcell = Cell.copyNodeProto(fromSchCell, toLib, toName, false);
            if (schcell == null) {
                prErr("Unable to copy purple cell "+fromSchCell.describe(false)+" to library "+toLib);
                return false;
            }
            // replace master icon cell in schematic
            Cell iconcell = toLib.findNodeProto(toName+"{ic}");
            Cell fromIconCell = lib.findNodeProto(name+"{ic}");
            if (iconcell != null && fromIconCell != null) {
                for (Iterator<NodeInst> it = schcell.getNodes(); it.hasNext(); ) {
                    NodeInst ni = it.next();
                    if (ni.isCellInstance()) {
                        Cell np = (Cell)ni.getProto();
                        if (np == fromIconCell) {
                            ni.replace(iconcell, true, true);
                        }
                    }
                }
            }
            // remove 'X' attribute
            if (schcell.getVar(sizeKey) != null) {
                schcell.delVar(sizeKey);
            }
            // change X value on red gate
            for (Iterator<NodeInst> it = schcell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                if (ni.isCellInstance()) {
                    Cell np = (Cell)ni.getProto();
                    if (np.getLibrary() == redLibrary) {
                        Variable var = ni.getVar(sizeKey);
                        if (var != null) {
                            ni.newVar(sizeKey, new Double(size), var.getTextDescriptor());
                        }
                    }
                    if (np.isIconOf(schcell)) {
                        // remove size attribute
                        ni.delVar(sizeKey);
                    }
                }
            }
        }
        return true;
    }

    /* =======================================================
     * Utility
     * ======================================================= */

    /**
     * Mark the cell as a standard cell.
     * This version of the method performs the task in a Job.
     * @param cell the cell to mark with the standard cell attribute marker
     */
    public static void markStandardCellJob(Cell cell) {
//        PowerAnalysis.CreateVar job = new PowerAnalysis.CreateVar(cell, STANDARDCELL, new Integer(1));
//        job.startJob();
    }

    /**
     * Mark the cell as a standard cell
     * @param cell the cell to mark with the standard cell attribute marker
     */
    public static void markStandardCell(Cell cell) {
//        PowerAnalysis.CreateVar job = new PowerAnalysis.CreateVar(cell, STANDARDCELL, new Integer(1));
//        job.doIt();
    }

    /**
     * Return the standard cells in a hierarchy starting from
     * the specified top cell. Note this returns only schematic cells.
     * @param topCell the top cell in the hierarchy
     * @return a set of standard cells in the hierarchy
     */
    public static Set<Cell> getStandardCellsInHierarchy(Cell topCell) {
        Set<Cell> cells = new TreeSet<Cell>();
        if (topCell.getView() == View.ICON)
            topCell = topCell.getCellGroup().getMainSchematics();
        for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.isCellInstance()) {
                Cell subcell = (Cell)ni.getProto();
                if (subcell.isIconOf(topCell)) continue;
                if (isStandardCell(subcell.getCellGroup().getMainSchematics())) {
                    cells.add(subcell.getCellGroup().getMainSchematics());
                } else {
                    cells.addAll(getStandardCellsInHierarchy(subcell));
                }
            }
        }
        return cells;
    }

    /**
     * Returns true if the cell is marked as a standard cell for Static
     * Timing Analysis
     * @param cell the cell to check
     * @return true if standard cell, false otherwise
     */
    public static boolean isStandardCell(Cell cell) {
        return cell.getVar(STANDARDCELL) != null;
    }

    private void prErr(String msg) {
        System.out.println("Standard Cell Library Generator Error: "+msg);
    }
//    private void prWarn(String msg) {
//        System.out.println("Standard Cell Library Generator Warning: "+msg);
//    }
    private void prMsg(String msg) {
        System.out.println("Standard Cell Library Generator: "+msg);
    }
}
