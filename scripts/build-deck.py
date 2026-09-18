#!/usr/bin/env python3
"""GenAI Java — quick workshop slides."""

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.util import Inches, Pt

W, H = Inches(13.333), Inches(7.5)
BG = RGBColor(0xF4, 0xF7, 0xFB)
CARD = RGBColor(0xFF, 0xFF, 0xFF)
CARD2 = RGBColor(0xE8, 0xF0, 0xFE)
LINE = RGBColor(0xCB, 0xD5, 0xE1)
CYAN = RGBColor(0x02, 0x6A, 0xA7)
AMBER = RGBColor(0xB4, 0x53, 0x09)
MINT = RGBColor(0x04, 0x78, 0x57)
ROSE = RGBColor(0xBE, 0x12, 0x3C)
TEXT = RGBColor(0x0F, 0x17, 0x2A)
MUTED = RGBColor(0x47, 0x55, 0x69)
NAVY = RGBColor(0x1E, 0x3A, 0x5F)
CODE_BG = RGBColor(0xF1, 0xF5, 0xF9)


def set_run(run, size=18, bold=False, color=TEXT, font="Calibri"):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = font


def fill(shape, color, line=None):
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    if line is None:
        shape.line.fill.background()
    else:
        shape.line.color.rgb = line
        shape.line.width = Pt(1)


def box(slide, l, t, w, h, color=CARD):
    s = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, l, t, w, h)
    fill(s, color, LINE)
    s.adjustments[0] = 0.08
    return s


def txt(slide, l, t, w, h, text, size=18, bold=False, color=TEXT, align=PP_ALIGN.LEFT, font="Calibri"):
    tb = slide.shapes.add_textbox(l, t, w, h)
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.alignment = align
    r = p.add_run()
    r.text = text
    set_run(r, size, bold, color, font)
    return tb


def code_box(slide, l, t, w, h, text, size=11):
    box(slide, l, t, w, h, CODE_BG)
    tb = slide.shapes.add_textbox(l + Inches(0.12), t + Inches(0.08), w - Inches(0.2), h - Inches(0.12))
    tf = tb.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    r = p.add_run()
    r.text = text
    set_run(r, size, False, NAVY, "Consolas")
    return tb


def footer(slide, n, total=8):
    txt(slide, Inches(0.5), Inches(7.15), Inches(8.5), Inches(0.28),
        "GenAI Java  ·  open weights  ·  Spring AI  ·  production RAG", 13, False, MUTED)
    txt(slide, Inches(11.0), Inches(7.15), Inches(1.8), Inches(0.28),
        f"{n}  /  {total}", 13, False, MUTED, PP_ALIGN.RIGHT)


def new_slide(prs):
    s = prs.slides.add_slide(prs.slide_layouts[6])
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, W, H)
    fill(bg, BG)
    accent = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(0.12), H)
    fill(accent, CYAN)
    return s


def kicker(slide, text):
    txt(slide, Inches(0.55), Inches(0.22), Inches(12), Inches(0.32), text.upper(), 14, True, CYAN)


def title(slide, text, size=32):
    txt(slide, Inches(0.55), Inches(0.48), Inches(12.2), Inches(0.62), text, size, True, TEXT)


