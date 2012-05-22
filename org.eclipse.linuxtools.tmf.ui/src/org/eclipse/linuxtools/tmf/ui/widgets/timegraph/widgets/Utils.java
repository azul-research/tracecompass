/*****************************************************************************
 * Copyright (c) 2007, 2008 Intel Corporation, 2009, 2012 Ericsson.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Intel Corporation - Initial API and implementation
 *   Ruslan A. Scherbakov, Intel - Initial API and implementation
 *   Alvaro Sanchez-Leon - Udpated for TMF
 *   Patrick Tasse - Refactoring
 *
 *****************************************************************************/

package org.eclipse.linuxtools.tmf.ui.widgets.timegraph.widgets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

public class Utils {

    public enum TimeFormat {
        RELATIVE, ABSOLUTE
    };

    static public final int IMG_THREAD_RUNNING = 0;
    static public final int IMG_THREAD_SUSPENDED = 1;
    static public final int IMG_THREAD_STOPPED = 2;
    static public final int IMG_METHOD_RUNNING = 3;
    static public final int IMG_METHOD = 4;
    static public final int IMG_NUM = 5;

    static public final Object[] _empty = new Object[0];

    public static enum Resolution {
        SECONDS, MILLISEC, MICROSEC, NANOSEC
    };

    static private final SimpleDateFormat stimeformat = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$
    static private final SimpleDateFormat sdateformat = new SimpleDateFormat("yyyy-MM-dd"); //$NON-NLS-1$

    static Rectangle clone(Rectangle source) {
        return new Rectangle(source.x, source.y, source.width, source.height);
    }

    static public void init(Rectangle rect) {
        rect.x = 0;
        rect.y = 0;
        rect.width = 0;
        rect.height = 0;
    }

    static public void init(Rectangle rect, int x, int y, int width, int height) {
        rect.x = x;
        rect.y = y;
        rect.width = width;
        rect.height = height;
    }

    static public void init(Rectangle rect, Rectangle source) {
        rect.x = source.x;
        rect.y = source.y;
        rect.width = source.width;
        rect.height = source.height;
    }

    static public void deflate(Rectangle rect, int x, int y) {
        rect.x += x;
        rect.y += y;
        rect.width -= x + x;
        rect.height -= y + y;
    }

    static public void inflate(Rectangle rect, int x, int y) {
        rect.x -= x;
        rect.y -= y;
        rect.width += x + x;
        rect.height += y + y;
    }

    static void dispose(Color col) {
        if (null != col)
            col.dispose();
    }

    static public Color mixColors(Device display, Color c1, Color c2, int w1,
            int w2) {
        return new Color(display, (w1 * c1.getRed() + w2 * c2.getRed())
                / (w1 + w2), (w1 * c1.getGreen() + w2 * c2.getGreen())
                / (w1 + w2), (w1 * c1.getBlue() + w2 * c2.getBlue())
                / (w1 + w2));
    }

    static public Color getSysColor(int id) {
        Color col = Display.getCurrent().getSystemColor(id);
        return new Color(col.getDevice(), col.getRGB());
    }

    static public Color mixColors(Color col1, Color col2, int w1, int w2) {
        return mixColors(Display.getCurrent(), col1, col2, w1, w2);
    }

    static public int drawText(GC gc, String text, Rectangle rect, boolean transp) {
        Point size = gc.stringExtent(text);
        gc.drawText(text, rect.x, rect.y, transp);
        return size.x;
    }

    static public int drawText(GC gc, String text, int x, int y, boolean transp) {
        Point size = gc.stringExtent(text);
        gc.drawText(text, x, y, transp);
        return size.x;
    }

    /**
     * Formats time in format: MM:SS:NNN
     * 
     * @param time time
     * @param format  0: MMMM:ss:nnnnnnnnn, 1: HH:MM:ss MMM.mmmm.nnn
     * @param resolution the resolution
     * @return the formatted time
     */
    static public String formatTime(long time, TimeFormat format, Resolution resolution) {
        // if format is absolute (Calendar)
        if (format == TimeFormat.ABSOLUTE) {
            return formatTimeAbs(time, resolution);
        }

        StringBuffer str = new StringBuffer();
        boolean neg = time < 0;
        if (neg) {
            time = -time;
            str.append('-');
        }

        long sec = (long) (time * 1E-9);
        // TODO: Expand to make it possible to select the minute, second, nanosecond format
        //printing minutes is suppressed just sec and ns
        // if (sec / 60 < 10)
        // str.append('0');
        // str.append(sec / 60);
        // str.append(':');
        // sec %= 60;
        // if (sec < 10)
        // str.append('0');
        str.append(sec);
        String ns = formatNs(time, resolution);
        if (!ns.equals("")) { //$NON-NLS-1$
            str.append('.');
            str.append(ns);
        }

        return str.toString();
    }

