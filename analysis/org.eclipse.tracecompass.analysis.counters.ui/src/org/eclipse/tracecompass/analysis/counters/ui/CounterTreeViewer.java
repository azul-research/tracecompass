/*******************************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.counters.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.tracecompass.analysis.counters.core.CounterAnalysis;
import org.eclipse.tracecompass.internal.analysis.counters.ui.CounterTreeViewerEntry;
import org.eclipse.tracecompass.internal.analysis.counters.ui.TriStateFilteredCheckboxTree;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceOpenedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.ILegendImageProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.AbstractTmfTreeViewer;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ICheckboxTreeViewerListener;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeColumnDataProvider;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.ITmfTreeViewerEntry;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeColumnData;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

/**
 * Display the state system as a filtered checkbox tree:
 *
 * <pre>
 * {trace name}
 *   +- Grouped
 *   |   +- {group id}
 *   |   |   +- {group element}
 *   |   |       +- ...
 *   |   +- {group id}
 *   |       +- ...
 *   +- Ungrouped
 *       +- {counter}
 *       +- ...
 * </pre>
 *
 * @author Matthew Khouzam
 * @author Mikael Ferland
 */
public class CounterTreeViewer extends AbstractTmfTreeViewer {

    private static final ViewerComparator COMPARATOR = new ViewerComparator() {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            TmfTreeViewerEntry entry1 = (TmfTreeViewerEntry) e1;
            TmfTreeViewerEntry entry2 = (TmfTreeViewerEntry) e2;
            String name1 = entry1.getName();
            String name2 = entry2.getName();
            Long longValue1 = Longs.tryParse(name1);
            Long longValue2 = Longs.tryParse(name2);

            return (longValue1 == null || longValue2 == null) ? name1.compareTo(name2) : longValue1.compareTo(longValue2);
        }
    };

    private final class CounterTreeLabelProvider extends TreeLabelProvider {

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 1 && element instanceof CounterTreeViewerEntry && fCheckboxTree.getChecked(element)) {
                /* If the image height match the row height, row height will increment */
                int imageHeight = getTreeViewer().getTree().getItemHeight() - 1;
                String name = ((CounterTreeViewerEntry) element).getFullPath();
                if (fLegendImageProvider != null) {
                    return fLegendImageProvider.getLegendImage(imageHeight, fLegendColumnWidth, name);
                }
                return null;
            }
            return null;
        }
    }

    private final class CheckStateChangedListener implements ICheckStateListener {
        @Override
        public void checkStateChanged(CheckStateChangedEvent event) {
            if (fCounterChartViewer != null) {
                fCounterChartViewer.handleCheckStateChangedEvent(getCheckedCounterEntries());

                // Legend image might have changed
                refresh();
            }
        }
    }

    private ICheckboxTreeViewerListener fCounterChartViewer;
    private TriStateFilteredCheckboxTree fCheckboxTree;
    private ILegendImageProvider fLegendImageProvider;
    private Map<ITmfTrace, Object[]> fViewContext = new HashMap<>();
    private Map<ITmfTrace, TmfTreeViewerEntry> fRoots = new HashMap<>();
    private final int fLegendColumnWidth;

    /**
     * Constructor
     *
     * @param parent
     *            Parent composite
     * @param checkboxTree
     *            <code>TriStateFilteredTree</code> wrapping a
     *            <code>CheckboxTreeViewer</code>
     */
    public CounterTreeViewer(Composite parent, TriStateFilteredCheckboxTree checkboxTree) {
        super(parent, checkboxTree.getViewer());

        TreeViewer treeViewer = checkboxTree.getViewer();
        treeViewer.setComparator(COMPARATOR);
        if (treeViewer instanceof CheckboxTreeViewer) {
            ((CheckboxTreeViewer) treeViewer).addCheckStateListener(new CheckStateChangedListener());
        }
        fCheckboxTree = checkboxTree;
        setLabelProvider(new CounterTreeLabelProvider());

        /* Legend column is at index 1 */
        TreeColumn legend = getTreeViewer().getTree().getColumn(1);
        legend.pack();
        fLegendColumnWidth = legend.getWidth() != 0 ? legend.getWidth() : 50;
    }

    /**
     * @param listener
     *            Chart listening to changes in the tree's selected entries
     */
    public void setTreeListener(ICheckboxTreeViewerListener listener) {
        fCounterChartViewer = listener;
    }

    /**
     * @param legendImageProvider
     *            Provides an image legend associated with a name
     */
    public void setLegendImageProvider(ILegendImageProvider legendImageProvider) {
        fLegendImageProvider = legendImageProvider;
    }

    @Override
    public void initializeDataSource() {
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }

        if (fRoots.containsKey(trace)) {
            // Return if the trace has already been initialized
            return;
        }

        TmfTreeViewerEntry root = new TmfTreeViewerEntry(StringUtils.EMPTY);
        fRoots.put(trace, root);

        Iterable<@NonNull CounterAnalysis> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, CounterAnalysis.class);
        for (CounterAnalysis module : modules) {

            ITmfTrace moduleTrace = module.getTrace();
            if (moduleTrace == null) {
                continue;
            }

            TmfTreeViewerEntry rootBranch = new TmfTreeViewerEntry(moduleTrace.getName());
            root.addChild(rootBranch);
            module.schedule();
            module.waitForCompletion();

            ITmfStateSystem stateSystem = module.getStateSystem();
            if (stateSystem != null) {
                /*
                 * Add grouped and ungrouped counters branches along with their entries (if
                 * applicable).
                 */
                addTreeViewerBranch(stateSystem, rootBranch, CounterAnalysis.GROUPED_COUNTER_ASPECTS_ATTRIB, moduleTrace.getUUID());
                addTreeViewerBranch(stateSystem, rootBranch, CounterAnalysis.UNGROUPED_COUNTER_ASPECTS_ATTRIB, moduleTrace.getUUID());
            }
        }
    }

    /**
     * Select previously checked entries when going back to trace.
     */
    @Override
    protected void contentChanged(ITmfTreeViewerEntry rootEntry) {
        Object[] checkedElements = fViewContext.get(getTrace());
        fCheckboxTree.setCheckedElements(checkedElements != null ? checkedElements : new Object[0]);

        if (fCounterChartViewer != null) {
            fCounterChartViewer.handleCheckStateChangedEvent(getCheckedCounterEntries());
        }
        getTreeViewer().refresh();
    }

    @Override
    protected ITmfTreeColumnDataProvider getColumnDataProvider() {
        return () -> {
            List<TmfTreeColumnData> columns = new ArrayList<>();
            TmfTreeColumnData column = new TmfTreeColumnData("Counters"); //$NON-NLS-1$
            columns.add(column);
            column = new TmfTreeColumnData("Legend"); //$NON-NLS-1$
            columns.add(column);
            return columns;
        };
    }

    @Override
    protected ITmfTreeViewerEntry updateElements(long start, long end, boolean isSelection) {
        /*
         * The tree displaying the trace's counters does not change when manipulating
         * the corresponding chart.
         */
        return fRoots.get(getTrace());
    }

    @TmfSignalHandler
    @Override
    public void traceOpened(@Nullable TmfTraceOpenedSignal signal) {
        saveViewContext();
        super.traceOpened(signal);
    }

    @TmfSignalHandler
    @Override
    public void traceSelected(@Nullable TmfTraceSelectedSignal signal) {
        if (signal != null && getTrace() != signal.getTrace()) {
            saveViewContext();
        }
        super.traceSelected(signal);
    }

    @TmfSignalHandler
    @Override
    public void traceClosed(@Nullable TmfTraceClosedSignal signal) {
        if (signal != null) {
            fViewContext.remove(signal.getTrace());
            fRoots.remove(signal.getTrace());
        }
        super.traceClosed(signal);
    }

    /**
     * Save the checked entries in the view context before changing trace.
     */
    private void saveViewContext() {
        ITmfTrace previousTrace = getTrace();
        Object[] checkedElements = fCheckboxTree.getCheckedElements();
        if (previousTrace != null) {
            fViewContext.put(previousTrace, checkedElements);
        }
    }

    private void addTreeViewerBranch(ITmfStateSystem stateSystem, TmfTreeViewerEntry rootBranch, String branchName, UUID traceID) {
        int quark = stateSystem.optQuarkAbsolute(branchName);
        if (quark != ITmfStateSystem.INVALID_ATTRIBUTE && !stateSystem.getSubAttributes(quark, false).isEmpty()) {
            TmfTreeViewerEntry branch = new TmfTreeViewerEntry(branchName);
            rootBranch.addChild(branch);
            addTreeViewerEntries(branch, quark, traceID, stateSystem);
        }
    }

    /**
     * Recursively add all child entries of a parent branch from the state system.
     */
    private void addTreeViewerEntries(TmfTreeViewerEntry parentBranch, int quark, UUID traceID, ITmfStateSystem ss) {
        for (int childQuark : ss.getSubAttributes(quark, false)) {
            TmfTreeViewerEntry childBranch;
            if (ss.getSubAttributes(childQuark, false).isEmpty()) {
                String fullPath = retrieveTraceName(parentBranch) + '/' + ss.getFullAttributePath(childQuark);
                childBranch = new CounterTreeViewerEntry(childQuark, ss.getAttributeName(childQuark), fullPath, traceID);
            } else {
                childBranch = new TmfTreeViewerEntry(ss.getAttributeName(childQuark));
            }

            parentBranch.addChild(childBranch);
            addTreeViewerEntries(childBranch, childQuark, traceID, ss);
        }
    }

    /**
     * Retrieve the name of the trace associated to an entry through recursion.
     */
    private String retrieveTraceName(ITmfTreeViewerEntry entry) {
        if (entry.getParent().getParent() == null) {
            // The child of the hidden root entry contains the trace name
            return entry.getName();
        }
        return retrieveTraceName(entry.getParent());
    }

    private Collection<ITmfTreeViewerEntry> getCheckedCounterEntries() {
        Object[] checkedElements = fCheckboxTree.getCheckedElements();
        return Lists.newArrayList(Iterables.filter(Arrays.asList(checkedElements), ITmfTreeViewerEntry.class));
    }

}