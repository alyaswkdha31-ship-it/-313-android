package com.thirteenhub.app;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Strict allowlist for Runcrate's GitHub-linked free inference models.
 *
 * This class intentionally does not discover arbitrary models from /v1/models:
 * that catalogue also contains paid models. All outgoing chat requests must
 * validate their model ID with isAllowed() before networking.
 */
public final class RuncrateFreeModels {
    public static final String DEEPSEEK_V41_FLASH =
            "deepseek-ai/DeepSeek-V4.1-Flash";
    public static final String GLM_53_FLASH =
            "zai-org/GLM-5.3-Flash";
    public static final String GLM_53 =
            "zai-org/GLM-5.3";
    public static final String GPT_OSS_120B =
            "openai/gpt-oss-120b";
    public static final String QWEN_38_FLASH =
            "Qwen/Qwen3.8-Flash";

    public static final class Model {
        public final String id;
        public final String name;
        public final String provider;

        private Model(String id, String name, String provider) {
            this.id = id;
            this.name = name;
            this.provider = provider;
        }
    }

    private static final List<Model> MODELS = Collections.unmodifiableList(
            Arrays.asList(
                    new Model(DEEPSEEK_V41_FLASH, "DeepSeek V4.1 Flash", "DeepSeek"),
                    new Model(GLM_53_FLASH, "GLM 5.3 Flash", "Z.ai"),
                    new Model(GLM_53, "GLM 5.3", "Z.ai"),
                    new Model(GPT_OSS_120B, "gpt-oss 120B", "OpenAI"),
                    new Model(QWEN_38_FLASH, "Qwen3.8 Flash", "Alibaba")
            )
    );

    private RuncrateFreeModels() { }

    public static List<Model> all() {
        return MODELS;
    }

    public static boolean isAllowed(String modelId) {
        if (modelId == null) return false;
        for (Model model : MODELS) {
            if (model.id.equals(modelId)) return true;
        }
        return false;
    }

    /**
     * Free allowance exhaustion and busy free capacity are terminal states.
     * Callers must not retry against a paid model automatically.
     */
    public static boolean isFreeLimitError(String providerErrorCode) {
        return "free_allowance_used".equals(providerErrorCode)
                || "free_capacity_busy".equals(providerErrorCode);
    }
}
