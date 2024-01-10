package com.whizpool.openfile.utils;

import android.os.Environment;

import java.io.File;
import java.util.Random;

public class FileUtil {
    public static String pickRandomFile() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles();

            if (files != null && files.length > 0) {
                // Pick a random file
                Random random = new Random();
                int randomIndex = random.nextInt(files.length);
                File randomFile = files[randomIndex];
                return  randomFile.getAbsolutePath();
            }
        }

      return "";
    }
}
