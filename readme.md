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
    

- inferencing
```ssh
    q ask your questions
    ai.ls to show list of available inference engines
    ai.testcase generate test case for last redfish command executed
```
 

- Scripting and repeatable workflows
```ssh
    save <name to save> <count of previous commands to save>
    r -f <name saved before>
    repeat <name save before> <no of times to execute>
``` 

### Build instructions

Prerequisites
- JDK 21 or newer installed and JAVA_HOME set.
- Maven (3.6+) or Gradle (6+) installed if the project uses them.
- Git (to clone the repository) and Docker (optional, for container images).

Quick steps (Maven)
1. Clone and enter repo:
    ```bash
    git clone <repo-url>
    cd <repo-root>
    ```
2. Build the executable JAR:
    ```bash
    mvn -B clean package
    ```
    - The artifact will appear under target/, e.g. target/bmcshell-*.jar



Run locally
- Start with default port 8443:
  ```bash
  java -Dserver.port=8443 -jar target/bmcshell-*.jar
  ```

Docker (optional)
- Build image:
  ```bash
  docker build -t bmcshell:latest .
  ```
- Run container:
  ```bash
  docker run -p 8443:8443 --name bmcshell bmcshell:latest
  ```
