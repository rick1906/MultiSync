# MultiSync
Java library/application for synchronizing multiple directories. This library can be used to keep multiple directories on the same file system in sync and up-to-date, while retaining configured differencies and priorities. All removed/replaced files are backuped. The synchronization is configured using data in JSON format. To keep track of changes to directory contents filelists in JSON format are used.

## Configuration
#### Configuration example
````json
{
    "backup": "global-backup",
    "list": "global-filelist.json",
    "log": "sync.log",
    "appendLog": true,
    "targets": {
        "C:\\sync1": {
            "readOnly": true,
            "priority": -1
        },
        "relative-path-to-config\\sync2": {
            "backup": ".backup",
            "include": ["only-path"]
        },
        "D:\\sync3": {
            "list": "local-filelist.json",
        },
        "X:\\removable-device": {
            "required": false,
            "delete": false,
            "ignore": ["path1", "path2\\path3"]
        },
        "Z:\\mounted\\data": {
            "device": {"mount":"X:\\mounted"},
            "required": false
        }
    }
}
````
#### Basic configuration elements
Configuration is divided to global (top-level) configuration and separate sync target "local" configuration. Sync targets use global configuration if no corresponding directives are defined in their own configuration. Relative paths are relative to config file by default. In sync target configuration relative paths are relative to target's root.

- `targets` - Array of sync targets' configurations.
- `backup` - Backup path (global, local). Backups are put in date-based subfolder.
- `list` - Filelist path (global, local).
- `log` - Sync log (global).
- `appendLog` - Append to log file, otherwise overwrite it (global).
- `readOnly` - Do not write to target directory (local).
- `priority` - Source priority, 0 by default, higher is better (local). Overrides file time comparison.
- `required` - Stop sync if this target is not available (local).
- `delete` - Allow deletion or not (global, local). Other similar options are available like `create` and `replace`.
- `include` - Array of subpaths to sync (local). Other subpaths are ignored.
- `ignore` - Array of subpaths to ignore (local).
- `device` - Device configuration (local). The only device provided in library is local file system. The `mount` directive can be used to check if specified path is mounted, otherwise sync target will be ignored.

#### Running
`java -jar multisync.jar multisync.json` where `multisync.json` is a path to configuraton file.

## Using as a library
The synchronization process can be managed by `Controller` and `SyncList` classes. The former is used just to store sync targets, global configuration and globally used objects. The latter manages synchronization of provided elements as well as generating/saving resulting filelist. All information about single relative path to be synchronized between all targets is stored in `SyncElement` object.
