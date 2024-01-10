import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';

import '../../open_file_plugin.dart';
import 'linux.dart' as linux;
import 'macos.dart' as mac;
import 'windows.dart' as windows;

// Class of Opening File
class OpenFile {
  static const MethodChannel _channel = MethodChannel('open_file_plugin');

  OpenFile._();

  ///linuxDesktopName like 'xdg'/'gnome'
  // Method to open file
  static Future<OpenResult> open(String? filePath,
      {String? type,
      String? uti,
      String linuxDesktopName = "xdg",
      bool linuxByProcess = false,
      bool isAskForAllFileAccess = false}) async {
    assert(filePath != null);
    if (!Platform.isIOS && !Platform.isAndroid) {
      int result;
      int? windowsResult;
      if (Platform.isMacOS) {
        result = mac.system(['open', '$filePath']);
      } else if (Platform.isLinux) {
        var filePathLinux = Uri.file(filePath!);
        if (linuxByProcess) {
          result =
              Process.runSync('xdg-open', [filePathLinux.toString()]).exitCode;
        } else {
          result = linux
              .system(['$linuxDesktopName-open', filePathLinux.toString()]);
        }
      } else if (Platform.isWindows) {
        windowsResult = windows.shellExecute('open', filePath!);
        result = windowsResult <= 32 ? 1 : 0;
      } else {
        result = -1;
      }
      return OpenResult(
          type: result == 0 ? ResultType.done : ResultType.error,
          message: result == 0
              ? "done"
              : result == -1
                  ? "This operating system is not currently supported"
                  : "there are some errors when open $filePath${Platform.isWindows ? "   HINSTANCE=$windowsResult" : ""}");
    }
    // It's data that required for opening file on native sides
    Map<String, dynamic> map = {
      "file_path": filePath!,
      "type": type,
      "uti": uti,
      "isAskForAllFileAccess": isAskForAllFileAccess,
    };
    final result = await _channel.invokeMethod('open_file', map);
    final resultMap = json.decode(result) as Map<String, dynamic>;
    return OpenResult.fromJson(resultMap);
  }

// Picking random file from download directory
  static Future<String> pickRandomFile() async {
    return await _channel.invokeMethod('pick_random_file_path');
  }
}
