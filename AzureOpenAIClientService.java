package com.rfpio.server.aimodel.service;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import com.rfpio.server.aimodel.utils.AIModelConstants;
import com.rfpio.server.aimodel.utils.AIModelEnum.AIModelID;
import com.rfpio.server.aimodel.vo.ModelRequestVO;
import com.rfpio.server.company.ml.model.AIKeyProvider;
import com.rfpio.server.company.ml.vo.MLModuleEnum;
import com.rfpio.server.company.ml.vo.ModelUsageType;
import com.rfpio.server.properties.service.PropertiesService;
import com.rfpio.server.utils.RfpioThreadLocalUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

@Service("azureAIModelClient")
public class AzureOpenAIClientService implements AIModelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureOpenAIClientService.class);

    @Autowired
    private PropertiesService propertiesService;
    
    @Autowired
    private AIKeyProvider aIKeyProvider;

    @Autowired
    private RestTemplate restTemplate;

    private static AIModelID defaultModel;
    
	private static final String AZURE = "azure";
	private static final String URL = "url";
	private static final String KEY = "key";

    @PostConstruct
    public void init() {
        try {
            final String defaultModelName = propertiesService.getProperty(AIModelConstants.AZURE_LLM_MODEL);
            if (isNotBlank(defaultModelName)) {
                defaultModel = AIModelID.valueOf(defaultModelName);
            }
        } catch (Exception e) {
            LOGGER.error("Error initializing default AI model", e);
        }
    }

    static class ModelConfig {
        final String endpoint;
        final String apiKey;

        public ModelConfig(final String endpoint, final String apiKey) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
        }
    }

    @Override
    public String invoke(final ModelRequestVO modelRequestVO) {
        String responseContent = null;
        try {
            final AIModelID model = modelRequestVO.getModel() != null ? modelRequestVO.getModel() : defaultModel;
            if (model == null) {
                throw new IllegalArgumentException("No model specified or default model configured");
            }

            final ModelConfig config = loadModelConfig(model);
            if (config == null || isBlank(config.endpoint) || isBlank(config.apiKey)) {
                throw new IllegalStateException("Model configuration missing for: " + model.name());
            }

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.apiKey);

            final Map<String, Object> payload = buildPayload(modelRequestVO);
            final HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            final ResponseEntity<Map> response = restTemplate.exchange(config.endpoint, HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                responseContent = RfpioThreadLocalUtils.getGson().toJson(response.getBody());
            }
        } catch (final Exception e) {
        	LOGGER.error("Error while invoking Azure OpenAI model", e);
			if (e.getMessage() != null && e.getMessage().contains("context_length_exceeded")) {
				return "CONTEXT_LENGTH_EXCEEDED";
			}
        }
		return responseContent;
    }

    private ModelConfig loadModelConfig(final AIModelID model) {
    	final Map<String, Object> additionalInfo = new HashMap<>();
    	final Map<String, String> credential = aIKeyProvider.getCredentials(AZURE, model.getModelId(), MLModuleEnum.MODEL_CLIENT, additionalInfo, ModelUsageType.CHAT);
    	return new ModelConfig(credential.get(URL), credential.get(KEY));
    }

    private Map<String, Object> buildPayload(final ModelRequestVO modelRequestVO) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("messages", buildMessages(modelRequestVO));
        if (modelRequestVO.getMaxTokens() > 0) {
            payload.put("max_tokens", modelRequestVO.getMaxTokens());
        }
        if (modelRequestVO.getTemperature() > 0) {
            payload.put("temperature", modelRequestVO.getTemperature());
        }
        if (modelRequestVO.getTopP() > 0) {
            payload.put("top_p", modelRequestVO.getTopP());
        }
        if (modelRequestVO.hasFunction()) {
            payload.put("functions", modelRequestVO.getFunctions());
            payload.put("function_call", "auto");
        }
        return payload;
    }

    private Object buildMessages(final ModelRequestVO modelRequestVO) {
        final List<Map<String, Object>> messages = new ArrayList<>();

        if (isNotBlank(modelRequestVO.getPrompt())) {
            messages.add(Map.of("role", "system", "content", modelRequestVO.getPrompt()));
        }

        if (isNotBlank(modelRequestVO.getInput())) {
            messages.add(Map.of("role", "user", "content", modelRequestVO.getInput()));
        }

        if (isNotEmpty(modelRequestVO.getFunctionResponses())) {
            messages.addAll(modelRequestVO.getFunctionResponses());
        }

        return messages;
    }
}
