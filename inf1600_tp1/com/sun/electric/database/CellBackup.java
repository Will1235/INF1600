/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBackup.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import static com.sun.electric.database.UsageCollector.EMPTY_BITSET;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.prototype.NodeProtoId;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.ArrayIterator;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.BoundsBuilder;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Iterator;

/**
 *
 */
public class CellBackup {
    public static final CellBackup[] NULL_ARRAY = {};
    public static final ImmutableArrayList<CellBackup> EMPTY_LIST = new ImmutableArrayList<CellBackup>(NULL_ARRAY);

    private static final int[] NULL_INT_ARRAY = {};
    static final CellUsageInfo[] NULL_CELL_USAGE_INFO_ARRAY = {};
    static int cellBackupsCreated = 0;
    static int cellBackupsMemoized = 0;
    /** Cell persistent data. */                                    public final ImmutableCell d;
	/** The date this ImmutableCell was last modified. */           public final long revisionDate;
    /** "Modified" flag of the Cell. */                             public final boolean modified;
    /** An array of Exports on the Cell by chronological index. */  public final ImmutableArrayList<ImmutableExport> exports;
	/** A list of NodeInsts in this Cell. */						public final ImmutableArrayList<ImmutableNodeInst> nodes;
    /** A list of ArcInsts in this Cell. */							public final ImmutableArrayList<ImmutableArcInst> arcs;
    /** CellUsageInfos indexed by CellUsage.indefInParent */        final CellUsageInfo[] cellUsages;
    /** definedExport == [0..definedExportLength) - deletedExports . */
    /** Map from chronIndex of Exports to sortIndex. */             final int exportIndex[];
    /** Bitmap of defined exports. */                               final BitSet definedExports;
    /** Length of defined exports. */                               final int definedExportsLength;
    /** Bitmap of deleted exports. */                               final BitSet deletedExports;

    /** Memoized data for size computation (connectivity etc). */   private volatile Memoization m;
    /** Arc shrinkage data */                                       private AbstractShapeBuilder.Shrinkage shrinkage;
    /** Bounds of primitive arcs in this Cell. */                   private ERectangle primitiveBounds;
    
    /** Creates a new instance of CellBackup */
    private CellBackup(ImmutableCell d, long revisionDate, boolean modified,
            ImmutableArrayList<ImmutableNodeInst> nodes,
            ImmutableArrayList<ImmutableArcInst> arcs,
            ImmutableArrayList<ImmutableExport> exports,
            CellUsageInfo[] cellUsages, int[] exportIndex, BitSet definedExports, int definedExportsLength, BitSet deletedExports) {
        this.d = d;
        this.revisionDate = revisionDate;
        this.modified = modified;
        this.nodes = nodes;
        this.arcs = arcs;
        this.exports = exports;
        this.cellUsages = cellUsages;
        this.exportIndex = exportIndex;
        this.definedExports = definedExports;
        this.definedExportsLength = definedExportsLength;
        this.deletedExports = deletedExports;
        cellBackupsCreated++;
//        check();
    }
    
    /** Creates a new instance of CellBackup */
    public CellBackup(ImmutableCell d) {
        this(d, 0, false,
                ImmutableNodeInst.EMPTY_LIST, ImmutableArcInst.EMPTY_LIST, ImmutableExport.EMPTY_LIST,
                NULL_CELL_USAGE_INFO_ARRAY, NULL_INT_ARRAY, EMPTY_BITSET, 0, EMPTY_BITSET);
        if (d.tech == null)
            throw new NullPointerException("tech");
    }
    
