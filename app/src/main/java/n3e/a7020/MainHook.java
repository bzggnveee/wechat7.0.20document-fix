package n3e.a7020;
//MainHook

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "WeChatFileHook";
    private static int HOOKED_REQUEST_CODE = -1;
    private static String TARGET_USER = null;
    private static Bundle ORIGINAL_EXTRAS = null;

    private static final String WECHAT_FILE_LIST_KEY = "selected_file_lst";
    private static final String WECHAT_TARGET_USER_KEY = "GalleryUI_ToUser";
    private static final String WECHAT_ORIGINAL_USER_KEY = "TO_USER";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.tencent.mm")) return;
        XposedBridge.log(TAG + ": Hooking WeChat process...");

        XposedHelpers.findAndHookMethod(Activity.class, "startActivityForResult",
                Intent.class, int.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Intent originalIntent = (Intent) param.args[0];
                        if (originalIntent == null || originalIntent.getComponent() == null) return;

                        if ("com.tencent.mm.pluginsdk.ui.tools.FileSelectorUI".equals(originalIntent.getComponent().getClassName())) {
                            XposedBridge.log(TAG + ": Intercepted call to FileSelectorUI.");

                            HOOKED_REQUEST_CODE = (int) param.args[1];
                            if (originalIntent.hasExtra(WECHAT_ORIGINAL_USER_KEY)) {
                                TARGET_USER = originalIntent.getStringExtra(WECHAT_ORIGINAL_USER_KEY);
                            }
                            ORIGINAL_EXTRAS = originalIntent.getExtras();
                            XposedBridge.log(TAG + ": Stored requestCode: " + HOOKED_REQUEST_CODE + ", user: " + TARGET_USER);

                            Intent newIntent = new Intent(Intent.ACTION_GET_CONTENT);
                            newIntent.addCategory(Intent.CATEGORY_OPENABLE);
                            newIntent.setType("*/*");
                            newIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                            param.args[0] = newIntent;
                            XposedBridge.log(TAG + ": Replaced Intent to use system file picker.");
                        }
                    }
                });

        XposedBridge.hookAllMethods(Activity.class, "onActivityResult", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args.length < 3 || !(param.args[0] instanceof Integer)) return;

                int requestCode = (int) param.args[0];

                if (requestCode == HOOKED_REQUEST_CODE) {
                    XposedBridge.log(TAG + ": Caught our hooked onActivityResult with requestCode " + requestCode);

                    // 无论成功失败，都重置，避免干扰下一次
                    HOOKED_REQUEST_CODE = -1;

                    int resultCode = (int) param.args[1];
                    Intent data = (Intent) param.args[2];

                    if (resultCode == Activity.RESULT_OK && data != null) {
                        Context context = (Context) param.thisObject;
                        ArrayList<String> filePaths = new ArrayList<>();

                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                String path = getPathFromUri(context, clipData.getItemAt(i).getUri());
                                if (path != null) filePaths.add(path);
                            }
                        } else if (data.getData() != null) {
                            String path = getPathFromUri(context, data.getData());
                            if (path != null) filePaths.add(path);
                        }

                        if (!filePaths.isEmpty()) {
                            XposedBridge.log(TAG + ": Successfully resolved paths: " + filePaths.toString());

                            Intent fakeResultIntent = new Intent();

                            if (ORIGINAL_EXTRAS != null) {
                                fakeResultIntent.putExtras(ORIGINAL_EXTRAS);
                                XposedBridge.log(TAG + ": Restored all original extras.");
                            }

                            fakeResultIntent.putStringArrayListExtra(WECHAT_FILE_LIST_KEY, filePaths);
                            if (TARGET_USER != null) {
                                fakeResultIntent.putExtra(WECHAT_TARGET_USER_KEY, TARGET_USER);
                            }

                            // **最终精修：添加一个场景值，这是解决“静默失败”的关键**
                            fakeResultIntent.putExtra("from_scene", "com.tencent.mm.ui.chatting.SendAppMessageWrapper_Token");

                            param.args[2] = fakeResultIntent;
                            XposedBridge.log(TAG + ": Forged the final, polished Intent. Passing to original method.");
                        } else {
                            XposedBridge.log(TAG + ": ERROR: Failed to resolve any file paths. Nullifying intent to prevent crash.");
                            // **最终精修：如果路径解析失败，就彻底清空返回的Intent，阻止后续流程，避免崩溃**
                            param.args[2] = null;
                        }
                    }

                    TARGET_USER = null;
                    ORIGINAL_EXTRAS = null;
                }
            }
        });
    }

    // V6版本的路径解析辅助函数保持不变
    private String getPathFromUri(final Context context, final Uri uri) {
        if (uri == null) {
            Log.e(TAG, "getPathFromUri: received a null URI.");
            return null;
        }
        Log.d(TAG, "getPathFromUri: Resolving URI -> " + uri.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
            Log.d(TAG, "URI is a Document URI.");
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    String path = Environment.getExternalStorageDirectory() + "/" + split[1];
                    Log.d(TAG, "Resolved from ExternalStorageProvider: " + path);
                    return path;
                }
            }
            else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                try {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id != null && id.startsWith("raw:")) {
                        return id.substring(4);
                    }
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    String path = getDataColumn(context, contentUri, null, null);
                    Log.d(TAG, "Resolved from DownloadsProvider: " + path);
                    return path;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "DownloadsProvider ID is not a number. Falling back to copy.", e);
                    return copyFileToCache(context, uri);
                }
            }
            else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                else if ("video".equals(type)) contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                else if ("audio".equals(type)) contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                String path = getDataColumn(context, contentUri, selection, selectionArgs);
                Log.d(TAG, "Resolved from MediaProvider: " + path);
                return path;
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "URI is a classic Content URI.");
            return getDataColumn(context, uri, null, null);
        }
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            Log.d(TAG, "URI is a File URI.");
            return uri.getPath();
        }
        Log.w(TAG, "All standard resolving methods failed. Falling back to copying to cache.");
        return copyFileToCache(context, uri);
    }
    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            Log.e(TAG, "getDataColumn: Error querying URI.", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
    private String copyFileToCache(Context context, Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Log.e(TAG, "copyFileToCache: Failed to open input stream for URI.");
                return null;
            }
            String fileName = "temp_file_" + System.currentTimeMillis();
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(0);
                }
            }
            File tempFile = new File(context.getCacheDir(), fileName);
            try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
            }
            String cachedPath = tempFile.getAbsolutePath();
            Log.i(TAG, "copyFileToCache: File successfully copied to: " + cachedPath);
            return cachedPath;
        } catch (Exception e) {
            Log.e(TAG, "copyFileToCache: Exception while copying file.", e);
            return null;
        }
    }
}
