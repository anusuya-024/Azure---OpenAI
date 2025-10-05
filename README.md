# Azure OpenAI Client Service

## Overview

The `AzureOpenAIClientService` is a Spring-based service implementation that provides integration with Azure OpenAI models. It implements the `AIModelClient` interface to enable seamless interaction with various Azure-hosted language models for chat completions and AI-powered functionalities.

## Features

- **Multi-Model Support**: Supports multiple Azure OpenAI models with configurable endpoints
- **Dynamic Model Configuration**: Automatically loads model configurations from properties and credential providers
- **Flexible Request Handling**: Supports customizable parameters including temperature, max tokens, and top-p
- **Function Calling**: Built-in support for OpenAI function calling capabilities
- **Error Handling**: Robust error handling with specific context length exceeded detection
- **Security**: Secure API key management through credential provider integration

## Architecture

### Key Components

- **Service Layer**: `AzureOpenAIClientService` - Main service implementing AI model client interface
- **Configuration**: Dynamic model configuration loaded via `PropertiesService` and `AIKeyProvider`
- **HTTP Client**: Uses Spring's `RestTemplate` for making API calls to Azure OpenAI endpoints
- **Credential Management**: Integrated with `AIKeyProvider` for secure credential retrieval

## Configuration

### Default Model Setup

The service initializes with a default model configured in properties:

```properties
# Azure OpenAI Default Model
azure.llm.model=GPT_4
```

### Model Configuration Structure

Each model requires:
- **Endpoint URL**: Azure OpenAI deployment endpoint
- **API Key**: Authentication key for the Azure service
- **Model ID**: Identifier for the specific model deployment

## API Usage

### Basic Request Structure

```java
ModelRequestVO modelRequest = new ModelRequestVO();
modelRequest.setModel(AIModelID.GPT_4);
modelRequest.setPrompt("You are a helpful assistant");
modelRequest.setInput("What is Azure OpenAI?");
modelRequest.setMaxTokens(500);
modelRequest.setTemperature(0.7);

String response = azureAIModelClient.invoke(modelRequest);
```

### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `model` | AIModelID | No | Specific model to use (defaults to configured model) |
| `prompt` | String | No | System message/context for the model |
| `input` | String | No | User message/question |
| `maxTokens` | int | No | Maximum tokens in the response |
| `temperature` | double | No | Controls randomness (0.0 to 1.0) |
| `topP` | double | No | Nucleus sampling parameter |
| `functions` | List | No | Function definitions for function calling |
| `functionResponses` | List | No | Previous function call responses |

## Message Structure

The service builds messages in OpenAI's expected format:

```json
{
  "messages": [
    {
      "role": "system",
      "content": "System prompt here"
    },
    {
      "role": "user",
      "content": "User input here"
    }
  ],
  "max_tokens": 500,
  "temperature": 0.7
}
```

## Integration Points

### AIKeyProvider Integration

The service integrates with the credential management system:

```java
Map<String, String> credentials = aIKeyProvider.getCredentials(
    "azure",
    modelId,
    MLModuleEnum.MODEL_CLIENT,
    additionalInfo,
    ModelUsageType.CHAT
);
```

### Response Format

Successful responses are returned as JSON strings containing the complete API response from Azure OpenAI.

## Best Practices

1. **Model Selection**: Always specify a model or ensure default model is configured
2. **Token Management**: Set appropriate `maxTokens` to control costs and response length
3. **Temperature Setting**: Use lower values (0.0-0.3) for factual responses, higher (0.7-1.0) for creative tasks
4. **Error Handling**: Always check for `CONTEXT_LENGTH_EXCEEDED` and handle gracefully
5. **Security**: Never hardcode API keys; always use credential provider

## Azure OpenAI Models

### Supported Models

The service supports various Azure OpenAI models through the `AIModelID` enum:
- GPT-4 (various versions)
- GPT-3.5-Turbo
- Custom deployed models

### Model Selection Strategy

1. Explicit model in request → Use specified model
2. No model in request → Use default configured model
3. No default configured → Throw error

## Monitoring and Maintenance

### Key Metrics to Monitor

- API response times
- Error rates (especially context length errors)
- Token usage per request
- Model configuration failures

### Maintenance Tasks

- Regular API key rotation
- Model endpoint updates
- Default model configuration review
- Log analysis for error patterns

## Troubleshooting

### Common Issues

**Issue**: "No model specified or default model configured"
- **Solution**: Configure default model in properties or specify model in request

**Issue**: "Model configuration missing"
- **Solution**: Verify AIKeyProvider has correct credentials for the model

**Issue**: Context length exceeded
- **Solution**: Reduce input size or increase max_tokens limit

**Issue**: API timeout
- **Solution**: Check Azure OpenAI endpoint availability and network connectivity

## Example Use Cases

### 1. Simple Chat Completion

```java
ModelRequestVO request = new ModelRequestVO();
request.setInput("Explain microservices architecture");
request.setTemperature(0.3);
String response = azureAIModelClient.invoke(request);
```

### 2. With System Prompt

```java
ModelRequestVO request = new ModelRequestVO();
request.setPrompt("You are a technical expert");
request.setInput("What is Docker?");
String response = azureAIModelClient.invoke(request);
```