/*
 * Copyright 2019-2020 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.bwd.impl.utils.fonts;

import java.util.Arrays;
import java.util.NoSuchElementException;

import net.jolikit.lang.NbrsUtils;

/**
 * Immutable.
 */
public final class CodePointSet {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final CodePointSet DEFAULT_EMPTY = new CodePointSet(new int[0]);
    
    private final int[] minMaxCpArr;
    
    private final int rangeCount;
    
    private final int cpCount;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param minMaxCpArr Code points ranges in increasing order,
     *        as a suit of {min,max} values. Can be empty.
     * @throws IllegalArgumentException if the specified array is invalid.
     */
    public CodePointSet(int[] minMaxCpArr) {
        // Sanity check.
        if (!NbrsUtils.isEven(minMaxCpArr.length)) {
            throw new IllegalArgumentException("array length [" + minMaxCpArr.length + "] must be even");
        }
        final int rangeCount = minMaxCpArr.length / 2;
        
        int prevMaxCp = -2;
        
        int cpCount = 0;
        for (int i = 0; i < rangeCount; i++) {
            final int minCp = minMaxCpArr[2*i];
            final int maxCp = minMaxCpArr[2*i+1];
            
            // Sanity checks.
            if (!(minCp <= maxCp)) {
                throw new IllegalArgumentException("minCp [" + minCp + "] must be <= maxCp [" + maxCp + "]");
            }
            // At least one empty value between two ranges.
            if (!(minCp - 1 > prevMaxCp)) {
                throw new IllegalArgumentException("minCp - 1 [" + (minCp - 1) + "] must be > previous maxCp [" + prevMaxCp + "]");
            }
            
            prevMaxCp = maxCp;
            
            cpCount += (maxCp - minCp + 1);
        }
        
        this.minMaxCpArr = minMaxCpArr.clone();
        this.rangeCount = rangeCount;
        this.cpCount = cpCount;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.rangeCount; i++) {
            final int minCp = this.minMaxCpArr[2*i];
            final int maxCp = this.minMaxCpArr[2*i+1];
            if (i != 0) {
                sb.append(",");
            }
            sb.append("[");
            if (minCp == maxCp) {
                sb.append(NbrsUtils.toString(minCp, 16));
            } else {
                sb.append(NbrsUtils.toString(minCp, 16));
                sb.append(",");
                sb.append(NbrsUtils.toString(maxCp, 16));
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * @return The number of code points.
     */
    public int getCodePointCount() {
        return this.cpCount;
    }
    
    /**
     * @return The number of contiguous ranges of code points.
     */
    public int getRangeCount() {
        return this.rangeCount;
    }
    
    /**
     * @return The min code point.
     * @throws NoSuchElementException if this set is empty.
     */
    public int getMin() {
        if (this.rangeCount == 0) {
            throw new NoSuchElementException();
        }
        return this.getRangeMin(0);
    }

    /**
     * @return The max code point.
     * @throws NoSuchElementException if this set is empty.
     */
    public int getMax() {
        if (this.rangeCount == 0) {
            throw new NoSuchElementException();
        }
        return this.getRangeMax(this.rangeCount - 1);
    }

    /**
     * @param rangeIndex Must be in [0,getRangeCount()[.
     * @return The min code point of the range of specified index.
     * @throws IllegalArgumentException if the specified index is out of range.
     */
    public int getRangeMin(int rangeIndex) {
        NbrsUtils.checkIsInRange(0, this.rangeCount - 1, rangeIndex);
        return this.minMaxCpArr[2 * rangeIndex];
    }

    /**
     * @param rangeIndex Must be in [0,getRangeCount()[.
     * @return The max code point of the range of specified index.
     * @throws IllegalArgumentException if the specified index is out of range.
     */
    public int getRangeMax(int rangeIndex) {
        NbrsUtils.checkIsInRange(0, this.rangeCount - 1, rangeIndex);
        return this.minMaxCpArr[2 * rangeIndex + 1];
    }

    /**
     * @param codePoint A code point. Can be any int value.
     */
    public boolean contains(int codePoint) {
        final int[] arr = this.minMaxCpArr;
        
        if ((arr.length == 0)
                || (codePoint < arr[0])
                || (codePoint > arr[arr.length - 1])) {
            // No need to bother with binary search.
            return false;
        }

        final int binarySearchResult = Arrays.binarySearch(arr, codePoint);
        if (binarySearchResult >= 0) {
            // Code point is a range bound.
            return true;
        } else {
            // Can't be 0 due to early checks.
            final int insertionIndex = -(binarySearchResult+1);
            if (NbrsUtils.isOdd(insertionIndex)) {
                // Code point is in a range.
                return true;
            } else {
                // Code point is between two ranges.
                return false;
            }
        }
    }
}
