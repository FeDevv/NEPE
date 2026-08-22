# Project Architectural Guidelines & Code Principles

## Software Design Principles
* **Proven Paradigms & Patterns**: Whenever applicable, adhere to established software engineering best practices, including Gang of Four (GoF) design patterns, SOLID principles, high cohesion, low coupling, and DRY (Don't Repeat Yourself).
* **Pragmatism & Simplicity**: Do not over-engineer. Avoid applying patterns dogmatically if they introduce unnecessary complexity, boilerplate, or architectural bloat.

## Performance & Resource Optimization
* **Simplicity & Efficiency First**: If a simpler, more direct implementation exists that is measurably superior in CPU usage, memory footprint, or execution performance, prioritize that approach over an overly abstract design.
* **Justification**: When deviating from a standard design pattern in favor of performance or simplicity, briefly explain the trade-offs and rationale.