    /**
     * Creates a new instance of CellBackup which differs from this CellBackup.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param d new persistent data of a cell.
     * @param revisionDate new revision date.
     * @param modified new modified flag
     * @param nodesArray new array of nodes
     * @param arcsArray new array of arcs
     * @param exportsArray new array of exports
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public CellBackup with(ImmutableCell d, long revisionDate, boolean modified,
            ImmutableNodeInst[] nodesArray, ImmutableArcInst[] arcsArray, ImmutableExport[] exportsArray) {
        ImmutableArrayList<ImmutableNodeInst> nodes = copyArray(nodesArray, this.nodes);
        ImmutableArrayList<ImmutableArcInst> arcs = copyArray(arcsArray, this.arcs);
        ImmutableArrayList<ImmutableExport> exports = copyArray(exportsArray, this.exports);
        if (this.d == d && this.revisionDate == revisionDate && this.modified == modified &&
                this.nodes == nodes && this.arcs == arcs && this.exports == exports) {
            return this;
        }
        
        CellId cellId = d.cellId;
        if (this.d != d) {
            if (d.tech == null)
                throw new NullPointerException("tech");
//            if (cellId != this.d.cellId)
//                throw new IllegalArgumentException("cellId");
        }
        
        CellUsageInfo[] cellUsages = this.cellUsages;
        if (this.d.cellId != d.cellId || this.d.getVars() != d.getVars() || nodes != this.nodes || arcs != this.arcs || exports != this.exports) {
            UsageCollector uc = new UsageCollector(d, nodes, arcs, exports);
            cellUsages = uc.getCellUsages(cellId, this.cellUsages);
        }
        
        if (nodes != this.nodes && !nodes.isEmpty()) {
            boolean hasCellCenter = false;
            ImmutableNodeInst prevN = null;
            for (int i = 0; i < nodes.size(); i++) {
                ImmutableNodeInst n = nodes.get(i);
                if (n.protoId == Generic.tech.cellCenterNode) {
                    if (hasCellCenter)
                        throw new IllegalArgumentException("Duplicate cell center");
                    hasCellCenter = true;
                }
                if (prevN != null && TextUtils.STRING_NUMBER_ORDER.compare(prevN.name.toString(), n.name.toString()) >= 0)
                    throw new IllegalArgumentException("nodes order");
                prevN = n;
            }
        }
        
        int[] exportIndex = this.exportIndex;
        BitSet definedExports = this.definedExports;
        int definedExportsLength = this.definedExportsLength;
        BitSet deletedExports = this.deletedExports;
        if (exports != this.exports) {
            int exportIndexLength = 0;
            String prevExportName = null;
            for (ImmutableExport e: exports) {
                if (e.exportId.parentId != cellId)
                    throw new IllegalArgumentException("exportId");
                String exportName = e.name.toString();
                if (prevExportName != null && TextUtils.STRING_NUMBER_ORDER.compare(prevExportName, exportName) >= 0)
                    throw new IllegalArgumentException("exportName");
                prevExportName = exportName;
                int chronIndex = e.exportId.chronIndex;
                exportIndexLength = Math.max(exportIndexLength, chronIndex + 1);
            }
            exportIndex = new int[exportIndexLength];
            Arrays.fill(exportIndex, -1);
            for (int portIndex = 0; portIndex < exports.size(); portIndex++) {
                ImmutableExport e = exports.get(portIndex);
                int chronIndex = e.exportId.chronIndex;
                if (exportIndex[chronIndex] >= 0)
                    throw new IllegalArgumentException("exportChronIndex");
                exportIndex[chronIndex] = portIndex;
                //checkPortInst(nodesById.get(e.originalNodeId), e.originalPortId);
            }
            if (Arrays.equals(this.exportIndex, exportIndex)) {
                exportIndex = this.exportIndex;
            } else {
                definedExports = new BitSet();
                for (int chronIndex = 0; chronIndex < exportIndex.length; chronIndex++) {
                    if (exportIndex[chronIndex] < 0) continue;
                    definedExports.set(chronIndex);
                }
                definedExports = UsageCollector.bitSetWith(this.definedExports, definedExports);
                if (definedExports != this.definedExports) {
                    definedExportsLength = definedExports.length();
                    deletedExports = new BitSet();
                    deletedExports.set(0, definedExportsLength);
                    deletedExports.andNot(definedExports);
                    deletedExports = UsageCollector.bitSetWith(this.deletedExports, deletedExports);
                }
            }
        }
        
        CellBackup backup = new CellBackup(d, revisionDate, modified,
                nodes, arcs, exports, cellUsages, exportIndex, definedExports, definedExportsLength, deletedExports);
        return backup;
    }
    
    private static <T> ImmutableArrayList<T> copyArray(T[] newArray, ImmutableArrayList<T> oldList) {
        return newArray != null ? new ImmutableArrayList<T>(newArray) : oldList;
    }
    
	/**
	 * Returns CellBackup which differs from this CellBackup by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return CellBackup with renamed Ids.
	 */
    CellBackup withRenamedIds(IdMapper idMapper, CellName newGroupName) {
        ImmutableCell d = this.d.withRenamedIds(idMapper).withGroupName(newGroupName);
        
        ImmutableNodeInst[] nodesArray = null;
        for (int i = 0; i < nodes.size(); i++) {
            ImmutableNodeInst oldNode = nodes.get(i);
            ImmutableNodeInst newNode = oldNode.withRenamedIds(idMapper);
            if (newNode != oldNode && nodesArray == null) {
                nodesArray = new ImmutableNodeInst[nodes.size()];
                for (int j = 0; j < i; j++)
                    nodesArray[j] = nodes.get(j);
            }
            if (nodesArray != null)
                nodesArray[i] = newNode;
        }
        
        ImmutableArcInst[] arcsArray = null;
        for (int i = 0; i < arcs.size(); i++) {
            ImmutableArcInst oldArc = arcs.get(i);
            ImmutableArcInst newArc = oldArc.withRenamedIds(idMapper);
            if (newArc != oldArc && arcsArray == null) {
                arcsArray = new ImmutableArcInst[arcs.size()];
                for (int j = 0; j < i; j++)
                    arcsArray[j] = arcs.get(j);
            }
            if (arcsArray != null)
                arcsArray[i] = newArc;
        }

        ImmutableExport[] exportsArray = null;
        for (int i = 0; i < exports.size(); i++) {
            ImmutableExport oldExport = exports.get(i);
            ImmutableExport newExport = oldExport.withRenamedIds(idMapper);
            if (newExport != oldExport && exportsArray == null) {
                exportsArray = new ImmutableExport[exports.size()];
                for (int j = 0; j < i; j++)
                    exportsArray[j] = exports.get(j);
            }
            if (exportsArray != null)
                exportsArray[i] = newExport;
        }
        
        if (this.d == d && nodesArray == null && arcsArray == null && exportsArray == null) return this;
        CellBackup newBackup = with(d, revisionDate, true, nodesArray, arcsArray, exportsArray);
        newBackup.check();
        return newBackup;
    }
    
