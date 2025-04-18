package me.rerere.ai.provider.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageAnnotation
import me.rerere.ai.ui.UIMessageChoice
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.util.encodeBase64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

object OpenAIProvider : Provider<ProviderSetting.OpenAI> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Title", "RikkaHub")
                .addHeader("HTTP-Referer", "https://github.com/re-ovo/rikkahub")
                .build()
            chain.proceed(request)
        }
        .build()

    override suspend fun listModels(providerSetting: ProviderSetting.OpenAI): List<Model> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${providerSetting.baseUrl}/models")
                .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Failed to get models: ${response.code} ${response.body?.string()}")
            }

            val bodyStr = response.body?.string() ?: ""
            val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
            val data = bodyJson["data"]?.jsonArray ?: return@withContext emptyList()

            data.mapNotNull { modelJson ->
                val modelObj = modelJson.jsonObject
                val id = modelObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                Model(
                    modelId = id,
                    displayName = id,
                )
            }
        }

    override suspend fun generateText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
        messageTransformers: List<MessageTransformer>
    ): MessageChunk = withContext(Dispatchers.IO) {
        val requestBody =
            buildChatCompletionRequest(messages, params, messageTransformers = messageTransformers)
        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        println(json.encodeToString(requestBody))

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get response: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject

        // 从 JsonObject 中提取必要的信息
        val id = bodyJson["id"]?.jsonPrimitive?.contentOrNull ?: ""
        val model = bodyJson["model"]?.jsonPrimitive?.contentOrNull ?: ""
        val choice = bodyJson["choices"]?.jsonArray?.get(0)?.jsonObject ?: error("choices is null")

        val message = choice["message"]?.jsonObject ?: throw Exception("message is null")
        val finishReason = choice["finish_reason"]
            ?.jsonPrimitive
            ?.content
            ?: "unknown"

        MessageChunk(
            id = id,
            model = model,
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = null,
                    message = parseMessage(message),
                    finishReason = finishReason
                )
            )
        )
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams,
        messageTransformers: List<MessageTransformer>
    ): Flow<MessageChunk> = callbackFlow {
        val requestBody = buildChatCompletionRequest(
            messages,
            params,
            stream = true,
            messageTransformers = messageTransformers
        )
        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        println(requestBody)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    println("[onEvent] (done) 结束流: $data")
                    eventSource.cancel()
                    close()
                    return
                }
                println(data)
                data
                    .trim()
                    .split("\n")
                    .filter { it.isNotBlank() }
                    .map { json.parseToJsonElement(it).jsonObject }
                    .forEach {
                        // println(it)
                        val id = it["id"]?.jsonPrimitive?.contentOrNull ?: ""
                        val model = it["model"]?.jsonPrimitive?.contentOrNull ?: ""
                        val choices = it["choices"]?.jsonArray ?: JsonArray(emptyList())
                        if (choices.isEmpty()) return@forEach
                        val choice = choices[0].jsonObject
                        val message = choice["delta"]?.jsonObject ?: choice["message"]?.jsonObject
                        ?: throw Exception("delta/message is null")
                        val finishReason =
                            choice["finish_reason"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                        val messageChunk = MessageChunk(
                            id = id,
                            model = model,
                            choices = listOf(
                                UIMessageChoice(
                                    index = 0,
                                    delta = parseMessage(message),
                                    message = null,
                                    finishReason = finishReason,
                                )
                            )
                        )
                        trySend(messageChunk)
                    }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                var exception = t

                t?.printStackTrace()
                println("[onFailure] 发生错误: ${t?.message}")

                try {
                    if (t == null && response != null) {
                        val bodyElement = Json.parseToJsonElement(response.body?.string() ?: "{}")
                        println(bodyElement)
                        if (bodyElement is JsonObject) {
                            exception = Exception(
                                bodyElement["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                                    ?: "unknown",
                            )
                        } else if (bodyElement is JsonArray) {
                            exception = Exception(
                                bodyElement[0].jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                                    ?: "unknown",
                            )
                        } else {
                            exception = Exception("unknown error")
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    close(exception)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        awaitClose {
            println("[awaitClose] 关闭eventSource ")
            eventSource.cancel()
        }
    }

    private fun buildChatCompletionRequest(
        messages: List<UIMessage>,
        params: TextGenerationParams,
        stream: Boolean = false,
        messageTransformers: List<MessageTransformer> = emptyList()
    ): JsonObject {
        return buildJsonObject {
            put("model", params.model.modelId)
            put("messages", buildMessages(MessageTransformer.transform(messages, messageTransformers)))
            put("temperature", params.temperature)
            put("top_p", params.topP)
            put("stream", stream)
        }
    }

    private fun buildMessages(messages: List<UIMessage>) = buildJsonArray {
        messages
            .filter {
                it.isValidToUpload()
            }
            .forEachIndexed { index, message ->
                add(buildJsonObject {
                    put("role", JsonPrimitive(message.role.name.lowercase()))
                    putJsonArray("content") {
                        message.parts.forEach { part ->
                            val partJson = buildJsonObject {
                                when (part) {
                                    is UIMessagePart.Text -> {
                                        put("type", "text")
                                        put("text", part.text)
                                    }

                                    is UIMessagePart.Image -> {
                                        part.encodeBase64().onSuccess {
                                            put("type", "image_url")
                                            put("image_url", buildJsonObject {
                                                put("url", it)
                                            })
                                        }.onFailure {
                                            it.printStackTrace()
                                            println("encode image failed: ${part.url}")

                                            put("type", "text")
                                            put("text", "")
                                        }
                                    }

                                    else -> {
                                        println("message part not supported: $part")
                                        // DO NOTHING
                                    }
                                }
                            }
                            add(partJson)
                        }
                    }
                })
            }
    }

    private fun parseMessage(jsonObject: JsonObject): UIMessage {
        val role = MessageRole.valueOf(
            jsonObject["role"]?.jsonPrimitive?.content?.uppercase() ?: "ASSISTANT"
        )
        val reasoning = jsonObject["reasoning_content"] ?: jsonObject["reasoning"]

        // 也许支持其他模态的输出content? 暂时只支持文本吧
        val content = jsonObject["content"]?.jsonPrimitive?.contentOrNull ?: ""

        return UIMessage(
            role = role,
            parts = buildList {
                if (reasoning?.jsonPrimitive?.contentOrNull != null) {
                    add(
                        UIMessagePart.Reasoning(
                            reasoning = reasoning.jsonPrimitive.contentOrNull ?: ""
                        )
                    )
                }
                add(UIMessagePart.Text(content))
            },
            annotations = parseAnnotations(
                jsonObject["annotations"]?.jsonArray ?: JsonArray(
                    emptyList()
                )
            )
        )
    }

    private fun parseAnnotations(jsonArray: JsonArray): List<UIMessageAnnotation> {
        return jsonArray.map { element ->
            val type =
                element.jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: error("type is null")
            when (type) {
                "url_citation" -> {
                    UIMessageAnnotation.UrlCitation(
                        title = element.jsonObject["url_citation"]?.jsonObject["title"]?.jsonPrimitive?.contentOrNull
                            ?: "",
                        url = element.jsonObject["url_citation"]?.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                            ?: "",
                    )
                }

                else -> error("unknown annotation type: $type")
            }
        }
    }
}