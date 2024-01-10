#include "include/openfile/openfile_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "openfile_plugin.h"

void OpenfilePluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  openfile::OpenfilePlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
