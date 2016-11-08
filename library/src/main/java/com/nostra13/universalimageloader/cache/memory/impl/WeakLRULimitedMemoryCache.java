package com.nostra13.universalimageloader.cache.memory.impl;

import android.graphics.Bitmap;

import com.nostra13.universalimageloader.cache.memory.BaseMemoryCache;
import com.nostra13.universalimageloader.utils.L;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class WeakLRULimitedMemoryCache extends BaseMemoryCache {

    private static final int MAX_NORMAL_CACHE_SIZE_IN_MB = 16;
    private static final int MAX_NORMAL_CACHE_SIZE = MAX_NORMAL_CACHE_SIZE_IN_MB * 1024 * 1024;

    private static final int INITIAL_CAPACITY = 10;
    private static final float LOAD_FACTOR = 1.1f;

    private final int mLimitCacheSize;

    private final AtomicInteger mCacheSize;

    private final Map<String, Bitmap> mLruCache = Collections.synchronizedMap(new LinkedHashMap<String, Bitmap>(INITIAL_CAPACITY, LOAD_FACTOR, true));


    /**
     * @param pLimitCacheSize Maximum size for cache (in bytes)
     */
    public WeakLRULimitedMemoryCache(int pLimitCacheSize) {
        this.mLimitCacheSize = pLimitCacheSize;
        mCacheSize = new AtomicInteger();
        if (pLimitCacheSize > MAX_NORMAL_CACHE_SIZE) {
            L.w("You set too large memory cache size (more than %1$d Mb)", MAX_NORMAL_CACHE_SIZE_IN_MB);
        }
    }

    @Override
    public boolean put(String key, Bitmap value) {
        boolean putSuccessfully = false;
        // Try to add value to hard cache
        int valueSize = getSize(value);
        int sizeLimit = getLimitCacheSize();
        int curCacheSize = mCacheSize.get();
        if (valueSize < sizeLimit) {
            while (curCacheSize + valueSize > sizeLimit) {
                Map.Entry<String, Bitmap> removedValue = removeNext();
                if (removedValue != null && removedValue.getValue() != null) {
                    curCacheSize = mCacheSize.addAndGet(-getSize(removedValue.getValue()));
                    super.remove(removedValue.getKey());
                }
            }
            mCacheSize.addAndGet(valueSize);

            putSuccessfully = true;
        }
        // Add value to soft cache
        super.put(key, value);

        if (putSuccessfully) {
            mLruCache.put(key, value);
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public Bitmap remove(String key) {
        Bitmap value = super.get(key);
        if (value != null) {
            if (mLruCache.remove(key) != null) {
                mCacheSize.addAndGet(-getSize(value));
            }
        }
        return super.remove(key);
    }

    @Override
    public void clear() {
        mLruCache.clear();
        mCacheSize.set(0);
        super.clear();
    }

    protected int getLimitCacheSize() {
        return mLimitCacheSize;
    }

    protected int getSize(Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }

    private Map.Entry<String, Bitmap> removeNext() {
        Map.Entry<String, Bitmap> entry = null;
        synchronized (mLruCache) {
            Iterator<Map.Entry<String, Bitmap>> it = mLruCache.entrySet().iterator();
            if (it.hasNext()) {
                entry = it.next();
                it.remove();
            }
        }
        return entry;
    }

    @Override
    protected Reference<Bitmap> createReference(Bitmap value) {
        return new WeakReference<Bitmap>(value);
    }
}
