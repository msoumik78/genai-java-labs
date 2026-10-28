# Lab 10 — Agent control plane

The plane is the directory. Each agent publishes a card. For every capability on the task route the plane asks `accept`, then `execute`. A refusal or a thrown exception tries the next agent registered for that capability.

State moves only `PENDING → RUNNING → COMPLETED | FAILED | CANCELLED`. Each move is an event.

```bash
mvn test
mvn -q compile exec:java -Dexec.mainClass=com.javagenai.lab10.Demo
```

The agents sleep, then print. `reasoning-primary` fails on purpose so `reasoning-backup` runs. The demo then starts a second task and cancels it during ingestion. Validation does not print for that task.
