package com.whizpool.openfile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.PermissionChecker;

import com.whizpool.openfile.utils.FileUtil;
import com.whizpool.openfile.utils.JsonUtil;
import com.whizpool.openfile.utils.MapUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * OpenfilePlugin
 */
public class OpenfilePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {
    private static final int REQUEST_CODE = 33432;
    private static final int RESULT_CODE = 0x12;
    private static final String TYPE_PREFIX_IMAGE = "image/";
    private static final String TYPE_PREFIX_VIDEO = "video/";
    private static final String TYPE_PREFIX_AUDIO = "audio/";
    private static final String TYPE_STRING_APK = "application/vnd.android.package-archive";

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    private Context context;
    private Activity activity;

    private Result result;
    private String filePath;
    private String typeString;
private  boolean isAskForAllFileAccess;
    private boolean isResultSubmitted = false;

    @Deprecated
    public static void registerWith(PluginRegistry.Registrar registrar) {
        OpenfilePlugin plugin = new OpenfilePlugin();
        plugin.activity = registrar.activity();
        plugin.context = registrar.context();
        plugin.channel = new MethodChannel(registrar.messenger(), "open_file");
        plugin.channel.setMethodCallHandler(plugin);
        registrar.addRequestPermissionsResultListener(plugin);
        registrar.addActivityResultListener(plugin);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        this.context = flutterPluginBinding.getApplicationContext();
        this.channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "open_file_plugin");
        this.channel.setMethodCallHandler(this);
    }

    @Override
    @SuppressLint("NewApi")
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        isResultSubmitted = false;
        if (call.method.equals("open_file")) {
            this.result = result;
            filePath = call.argument("file_path");
            isAskForAllFileAccess = call.argument("isAskForAllFileAccess");
            if (call.hasArgument("type") && call.argument("type") != null) {
                typeString = call.argument("type");
            } else {
                typeString = getFileType(filePath);
            }
            if (pathRequiresPermission()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (isFileAvailable()) {
                        return;
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        if (!isMediaStorePath() && !Environment.isExternalStorageManager()) {
                            result(-3, "Permission denied: android.Manifest.permission.MANAGE_EXTERNAL_STORAGE");
                            return;
                        }
                    }
                }
                if (checkPermissions()) {
                    if (TYPE_STRING_APK.equals(typeString)) {
                        openApkFile();
                        return;
                    }
                    startActivity();
                }
            } else {
                startActivity();
            }
        }
        else if(call.method.equals("pick_random_file_path")){
           String pickedFilePath= FileUtil.pickRandomFile();
            result.success(pickedFilePath);
        }
        else {
            result.notImplemented();
            isResultSubmitted = true;
        }
    }
    private boolean hasManageAllFilesAccessPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return true; // For lower Android versions
    }

    private boolean checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (typeStartsWith(TYPE_PREFIX_IMAGE, typeString)) {
                if (!hasPermission(Manifest.permission.READ_MEDIA_IMAGES)) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE);
                    return false;
                }
            } else if (typeStartsWith(TYPE_PREFIX_VIDEO, typeString)) {
                if (!hasPermission(Manifest.permission.READ_MEDIA_VIDEO)) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_CODE);
                    return false;
                }
            }
            else if (typeStartsWith(TYPE_PREFIX_AUDIO, typeString)) {
                if (!hasPermission(Manifest.permission.READ_MEDIA_AUDIO)) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_CODE);
                    return false;
                }
            }else if (!hasPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)&&!hasManageAllFilesAccessPermission() && isAskForAllFileAccess) {
                requestManageAllFilesAccessPermission();
                return false;
            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&!hasManageAllFilesAccessPermission() && isAskForAllFileAccess) {
                    requestManageAllFilesAccessPermission();
                    return false;
            }else {
                if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && !isAskForAllFileAccess) {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
                    return false;
                }
            }
