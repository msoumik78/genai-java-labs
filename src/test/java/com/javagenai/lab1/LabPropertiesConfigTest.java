package com.javagenai.lab1;

import static org.assertj.core.api.Assertions.assertThat;

import com.javagenai.lab1.config.LabPropertiesConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LabPropertiesConfigTest {

    @Autowired
    LabPropertiesConfig lab;

    @Test
    void nestedYamlBindsHoldAndFence() {
        assertThat(lab.extraHoldMs()).isEqualTo(8000);
        assertThat(lab.fenceSize()).isEqualTo(2);
    }
}
