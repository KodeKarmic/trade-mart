# Autoscaling `trade-store` using the pending-message metric

This document explains how to autoscale the `trade-store` deployment using the Micrometer gauge
exposed by the application.

Metric provided by the application
- Metric name: `trade_processor_pending_messages`
- What it measures: number of trade messages currently pending/in-flight on this instance (an AtomicLong bound to a Micrometer Gauge).
- Where it's exposed: via the Prometheus endpoint (if `micrometer-registry-prometheus` + actuator are enabled):
  - `/actuator/prometheus`

Notes
- This metric reflects work currently being processed on each pod, not the total topic lag. For many autoscaling scenarios that correlate instance load to backlog, summing this gauge across pods works well (Prometheus `sum(trade_processor_pending_messages)`).
- If you need accurate Kafka topic lag (unconsumed messages in the topic across the consumer group), consider adding an AdminClient-based lag calculator or use KEDA's Kafka scaler which supports consumer-group lag directly.

Examples

1) KEDA ScaledObject (Prometheus scaler)

This example instructs KEDA to query Prometheus and scale the `trade-store-deployment` when the summed pending messages across all pods exceed `5`.

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: trade-store-scaledobject
  namespace: default
spec:
  scaleTargetRef:
    name: trade-store-deployment
  pollingInterval: 30     # seconds between checks
  cooldownPeriod: 300     # seconds to wait after scale down before next scale action
  minReplicaCount: 1
  maxReplicaCount: 10
  triggers:
  - type: prometheus
    metadata:
      serverAddress: http://prometheus-operated.monitoring.svc:9090
      # Query should return a single numeric value. We sum the pending gauge across pods.
      query: "sum(trade_processor_pending_messages)"
      threshold: '5'
```

Make sure Prometheus is scraping the application `/actuator/prometheus` endpoint (ServiceMonitor / PodMonitor). The `serverAddress` must point to your Prometheus server.

2) Kubernetes HPA (using Prometheus Adapter / Custom Metrics)

If you have the Prometheus Adapter (or other custom metrics adapter) installed and configured to expose the `trade_processor_pending_messages` metric as a Pods metric, you can define an HPA like this:

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: trade-store-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: trade-store-deployment
  minReplicas: 1
  maxReplicas: 10
  metrics:
  - type: Pods
    pods:
      metric:
        name: trade_processor_pending_messages
      target:
        type: AverageValue
        # target average pending messages per pod
        averageValue: "2"
```

Adjust `averageValue`, `minReplicas`, and `maxReplicas` to fit your traffic patterns.

Alternative: KEDA Kafka scaler
- KEDA also provides a Kafka scaler that can scale by actual consumer group lag. Example:

```yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: trade-store-kafka-scaledobject
spec:
  scaleTargetRef:
    name: trade-store-deployment
  pollingInterval: 30
  cooldownPeriod: 300
  minReplicaCount: 1
  maxReplicaCount: 50
  triggers:
  - type: kafka
    metadata:
      bootstrapServers: my-cluster-kafka-bootstrap.kafka.svc:9092
      topic: trades
      consumerGroup: trade-store-group
      lagThreshold: '100'   # scale when lag > 100
```

Security and networking notes
- Ensure Prometheus can reach the application metrics endpoint. If using RBAC/service discovery (ServiceMonitor), configure accordingly.
- If running in Kubernetes, expose an internal Service for Prometheus scraping.

Troubleshooting
- If the metric does not appear in Prometheus, confirm `/actuator/prometheus` contains the `trade_processor_pending_messages` metric and Prometheus scrape target is configured.
- If autoscaler does not react, verify KEDA or Prometheus Adapter logs and that the query/metric is returning expected values.

If you want, I can add a sample `ServiceMonitor` / `PodMonitor` and a KEDA ScaledObject configured for your cluster's Prometheus endpoint.
