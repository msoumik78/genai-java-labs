package com.javagenai.lab2;

import com.javagenai.lab2.kb.TextEmbedder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class StubEmbedderConfig {

    @Bean
    @Primary
    TextEmbedder stubEmbedder() {
        return new TextEmbedder() {
            @Override
            public float[] embedDocument(String text) {
                return vec(text);
            }

            @Override
            public float[] embedQuery(String text) {
                return vec(text);
            }

            @Override
            public int dimensions() {
                return 8;
            }

            @Override
            public String name() {
                return "stub";
            }

            private float[] vec(String text) {
                float[] v = new float[8];
                String t = text == null ? "" : text.toLowerCase();
                if (t.contains("orion") || t.contains("northwind") || t.contains("globex") || t.contains("acquisition")) {
                    v[0] = 1f;
                } else {
                    v[1] = 1f;
                }
                return v;
            }
        };
    }
}
