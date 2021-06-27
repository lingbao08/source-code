/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.buffer;

import static io.netty.buffer.PoolThreadCache.*;

/**
 * SizeClasses requires {@code pageShifts} to be defined prior to inclusion,
 * and it in turn defines:
 *
 * SizeClasses要求在包含之前定义{@code pageshifts}，然后定义：
 *
 * <p>
 *   LOG2_SIZE_CLASS_GROUP: Log of size class count for each size doubling.
 *   LOG2_MAX_LOOKUP_SIZE: Log of max size class in the lookup table.
 *   sizeClasses: Complete table of [index, log2Group, log2Delta, nDelta, isMultiPageSize,
 *                 isSubPage, log2DeltaLookup] tuples.
 *     index: Size class index.
 *     log2Group: Log of group base size (no deltas added).
 *     log2Delta: Log of delta to previous size class.
 *     nDelta: Delta multiplier.
 *     isMultiPageSize: 'yes' if a multiple of the page size, 'no' otherwise.
 *     isSubPage: 'yes' if a subpage size class, 'no' otherwise.
 *     log2DeltaLookup: Same as log2Delta if a lookup table size class, 'no'
 *                      otherwise.
 *
 *
 *      LOG2_SIZE_CLASS_GROUP: 每一个大小加倍的尺寸类别的和的对数
 *      LOG2_MAX_LOOKUP_SIZE: 查找表中最大尺寸类别的的对数
 *      sizeClasses: 完成表的元组： [index, log2Group, log2Delta, nDelta, isMultiPageSize,
 *                    isSubPage, log2DeltaLookup] .
 *        index: 规格类目的下标.
 *        log2Group: 基于尺寸的组的对数（无添加差值）
 *        log2Delta: 差值以前的尺寸类别的对数。
 *        nDelta: 差值倍数。
 *        isMultiPageSize: 'yes'表示是多页page，否则就是'no'。
 *        isSubPage: 'yes' 是子页尺寸类别。否则就是'no'。
 *        log2DeltaLookup: 如果是查找表中尺寸类别，那么和 log2Delta 是一样的。否则就是'no'。
 *
 *
 * <p>
 *   nSubpages: Number of subpages size classes.
 *   nSizes: Number of size classes.
 *   nPSizes: Number of size classes that are multiples of pageSize.
 *
 *   smallMaxSizeIdx: Maximum small size class index.
 *
 *   lookupMaxclass: Maximum size class included in lookup table.
 *   log2NormalMinClass: Log of minimum normal size class.
 *
 *
 *
 *   nSubpages: 子页尺寸类别的数目（下表中子页的总行数）.给small类型用的。
 *   nSizes: 尺寸类别的总数目（下表的总行数）. 给normal类型用的。nSizes-nSubpages就全是normal类型的。
 *
 *   nPSizes: pageSize的倍数的尺寸类别的数目.
 *   smallMaxSizeIdx: 最大的小尺寸类别的索引。
 *   lookupMaxclass: 表中最大尺寸类别的类目。
 *
 *   smallMaxSizeIdx 可以用来判断是否是small类型的buf。
 *   如果是一个normal类型的索引Idx - smallMaxSizeIdx ，就可以在{@code io.netty.buffer.PoolThreadCache#normalHeapCaches}
 *   或{@code io.netty.buffer.PoolThreadCache#normalDirectCaches}中通过下标来找到对应型号的cache。
 *   如果是一个small类型的索引Idx，就可以直接在{@code io.netty.buffer.PoolThreadCache#smallHeapCaches}
 *   或{@code io.netty.buffer.PoolThreadCache#smallDirectCaches}中通过下标来找到对应型号的cache。
 *
 *   而这个下标是可以通过容量值在下面的表中查询出来的。
 *
 *
 *   log2NormalMinClass: Log of minimum normal size class.最小的一般大小尺寸类别的对数。
 *
 *
 *
 * <p>
 *   The first size class and spacing are 1 << LOG2_QUANTUM.
 *   Each group has 1 << LOG2_SIZE_CLASS_GROUP of size classes.
 *
 *   size = 1 << log2Group + nDelta * (1 << log2Delta)
 *
 *   The first size class has an unusual encoding, because the size has to be
 *   split between group and delta*nDelta.
 *
 *
 *   第一个尺寸等级和间距是 1<<4 = 16.
 *   每一个组有 4个尺寸的类目。
 *
 *   size = 1 << log2Group + nDelta * (1 << log2Delta)
 *   第一个尺寸等级有特殊的编码，因为这个尺寸不得不分割开组 和 差值*nDelta。
 *
 *
 *   If pageShift = 13, sizeClasses looks like this:
 *   如果pageShift = 13,sizeClasses看起来是这样的：
 *
 *   (index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup)
 * <p>
 *   ( 0,     4,        4,         0,       no,             yes,        4)
 *   ( 1,     4,        4,         1,       no,             yes,        4)
 *   ( 2,     4,        4,         2,       no,             yes,        4)
 *   ( 3,     4,        4,         3,       no,             yes,        4)
 * <p>
 *   ( 4,     6,        4,         1,       no,             yes,        4)
 *   ( 5,     6,        4,         2,       no,             yes,        4)
 *   ( 6,     6,        4,         3,       no,             yes,        4)
 *   ( 7,     6,        4,         4,       no,             yes,        4)
 * <p>
 *   ( 8,     7,        5,         1,       no,             yes,        5)
 *   ( 9,     7,        5,         2,       no,             yes,        5)
 *   ( 10,    7,        5,         3,       no,             yes,        5)
 *   ( 11,    7,        5,         4,       no,             yes,        5)
 *   ...
 *   ...
 *   ( 72,    23,       21,        1,       yes,            no,        no)
 *   ( 73,    23,       21,        2,       yes,            no,        no)
 *   ( 74,    23,       21,        3,       yes,            no,        no)
 *   ( 75,    23,       21,        4,       yes,            no,        no)
 * <p>
 *   ( 76,    24,       22,        1,       yes,            no,        no)
 */
