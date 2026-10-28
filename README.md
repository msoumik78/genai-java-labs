# Lab 10 — Agent control plane

The plane looks up an agent for each capability on the task route and runs it. If that agent throws, the next agent registered for the same capability is tried.

```bash
mvn test
mvn -q compile exec:java -Dexec.mainClass=com.javagenai.lab10.Demo
```

The three agents only print a completion line and pass the text through. A task ends `COMPLETED` or `FAILED`.
