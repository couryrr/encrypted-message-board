# Agent Guidelines for Backend Playbook

## Build/Test/Lint Commands
- **API (Go)**: `cd api && go build`, `go test ./...`, `go fmt ./...`, `go vet ./...`
- **Data (Java/Spring Boot)**: `cd data && ./mvnw clean compile`, `./mvnw test`, `./mvnw test -Dtest=ClassName#methodName`
- **UI Dashboard/Signup (SolidJS)**: `cd ui/dashboard && npm run build`, `npm run dev`, `npm test` (if available)

## Code Style Guidelines
- **Java**: Use Spring Boot conventions, package names follow `github.couryrr.backend.playbook.*`, camelCase for methods/variables
- **Go**: Follow standard Go formatting with `go fmt`, use descriptive package names, error handling with explicit returns
- **JavaScript/SolidJS**: Use ES6+ syntax, functional components, CSS modules for styling, JSX with `class` not `className`
- **Imports**: Group standard library first, then third-party, then local imports with blank lines between groups
- **Naming**: Use descriptive names, avoid abbreviations, follow language conventions (camelCase for Java/JS, snake_case for Go when appropriate)
- **Error Handling**: Always handle errors explicitly, use appropriate logging levels, return meaningful error messages

## Git Workflow
- Work on branches: `llm/<short-feature-description>`
- Rebase against main before merging
- Follow conventional commit messages

## Architecture Notes
- Multi-service setup: API (Go/Chi), Data (Spring Boot/JDBC), UI (SolidJS)
- Communication: REST between UI-API, gRPC between API-Data (planned)
- Build tools: Maven (data), Go modules (api), npm/Vite (ui)