    /**
     * From input time in nanoseconds, convert to Date format YYYY-MM-dd
     * 
     * @param absTime
     * @return the formatted date
     */
    public static String formatDate(long absTime) {
        String sdate = sdateformat.format(new Date((long) (absTime * 1E-6)));
        return sdate;
    }

    /**
     * Formats time in ns to Calendar format: HH:MM:SS MMM.mmm.nnn
     * 
     * @param time
     * @return the formatted time
     */
    static public String formatTimeAbs(long time, Resolution res) {
        StringBuffer str = new StringBuffer();

        // format time from nanoseconds to calendar time HH:MM:SS
        String stime = stimeformat.format(new Date((long) (time * 1E-6)));
        str.append(stime);
        str.append('.');
        // append the Milliseconds, MicroSeconds and NanoSeconds as specified in
        // the Resolution
        str.append(formatNs(time, res));
        return str.toString();
    }

    /**
     * Obtains the remainder fraction on unit Seconds of the entered value in
     * nanoseconds. e.g. input: 1241207054171080214 ns The number of fraction
     * seconds can be obtained by removing the last 9 digits: 1241207054 the
     * fractional portion of seconds, expressed in ns is: 171080214
     * 
     * @param time
     * @param res
     * @return the formatted nanosec
     */
    public static String formatNs(long time, Resolution res) {
        StringBuffer str = new StringBuffer();
        boolean neg = time < 0;
        if (neg) {
            time = -time;
        }

        // The following approach could be used although performance
        // decreases in half.
        // String strVal = String.format("%09d", time);
        // String tmp = strVal.substring(strVal.length() - 9);

        long ns = time;
        ns %= 1000000000;
        if (ns < 10) {
            str.append("00000000"); //$NON-NLS-1$
        } else if (ns < 100) {
            str.append("0000000"); //$NON-NLS-1$
        } else if (ns < 1000) {
            str.append("000000"); //$NON-NLS-1$
        } else if (ns < 10000) {
            str.append("00000"); //$NON-NLS-1$
        } else if (ns < 100000) {
            str.append("0000"); //$NON-NLS-1$
        } else if (ns < 1000000) {
            str.append("000"); //$NON-NLS-1$
        } else if (ns < 10000000) {
            str.append("00"); //$NON-NLS-1$
        } else if (ns < 100000000) {
            str.append("0"); //$NON-NLS-1$
        }
        str.append(ns);

        if (res == Resolution.MILLISEC) {
            return str.substring(0, 3);
        } else if (res == Resolution.MICROSEC) {
            return str.substring(0, 6);
        } else if (res == Resolution.NANOSEC) {
            return str.substring(0, 9);
        }
        return ""; //$NON-NLS-1$
    }

    static public int loadIntOption(String opt, int def, int min, int max) {
        // int val =
        // TraceUIPlugin.getDefault().getPreferenceStore().getInt(opt);
        // if (0 == val)
        // val = def;
        // if (val < min)
        // val = min;
        // if (val > max)
        // val = max;
        return def;
    }

    static public void saveIntOption(String opt, int val) {
        // TraceUIPlugin.getDefault().getPreferenceStore().setValue(opt, val);
    }

    static ITimeEvent getFirstEvent(ITimeGraphEntry thread) {
        if (null == thread)
            return null;
        Iterator<ITimeEvent> iterator = thread.getTimeEventsIterator();
        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }

    /**
     * N means: <list> <li>-1: Previous Event</li> <li>0: Current Event</li> <li>
     * 1: Next Event</li> <li>2: Previous Event when located in a non Event Area
     * </list>
     * 
     * @param thread
     * @param time
     * @param n
     * @return
     */
    static ITimeEvent findEvent(ITimeGraphEntry thread, long time, int n) {
        if (null == thread)
            return null;
        Iterator<ITimeEvent> iterator = thread.getTimeEventsIterator();
        if (iterator == null) {
            return null;
        }
        ITimeEvent nextEvent = null;
        ITimeEvent currEvent = null;
        ITimeEvent prevEvent = null;

        while (iterator.hasNext()) {
            nextEvent = (ITimeEvent) iterator.next();
            long nextStartTime = nextEvent.getTime();

            if (nextStartTime > time) {
                break;
            }

            if (currEvent == null || currEvent.getTime() != nextStartTime) {
                prevEvent = currEvent;
                currEvent = nextEvent;
            }
        }

        if (n == -1) { //previous
            if (currEvent != null && currEvent.getTime() + currEvent.getDuration() >= time) {
                return prevEvent;
            } else {
                return currEvent;
            }
        } else if (n == 0) { //current
            if (currEvent != null && currEvent.getTime() + currEvent.getDuration() >= time) {
                return currEvent;
            } else {
                return null;
            }
        } else if (n == 1) { //next
            return nextEvent;
        } else if (n == 2) { //current or previous when in empty space
            return currEvent;
        }

        return null;
    }

    static public String fixMethodSignature(String sig) {
        int pos = sig.indexOf('(');
        if (pos >= 0) {
            String ret = sig.substring(0, pos);
            sig = sig.substring(pos);
            sig = sig + " " + ret; //$NON-NLS-1$
        }
        return sig;
    }

    static public String restoreMethodSignature(String sig) {
        String ret = ""; //$NON-NLS-1$
        int pos = sig.indexOf('(');
        if (pos >= 0) {
            ret = sig.substring(0, pos);
            sig = sig.substring(pos + 1);
        }
        pos = sig.indexOf(')');
        if (pos >= 0) {
            sig = sig.substring(0, pos);
        }
        String args[] = sig.split(","); //$NON-NLS-1$
        StringBuffer result = new StringBuffer("("); //$NON-NLS-1$
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.length() == 0 && args.length == 1)
                break;
            result.append(getTypeSignature(arg));
        }
        result.append(")").append(getTypeSignature(ret)); //$NON-NLS-1$
        return result.toString();
    }

    static public String getTypeSignature(String type) {
        int dim = 0;
        for (int j = 0; j < type.length(); j++) {
            if (type.charAt(j) == '[')
                dim++;
        }
        int pos = type.indexOf('[');
        if (pos >= 0)
            type = type.substring(0, pos);
        StringBuffer sig = new StringBuffer(""); //$NON-NLS-1$
        for (int j = 0; j < dim; j++)
            sig.append("[");                 //$NON-NLS-1$
        if (type.equals("boolean"))     //$NON-NLS-1$
            sig.append("Z");                 //$NON-NLS-1$
        else if (type.equals("byte"))   //$NON-NLS-1$
            sig.append("B");                 //$NON-NLS-1$
        else if (type.equals("char"))   //$NON-NLS-1$
            sig.append("C");                 //$NON-NLS-1$
        else if (type.equals("short"))  //$NON-NLS-1$
            sig.append("S");                 //$NON-NLS-1$
        else if (type.equals("int"))    //$NON-NLS-1$
            sig.append("I");                 //$NON-NLS-1$
        else if (type.equals("long"))   //$NON-NLS-1$
            sig.append("J");                 //$NON-NLS-1$
        else if (type.equals("float"))  //$NON-NLS-1$
            sig.append("F");                 //$NON-NLS-1$
        else if (type.equals("double")) //$NON-NLS-1$
            sig.append("D");                 //$NON-NLS-1$
        else if (type.equals("void"))   //$NON-NLS-1$
            sig.append("V");                 //$NON-NLS-1$
        else
            sig.append("L").append(type.replace('.', '/')).append(";"); //$NON-NLS-1$ //$NON-NLS-2$
        return sig.toString();
    }

    static public int compare(double d1, double d2) {
        if (d1 > d2)
            return 1;
        if (d1 < d2)
            return 1;
        return 0;
    }

    static public int compare(String s1, String s2) {
        if (s1 != null && s2 != null)
            return s1.compareToIgnoreCase(s2);
        if (s1 != null)
            return 1;
        if (s2 != null)
            return -1;
        return 0;
    }
}