    /**
     * Returns ImmutableNodeInst by its node id.
     * @param nodeId id of node.
     * @return ImmutableNodeInst with this id or null if node doesn't exist.
     */
    // FIX ME !!!
    public ImmutableNodeInst getNode(int nodeId) { return nodeId < nodes.size() ? nodes.get(nodeId) : null; }
    
    /**
     * Returns ImmutableArcInst by its arc id.
     * @param arcId id of node.
     * @return ImmutableArcInst with this id or null if node doesn't exist.
     */
    // FIX ME !!!
    public ImmutableArcInst getArc(int arcId) { return arcId < arcs.size() ? arcs.get(arcId) : null; }
    
    /**
     * Returns ImmutableExport by its export id.
     * @param exportId id of export.
     * @return ImmutableExport with this id or null if node doesn't exist.
     */
    public ImmutableExport getExport(ExportId exportId) {
        if (exportId.parentId != d.cellId)
            throw new IllegalArgumentException();
        int chronIndex = exportId.chronIndex;
        int portIndex = chronIndex < exportIndex.length ? exportIndex[chronIndex] : -1;
        return portIndex >= 0 ? exports.get(portIndex) : null;
    }
    
    /**
     * Returns subcell instance counts, indexed by CellUsage.indexInParent. 
     * @return subcell instance counts, indexed by CellUsage.indexInParent. 
     */
    public int[] getInstCounts() {
        int l = cellUsages.length;
        while (l > 0 && (cellUsages[l - 1] == null || cellUsages[l - 1].instCount == 0)) l--;
        if (l == 0) return NULL_INT_ARRAY;
        int[] instCounts = new int[l];
        for (int indexInParent = 0; indexInParent < l; indexInParent++) {
            if (cellUsages[indexInParent] != null)
                instCounts[indexInParent] = cellUsages[indexInParent].instCount;
        }
        return instCounts;
    }
    
