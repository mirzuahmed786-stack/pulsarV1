// hassam dev: Simple build script - no scaffolding needed for raw JNI

fn main() {
    // Rerun if source files change
    println!("cargo:rerun-if-changed=src/");
}

