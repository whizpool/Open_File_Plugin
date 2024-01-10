import 'dart:async';
import 'dart:html';

/// Method to open file in web
Future<bool> open(String uri) async {
  return window
      .resolveLocalFileSystemUrl(uri)
      .then((_) => true)
      .catchError((e) => false);
}
