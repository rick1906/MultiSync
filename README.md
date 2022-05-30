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

#### Example
In default sync implementation the sync process is divided between three stages performed with the whole file trees: scanning (adding whole file tree to sync list), analyzing (choosing sync source files for each path) and synchronization (creating/replacing/deleting files). The example below demonstrates how the classes from this library can be used for consecutive per-file sync process, where analyzing and synchronization stages are performed for a single file just after it was detected by scanning.
````java
public SyncList consecutiveSync(Controller controller)
{
    SyncList list = new SyncList(controller.getTargets());
    list.addAllTargets(false); // add roots non-recursively
    for (int i = 0; i < controller.getTargets().length; ++i) {
        list.startSync(i, controller.getGlobalOptions()); // initialize roots
    }
    int index = 0;
    while (index < list.size()) {
        SyncElement current = list.get(index);
        if (current.analyze(controller.getGlobalOptions(), true)) {
            current.sync(controller.getGlobalOptions()); // sync one element
            list.addChildren(current, false); // for directories add child files non-recursively
        }
        index++;
    }
    list.saveFileLists(); // save local filelists
    list.saveGlobalFileList(controller.getFileListContainer()); // save global filelist
    return list;
}
````
