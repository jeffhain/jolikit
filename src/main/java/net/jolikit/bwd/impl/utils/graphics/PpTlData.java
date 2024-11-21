/*
 * Copyright 2024 Jeff Hain
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
package net.jolikit.bwd.impl.utils.graphics;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.lang.LangUtils;

/**
 * Package-private.
 */
class PpTlData {
    
    //--------------------------------------------------------------------------
    // PUBLIC CLASSES
    //--------------------------------------------------------------------------
    
    public static class PooledIntArrHolder extends IntArrHolder {
        private final List<PooledIntArrHolder> listForRepool;
        private boolean needRepool = false;
        PooledIntArrHolder(List<PooledIntArrHolder> listForRepool) {
            this.listForRepool = listForRepool;
        }
        void onBorrow() {
            this.needRepool = true;
        }
        /**
         * No big deal if not released due to exception:
         * will just be GC-ed.
         */
        public void release() {
            if (this.needRepool) {
                this.listForRepool.add(this);
                this.needRepool = false;
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private static class MyIntArrHolderPool {
        private final List<PooledIntArrHolder> list = new ArrayList<>();
        public MyIntArrHolderPool() {
        }
        public PooledIntArrHolder borrow() {
            PooledIntArrHolder ret = null;
            if (this.list.size() != 0) {
                ret = LangUtils.removeLast(this.list);
            } else {
                ret = new PooledIntArrHolder(this.list);
            }
            ret.onBorrow();
            return ret;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * Sharing a default thread-local instance among multiple treatments,
     * to reduce overall memory footprint.
     */
    public static final ThreadLocal<PpTlData> DEFAULT_TL_DATA =
        new ThreadLocal<PpTlData>() {
        @Override
        public PpTlData initialValue() {
            return new PpTlData();
        }
    };
    
    private final MyIntArrHolderPool tmpBigArrPool = new MyIntArrHolderPool();
    
    private final MyIntArrHolderPool tmpArrPool = new MyIntArrHolderPool();
    
    /*
     * 
     */
    
    public final DoubleArrHolder tmpDoubleArr = new DoubleArrHolder();
    
    /*
     * 
     */
    
    public final PpColorSum tmpColorSum = new PpColorSum();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public PpTlData() {
    }
    
    /**
     * For arrays containing a whole area of pixels.
     * Should release after use.
     */
    public PooledIntArrHolder borrowBigArrHolder() {
        return this.tmpBigArrPool.borrow();
    }
    
    /**
     * For arrays containing a row or a column of pixels.
     * Should release after use.
     */
    public PooledIntArrHolder borrowArrHolder() {
        return this.tmpArrPool.borrow();
    }
}
