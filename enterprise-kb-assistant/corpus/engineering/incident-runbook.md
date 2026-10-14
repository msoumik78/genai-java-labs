# Incident Runbook

This runbook covers on-call response for the payments platform.

## Severity levels

- **SEV1**: customer-facing payments failing; page the primary on-call immediately
- **SEV2**: degraded latency above 2 seconds at p95; primary on-call responds within 15 minutes
- **SEV3**: single region degradation with failover holding; next business day

## First response steps

1. Acknowledge the page within 5 minutes
2. Open the incident channel with the severity tag
3. Check the payments dashboard for blast radius
4. If SEV1, page the secondary and notify the engineering manager

## Escalation

Escalate to the Director of Engineering if an incident is unresolved after 45 minutes.
SEV1 incidents always require an incident commander, even if the commander is not
personally fixing the problem.

## Post-incident

A blameless review is written within 5 working days for every SEV1 and SEV2. Action
items are tracked in the reliability backlog with an owner and a date.