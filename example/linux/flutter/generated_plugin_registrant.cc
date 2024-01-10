//
//  Generated file. Do not edit.
//

// clang-format off

#include "generated_plugin_registrant.h"

#include <open_file_plugin/openfile_plugin.h>

void fl_register_plugins(FlPluginRegistry* registry) {
  g_autoptr(FlPluginRegistrar) open_file_plugin_registrar =
      fl_plugin_registry_get_registrar_for_plugin(registry, "OpenfilePlugin");
  openfile_plugin_register_with_registrar(open_file_plugin_registrar);
}
