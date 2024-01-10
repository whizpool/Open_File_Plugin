import 'dart:io';

import 'package:flutter/material.dart';
import 'dart:async';
import 'package:open_file_plugin/open_file_plugin.dart';
import 'package:file_picker/file_picker.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  var _openResult = 'Unknown';
  String? filePath = '/storage/emulated/0/Download/Link File Share/3.pdf';
  String message = "Please pick file";
  Future<void> openFile() async {
    if (filePath != null) {
      final result = await OpenFile.open(filePath, isAskForAllFileAccess: true);
      setState(() {
        _openResult = "type=${result.type}  message=${result.message}";
      });
    }
  }

  Future<void> pickFile() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles();

    if (result != null) {
      setState(() {
        filePath = result.paths.first;
      });
    } else {
      // User canceled the picker
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Row(
                mainAxisAlignment: Platform.isAndroid
                    ? MainAxisAlignment.spaceAround
                    : MainAxisAlignment.center,
                children: [
                  Platform.isAndroid
                      ? ElevatedButton(
                          onPressed: () async {
                            filePath = await OpenFile.pickRandomFile();
                            setState(
                              () {},
                            );
                          },
                          child: const Text('Pick Simple File'),
                        )
                      : const SizedBox(),
                  ElevatedButton(
                    onPressed: pickFile,
                    child: const Text('Pick Media File'),
                  ),
                ],
              ),
              Text(
                  '\n${filePath == null ? message : "File Path => $filePath"}\n'),
              ElevatedButton(
                onPressed: openFile,
                child: const Text('Open File'),
              ),
              Text('\nopen result: $_openResult\n'),
            ],
          ),
        ),
      ),
    );
  }
}
