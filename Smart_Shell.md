## Smart Shell
A smart CLI for OpenBmc developers

## Idea Abstract
A Command Line Interface (CLI) for Enterprise Baseboard Management Controller (eBMC). Smart Shell enables developers to interact with Power server BMCs using both Redfish (over HTTPS) and SSH transports, providing a unified interface for management and automation tasks.

### Key Features

#### Dual Protocol Support
Smart Shell communicates with BMCs using both Redfish APIs and SSH, allowing users to execute a wide range of management commands and access Redfish menus seamlessly.

#### Intelligent Shell
Smart Shell integrates with a local Large Language Model (LLM), enabling an AI assistant feature. Developers can use this assistant to interpret responses from Redfish and SSH requests, and to generate unit test cases in their chosen frameworks.

#### Easy Onboarding

The AI assistant streamlines the onboarding process for new OpenBMC developers. By enabling seamless integration of custom Retrieval-Augmented Generation (RAG) models trained specifically on BMC and PowerPC knowledge bases, it provides targeted answers to developers’ questions. This transforms Smart Shell into an effective training and teaching tool, supporting developers throughout their onboarding journey. 

#### Copilot for BMC Developers

The AI assistant serves as a context-aware reference manual and cheat sheet for experienced BMC developers. It provides targeted answers during troubleshooting and supports rapid development and testing with intelligent auto-completion capabilities.

#### Automated Test Generation
The tool assists developers in generating automated test steps, streamlining the process of validating and troubleshooting BMC functionalities.

#### Dump Analysis

Smart Shell supports downloading and extracting dump files from the BMC. With the integrated LLM, developers can easily analyze dump data and troubleshoot root causes, streamlining the debugging process.

#### Data Privacy and Security
Smart Shell operates entirely offline. The integrated LLM runs locally and is packaged with the tool, ensuring that all sensitive data stays on the user's system. This approach eliminates external data exposure and enhances security for developers.

#### Containerized Deployment
The entire setup is packaged into a Docker container, making installation and usage convenient across different environments.

