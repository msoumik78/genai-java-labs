package com.javagenai.kb.eval;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * The tuning story: knobs to run an experiment, one row per run, deltas against
 * the first run, and recall plotted against faithfulness.
 */
@Path("/dashboard")
public class DashboardResource {

    @Inject
    EvaluationStore store;

    @Inject
    EvalResource experiments;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String page() {
        List<EvalRun> runs = store.list();
        return render(Dashboard.rows(runs), Dashboard.recallAgainstFaithfulness(runs));
    }

    /**
     * The knobs endpoint: submits one experiment and lands back on the
     * dashboard, where the new row appears against the baseline. Synchronous —
     * a run is a deliberate act and typically takes a minute or two.
     */
    @POST
    @Path("/run")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response runKnobs(
            @FormParam("label") String label,
            @FormParam("mode") String mode,
            @FormParam("rrfK") Double rrfK,
            @FormParam("candidateK") Integer candidateK,
            @FormParam("rerank") String rerank,
            @FormParam("gitSha") String gitSha) {
        String effectiveMode = (mode == null || mode.isBlank()) ? "hybrid" : mode.toLowerCase();
        String runLabel = (label == null || label.isBlank()) ? "knob-" + effectiveMode : label;
        EvalResource.RunRequest request = new EvalResource.RunRequest(
                runLabel, gitSha, null, effectiveMode, rrfK, candidateK, "true".equals(rerank));
        experiments.run(request);
        return Response.seeOther(URI.create("/dashboard")).build();
    }

