ifeq ($(OS),Windows_NT)
	build_os := windows
	build_arch := amd64
else
	UNAME_S := $(shell uname -s)

	ifeq ($(UNAME_S),Darwin)
		build_os := darwin
	endif

	ifeq ($(UNAME_S),Linux)
		build_os := linux
	endif

	ARCH := $(shell uname -m)
	ifeq ($(ARCH),x86_64)
		build_arch = amd64
	else ifeq ($(ARCH),arm64)
		build_arch = arm64
	else
	    $(error Architecture not supported yet)
	endif
endif

# Compile everything!
build: build-headers build-rust build-java

# Compile the Rust part (only for one target).
# We relay this command to the others, to make sure that
# artifacts are set properly.
build-rust: build-rust-$(build_arch)-$(build_os)

# Compile the Java part (incl. `build-test`, see `gradlew`).
build-java:
	"./gradlew" --info build

# Generate the Java C headers.
build-headers:
	"./gradlew" --info generateJniHeaders

# Run the tests.
test: build-headers build-rust test-rust build-java

# Run the Rust tests.
test-rust: test-rust-$(build_arch)-$(build_os)

test-rust-amd64-darwin:
	cargo test --lib --release --target=x86_64-apple-darwin

test-rust-arm64-darwin:
	cargo test --lib --release --target=aarch64-apple-darwin

test-rust-amd64-linux:
	cargo test --lib --release --target=x86_64-unknown-linux-gnu

test-rust-amd64-windows:
	cargo test --lib --release --target=x86_64-pc-windows-msvc

# Run the Java tests.
test-java:
	"./gradlew" --info test

# Test the examples.
test-examples:
	@for example in $(shell find examples -name "*Example.java") ; do \
		example=$${example%examples/}; \
		example=$${example%Example.java}; \
		echo "Testing $${example}"; \
		make run-example EXAMPLE=$${example}; \
	done

# Generate JavaDoc.
javadoc:
	"./gradlew" javadoc
	@echo "\n\n"'Open `build/docs/javadoc/index.html`.'

# Make a JAR-file.
package:
	"./gradlew" --info jar

# Run a specific example, with `make run-example EXAMPLE=Simple` for instance.
run-example:
	$(eval JAR := $(shell find ./build/libs/ -name "wasmer-jni-*.jar"))
	@cd examples; \
		javac -classpath "../${JAR}" ${EXAMPLE}Example.java; \
		java -Djava.library.path=$(CURDIR)/artifacts/$(build_os)-$(build_arch) -classpath ".:../${JAR}" -enableassertions ${EXAMPLE}Example

# Runs the main class, cd to java is necessary because java.org.wasmer is a "Prohibited package name"
run-main:
	$(eval JAR := $(shell find ./build/libs/ -name "wasmer-jni-*.jar"))
	@cd src; \
		javac -sourcepath ./java -classpath "./${JAR}" java/org/wasmer/Main.java; \
		cd java && java -Djava.library.path=$(CURDIR)/artifacts/$(build_os)-$(build_arch) -classpath ".:../${JAR}" org/wasmer/Main; \
		find org -type f -name "*.class" -delete
# Clean
clean:
	cargo clean
	rm -rf build
	rm -rf artifacts
