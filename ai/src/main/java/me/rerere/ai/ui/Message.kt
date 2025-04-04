package me.rerere.ai.ui

import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import kotlin.uuid.Uuid

// 公共消息抽象, 具体的Provider实现会转换为API接口需要的DTO
@Serializable
data class UIMessage(
    val id: String = Uuid.random().toString(),
    val role: MessageRole,
    val parts: List<UIMessagePart>
) {
    private fun appendChunk(chunk: MessageChunk): UIMessage {
        val choice = chunk.choices[0]
        return choice.delta?.let { delta ->
            val newParts = delta.parts.fold(parts) { acc, deltaPart ->
                when (deltaPart) {
                    is UIMessagePart.Text -> {
                        val existingTextPart = acc.find { it is UIMessagePart.Text } as? UIMessagePart.Text
                        if (existingTextPart != null) {
                            acc.map { part ->
                                if (part is UIMessagePart.Text) {
                                    UIMessagePart.Text(existingTextPart.text + deltaPart.text)
                                } else part
                            }
                        } else {
                            acc + UIMessagePart.Text(deltaPart.text)
                        }
                    }
                    is UIMessagePart.Reasoning -> {
                        val existingReasoningPart = acc.find { it is UIMessagePart.Reasoning } as? UIMessagePart.Reasoning
                        if (existingReasoningPart != null) {
                            acc.map { part ->
                                if (part is UIMessagePart.Reasoning) {
                                    UIMessagePart.Reasoning(existingReasoningPart.reasoning + deltaPart.reasoning)
                                } else part
                            }
                        } else {
                            acc + UIMessagePart.Reasoning(deltaPart.reasoning)
                        }
                    }
                    else -> {
                        println("delta part append not supported: $deltaPart")
                        acc
                    }
                }
            }
            copy(parts = newParts)
        } ?: this
    }

    operator fun plus(chunk: MessageChunk): UIMessage {
        return this.appendChunk(chunk)
    }

    companion object {
        fun ofText(role: MessageRole, text: String): UIMessage {
            return UIMessage(Uuid.random().toString(), role, listOf(UIMessagePart.Text(text)))
        }
    }
}

fun List<UIMessage>.handleMessageChunk(chunk: MessageChunk): List<UIMessage> {
    require(this.isNotEmpty()) {
        "messages must not be empty"
    }
    val choice = chunk.choices[0]
    val message = choice.delta ?: choice.message ?: throw Exception("delta/message is null")
    if(this.last().role != message.role) {
        return this + message
    } else {
        val last = this.last() + chunk
        return this.dropLast(1) + last
    }
}

@Serializable
sealed class UIMessagePart {
    @Serializable
    data class Text(val text: String) : UIMessagePart()

    @Serializable
    data class Image(val url: String) : UIMessagePart()

    @Serializable
    data class Reasoning(val reasoning: String) : UIMessagePart()
}

@Serializable
data class MessageChunk(
    val id: String,
    val model: String,
    val choices: List<UIMessageChoice>,
)

@Serializable
data class UIMessageChoice(
    val index: Int,
    val delta: UIMessage?,
    val message: UIMessage?,
    val finishReason: String?
)