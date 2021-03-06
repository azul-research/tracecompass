/*******************************************************************************
 * Copyright (c) 2017, 2019 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.lttng2.kernel.ui.swtbot.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.io.diskioactivity.DiskIOActivityView;
import org.eclipse.tracecompass.testtraces.ctf.CtfTestTrace;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.tests.shared.CtfTmfTestTraceUtils;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.XYDataProviderBaseTest;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.ConditionHelpers;
import org.eclipse.tracecompass.tmf.ui.swtbot.tests.shared.SWTBotUtils;
import org.eclipse.tracecompass.tmf.ui.tests.shared.WaitUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.linecharts.TmfCommonXAxisChartViewer;
import org.eclipse.ui.IViewPart;
import org.junit.Test;
import org.swtchart.Chart;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

/**
 * SWTBot tests for Disks Activity view
 *
 * @author Yonni Chen
 */
public class DisksIOViewTest extends XYDataProviderBaseTest {

    private static final RGB PURPLE = new RGB(255, 0, 255);
    private static final RGB GREEN = new RGB(0, 255, 0);

    private static final int NUMBER_OF_POINT = 50;
    private static final int MORE_POINTS = 100;

    private static final @NonNull String TITLE = "Disk I/O View";
    private static final @NonNull String READ_SERIES_NAME = "scp_dest/8,0/read";
    private static final @NonNull String WRITE_SERIES_NAME = "scp_dest/8,0/write";

    private static final @NonNull ITmfTimestamp ZOOM_START_TIME = TmfTimestamp.fromNanos(1361214078967381303L);
    private static final @NonNull ITmfTimestamp ZOOM_END_TIME = TmfTimestamp.fromNanos(1361214078967971599L);

    /**
     * Test to check the Disks IO Activity view. First, when trace opened, there
     * should not be any activity. Then, we move to a time range where there are
     * write activity. Afterward, we test the zoom
     */
    @Test
    public void testDiskView() {
        // Wait for analysis to finish.
        WaitUtils.waitForJobs();
        IViewPart viewPart = getSWTBotView().getViewReference().getView(true);
        assertTrue(viewPart instanceof DiskIOActivityView);
        final TmfCommonXAxisChartViewer chartViewer = (TmfCommonXAxisChartViewer) getChartViewer(viewPart);
        assertNotNull(chartViewer);
        fBot.waitUntil(ConditionHelpers.xyViewerIsReadyCondition(chartViewer));

        final Chart chart = getChart();
        assertNotNull(chart);
        SWTBotTreeItem[] items = getSWTBotView().bot().tree().getAllItems();
        for (SWTBotTreeItem item : items) {
            item.check();
        }

        SWTBotUtils.waitUntil(c -> c.getSeriesSet().getSeries().length > 0, chart, "No data available");
        chartViewer.setNbPoints(NUMBER_OF_POINT);

        /* Initially, no disk activity */
        SWTBotUtils.waitUntil(json -> isChartDataValid(chart, json, WRITE_SERIES_NAME), "resources/disk/disk0-res50.json", "Chart data is not valid");

        /* Change time range where there is disks activity */
        TmfSignalManager.dispatchSignal(new TmfWindowRangeUpdatedSignal(this, new TmfTimeRange(ZOOM_START_TIME, ZOOM_END_TIME)));
        fBot.waitUntil(ConditionHelpers.windowRange(new TmfTimeRange(ZOOM_START_TIME, ZOOM_END_TIME)));
        fBot.waitUntil(ConditionHelpers.xyViewerIsReadyCondition(chartViewer));

        /* Test type, style and color of series */
        verifyChartStyle();

        /* Test data model */
        SWTBotUtils.waitUntil(json -> isChartDataValid(chart, json, WRITE_SERIES_NAME), "resources/disk/disk1-res50.json", "Chart data is not valid");

        /* Change Zoom and number of points */
        chartViewer.setNbPoints(MORE_POINTS);

        /* Test type, style and color of series */
        verifyChartStyle();

        /* Test data model */
        SWTBotUtils.waitUntil(json -> isChartDataValid(chart, json, WRITE_SERIES_NAME), "resources/disk/disk2-res100.json", "Chart data is not valid");
    }

    private void verifyChartStyle() {
        verifySeriesStyle(READ_SERIES_NAME, ISeries.SeriesType.LINE, GREEN, LineStyle.SOLID, true);
        verifySeriesStyle(WRITE_SERIES_NAME, ISeries.SeriesType.LINE, PURPLE, LineStyle.SOLID, true);
    }

    @Override
    protected String getMainSeriesName() {
        return READ_SERIES_NAME;
    }

    @Override
    protected String getTitle() {
        return TITLE;
    }

    @Override
    protected String getViewID() {
        return DiskIOActivityView.ID;
    }

    @Override
    protected ITmfTrace getTestTrace() {
        return CtfTmfTestTraceUtils.getTrace(CtfTestTrace.SYNC_DEST);
    }

    @Override
    protected void disposeTestTrace() {
        CtfTmfTestTraceUtils.dispose(CtfTestTrace.SYNC_DEST);
    }
}