def main():
    prs = Presentation()
    prs.slide_width, prs.slide_height = W, H
    total = 8

    # 1 title
    s = new_slide(prs)
    txt(s, Inches(0.7), Inches(1.65), Inches(12), Inches(0.4),
        "WORKSHOP  ·  JAVA 21  ·  SPRING AI", 17, True, AMBER)
    txt(s, Inches(0.7), Inches(2.1), Inches(12), Inches(1.2),
        "GenAI on the JVM is not a Python port.", 42, True, TEXT)
    txt(s, Inches(0.7), Inches(3.4), Inches(11.5), Inches(1.15),
        "Open-weight models are exploding (MoE, 1-bit, disk-streamed experts).\n"
        "Spring still needs typed beans, pools, and SQL — not a prompt ACL.",
        22, False, MUTED)
    box(s, Inches(0.7), Inches(5.05), Inches(3.6), Inches(1.25), CARD)
    txt(s, Inches(0.9), Inches(5.2), Inches(3.3), Inches(1.0),
        "Lab 1  ·  Bulkhead\nSlow LLM must not starve /orders", 17, False, TEXT)
    box(s, Inches(4.5), Inches(5.05), Inches(3.9), Inches(1.25), CARD)
    txt(s, Inches(4.7), Inches(5.2), Inches(3.55), Inches(1.0),
        "Lab 2  ·  Multi-tenant RAG\ntenant_id in SQL / RLS, not the prompt", 17, False, TEXT)
    box(s, Inches(8.6), Inches(5.05), Inches(3.9), Inches(1.25), CARD2)
    txt(s, Inches(8.8), Inches(5.2), Inches(3.55), Inches(1.0),
        "Cohort  ·  6 Sundays from 10 Oct\n₹10,000 / seat  ·  4 hrs each", 17, False, TEXT)
    footer(s, 1, total)

    # 2 merged landscape
    s = new_slide(prs)
    kicker(s, "01  ·  the map")
    title(s, "We all know LLMs. The interesting layer is how they run.")
    cards = [
        ("Dense LLM", "Every parameter fires every token. GPT-class API. Simple. Expensive at the edge.", CYAN),
        ("MoE", "Hundreds of experts; few activate. Qwen / Mixtral / GLM scale without dense cost.", AMBER),
        ("Colibri", "Pure C. Stream routed experts from NVMe. ~744B MoE on ~25 GB RAM. SSD is a memory tier.", MINT),
        ("Bonsai · PrismML", "1-bit / ternary Qwen 27B. ~4 GB on a phone. Apache-2. End-to-end low-bit, not fake quant.", ROSE),
    ]
    for i, (h, b, c) in enumerate(cards):
        x = Inches(0.45 + i * 3.2)
        box(s, x, Inches(1.25), Inches(3.05), Inches(3.55), CARD)
        bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, x, Inches(1.25), Inches(3.05), Inches(0.1))
        fill(bar, c)
        txt(s, x + Inches(0.16), Inches(1.48), Inches(2.75), Inches(0.55), h, 20, True, TEXT)
        txt(s, x + Inches(0.16), Inches(2.1), Inches(2.75), Inches(2.45), b, 16, False, MUTED)
    box(s, Inches(0.45), Inches(4.95), Inches(12.4), Inches(1.9), CARD2)
    txt(s, Inches(0.7), Inches(5.1), Inches(12.0), Inches(0.4), "Why Java teams should care", 18, True, CYAN)
    txt(s, Inches(0.7), Inches(5.5), Inches(12.0), Inches(1.2),
        "MoE sparsity, Colibri’s disk-streamed experts, and Bonsai on-device are systems problems — "
        "pools, RAM, NVMe — not prompt engineering. Python notebooks hide the runtime. "
        "Spring Boot cannot: ChatClient is a bean with a thread pool, a timeout, and a bill. "
        "IT will ask for zero-cloud (still a laptop, still weights on disk). That is the workshop.",
        17, False, MUTED)
    footer(s, 2, total)

    # 3 Spring AI beans
    s = new_slide(prs)
    kicker(s, "02  ·  spring ai")
    title(s, "Main beans — one interface, many auto-configs")
    beans = [
        ("ChatClient", "Fluent API controllers call. Built from ChatClient.Builder. Advisors, memory, tools."),
        ("ChatModel", "The SPI. OpenAiChatModel, OllamaChatModel, VertexAiGeminiChatModel…"),
        ("EmbeddingModel", "Vectors for RAG. Dims, prefixes, ONNX vs HTTP differ per provider."),
        ("VectorStore", "Similarity API. Filter is your job — or you leak tenants."),
        ("Observation", "Micrometer: model, tokens, latency. Guardrails stop the span as error."),
        ("ToolCallback", "Function calling. Side effects must sit outside retries."),
    ]
    for i, (h, b) in enumerate(beans):
        col, row = i % 3, i // 3
        x, y = Inches(0.45 + col * 4.25), Inches(1.28 + row * 2.7)
        box(s, x, y, Inches(4.05), Inches(2.5), CARD)
        txt(s, x + Inches(0.22), y + Inches(0.22), Inches(3.6), Inches(0.5), h, 22, True, AMBER)
        txt(s, x + Inches(0.22), y + Inches(0.8), Inches(3.6), Inches(1.45), b, 17, False, MUTED)
    footer(s, 3, total)

    # 4 Python vs Java providers (was slide 5)
    s = new_slide(prs)
    kicker(s, "02  ·  same chat completions myth")
    title(s, "Python swaps base_url. Spring still wants a bean per runtime.", 28)
    cols = [
        ("OpenAI",
         "from openai import OpenAI\n"
         "c = OpenAI()  # api.openai.com\n"
         "c.chat.completions.create(\n"
         "  model=\"gpt-4o-mini\",\n"
         "  messages=[{\"role\":\"user\",\n"
         "    \"content\": q}])",
         "@Bean ChatClient openai(\n"
         "  OpenAiChatModel m) {\n"
         "  return ChatClient.create(m);\n"
         "}\n"
         "spring.ai.openai.api-key\n"
         "spring.ai.openai.chat.options.model"),
        ("Ollama",
         "from openai import OpenAI\n"
         "c = OpenAI(\n"
         "  base_url=\"http://127.0.0.1:11434/v1\",\n"
         "  api_key=\"ollama\")\n"
         "c.chat.completions.create(\n"
         "  model=\"llama3.2\", messages=…)",
         "@Bean ChatClient ollama(\n"
         "  OllamaChatModel m) {\n"
         "  return ChatClient.create(m);\n"
         "}\n"
         "spring.ai.ollama.base-url\n"
         "spring.ai.ollama.chat.options.model"),
        ("Gemini",
         "from google import genai\n"
         "c = genai.Client()\n"
         "c.models.generate_content(\n"
         "  model=\"gemini-2.5-flash\",\n"
         "  contents=q)\n"
         "# not /v1/chat/completions",
         "@Bean ChatClient gemini(\n"
         "  VertexAiGeminiChatModel m) {\n"
         "  return ChatClient.create(m);\n"
         "}\n"
         "spring.ai.vertex.ai.gemini\n"
         "  .project-id  .location"),
    ]
    for i, (name, py, jv) in enumerate(cols):
        x = Inches(0.4 + i * 4.3)
        box(s, x, Inches(1.18), Inches(4.15), Inches(5.7), CARD)
        txt(s, x + Inches(0.18), Inches(1.28), Inches(3.8), Inches(0.38), name, 20, True, CYAN)
        txt(s, x + Inches(0.18), Inches(1.68), Inches(3.8), Inches(0.28), "Python", 14, True, AMBER)
        code_box(s, x + Inches(0.15), Inches(1.98), Inches(3.85), Inches(2.15), py, 12)
        txt(s, x + Inches(0.18), Inches(4.2), Inches(3.8), Inches(0.28), "Spring bean", 14, True, MINT)
        code_box(s, x + Inches(0.15), Inches(4.5), Inches(3.85), Inches(2.2), jv, 12)
    footer(s, 4, total)

    # 5 bulkhead
    s = new_slide(prs)
    kicker(s, "03  ·  lab 1")
    title(s, "Bulkhead: a slow model must not starve the rest of the app")
    box(s, Inches(0.45), Inches(1.28), Inches(4.05), Inches(5.5), CARD)
    txt(s, Inches(0.65), Inches(1.45), Inches(3.65), Inches(0.4), "Naive", 20, True, ROSE)
    txt(s, Inches(0.65), Inches(1.95), Inches(3.65), Inches(4.5),
        "Chat holds the Tomcat request thread (max 8 on the laptop demo).\n\n"
        "/orders is 1 ms — until ten chats arrive.\n\n"
        "LLM-shaped failure: 429 vs 5xx vs hang. Retrying a completion like GET double-posts a refund.",
        17, False, MUTED)
    box(s, Inches(4.65), Inches(1.28), Inches(4.05), Inches(5.5), CARD)
    txt(s, Inches(4.85), Inches(1.45), Inches(3.65), Inches(0.4), "Fenced", 20, True, MINT)
    txt(s, Inches(4.85), Inches(1.95), Inches(3.65), Inches(4.5),
        "Dedicated executor, fence-size = 2.\n\n"
        "Extra chat → 429, not a stuck servlet.\n\n"
        "/orders stays snappy. Bulkhead is isolation, not “a connection pool slide.”",
        17, False, MUTED)
    box(s, Inches(8.85), Inches(1.28), Inches(4.0), Inches(5.5), CARD)
    txt(s, Inches(9.05), Inches(1.45), Inches(3.6), Inches(0.4), "Idempotency", 20, True, AMBER)
    txt(s, Inches(9.05), Inches(1.95), Inches(3.6), Inches(4.5),
        "Side effect once (ledger), then retry the model.\n\n"
        "Never refund inside the retry loop.\n\n"
        "Tools are not GETs.",
        17, False, MUTED)
    footer(s, 5, total)

    # 6 real RAG ingest
    s = new_slide(prs)
    kicker(s, "04  ·  ingest before you embed")
    title(s, "A real RAG pipeline starts with the file, not the vector DB", 28)
    steps = [
        ("1. Sniff", "Open the PDF. If extractable text density is high → born-digital. If almost no Unicode / only images → scanned. Do not OCR a text PDF; do not PDFBox a scan."),
        ("2. Digital parse", "Apache PDFBox (Java): page text, bookmarks, fonts. Keep reading order. Fail closed if glyphs are empty."),
        ("3. Scanned parse", "Docling (layout + OCR): pages, tables, figures. Call it from Java as a sidecar if you must; do not pretend PDFBox reads pixels."),
        ("4. Section-aware chunks", "Split on headings / ToC / H1–H3, not a blind 512-token window. A “chunk” is a section with parent title — that is what retrieval ranks."),
        ("5. Enrich + store", "tenant_id, doc_id, section_path on the row. Embed the section (BGE). pgvector. Isolation is a column, not a prompt."),
        ("6. Retrieve + generate", "WHERE tenant_id = ? then kNN. Distance cutoff. Stuff surviving sections only. Trace prompt version → tokens → cost."),
    ]
    for i, (h, b) in enumerate(steps):
        col, row = i % 3, i // 3
        x, y = Inches(0.4 + col * 4.3), Inches(1.22 + row * 2.75)
        box(s, x, y, Inches(4.15), Inches(2.55), CARD)
        txt(s, x + Inches(0.18), y + Inches(0.16), Inches(3.8), Inches(0.4), h, 20, True, CYAN)
        txt(s, x + Inches(0.18), y + Inches(0.62), Inches(3.8), Inches(1.75), b, 16, False, MUTED)
    footer(s, 6, total)

    # 7 leak proof
    s = new_slide(prs)
    kicker(s, "04  ·  the 90-minute proof")
    title(s, "Acme asks about PROJECT-ORION")
    box(s, Inches(0.45), Inches(1.28), Inches(6.1), Inches(5.5), CARD)
    txt(s, Inches(0.7), Inches(1.48), Inches(5.6), Inches(0.5), "Broken  ·  /search/naive", 22, True, ROSE)
    txt(s, Inches(0.7), Inches(2.15), Inches(5.6), Inches(4.3),
        "kNN only. tenant_id is unused metadata.\n\n"
        "leakedOtherTenant: true\n"
        "Globex M&A memo in the context window.\n\n"
        "/search/prompt = same retrieval.\n"
        "“Answer only for Acme” cannot unread tokens.",
        19, False, MUTED)
    box(s, Inches(6.8), Inches(1.28), Inches(6.05), Inches(5.5), CARD)
    txt(s, Inches(7.05), Inches(1.48), Inches(5.55), Inches(0.5), "Fixed  ·  /search/sql  and  /rls", 22, True, MINT)
    txt(s, Inches(7.05), Inches(2.15), Inches(5.55), Inches(4.3),
        "SQL: WHERE tenant_id = $1 then kNN.\n\n"
        "RLS: no tenant predicate in SQL.\n"
        "SET LOCAL ROLE lab_app +\n"
        "app.current_tenant. Superusers skip RLS.\n\n"
        "leakedOtherTenant: false",
        19, False, MUTED)
    footer(s, 7, total)

    # 8 cohort
    s = new_slide(prs)
    kicker(s, "05  ·  join")
    title(s, "GenAI Java cohort")
    txt(s, Inches(0.55), Inches(1.18), Inches(12), Inches(0.5),
        "Six Sundays. Four hours. From the first fence to a tenant-safe RAG.", 22, False, MUTED)
    facts = [
        ("Starts", "10 October 2026"),
        ("Cadence", "Every Sunday · 4 hours"),
        ("Duration", "6 Sundays"),
        ("Seat", "₹10,000 INR"),
    ]
    for i, (h, b) in enumerate(facts):
        x = Inches(0.45 + i * 3.2)
        box(s, x, Inches(1.8), Inches(3.05), Inches(1.75), CARD)
        txt(s, x + Inches(0.18), Inches(1.98), Inches(2.7), Inches(0.35), h.upper(), 14, True, CYAN)
        txt(s, x + Inches(0.18), Inches(2.4), Inches(2.7), Inches(0.9), b, 20, True, TEXT)
    box(s, Inches(0.45), Inches(3.8), Inches(12.4), Inches(2.95), CARD2)
    txt(s, Inches(0.75), Inches(4.0), Inches(11.9), Inches(0.4), "What you leave with", 20, True, AMBER)
    txt(s, Inches(0.75), Inches(4.5), Inches(11.9), Inches(2.0),
        "Spring AI ChatClient vs ChatModel vs provider auto-config  ·  bulkheads and idempotent tools  ·  "
        "PDF sniff → PDFBox / Docling → section-aware chunks  ·  "
        "pgvector with tenant_id in SQL  ·  in-process embeddings  ·  "
        "how open-weight runtimes fail in production-shaped ways.\n\n"
        "Java 21. Architects in the room. No “hello ChatGPT” labs.",
        18, False, MUTED)
    footer(s, 8, total)

    out = "/Users/soumikmukherjee/personalCodebase/genai-java-labs/GenAI-Java-Quick-Slides.pptx"
    prs.save(out)
    print(out)


if __name__ == "__main__":
    main()
