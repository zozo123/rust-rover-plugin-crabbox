//! A tiny library that exists so the Crabbox Runner plugin has a real Rust
//! project to execute remotely. Open `examples/hello-crabbox` in RustRover and
//! run `Tools > Crabbox > Run Cargo Test on Islo` to prove the path end to end.

/// Returns a greeting that names where the code actually ran.
pub fn greet(runner: &str) -> String {
    format!("Hello from {runner}!")
}

/// Sums a slice. Trivial, but it gives the remote sandbox something to compile
/// and a test to pass.
pub fn sum(values: &[i64]) -> i64 {
    values.iter().sum()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn greet_names_the_runner() {
        assert_eq!(greet("crabbox"), "Hello from crabbox!");
    }

    #[test]
    fn sum_adds_values() {
        assert_eq!(sum(&[1, 2, 3, 4]), 10);
        assert_eq!(sum(&[]), 0);
    }
}