    private static String render(List<Dashboard.Row> rows, List<Dashboard.Point> points) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="utf-8">
                <title>Evaluation</title>
                <style>
                  body { font-family: Georgia, serif; margin: 2rem; color: #1c1917; background: #fafaf9; }
                  h1 { font-weight: normal; }
                  table { border-collapse: collapse; width: 100%; background: white; }
                  th, td { text-align: right; padding: 0.4rem 0.7rem; border-bottom: 1px solid #e7e5e4; }
                  th:first-child, td:first-child { text-align: left; }
                  th { font-weight: normal; color: #57532e; }
                  .note { color: #78716c; font-size: 0.9rem; }
                  .good { color: #15803d; }
                  .bad { color: #b91c1c; }
                  .warn { color: #b45309; }
                  .config { color: #57532e; font-size: 0.85rem; }
                  .knobs { background: white; border: 1px solid #e7e5e4; padding: 1rem; margin: 1rem 0;
                           display: flex; flex-wrap: wrap; gap: 1rem; align-items: end; }
                  .knobs label { display: flex; flex-direction: column; font-size: 0.85rem; color: #57532e; gap: 0.25rem; }
                  .knobs input, .knobs select { padding: 0.35rem; border: 1px solid #d6d3d1; border-radius: 3px; font-size: 0.95rem; }
                  .knobs button { padding: 0.5rem 1.2rem; background: #1d4ed8; color: white; border: 0;
                                  border-radius: 3px; font-size: 1rem; cursor: pointer; }
                  .knobs button:hover { background: #1e40af; }
                  .knobs .hint { font-size: 0.8rem; color: #78716c; }
                  svg { background: white; border: 1px solid #e7e5e4; }
                </style>
                </head>
                <body>
                <h1>Experiment dashboard</h1>
                <p class="note">Tier 1 metrics never depend on a model; Tier 2 is LLM-judged
                (needs DEEPSEEK_API_KEY — without it runs still record Tier 1 and count failures).</p>
                """);
        html.append(knobsForm(rows));
        html.append("""
                <table>
                <thead><tr>
                <th>Step</th><th>Config</th><th>Cases</th><th>Failed</th>
                <th>Recall (T1)</th><th>Phrase (T1)</th><th>Citations (T1)</th>
                <th>Abstain</th><th>Faithfulness (T2)</th><th>Relevancy (T2)</th><th>Completeness (T2)</th>
                <th>Latency ms</th>
                </tr></thead><tbody>
                """);
        for (Dashboard.Row row : rows) {
            html.append("<tr><td>").append(escape(row.label())).append("</td>");
            html.append("<td class=\"config\">").append(escape(row.config())).append("</td>");
            html.append("<td>").append(row.cases()).append("</td>");
            html.append("<td>").append(row.failed() > 0
                    ? "<span class=\"warn\">" + row.failed() + "</span>" : "0").append("</td>");
            html.append("<td>").append(pct(row.contextRecall())).append("</td>");
            html.append("<td>").append(pct(row.phraseHit())).append("</td>");
            html.append("<td>").append(pct(row.citationValidity())).append("</td>");
            html.append("<td>").append(pct(row.abstainRate())).append("</td>");
            html.append("<td>").append(pct(row.faithfulness())).append("</td>");
            html.append("<td>").append(pct(row.relevancy())).append("</td>");
            html.append("<td>").append(pct(row.completeness())).append("</td>");
            html.append("<td>").append(row.meanLatencyMs() == null ? "—" : String.format(Locale.US, "%.0f", row.meanLatencyMs()));
            html.append("</td></tr>");
        }
        html.append("</tbody></table>");
        html.append(deltas(rows));
        html.append("<h1>Context recall vs faithfulness</h1>");
        html.append(scatter(points));
        html.append("</body></html>");
        return html.toString();
    }

    /**
     * The knobs: pick the retrieval mode, tweak RRF k and the candidate pool,
     * toggle the reranker, label the run. First run becomes the baseline that
     * later rows are diffed against.
     */
    private static String knobsForm(List<Dashboard.Row> rows) {
        String hint = rows.isEmpty()
                ? "this run will become your baseline"
                : "baseline: " + rows.getFirst().label() + " — new run is diffed against it";
        return """
                <form class="knobs" method="post" action="/dashboard/run">
                  <label>Mode
                    <select name="mode">
                      <option value="HYBRID">hybrid (vector+BM25+RRF)</option>
                      <option value="VECTOR_ONLY">vector only</option>
                      <option value="LEXICAL_ONLY">lexical only</option>
                    </select>
                  </label>
                  <label>RRF k
                    <input type="number" name="rrfK" value="60" min="1" max="1000" step="1">
                  </label>
                  <label>Candidate pool
                    <input type="number" name="candidateK" value="50" min="5" max="200" step="5">
                  </label>
                  <label style="flex-direction:row; align-items:center; gap:0.4rem;">
                    <input type="checkbox" name="rerank" value="true" checked> rerank
                  </label>
                  <label>Run label
                    <input type="text" name="label" placeholder="NEXT_LABEL">
                  </label>
                  <button type="submit">Run experiment</button>
                  <span class="hint">HINT_TEXT. Runs the full golden set, then reloads.</span>
                </form>
                """
                .replace("NEXT_LABEL", "r" + rows.size())
                .replace("HINT_TEXT", hint);
    }

    /**
     * First run is the baseline, last run the candidate: the delta band is the
     * "71% → 89% and here's how" statement, computed rather than asserted.
     */
    private static String deltas(List<Dashboard.Row> rows) {
        if (rows.size() < 2) {
            return "";
        }
        Dashboard.Row baseline = rows.getFirst();
        Dashboard.Row candidate = rows.getLast();
        StringBuilder html = new StringBuilder();
        html.append("<h1>").append(escape(candidate.label()))
                .append(" vs ").append(escape(baseline.label())).append("</h1>");
        html.append("<table><thead><tr><th>Metric</th><th>Before</th><th>After</th><th>Change</th></tr></thead><tbody>");
        for (Dashboard.Delta delta : Dashboard.deltas(baseline, candidate)) {
            String sign = delta.change() >= 0 ? "+" : "";
            String style = delta.change() >= 0 ? "good" : "bad";
            html.append("<tr><td>").append(escape(delta.metric())).append("</td>");
            html.append("<td>").append(pct(delta.before())).append("</td>");
            html.append("<td>").append(pct(delta.after())).append("</td>");
            html.append("<td class=\"").append(style).append("\">")
                    .append(sign).append(String.format(Locale.US, "%.1f", delta.change() * 100))
                    .append(" pp</td></tr>");
        }
        return html.append("</tbody></table>").toString();
    }

    /** Both axes are 0 to 1. A point exists only when both metrics were recorded. */
    private static String scatter(List<Dashboard.Point> points) {
        int size = 360;
        int pad = 28;
        StringBuilder svg = new StringBuilder();
        svg.append("<svg width=\"").append(size).append("\" height=\"").append(size).append("\" role=\"img\">");
        svg.append("<line x1=\"").append(pad).append("\" y1=\"").append(size - pad)
                .append("\" x2=\"").append(size - pad).append("\" y2=\"").append(size - pad)
                .append("\" stroke=\"#a8a29e\"/>");
        svg.append("<line x1=\"").append(pad).append("\" y1=\"").append(pad)
                .append("\" x2=\"").append(pad).append("\" y2=\"").append(size - pad)
                .append("\" stroke=\"#a8a29e\"/>");
        for (Dashboard.Point point : points) {
            int x = pad + (int) (point.contextRecall() * (size - 2 * pad));
            int y = size - pad - (int) (point.faithfulness() * (size - 2 * pad));
            svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                    .append("\" r=\"4\" fill=\"#1d4ed8\"><title>")
                    .append(escape(point.label())).append(' ').append(escape(point.caseId()))
                    .append("</title></circle>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private static String pct(Double value) {
        if (value == null) {
            return "—";
        }
        return String.format(Locale.US, "%.0f%%", value * 100);
    }

    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
