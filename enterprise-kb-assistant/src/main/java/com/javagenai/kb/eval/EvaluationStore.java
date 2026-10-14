package com.javagenai.kb.eval;

import java.util.List;

public interface EvaluationStore {

    void save(EvalRun run);

    List<EvalRun> list();
}