    /**
     * For given CellUsage in this cell returns count of subcell instances.
     * @param u CellUsage.
     * @return count of subcell instances.
     * @throws IllegalArgumentException if CellUsage's parent is not this cell.
     */
    public int getInstCount(CellUsage u) {
        if (u.parentId != d.cellId)
            throw new IllegalArgumentException();
        if (u.indexInParent >= cellUsages.length) return 0;
        CellUsageInfo cui = cellUsages[u.indexInParent];
        if (cui == null) return 0;
        return cui.instCount;
    }

    /**
     * Writes this CellBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        d.write(writer);
        writer.writeLong(revisionDate);
        writer.writeBoolean(modified);
        writer.writeInt(nodes.size());
        for (ImmutableNodeInst n: nodes)
            n.write(writer);
        writer.writeInt(arcs.size());
        for (ImmutableArcInst a: arcs)
            a.write(writer);
        writer.writeInt(exports.size());
        for (ImmutableExport e: exports)
            e.write(writer);
    }
    
    /**
     * Reads CellBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellBackup read(SnapshotReader reader) throws IOException {
        ImmutableCell d = ImmutableCell.read(reader);
        long revisionDate = reader.readLong();
        boolean modified = reader.readBoolean();
        CellBackup backup = new CellBackup(d.withoutVariables());
        
        int nodesLength = reader.readInt();
        ImmutableNodeInst[] nodes = new ImmutableNodeInst[nodesLength];
        for (int i = 0; i < nodesLength; i++)
            nodes[i] = ImmutableNodeInst.read(reader);
        
        int arcsLength = reader.readInt();
        ImmutableArcInst[] arcs = new ImmutableArcInst[arcsLength];
        for (int i = 0; i < arcsLength; i++)
            arcs[i] = ImmutableArcInst.read(reader);
        
        int exportsLength = reader.readInt();
        ImmutableExport[] exports = new ImmutableExport[exportsLength];
        for (int i = 0; i < exportsLength; i++)
            exports[i] = ImmutableExport.read(reader);
        
        backup = backup.with(d, revisionDate, modified, nodes, arcs, exports);
        return backup;
    }

    /**
	 * Checks invariant of this CellBackup.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
        d.check();
        assert d.tech != null;
        CellId cellId = d.cellId;
        int[] checkCellUsages = getInstCounts();
        boolean hasCellCenter = false;
        ArrayList<ImmutableNodeInst> nodesById = new ArrayList<ImmutableNodeInst>();
        ImmutableNodeInst prevN = null;
        for (ImmutableNodeInst n: nodes) {
            n.check();
            if (n.protoId == Generic.tech.cellCenterNode) {
                assert !hasCellCenter;
                hasCellCenter = true;
            }
            while (n.nodeId >= nodesById.size()) nodesById.add(null);
            ImmutableNodeInst oldNode = nodesById.set(n.nodeId, n);
            assert oldNode == null;
			if (prevN != null)
				assert TextUtils.STRING_NUMBER_ORDER.compare(prevN.name.toString(), n.name.toString()) < 0;
            prevN = n;
            if (n.protoId instanceof CellId) {
                CellId subCellId = (CellId)n.protoId;
                CellUsage u = cellId.getUsageIn(subCellId);
                checkCellUsages[u.indexInParent]--;
                assert cellUsages[u.indexInParent] != null;
                for (int j = 0; j < n.ports.length; j++) {
                    ImmutablePortInst pid = n.ports[j];
                    if (pid == ImmutablePortInst.EMPTY) continue;
                    checkPortInst(n, subCellId.getPortId(j));
                }
            }
        }
        for (int i = 0; i < checkCellUsages.length; i++)
            assert checkCellUsages[i] == 0;
        BitSet arcIds = new BitSet();
        ImmutableArcInst prevA = null;
        for (ImmutableArcInst a: arcs) {
            assert !arcIds.get(a.arcId);
            arcIds.set(a.arcId);
			if (prevA != null) {
				int cmp = TextUtils.STRING_NUMBER_ORDER.compare(prevA.name.toString(), a.name.toString());
				assert cmp <= 0;
				if (cmp == 0) {
                    assert !a.name.isTempname();
					assert prevA.arcId < a.arcId;
                }
			}
            prevA = a;
            
            a.check();
            checkPortInst(nodesById.get(a.tailNodeId), a.tailPortId);
            checkPortInst(nodesById.get(a.headNodeId), a.headPortId);
        }
        
        if (exportIndex.length > 0)
            assert exportIndex[exportIndex.length - 1] >= 0;
        assert exportIndex.length == definedExportsLength;
        assert definedExports.length() == definedExportsLength;
        for (int i = 0; i < exports.size(); i++) {
            ImmutableExport e = exports.get(i);
            e.check();
            assert e.exportId.parentId == cellId;
            assert exportIndex[e.exportId.chronIndex] == i;
            if (i > 0)
                assert(TextUtils.STRING_NUMBER_ORDER.compare(exports.get(i - 1).name.toString(), e.name.toString()) < 0) : i;
            checkPortInst(nodesById.get(e.originalNodeId), e.originalPortId);
        }
        int exportCount = 0;
        for (int chronIndex = 0; chronIndex < exportIndex.length; chronIndex++) {
            int portIndex = exportIndex[chronIndex];
            if (portIndex == -1) {
                assert !definedExports.get(chronIndex);
                continue;
            }
            assert definedExports.get(chronIndex);
            exportCount++;
            assert exports.get(portIndex).exportId.chronIndex == chronIndex;
        }
        assert exports.size() == exportCount;
        BitSet checkDeleted = new BitSet();
        checkDeleted.set(0, definedExportsLength);
        checkDeleted.andNot(definedExports);
        assert deletedExports.equals(checkDeleted);
        if (definedExports.isEmpty())
            assert definedExports == EMPTY_BITSET;
        if (deletedExports.isEmpty())
            assert deletedExports == EMPTY_BITSET;
        for (CellUsageInfo cui: cellUsages) {
            if (cui != null)
                cui.check();
        }
        
        if (m != null)
            m.check();
    }
    
    private void checkPortInst(ImmutableNodeInst node, PortProtoId portId) {
        assert node != null;
        assert portId.getParentId() == node.protoId;
        if (portId instanceof ExportId)
            checkExportId((ExportId)portId);
    }
    
    private void checkExportId(ExportId exportId) {
        CellUsage u = d.cellId.getUsageIn(exportId.parentId);
        assert cellUsages[u.indexInParent].usedExports.get(exportId.getChronIndex());
    }
    
    public boolean sameExports(CellBackup thatBackup) {
        if (thatBackup == this) return true;
        if (exports.size() != thatBackup.exports.size())
            return false;
        for (int i = 0; i < exports.size(); i++) {
            if (exports.get(i).exportId != thatBackup.exports.get(i).exportId)
                return false;
        }
        return true;
    }
    
    static class CellUsageInfo {
        final int instCount;
        final BitSet usedExports;
        final int usedExportsLength;
        
        CellUsageInfo(int instCount, BitSet usedExports) {
            this.instCount = instCount;
            usedExportsLength = usedExports.length();
            this.usedExports = usedExportsLength > 0 ? usedExports : EMPTY_BITSET;
        }
        
        CellUsageInfo with(int instCount, BitSet usedExports) {
            usedExports = UsageCollector.bitSetWith(this.usedExports, usedExports);
            if (this.instCount == instCount && this.usedExports == usedExports) return this;
            return new CellUsageInfo(instCount, usedExports);
        }
        
        void checkUsage(CellBackup subCellBackup) {
            if (subCellBackup == null)
                throw new IllegalArgumentException("subCell deleted");
            if (subCellBackup.definedExportsLength < usedExportsLength || subCellBackup.deletedExports.intersects(usedExports))
                throw new IllegalArgumentException("exportUsages");
        }
        
        private void check() {
            assert instCount >= 0;
            assert usedExportsLength == usedExports.length();
            if (usedExportsLength == 0)
                assert usedExports == EMPTY_BITSET;
        }
    }
    
    @Override
    public String toString() { return d.toString(); }

    /**
     * Returns data for size computation (connectivity etc).
     * @return data for size computation.
     */
    public Memoization getMemoization() {
        Memoization m = this.m;
        if (m != null) return m;
        return this.m = new Memoization();
    }
    