//            if (!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
//                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
//                    return false;
//
//            }

        }
        return true;
    }

    private boolean typeStartsWith(String prefix, String fileType) {
        return fileType != null && fileType.startsWith(prefix);
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission) == PermissionChecker.PERMISSION_GRANTED;
    }

    private boolean isMediaStorePath() {
        boolean isMediaStorePath = false;
        String[] mediaStorePath = {
            "/DCIM/",
            "/Pictures/",
            "/Movies/",
            "/Alarms/",
            "/Audiobooks/",
            "/Music/",
            "/Notifications/",
            "/Podcasts/",
            "/Ringtones/",
            "/Download/"
        };
        for (String s : mediaStorePath) {
            if (filePath.contains(s)) {
                isMediaStorePath = true;
                break;
            }
        }
        return isMediaStorePath;
    }

    private boolean pathRequiresPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        try {
            String appDirCanonicalPath = new File(context.getApplicationInfo().dataDir).getCanonicalPath();
            String extCanonicalPath = context.getExternalFilesDir(null).getCanonicalPath();
            String fileCanonicalPath = new File(filePath).getCanonicalPath();
            return !(fileCanonicalPath.startsWith(appDirCanonicalPath) || fileCanonicalPath.startsWith(extCanonicalPath));
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean isFileAvailable() {
        if (filePath == null) {
            result(-4, "the file path cannot be null");
            return true;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            result(-2, "the " + filePath + " file does not exists");
            return true;
        }
        return false;
    }
    public static boolean isNewlyAdded(File file) {
        long currentTime = System.currentTimeMillis();
        long fileLastModified = file.lastModified();

        // Adjust the threshold as needed (e.g., within the last 5 minutes)
        long threshold = 5 * 60 * 1000; // 5 minutes in milliseconds

        return (currentTime - fileLastModified) <= threshold;
    }
    private void startActivity() {
        if (isFileAvailable()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Intent chooserIntent = null;
        File file = new File(filePath);
        if(Objects.equals(typeString, "*/*") && isNewlyAdded(file)){
            MediaScannerConnection.scanFile(this.context,
                    new String[] { filePath }, null,
                    (path, uri1) -> {

                    });
        }
        if((TYPE_STRING_APK.equals(typeString) && !hasPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES))){
            typeString="*/*";
        }
        if (TYPE_STRING_APK.equals(typeString) && hasPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES) )
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        else
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String packageName = context.getPackageName();
            Uri uri = FileProvider.getUriForFile(context, packageName + ".fileProvider.com.whizpool.openfile", file);
            intent.setDataAndType(uri, typeString);
            if(typeString.equals("*/*")){
                chooserIntent = Intent.createChooser(intent, "Open with");
            }
        } else {
            intent.setDataAndType(Uri.fromFile(file), typeString);
        }
        int type = 0;
        String message = "done";
        try {
            activity.startActivity(chooserIntent != null ? chooserIntent : intent);
        } catch (ActivityNotFoundException e) {
            type = -1;
            message = "No APP found to open this file。";
        } catch (Exception e) {
            type = -4;
            message = "File opened incorrectly。";
        }
        result(type, message);
    }

    private String getFileType(String filePath) {
        String[] fileStrs = filePath.split("\\.");
        String fileTypeStr = fileStrs[fileStrs.length - 1].toLowerCase();
        switch (fileTypeStr) {
            case "3gp":
                return "video/3gpp";
            case "torrent":
                return "application/x-bittorrent";
            case "kml":
                return "application/vnd.google-earth.kml+xml";
            case "gpx":
                return "application/gpx+xml";
            case "apk":
                return TYPE_STRING_APK;
            case "asf":
                return "video/x-ms-asf";
            case "avi":
                return "video/x-msvideo";
            case "bin":
            case "class":
            case "exe":
                return "application/octet-stream";
            case "bmp":
                return "image/bmp";
            case "c":
                return "text/plain";
            case "conf":
                return "text/plain";
            case "cpp":
                return "text/plain";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
            case "csv":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "gif":
                return "image/gif";
            case "gtar":
                return "application/x-gtar";
            case "gz":
                return "application/x-gzip";
            case "h":
                return "text/plain";
            case "htm":
                return "text/html";
            case "html":
                return "text/html";
            case "jar":
                return "application/java-archive";
            case "java":
                return "text/plain";
            case "jpeg":
                return "image/jpeg";
            case "jpg":
                return "image/jpeg";
            case "js":
                return "application/x-javascript";
            case "log":
                return "text/plain";
            case "m3u":
                return "audio/x-mpegurl";
            case "m4a":
                return "audio/mp4a-latm";
            case "m4b":
                return "audio/mp4a-latm";
            case "m4p":
                return "audio/mp4a-latm";
            case "m4u":
                return "video/vnd.mpegurl";
            case "m4v":
                return "video/x-m4v";
            case "mov":
                return "video/quicktime";
            case "mp2":
                return "audio/x-mpeg";
            case "mp3":
                return "audio/x-mpeg";
            case "mp4":
                return "video/mp4";
            case "mpc":
                return "application/vnd.mpohun.certificate";
            case "mpe":
                return "video/mpeg";
            case "mpeg":
                return "video/mpeg";
            case "mpg":
                return "video/mpeg";
            case "mpg4":
                return "video/mp4";
            case "mpga":
                return "audio/mpeg";
            case "msg":
                return "application/vnd.ms-outlook";
            case "ogg":
                return "audio/ogg";
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "pps":
                return "application/vnd.ms-powerpoint";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "prop":
                return "text/plain";
            case "rc":
                return "text/plain";
            case "rmvb":
                return "audio/x-pn-realaudio";
            case "rtf":
                return "application/rtf";
            case "sh":
                return "text/plain";
            case "tar":
                return "application/x-tar";
            case "tgz":
                return "application/x-compressed";
            case "txt":
                return "text/plain";
            case "wav":
                return "audio/x-wav";
            case "wma":
                return "audio/x-ms-wma";
            case "wmv":
                return "audio/x-ms-wmv";
            case "wps":
                return "application/vnd.ms-works";
            case "xml":
                return "text/plain";
            case "z":
                return "application/x-compress";
            case "zip":
                return "application/x-zip-compressed";
            default:
                return "*/*";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openApkFile() {
        if (!canInstallApk()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isResultSubmitted) {
                    startInstallPermissionSettingActivity();
                }
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES},
                    REQUEST_CODE
                );
            }
        } else {
            startActivity();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getContentUriForFile(String filePath) {
        Uri contentUri = null;

        // Assuming filePath is the absolute path to the file
        String selection = MediaStore.Files.FileColumns.DATA + "=?";
        String[] selectionArgs = new String[]{filePath};

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID);
                long fileId = cursor.getLong(columnIndex);
                contentUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, fileId);
            }
            cursor.close();
        }

        return contentUri;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canInstallApk() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                return activity.getPackageManager().canRequestPackageInstalls();
            } catch (Exception e) {
                result(e instanceof SecurityException ? -3 : -4, e.getMessage());
                return false;
            }
        }
        return hasPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        if (activity == null) {
            return;
        }
        Uri packageURI = Uri.parse("package:" + activity.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        activity.startActivityForResult(intent, RESULT_CODE);
    }
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestManageAllFilesAccessPermission() {
        try {
            Uri uri = Uri.parse("package:"+context.getPackageName());
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
            activity.startActivity(intent);
        } catch (Exception ex){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            activity.startActivity(intent);
        }

    }
    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE) return false;
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)&&hasPermission(Manifest.permission.REQUEST_INSTALL_PACKAGES) && TYPE_STRING_APK.equals(typeString)) {
            openApkFile();
            return false;
        }
        for (String string : permissions) {
            if (!hasPermission(string) && !TYPE_STRING_APK.equals(typeString)) {
                result(-3, "Permission denied: " + string);
                return false;
            }
        }
        startActivity();
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RESULT_CODE) {
            if (canInstallApk()) {
                startActivity();
            } else {
                result(-3, "Permission denied: " + Manifest.permission.REQUEST_INSTALL_PACKAGES);
            }
        }
        return false;
    }

    private void result(int type, String message) {
        if (result != null && !isResultSubmitted) {
            Map<String, Object> map = MapUtil.createMap(type, message);
            result.success(JsonUtil.toJson(map));
            isResultSubmitted = true;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (channel == null) {
            // Could be on too low of an SDK to have started listening originally.
            return;
        }

        channel.setMethodCallHandler(null);
        channel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addRequestPermissionsResultListener(this);
        binding.addActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // Do nothing
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        // Do nothing
    }
}
