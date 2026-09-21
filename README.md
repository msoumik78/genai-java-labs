# Hugging Face pipelines in Java (no Spring)

Standalone Maven project: run **Hugging Face Hub** models **in-process** from Java 21. No Spring Boot, no Python sidecar, no Hugging Face token for the public checkpoints used here.

Python is still the easiest HF UX (`pipeline()`, `AutoModel`). This repo is for **Java shops** that want the same *local inference* idea next to the rest of the estate.

## Two paths

| | DJL + PyTorch | ONNX Runtime |
|---|---|---|
| Main class | `com.javagenai.lab3.djl.DjlPytorchMpsMain` | `com.javagenai.lab3.onnx.OnnxCoreMlMain` |
| What you download | Hub PyTorch repo (`safetensors` / `.bin` + config + tokenizer) plus **libtorch** natives | Exported `model.onnx` + `tokenizer.json` |
| Accelerator on Apple Silicon | **MPS** (Metal), then CPU | **CoreML** if the graph loads, else CPU |
| Closest Python analogue | `pipeline(task)` (string in, labels out) | `AutoTokenizer` + a frozen `forward` (`session.run`) |
| Cache | `~/.djl.ai/` (and sometimes `~/.cache/huggingface/`) | `~/.cache/lab3-hf/` |

Inference uses **native engines inside the Java process** (JNI). Weights are not Java heap objects.

There is **no CUDA** on a Mac. NVIDIA CUDA is not used.

Gated Hub models (Llama, some Gemma, private repos) need `HF_TOKEN`. These demos do not.

## Requirements

- JDK 21+
- Maven 3.9+
- Network on **first** run of each task (model download)
- Apple Silicon recommended for MPS/CoreML; Linux/Windows run on CPU

## Run

```bash
mvn -q compile

# DJL / PyTorch (first run is the large download)
mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.djl.DjlPytorchMpsMain \
  -Dexec.args='sentiment I love this masterclass'

# Force CPU
mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.djl.DjlPytorchMpsMain \
  -Dlab3.device=cpu \
  -Dexec.args=sentiment

# ONNX Runtime
mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.onnx.OnnxCoreMlMain \
  -Dexec.args='ner Apple hired Tim Cook in London'
```

First argument is the **task**; the rest is text. For QA, separate question and context with `|`.

```bash
mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.djl.DjlPytorchMpsMain \
  -Dexec.args='qa Where does the class start? | The GenAI Java lab starts in Bangalore on 10 October 2026.'

mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.djl.DjlPytorchMpsMain \
  -Dexec.args='embed Java on Apple Silicon'

mvn -q exec:java -Dexec.mainClass=com.javagenai.lab3.djl.DjlPytorchMpsMain \
  -Dexec.args='fill-mask Paris is the [MASK] of France.'
```

| Task | DJL | ONNX |
|---|---|---|
| `sentiment` | DistilBERT SST-2 | Xenova DistilBERT ONNX |
| `ner` | `dslim/bert-base-NER` | Xenova `bert-base-NER` |
| `qa` | DistilBERT SQuAD | Xenova SQuAD ONNX |
| `embed` | MiniLM-L6-v2 | Xenova MiniLM ONNX |
| `fill-mask` | BERT-base `[MASK]` | Not implemented (use DJL) |

Omit the task name to default to `sentiment` with a built-in sentence. `help` prints usage.

## Layout

```
src/main/java/com/javagenai/lab3/
  Cli.java                 # task + text parsing
  HubCache.java            # ONNX downloads → ~/.cache/lab3-hf
  djl/DjlPytorchMpsMain.java
  onnx/OnnxCoreMlMain.java
```

## What this is not

Hugging Face is more than `pipeline()`. **Useful from Java:** tokenizers, embeddings, classification, NER, extractive QA, ONNX exports. **Stay in Python:** Datasets, Trainer, PEFT, TRL, most Diffusers work.

Seq2seq (summarize / translate) and image generation use the **same two engines** with different graphs and a decode loop. They are not wired in this lab.

Spring AI / LangChain4j / LangGraph4j are **chat and agent** layers. They do not replace loading an encoder from the Hub. Spring AI can still run some ONNX embeddings; this repo is the explicit HF local-inference walkthrough.

## License of models

Demo checkpoints are third-party Hub models (Apache/MIT-style cards unless the model page says otherwise). Check each [model card](https://huggingface.co) before production use.
