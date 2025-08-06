```mermaid
graph TD
    A[Client (CLI, App, Browser)] --> B(HTTP API)
    B --> C[AI Model Server]
    C --> D[Model Runners]
    D --> E[Local Model Storage]
```