abstract class SizeClasses implements SizeClassesMetric {

    static final int LOG2_QUANTUM = 4;

    private static final int LOG2_SIZE_CLASS_GROUP = 2;
    private static final int LOG2_MAX_LOOKUP_SIZE = 12;

    private static final int INDEX_IDX = 0;
    private static final int LOG2GROUP_IDX = 1;
    private static final int LOG2DELTA_IDX = 2;
    private static final int NDELTA_IDX = 3;
    private static final int PAGESIZE_IDX = 4;
    private static final int SUBPAGE_IDX = 5;
    private static final int LOG2_DELTA_LOOKUP_IDX = 6;

    private static final byte no = 0, yes = 1;

    protected SizeClasses(int pageSize, int pageShifts, int chunkSize, int directMemoryCacheAlignment) {
        this.pageSize = pageSize;
        this.pageShifts = pageShifts;
        this.chunkSize = chunkSize;
        this.directMemoryCacheAlignment = directMemoryCacheAlignment;

        int group = log2(chunkSize) + 1 - LOG2_QUANTUM;

        //generate size classes
        //[index, log2Group, log2Delta, nDelta, isMultiPageSize, isSubPage, log2DeltaLookup]
        sizeClasses = new short[group << LOG2_SIZE_CLASS_GROUP][7];
        nSizes = sizeClasses();

        //generate lookup table
        sizeIdx2sizeTab = new int[nSizes];
        pageIdx2sizeTab = new int[nPSizes];
        idx2SizeTab(sizeIdx2sizeTab, pageIdx2sizeTab);

        size2idxTab = new int[lookupMaxSize >> LOG2_QUANTUM];
        size2idxTab(size2idxTab);
    }

    protected final int pageSize;
    protected final int pageShifts;
    // 每一个chunk的大小
    protected final int chunkSize;
    protected final int directMemoryCacheAlignment;

    final int nSizes;
    int nSubpages;
    int nPSizes;

    int smallMaxSizeIdx;

    private int lookupMaxSize;

    private final short[][] sizeClasses;

    private final int[] pageIdx2sizeTab;

    // lookup table for sizeIdx <= smallMaxSizeIdx
    private final int[] sizeIdx2sizeTab;

    // lookup table used for size <= lookupMaxclass
    // spacing is 1 << LOG2_QUANTUM, so the size of array is lookupMaxclass >> LOG2_QUANTUM
    private final int[] size2idxTab;

