/**********************************************************************
 * Copyright (c) 2017 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 **********************************************************************/

package org.eclipse.tracecompass.internal.provisional.tmf.core.model.filters;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * This represents a specialized query filter used by data some providers. In
 * addition to base query filters, it encapsulated the selected thread and
 * selected CPUs.
 *
 * @author Yonni Chen
 * @since 3.0
 */
public class SelectedCpuQueryFilter extends SelectedThreadQueryFilter implements IMultipleSelectionQueryFilter<Set<Integer>> {

    private final Set<Integer> fCpus;

    /**
     * Constructor. Given a start value, end value and n entries, this constructor
     * will set x values property to an array of n entries uniformly distributed and
     * ordered ascendingly.
     *
     * @param start
     *            The starting value
     * @param end
     *            The ending value
     * @param n
     *            The number of entries
     * @param selectedThread
     *            A selected thread
     * @param cpu
     *            The set of CPU
     */
    public SelectedCpuQueryFilter(long start, long end, int n, String selectedThread, Set<Integer> cpu) {
        super(start, end, n, selectedThread);
        fCpus = ImmutableSet.copyOf(cpu);
    }

    /**
     * Gets a set of selected CPUs
     *
     * @return A set of cpu id
     */
    @Override
    public Set<Integer> getSelectedItems() {
        return fCpus;
    }
}