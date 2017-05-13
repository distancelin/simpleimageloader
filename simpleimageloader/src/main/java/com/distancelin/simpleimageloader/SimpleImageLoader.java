package com.distancelin.simpleimageloader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * 图片加载过程概述：
 * 首先调用getBitmapFromMemoryCache(url)从内存缓存中加载，失败则getBitmapFromDiskCache加载磁盘缓存，
 * 再次失败就getBitmapFromHttp，成功之后将流写入缓存文件，再decodeFileDescriptor加载到内存，
 * 之所以不先加载到内存再写入缓存文件是因为直接两次decodeStream()进行图片也所会导致图片本身出现问题，该知识点参考自《android开发艺术探索》
 *
 *
 * Created by distancelin on 2017/5/12.
 */

public class SimpleImageLoader {
    private static final String TAG = "SimpleImageLoader";
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private ImageResizer mImageResizer;
    private Context mContext;
    private static volatile SimpleImageLoader mInstance;
    private boolean mIsDiskLruCacheCreated = false;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAX_POOL_SIZE = 2 * CPU_COUNT + 1;
    private static final int THREAD_LIFETIME = 10;
    //50MB
    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private static final ThreadFactory mThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "SimpleImageLoader#" + mCount.getAndIncrement());
        }
    };
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, THREAD_LIFETIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), mThreadFactory);

    private SimpleImageLoader(Context context) {
        mContext = context;
        initCache();
        initImageResizer();
    }

    /**
     * @param context 用于创建缓存
     * @return 单例的ImageLoader对象
     */
    public static SimpleImageLoader getSingleton(Context context) {
        if (mInstance == null) {
            synchronized (SimpleImageLoader.class) {
                if (mInstance == null) {
                    //传入appContext避免activity带来的内存泄露
                    mInstance = new SimpleImageLoader(context.getApplicationContext());
                }
            }
        }
        return mInstance;
    }

    private void initImageResizer() {
        mImageResizer = ImageResizer.getInstance();
    }

    private void initCache() {
        //单位是KB
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int memoryCacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(memoryCacheSize) {
            @Override
            //sizeOf的返回值单位必须和memoryCacheSize一致，这里为KB
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight() / 1024;
            }
        };
        File diskCacheDir = getDiskCacheDir(mContext, "bitmaps");
        if (!diskCacheDir.exists()) {
            //mkdirs可以在不存在的目录中创建文件夹
            diskCacheDir.mkdirs();
        }
        if (getUsableDiskCacheSpace(diskCacheDir) > DISK_CACHE_SIZE) {
            try {
                mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
                mIsDiskLruCacheCreated = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //返回可用的磁盘空间
    private long getUsableDiskCacheSpace(File diskCacheDir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            //获取分区所剩下的磁盘空间
            return diskCacheDir.getUsableSpace();
        }
        StatFs stats = new StatFs(diskCacheDir.getPath());
        //block代表磁头读取磁盘文件的最小单元，又叫簇，多个扇区组成了一个簇，簇一般为2的n次方个扇区
        return stats.getBlockSizeLong() * stats.getAvailableBlocksLong();
    }

    private File getDiskCacheDir(Context context, String bitmaps) {
        //外存是否可用
        boolean externalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
        final String cachePath;
        if (externalStorageAvailable) {
            cachePath = context.getExternalCacheDir().getPath();
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.pathSeparator + bitmaps);
    }

    /**
     * @param url
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public void loadBitmapSyn(String url, ImageView target, int reqWidth, int reqHeight) {
        Bitmap bitmap = getBitmapFromMemoryCache(url);
        if (bitmap != null) {
            Log.d(TAG, "load bitmap from memoryCache successfully" + url);
            showBitmapInTargetOnUiThread(target, bitmap);
            return;
        }
        try {
            bitmap = getBitmapFromDiskCache(url, reqWidth, reqHeight);
            if (bitmap != null) {
                Log.d(TAG, "load bitmap from diskCache successfully" + url);
                showBitmapInTargetOnUiThread(target, bitmap);
                return;
            }
            bitmap = getBitmapFromHttp(url, reqWidth, reqHeight);
            Log.d(TAG, "load bitmap from network " + url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bitmap == null && !mIsDiskLruCacheCreated) {
            Log.w(TAG, "DiskLruCache is not created,");
            downloadBitmapFromUrl(url);
        }
        showBitmapInTargetOnUiThread(target, bitmap);
    }


    /**
     * 采用异步的方式加载，并在UI线程中显示图片
     *
     * @param target 目标imageView
     * @param url    图片的url
     */
    public void loadBitmapAsync(ImageView target, String url) {
        loadBitmapAsync(target, url, target.getWidth(), target.getHeight());
    }

    public void loadBitmapAsync(final ImageView target, final String url, final int reqWidth, final int reqHeight) {
        final Bitmap bitmap = getBitmapFromMemoryCache(url);
        if (bitmap != null) {
            target.setImageBitmap(bitmap);
        }
        Runnable task = new Runnable() {
            @Override
            public void run() {
                loadBitmapSyn(url, target, reqWidth, reqHeight);
            }
        };
        THREAD_POOL_EXECUTOR.execute(task);
    }

    /**
     * @param target 目标imageView
     * @param bitmap 需要显示的图片
     */
    private void showBitmapInTargetOnUiThread(final ImageView target, final Bitmap bitmap) {
        target.post(new Runnable() {
            @Override
            public void run() {
                target.setImageBitmap(bitmap);
            }
        });
    }


    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private Bitmap getBitmapFromDiskCache(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("loading bitmap on UI thread!");
        }
        Bitmap bitmap = null;
        String key = hashKeyFromUrl(url);
        DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
        if (snapShot != null) {
            FileInputStream fis = (FileInputStream) snapShot.getInputStream(DISK_CACHE_INDEX);
            FileDescriptor fd = fis.getFD();
            bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor(fd, reqWidth, reqHeight);
            if (bitmap != null) {
                addBitmapToMemoryCache(key, bitmap);
            }
        }
        return bitmap;
    }

    private Bitmap getBitmapFromHttp(String url, int reqWidth, int reqHeight) throws IOException {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new RuntimeException("can not network on UI thread!");
        }
        String key = hashKeyFromUrl(url);
        DiskLruCache.Editor editor = mDiskLruCache.edit(key);
        if (editor != null) {
            OutputStream os = editor.newOutputStream(DISK_CACHE_INDEX);
            if (downloadFromUrlToStream(url, os)) {
                editor.commit();
            } else {
                editor.abort();
            }
        }
        mDiskLruCache.flush();
        //由于此处如果直接decodeStream会导致图片出现错误，所以先写入文件，在decodeFileDescriptor()
        return getBitmapFromDiskCache(url, reqWidth, reqHeight);
    }

    /**
     * 根据图片的url获取到对应的inputStream,再将该inputStream写入输出流
     *
     * @param urlString 图片的url
     * @param os        图片的输出流
     * @return true 代表从网络获取图片输出流成功，false代表失败
     */
    private boolean downloadFromUrlToStream(String urlString, OutputStream os) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(urlConnection.getInputStream());
            bos = new BufferedOutputStream(os);
            int temp;
            while ((temp = bis.read()) != -1) {
                bos.write(temp);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "download bitmap failed." + e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                bos.close();
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream bis = null;
        try {
            URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            bis = new BufferedInputStream(urlConnection.getInputStream());
            bitmap = BitmapFactory.decodeStream(bis);
        } catch (IOException e) {
            Log.e(TAG, "download bitmap failed." + e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            try {
                bis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

    /**
     * @param url 图片的url
     * @return url对应的加密摘要md5
     */
    private String hashKeyFromUrl(String url) {
        String cacheKey;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(url.getBytes());
            cacheKey = bytesToHexString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(url.hashCode());
            e.printStackTrace();
        }
        return cacheKey;
    }

    /**
     * @param digest 需要加密的byte数组
     * @return 加密后的字符串
     */
    private String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            //base16加密
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }


}