    private int sizeClasses() {
        int normalMaxSize = -1;

        int index = 0;
        int size = 0;

        int log2Group = LOG2_QUANTUM;
        int log2Delta = LOG2_QUANTUM;
        int ndeltaLimit = 1 << LOG2_SIZE_CLASS_GROUP; // 4

        //First small group, nDelta start at 0.
        //first size class is 1 << LOG2_QUANTUM
        // 最开始的小组，nDelta（差值）开始于0.
        // size的大小时16.
        int nDelta = 0;
        while (nDelta < ndeltaLimit) {
            size = sizeClass(index++, log2Group, log2Delta, nDelta++);
        }
        log2Group += LOG2_SIZE_CLASS_GROUP;

        //All remaining groups, nDelta start at 1.
        // 所有剩余的组，nDelta（差值）开始于1.
        while (size < chunkSize) {
            nDelta = 1;

            while (nDelta <= ndeltaLimit && size < chunkSize) {
                size = sizeClass(index++, log2Group, log2Delta, nDelta++);
                normalMaxSize = size;
            }

            log2Group++;
            log2Delta++;
        }

        //chunkSize must be normalMaxSize
        assert chunkSize == normalMaxSize;

        //return number of size index
        return index;
    }

    //calculate size class
    private int sizeClass(int index, int log2Group, int log2Delta, int nDelta) {
        short isMultiPageSize;
        if (log2Delta >= pageShifts) {
            isMultiPageSize = yes;
        } else {
            int pageSize = 1 << pageShifts;
            int size = (1 << log2Group) + (1 << log2Delta) * nDelta;

            isMultiPageSize = size == size / pageSize * pageSize? yes : no;
        }

        int log2Ndelta = nDelta == 0? 0 : log2(nDelta);

        byte remove = 1 << log2Ndelta < nDelta? yes : no;

        int log2Size = log2Delta + log2Ndelta == log2Group? log2Group + 1 : log2Group;
        if (log2Size == log2Group) {
            remove = yes;
        }

        short isSubpage = log2Size < pageShifts + LOG2_SIZE_CLASS_GROUP? yes : no;

        int log2DeltaLookup = log2Size < LOG2_MAX_LOOKUP_SIZE ||
                              log2Size == LOG2_MAX_LOOKUP_SIZE && remove == no
                ? log2Delta : no;

        short[] sz = {
                (short) index, (short) log2Group, (short) log2Delta,
                (short) nDelta, isMultiPageSize, isSubpage, (short) log2DeltaLookup
        };

        sizeClasses[index] = sz;
        int size = (1 << log2Group) + (nDelta << log2Delta);

        if (sz[PAGESIZE_IDX] == yes) {
            nPSizes++;
        }
        if (sz[SUBPAGE_IDX] == yes) {
            nSubpages++;
            smallMaxSizeIdx = index;
        }
        if (sz[LOG2_DELTA_LOOKUP_IDX] != no) {
            lookupMaxSize = size;
        }
        return size;
    }

    private void idx2SizeTab(int[] sizeIdx2sizeTab, int[] pageIdx2sizeTab) {
        int pageIdx = 0;

        for (int i = 0; i < nSizes; i++) {
            short[] sizeClass = sizeClasses[i];
            int log2Group = sizeClass[LOG2GROUP_IDX];
            int log2Delta = sizeClass[LOG2DELTA_IDX];
            int nDelta = sizeClass[NDELTA_IDX];

            int size = (1 << log2Group) + (nDelta << log2Delta);
            sizeIdx2sizeTab[i] = size;

            if (sizeClass[PAGESIZE_IDX] == yes) {
                pageIdx2sizeTab[pageIdx++] = size;
            }
        }
    }

    private void size2idxTab(int[] size2idxTab) {
        int idx = 0;
        int size = 0;

        for (int i = 0; size <= lookupMaxSize; i++) {
            int log2Delta = sizeClasses[i][LOG2DELTA_IDX];
            int times = 1 << log2Delta - LOG2_QUANTUM;

            while (size <= lookupMaxSize && times-- > 0) {
                size2idxTab[idx++] = i;
                size = idx + 1 << LOG2_QUANTUM;
            }
        }
    }

    @Override
    public int sizeIdx2size(int sizeIdx) {
        return sizeIdx2sizeTab[sizeIdx];
    }

