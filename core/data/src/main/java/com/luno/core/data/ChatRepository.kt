package com.luno.core.data

import com.luno.core.model.Conversation
import com.luno.core.model.Message
import com.luno.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChatRepository {
    private val initialUsers = listOf(
        User("1", "Nguyễn Văn An", "https://picsum.photos/200?1", true, "Đang hoạt động"),
        User("2", "Trần Thị Mai", "https://picsum.photos/200?2", false, "Truy cập 15 phút trước"),
        User("3", "Lê Hoàng Long", "https://picsum.photos/200?3", true, "Đang hoạt động"),
        User("4", "Phạm Quỳnh Nga", "https://picsum.photos/200?4", false, "Truy cập hôm qua"),
    )

    private val initialConversations = listOf(
        Conversation(
            id = "conv_1",
            recipient = initialUsers[0],
            lastMessage = Message("m1", "1", "Chào bạn, dự án Luno đến đâu rồi nhỉ?", System.currentTimeMillis() - 3600000, false, null),
            unreadCount = 2,
            isPinned = true
        ),
        Conversation(
            id = "conv_2",
            recipient = initialUsers[1],
            lastMessage = Message("m2", "2", "Chiều nay họp lúc 2 giờ nhé.", System.currentTimeMillis() - 7200000, false, null),
            unreadCount = 0,
            isPinned = false
        ),
        Conversation(
            id = "conv_3",
            recipient = initialUsers[2],
            lastMessage = Message("m3", "3", "Đã gửi file thiết kế Zalo UI cho bạn.", System.currentTimeMillis() - 86400000, true, null),
            unreadCount = 0,
            isPinned = false
        ),
        Conversation(
            id = "conv_4",
            recipient = initialUsers[3],
            lastMessage = Message("m4", "4", "Cảm ơn bạn nhiều nha! ❤️", System.currentTimeMillis() - 172800000, true, null),
            unreadCount = 0,
            isPinned = false
        )
    )

    private val _conversationsFlow = MutableStateFlow(initialConversations)
    val conversationsFlow: Flow<List<Conversation>> = _conversationsFlow.asStateFlow()

    private val messagesMap = mutableMapOf<String, MutableList<Message>>(
        "conv_1" to mutableListOf(
            Message("m1_1", "1", "Chào bạn, dự án Luno đến đâu rồi nhỉ?", System.currentTimeMillis() - 3600000, false)
        ),
        "conv_2" to mutableListOf(
            Message("m2_1", "2", "Chiều nay họp lúc 2 giờ nhé.", System.currentTimeMillis() - 7200000, false)
        ),
        "conv_3" to mutableListOf(
            Message("m3_1", "3", "Đã gửi file thiết kế Zalo UI cho bạn.", System.currentTimeMillis() - 86400000, true)
        ),
        "conv_4" to mutableListOf(
            Message("m4_1", "4", "Cảm ơn bạn nhiều nha! ❤️", System.currentTimeMillis() - 172800000, true)
        )
    )

    private val _messagesFlowMap = mutableMapOf<String, MutableStateFlow<List<Message>>>().apply {
        messagesMap.forEach { (convId, list) ->
            put(convId, MutableStateFlow(list.toList()))
        }
    }

    fun getMessagesForConversation(convId: String): Flow<List<Message>> {
        if (!_messagesFlowMap.containsKey(convId)) {
            _messagesFlowMap[convId] = MutableStateFlow(emptyList())
        }
        return _messagesFlowMap[convId]!!.asStateFlow()
    }

    fun sendMessage(convId: String, text: String) {
        if (text.isBlank()) return
        val newMessage = Message(
            id = "msg_${System.currentTimeMillis()}",
            senderId = "me",
            text = text,
            timestamp = System.currentTimeMillis(),
            isRead = true
        )

        val list = messagesMap.getOrPut(convId) { mutableListOf() }
        list.add(newMessage)
        _messagesFlowMap[convId]?.value = list.toList()

        // Update last message in conversation list
        val currentConvs = _conversationsFlow.value.toMutableList()
        val index = currentConvs.indexOfFirst { it.id == convId }
        if (index != -1) {
            val conv = currentConvs[index]
            currentConvs[index] = conv.copy(lastMessage = newMessage, unreadCount = 0)
            _conversationsFlow.value = currentConvs
        }
    }

    fun getConversation(convId: String): Conversation? {
        return _conversationsFlow.value.find { it.id == convId }
    }
}
