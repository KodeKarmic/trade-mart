# Trade-Mart architecture diagrams

This folder contains PlantUML diagrams describing the Trade-Mart system architecture.

Files:

- `trade-mart.puml` — high-level system architecture diagram including components: `trade-store`, `trade-expiry`, `Kafka`, `Postgres`, `MongoDB`, observability (Prometheus, OpenTelemetry), and autoscaling (KEDA).
- `trade-mart-sequences.puml` — (PlantUML) sequence diagrams for key flows and modules (`trade-store`, worker consumers, `trade-expiry`, `trade-repair`, and OIDC token flow).
- `trade-mart-sequences.mmd` — (Mermaid) sequence diagrams for key module-level flows with alternate paths (preferred for quick editing/preview in VS Code).
- `system-diagram-trade-mart.puml` — PlantUML system diagram (high level).
- `system-diagram-trade-mart.mmd` — Mermaid system architecture diagram (high level).

Render the diagram locally (requires PlantUML and Graphviz):

```powershell
# render PNG
plantuml -tpng docs/architecture/trade-mart.puml

# render SVG
plantuml -tsvg docs/architecture/trade-mart.puml
```

If you use VS Code, install the "PlantUML" extension and open the `.puml` file to preview.

To render the sequence diagrams:

With Mermaid CLI (mmdc) installed, render Mermaid (.mmd) diagrams:

```powershell
# render PNG
mmdc -i docs/architecture/trade-mart-sequences.mmd -o docs/architecture/trade-mart-sequences.png

# render SVG
mmdc -i docs/architecture/trade-mart-sequences.mmd -o docs/architecture/trade-mart-sequences.svg
```

Or use the PlantUML commands to render the existing PlantUML architecture file:

```powershell
plantuml -tpng docs/architecture/trade-mart.puml
plantuml -tsvg docs/architecture/trade-mart.puml
```

In VS Code you can preview Mermaid files with the built-in Mermaid preview (or the "Markdown Preview Mermaid Support" extension) and PlantUML files with the PlantUML extension.

To render the Mermaid system diagram with Mermaid CLI (`mmdc`):

```powershell
# render PNG
mmdc -i docs/architecture/system-diagram-trade-mart.mmd -o docs/architecture/system-diagram-trade-mart.png

# render SVG
mmdc -i docs/architecture/system-diagram-trade-mart.mmd -o docs/architecture/system-diagram-trade-mart.svg
```

If you prefer PlantUML for the system diagram you can still render:

```powershell
plantuml -tpng docs/architecture/system-diagram-trade-mart.puml
plantuml -tsvg docs/architecture/system-diagram-trade-mart.puml
```