    /**
     * Returns data for arc shrinkage computation.
     * @return data for arc shrinkage computation.
     */
    public AbstractShapeBuilder.Shrinkage getShrinkage() {
        if (shrinkage == null)
            shrinkage = new AbstractShapeBuilder.Shrinkage(this);
        return shrinkage;
    }
    
    /**
     * Returns bounds of all primitive arcs in this Cell or null if there are not primitives.
     * @return bounds of all primitive arcs or null.
     */
    public ERectangle getPrimitiveBounds() {
        ERectangle primitiveBounds = this.primitiveBounds;
        if (primitiveBounds != null) return primitiveBounds;
        return this.primitiveBounds = computePrimitiveBounds();
    }
    
    public ERectangle computePrimitiveBounds() {
        if (arcs.isEmpty()) return null;
        int intMinX = Integer.MAX_VALUE, intMinY = Integer.MAX_VALUE, intMaxX = Integer.MIN_VALUE, intMaxY = Integer.MIN_VALUE;
        int[] intCoords = new int[4];
        AbstractShapeBuilder.Shrinkage shrinkage = getShrinkage();
        BoundsBuilder boundsBuilder = new BoundsBuilder(shrinkage);
        for (ImmutableArcInst a: arcs) {
            if (a.genBoundsEasy(shrinkage, intCoords)) {
                int x1 = intCoords[0];
                if (x1 < intMinX) intMinX = x1;
                int y1 = intCoords[1];
                if (y1 < intMinY) intMinY = y1;
                int x2 = intCoords[2];
                if (x2 > intMaxX) intMaxX = x2;
                int y2 = intCoords[3];
                if (y2 > intMaxY) intMaxY = y2;
                continue;
            }
            boundsBuilder.genShapeOfArc(a);
        }
        ERectangle bounds = boundsBuilder.makeBounds();
        if (bounds == null) {
            assert intMinX <= intMaxX && intMinY <= intMaxY;
            int iw = intMaxX - intMinX;
            int ih = intMaxY - intMinY;
            return ERectangle.fromGrid(intMinX, intMinY,
                    iw >= 0 ? iw : (long)intMaxX - (long)intMinX,
                    ih >= 0 ? ih : (long)intMaxY - (long)intMinY);
        }
        if (intMinX > intMaxX)
            return bounds;
        long longMinX = Math.min(bounds.getGridMinX(), intMinX);
        long longMinY = Math.min(bounds.getGridMinY(), intMinY);
        long longMaxX = Math.max(bounds.getGridMaxX(), intMaxX);
        long longMaxY = Math.max(bounds.getGridMaxY(), intMaxY);
        return ERectangle.fromGrid(longMinX, longMinY, longMaxX - longMinX, longMaxY - longMinY);
    }
    
