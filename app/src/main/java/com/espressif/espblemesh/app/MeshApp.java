package com.espressif.espblemesh.app;

import android.app.Application;
import android.os.Environment;

import com.espressif.blemesh.MeshInitialize;
import com.espressif.blemesh.db.box.MeshObjectBox;
import com.espressif.blemesh.user.MeshUser;

import java.util.HashMap;
import java.util.Random;

import libs.espressif.utils.RandomUtil;

public class MeshApp extends Application {
    private static MeshApp instance;

    private final Object mCacheLock = new Object();
    private HashMap<String, Object> mCacheMap;

    @Override
    public void onCreate() {
        super.onCreate();

        mCacheMap = new HashMap<>();

        instance = this;

        MeshInitialize.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        MeshObjectBox.getInstance().close();
        mCacheMap.clear();
    }

    public static MeshApp getInstance() {
        return instance;
    }

    public static String getAppDirPath() {
        return Environment.getExternalStorageDirectory().toString() + "/Espressif/EspBleMesh/";
    }

    public void putCache(String key, Object value) {
        synchronized (mCacheLock) {
            mCacheMap.put(key, value);
        }
    }

    /**
     * Save an object in app
     *
     * @param value the object will be saved
     * @return the key to used in #takeCache
     */
    public String putCache(Object value) {
        synchronized (mCacheLock) {
            String key;
            do {
                int keyLength = new Random().nextInt(20) + 20;
                key = RandomUtil.randomString(keyLength);
            } while (mCacheMap.containsKey(key));

            mCacheMap.put(key, value);
            return key;
        }
    }

    /**
     * Take the object and remove from app
     *
     * @param key the key generated when put
     * @return the target object
     */
    public Object takeCache(String key) {
        synchronized (mCacheLock) {
            Object result = mCacheMap.get(key);
            if (result != null) {
                mCacheMap.remove(key);
            }

            return result;
        }
    }

    public Object getCache(String key) {
        synchronized (mCacheLock) {
            return mCacheMap.get(key);
        }
    }
}
