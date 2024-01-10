/// Model class for result on opening file
class OpenResult {
  ResultType type;
  String message;

  OpenResult({this.type = ResultType.done, this.message = "done"});

  /// Method to retrun OpenResult form map
  OpenResult.fromJson(Map<String, dynamic> json)
      : message = json['message'],
        type = _convertJson(json['type']);

  /// method to conver json into ResultType model
  static ResultType _convertJson(int? jsonType) {
    switch (jsonType) {
      case -1:
        return ResultType.noAppToOpen;
      case -2:
        return ResultType.fileNotFound;
      case -3:
        return ResultType.permissionDenied;
      case -4:
        return ResultType.error;
    }
    return ResultType.done;
  }
}

/// Enum for OpenResult
enum ResultType {
  done,
  fileNotFound,
  noAppToOpen,
  permissionDenied,
  error,
}