    /**
     * Class which memoizes data for size computation (connectivity etc).
     */
    public class Memoization {
//        /**
//         * ImmutableNodeInsts accessed by their nodeId.
//         */
//        private final int[] nodesById;
        public final int[] connections;
        /** ImmutableExports sorted by original PortInst. */
        private final ImmutableExport[] exportIndexByOriginalPort;
        private final BitSet wiped;
        
        Memoization() {
            cellBackupsMemoized++;
            int maxNodeId = -1;
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++)
                maxNodeId = Math.max(maxNodeId, nodes.get(nodeIndex).nodeId);
//            int[] nodesById = new int[maxNodeId + 1];
//            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
//                ImmutableNodeInst n = nodes.get(nodeIndex);
//                nodesById[n.nodeId] = nodeIndex;
//            }
//            this.nodesById = nodesById;

            int[] connections = new int[arcs.size()*2];
            for (int i = 0; i < connections.length; i++) connections[i] = i;
            sortConnections(connections, 0, connections.length - 1);
            this.connections = connections;
            
            ImmutableExport[] exportIndexByOriginalPort = exports.toArray(new ImmutableExport[exports.size()]);
            Arrays.sort(exportIndexByOriginalPort, BY_ORIGINAL_PORT);
            this.exportIndexByOriginalPort = exportIndexByOriginalPort;
            
