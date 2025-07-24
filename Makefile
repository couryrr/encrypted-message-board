.PHONY: generate clean lint breaking format setup-plugins

# Generate code from protobuf definitions
generate:
	cd proto && buf generate

# Clean generated files
clean:
	rm -rf api/gen/*
	find data/src/main/java -name "*Grpc.java" -delete
	find data/src/main/java -name "*Proto.java" -delete

# Lint protobuf files
lint:
	cd proto && buf lint

# Check for breaking changes
breaking:
	cd proto && buf breaking --against '.git#branch=main'

# Format protobuf files
format:
	cd proto && buf format -w

# Setup protoc plugins
setup-plugins:
	go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
	go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest