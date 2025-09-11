### BMCSHELL

BMCSHELL is a command-line interactive shell for managing Baseboard Management Controllers (BMCs). It provides a unified CLI to interact with BMCs over Redfish and SSH, integrates with offline inferencing engines (via Ollama APIs), and exposes tools and utilities commonly used in firmware and system management flows.

Features
- Unified CLI for BMC management via:
    - Redfish requests (GET/POST/PATCH/DELETE)
    - Remote SSH command execution
- Integration with offline inferencing engines through Ollama APIs for local/model-based queries
- Commands and helpers for:
    - busctl APIs (systemd bus interactions)
    - systemctl operations (service control)
    - PLDM tools (platform management)
    - obmc utility commands (OpenBMC helpers)
- Scripting support:
    - Save previously executed commands to script files
    - Re-run saved scripts or loop scripts for repeated execution
- Authentication and session handling for Redfish and SSH connections
- Configurable endpoints, timeouts, and credentials

Quick start
- Launch bmc shell:java -Dserver.port=8443 -jar *.jar
- Type following
```ssh
machine rain100bmc
username <bmc user name eg service>
password <bmc password>
```

Common commands
- Redfish request:
    ```ssh
    apis
    Redfish menu will be shown 
    s 2 <selecting second menu item>
    Child menu will be shown
    or 
    get/post/patch /redfish/v1/*/* -d 'data'
    ```
- SSH command:
    ```ssh
    ro.cmd 'command to execute'
    ro.ls 'path'
    ro.cat 'filepath'
    ssh <for ssh console>
    ```
- busctl:
    ```ssh
    bs.search <name to search>
    bs.introspect --ser xyz.openbmc_project.User.Manager --path /xyz/openbmc_project/user 
    ```
- systemctl:
    ro.service.start <service name>
    ro.service.status <service name>
    

Inferencing 
- 

Scripting and repeatable workflows
- Save current session commands to a script:
    script save my-maintenance-script.bms
- Execute a saved script:
    script run my-maintenance-script.bms
- Repeat a script N times or on a schedule:
    script run --repeat 10 my-maintenance-script.bms
    script run --interval 60 my-maintenance-script.bms  # every 60s

Configuration
- Default config file: ~/.bmcshell/config.yaml
- Typical settings:
    - Redfish endpoint and credentials
    - SSH keys and known hosts handling
    - Ollama endpoint and models
    - Logging and timeouts

Security notes
- Store credentials securely (use OS keyring where supported)
- Limit access to saved scripts and config files (restrict filesystem permissions)
- Verify host keys for SSH connections and use TLS/HTTPS for Redfish endpoints when possible

Contributing
- Follow repository CONTRIBUTING.md for code style, testing, and PR process
- Provide tests for new protocol handlers or integrations

License and support
- See LICENSE in the project root for licensing details
- For issues and feature requests, open an issue in the project tracker

This README provides a concise overview. Use bmcshell --help for full command reference and examples.