            BitSet wiped = new BitSet();
            for (ImmutableArcInst a: arcs) {
                // wipe status
                if (a.protoType.isWipable()) {
                    wiped.set(a.tailNodeId);
                    wiped.set(a.headNodeId);
                }
            }
//            short[] shrink = new short[maxNodeId + 1];
            for (int nodeIndex = 0; nodeIndex < nodes.size(); nodeIndex++) {
                ImmutableNodeInst n = nodes.get(nodeIndex);
                NodeProtoId np = n.protoId;
                if (!(np instanceof PrimitiveNode && ((PrimitiveNode)np).isArcsWipe()))
                    wiped.clear(n.nodeId);
            }
            this.wiped = wiped;
        }
        
       /**
         * Returns true of there are Exports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return true if there are Exports on specified NodeInst.
         */
        public boolean hasExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            if (startIndex >= exportIndexByOriginalPort.length) return false;
            ImmutableExport e = exportIndexByOriginalPort[startIndex];
            return e.originalNodeId == originalNodeId;
        }
        
        /**
         * Method to return the number of Exports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return the number of Exports on specified NodeInst.
         */
        public int getNumExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            int j = startIndex;
            for (; j < exportIndexByOriginalPort.length; j++) {
                ImmutableExport e = exportIndexByOriginalPort[j];
                if (e.originalNodeId != originalNodeId) break;
            }
            return j - startIndex;
        }
        
        /**
         * Method to return an Iterator over all ImmutableExports on specified NodeInst.
         * @param originalNodeId nodeId of specified NodeInst.
         * @return an Iterator over all ImmutableExports on specified NodeInst.
         */
        public Iterator<ImmutableExport> getExports(int originalNodeId) {
            int startIndex = searchExportByOriginalPort(originalNodeId, 0);
            int j = startIndex;
            for (; j < exportIndexByOriginalPort.length; j++) {
                ImmutableExport e = exportIndexByOriginalPort[j];
                if (e.originalNodeId != originalNodeId) break;
            }
            return ArrayIterator.iterator(exportIndexByOriginalPort, startIndex, j);
        }
        
        private int searchExportByOriginalPort(int originalNodeId, int originalChronIndex) {
            int low = 0;
            int high = exportIndexByOriginalPort.length-1;
            while (low <= high) {
                int mid = (low + high) >> 1; // try in a middle
                ImmutableExport e = exportIndexByOriginalPort[mid];
                int cmp = e.originalNodeId - originalNodeId;
                if (cmp == 0)
                    cmp = e.originalPortId.getChronIndex() >= originalChronIndex ? 1 : -1;
                
                if (cmp < 0)
                    low = mid + 1;
                else
                    high = mid - 1;
            }
            return low;
        }
        
        public int searchConnectionByPort(int nodeId, int chronIndex) {
            int low = 0;
            int high = connections.length-1;
            while (low <= high) {
                int mid = (low + high) >> 1; // try in a middle
                int con = connections[mid];
                ImmutableArcInst a = arcs.get(con >>> 1);
                boolean end = (con & 1) != 0;
                int endNodeId = end ? a.headNodeId : a.tailNodeId;
                int cmp = endNodeId - nodeId;
                if (cmp == 0) {
                    PortProtoId portId = end ? a.headPortId : a.tailPortId;
                    cmp = portId.getChronIndex() - chronIndex;
                }
                
                if (cmp < 0)
                    low = mid + 1;
                else
                    high = mid - 1;
            }
            return low;
        }
        
        public ImmutableArrayList<ImmutableArcInst> getArcs() { return arcs; }
        
        /**
         * Method to tell whether the specified ImmutableNodeInst is wiped.
         * Wiped ImmutableNodeInsts are erased.  Typically, pin ImmutableNodeInsts can be wiped.
         * This means that when an arc connects to the pin, it is no longer drawn.
         * In order for a ImmutableNodeInst to be wiped, its prototype must have the "setArcsWipe" state,
         * and the arcs connected to it must have "setWipable" in their prototype.
         * @param nodeId nodeId of specified ImmutableNodeInst
         * @return true if specified ImmutableNodeInst is wiped.
         */
        public boolean isWiped(int nodeId) {
            return wiped.get(nodeId);
        }
        
        /**
         * Checks invariant of this CellBackup.
         * @throws AssertionError if invariant is broken.
         */
        private void check() {
            assert exportIndexByOriginalPort.length == exports.size();
            ImmutableExport prevE = null;
            for (ImmutableExport e: exportIndexByOriginalPort) {
                if (prevE != null)
                    assert BY_ORIGINAL_PORT.compare(prevE, e) < 0;
                assert e == exports.get(exportIndex[e.exportId.chronIndex]);
                prevE = e;
            }
            assert connections.length == arcs.size()*2;
            for (int i = 1; i < connections.length; i++)
                assert compareConnections(connections[i - 1], connections[i]) < 0;
        }

        private void sortConnections(int[] connections, int l, int r) {
            while (l < r) {
                int x = connections[(l + r) >>> 1];
                int i = l, j = r;
                do {
                    while (compareConnections(connections[i], x) < 0) i++;
                    while (compareConnections(x, connections[j]) < 0) j--;
                    if (i <= j) {
                        int w = connections[i];
                        connections[i] = connections[j];
                        connections[j] = w;
                        i++; j--;
                    }
                } while (i <= j);
                if (j - l < r - i) {
                    sortConnections(connections, l, j);
                    l = i;
                } else {
                    sortConnections(connections, i, r);
                    r = j;
                }
            }
        }
        
        private int compareConnections(int con1, int con2) {
            ImmutableArcInst a1 = arcs.get(con1 >>> 1);
            ImmutableArcInst a2 = arcs.get(con2 >>> 1);
            boolean end1 = (con1 & 1) != 0;
            boolean end2 = (con2 & 1) != 0;
            int nodeId1 = end1 ? a1.headNodeId : a1.tailNodeId;
            int nodeId2 = end2 ? a2.headNodeId : a2.tailNodeId;
            int cmp = nodeId1 - nodeId2;
            if (cmp != 0) return cmp;
            PortProtoId portId1 = end1 ? a1.headPortId : a1.tailPortId;
            PortProtoId portId2 = end2 ? a2.headPortId : a2.tailPortId;
            cmp = portId1.getChronIndex() - portId2.getChronIndex();
            if (cmp != 0) return cmp;
            return con1 - con2;
        }
    }
    
    private static final Comparator<ImmutableExport> BY_ORIGINAL_PORT = new Comparator<ImmutableExport>() {
        public int compare(ImmutableExport e1, ImmutableExport e2) {
            int result = e1.originalNodeId - e2.originalNodeId;
            if (result != 0) return result;
            result = e1.originalPortId.getChronIndex() - e2.originalPortId.getChronIndex();
            if (result != 0) return result;
            return e1.exportId.chronIndex - e2.exportId.chronIndex;
        }
    };
 }