    @Override
    public int sizeIdx2sizeCompute(int sizeIdx) {
        int group = sizeIdx >> LOG2_SIZE_CLASS_GROUP;
        int mod = sizeIdx & (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        int groupSize = group == 0? 0 :
                1 << LOG2_QUANTUM + LOG2_SIZE_CLASS_GROUP - 1 << group;

        int shift = group == 0? 1 : group;
        int lgDelta = shift + LOG2_QUANTUM - 1;
        int modSize = mod + 1 << lgDelta;

        return groupSize + modSize;
    }

    @Override
    public long pageIdx2size(int pageIdx) {
        return pageIdx2sizeTab[pageIdx];
    }

    @Override
    public long pageIdx2sizeCompute(int pageIdx) {
        int group = pageIdx >> LOG2_SIZE_CLASS_GROUP;
        int mod = pageIdx & (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        long groupSize = group == 0? 0 :
                1L << pageShifts + LOG2_SIZE_CLASS_GROUP - 1 << group;

        int shift = group == 0? 1 : group;
        int log2Delta = shift + pageShifts - 1;
        int modSize = mod + 1 << log2Delta;

        return groupSize + modSize;
    }

    /**
     * 计算出所需大小在sizeClasses表格中的下标
     * 关于这个下标可以看看顶部的注释翻译
     * @param size request size
     *
     * @return
     */
    @Override
    public int size2SizeIdx(int size) {
        if (size == 0) {
            return 0;
        }
        if (size > chunkSize) {
            return nSizes;
        }

        // 将size变为2的次方数
        if (directMemoryCacheAlignment > 0) {
            size = alignSize(size);
        }

        if (size <= lookupMaxSize) {
            //size-1 / MIN_TINY
            return size2idxTab[size - 1 >> LOG2_QUANTUM];
        }

        int x = log2((size << 1) - 1);
        int shift = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? 0 : x - (LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM);

        int group = shift << LOG2_SIZE_CLASS_GROUP;

        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? LOG2_QUANTUM : x - LOG2_SIZE_CLASS_GROUP - 1;

        int deltaInverseMask = -1 << log2Delta;
        int mod = (size - 1 & deltaInverseMask) >> log2Delta &
                  (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        return group + mod;
    }

    @Override
    public int pages2pageIdx(int pages) {
        return pages2pageIdxCompute(pages, false);
    }

    @Override
    public int pages2pageIdxFloor(int pages) {
        return pages2pageIdxCompute(pages, true);
    }

    private int pages2pageIdxCompute(int pages, boolean floor) {
        int pageSize = pages << pageShifts;
        if (pageSize > chunkSize) {
            return nPSizes;
        }

        int x = log2((pageSize << 1) - 1);

        int shift = x < LOG2_SIZE_CLASS_GROUP + pageShifts
                ? 0 : x - (LOG2_SIZE_CLASS_GROUP + pageShifts);

        int group = shift << LOG2_SIZE_CLASS_GROUP;

        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + pageShifts + 1?
                pageShifts : x - LOG2_SIZE_CLASS_GROUP - 1;

        int deltaInverseMask = -1 << log2Delta;
        int mod = (pageSize - 1 & deltaInverseMask) >> log2Delta &
                  (1 << LOG2_SIZE_CLASS_GROUP) - 1;

        int pageIdx = group + mod;

        if (floor && pageIdx2sizeTab[pageIdx] > pages << pageShifts) {
            pageIdx--;
        }

        return pageIdx;
    }

    // Round size up to the nearest multiple of alignment.
    // 向上取整到下一个2的倍数，比如16返回16，17返回32.
    private int alignSize(int size) {
        int delta = size & directMemoryCacheAlignment - 1;
        return delta == 0? size : size + directMemoryCacheAlignment - delta;
    }

    @Override
    public int normalizeSize(int size) {
        if (size == 0) {
            return sizeIdx2sizeTab[0];
        }
        if (directMemoryCacheAlignment > 0) {
            size = alignSize(size);
        }

        if (size <= lookupMaxSize) {
            int ret = sizeIdx2sizeTab[size2idxTab[size - 1 >> LOG2_QUANTUM]];
            assert ret == normalizeSizeCompute(size);
            return ret;
        }
        return normalizeSizeCompute(size);
    }

    private static int normalizeSizeCompute(int size) {
        int x = log2((size << 1) - 1);
        int log2Delta = x < LOG2_SIZE_CLASS_GROUP + LOG2_QUANTUM + 1
                ? LOG2_QUANTUM : x - LOG2_SIZE_CLASS_GROUP - 1;
        int delta = 1 << log2Delta;
        int delta_mask = delta - 1;
        return size + delta_mask & ~delta_mask;
    }
}
