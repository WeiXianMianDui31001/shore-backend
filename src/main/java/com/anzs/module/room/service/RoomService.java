package com.anzs.module.room.service;

import com.anzs.common.exception.BizException;
import com.anzs.module.room.dto.RoomCreateDTO;
import com.anzs.module.room.entity.ChatMessage;
import com.anzs.module.room.entity.DiscussionRoom;
import com.anzs.module.room.entity.RoomMember;
import com.anzs.module.room.entity.WhiteboardOperation;
import com.anzs.module.room.mapper.ChatMessageMapper;
import com.anzs.module.room.mapper.DiscussionRoomMapper;
import com.anzs.module.room.mapper.RoomMemberMapper;
import com.anzs.module.room.mapper.WhiteboardOperationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final DiscussionRoomMapper roomMapper;
    private final RoomMemberMapper memberMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final WhiteboardOperationMapper whiteboardOperationMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public IPage<DiscussionRoom> roomList(String keyword, Integer page, Integer size) {
        Page<DiscussionRoom> p = new Page<>(page, size);
        LambdaQueryWrapper<DiscussionRoom> qw = new LambdaQueryWrapper<>();
        qw.eq(DiscussionRoom::getStatus, 0);
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(DiscussionRoom::getName, keyword);
        }
        qw.orderByDesc(DiscussionRoom::getCreatedAt);
        return roomMapper.selectPage(p, qw);
    }

    @Transactional
    public DiscussionRoom createRoom(Long userId, RoomCreateDTO dto) {
        DiscussionRoom room = new DiscussionRoom();
        room.setCreatorId(userId);
        room.setName(dto.getName());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            room.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        room.setMaxMembers(dto.getMaxMembers() == null ? 50 : dto.getMaxMembers());
        room.setExpireAt(LocalDateTime.now().plusMinutes(dto.getExpireMinutes() == null ? 120 : dto.getExpireMinutes()));
        room.setStatus(0);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.insert(room);

        RoomMember member = new RoomMember();
        member.setRoomId(room.getId());
        member.setUserId(userId);
        member.setRole(2); // 创建者编辑权限
        member.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member);
        return room;
    }

    @Transactional
    public void joinRoom(Long userId, Long roomId, String password) {
        DiscussionRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException("讨论室不存在");
        if (room.getStatus() != 0) throw new BizException("讨论室已关闭");
        if (room.getExpireAt() != null && room.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException("讨论室已过期");
        }
        if (room.getPasswordHash() != null && !room.getPasswordHash().isEmpty()) {
            if (password == null || !passwordEncoder.matches(password, room.getPasswordHash())) {
                throw new BizException("密码错误");
            }
        }
        // 检查是否已满
        Long count = memberMapper.selectCount(new LambdaQueryWrapper<RoomMember>().eq(RoomMember::getRoomId, roomId));
        if (count >= room.getMaxMembers()) {
            throw new BizException("讨论室已满员");
        }
        // 检查是否已加入
        RoomMember exist = memberMapper.selectOne(
                new LambdaQueryWrapper<RoomMember>()
                        .eq(RoomMember::getRoomId, roomId)
                        .eq(RoomMember::getUserId, userId)
        );
        if (exist == null) {
            RoomMember member = new RoomMember();
            member.setRoomId(roomId);
            member.setUserId(userId);
            member.setRole(1);
            member.setJoinedAt(LocalDateTime.now());
            memberMapper.insert(member);
        }
    }

    public List<ChatMessage> messageHistory(Long roomId, Long lastSeq, Integer limit) {
        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(ChatMessage::getRoomId, roomId);
        if (lastSeq != null && lastSeq > 0) {
            qw.gt(ChatMessage::getId, lastSeq);
        }
        qw.orderByAsc(ChatMessage::getCreatedAt);
        qw.last("LIMIT " + limit);
        return chatMessageMapper.selectList(qw);
    }

    public List<WhiteboardOperation> whiteboardHistory(Long roomId, Long lastSeq, Integer limit) {
        LambdaQueryWrapper<WhiteboardOperation> qw = new LambdaQueryWrapper<>();
        qw.eq(WhiteboardOperation::getRoomId, roomId);
        if (lastSeq != null && lastSeq > 0) {
            qw.gt(WhiteboardOperation::getSequenceNo, lastSeq);
        }
        qw.orderByAsc(WhiteboardOperation::getSequenceNo);
        qw.last("LIMIT " + limit);
        return whiteboardOperationMapper.selectList(qw);
    }

    @Transactional
    public ChatMessage saveChatMessage(Long roomId, Long senderId, String content, String clientMsgId, Integer msgType) {
        ChatMessage msg = new ChatMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(senderId);
        msg.setMsgType(msgType == null ? 0 : msgType);
        msg.setContent(content);
        msg.setClientMsgId(clientMsgId);
        msg.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(msg);
        return msg;
    }

    @Transactional
    public WhiteboardOperation saveWhiteboardOp(Long roomId, Long userId, String opType, String opData, Long sequenceNo) {
        WhiteboardOperation op = new WhiteboardOperation();
        op.setRoomId(roomId);
        op.setUserId(userId);
        op.setOpType(opType);
        op.setOpData(opData);
        op.setSequenceNo(sequenceNo);
        op.setCreatedAt(LocalDateTime.now());
        whiteboardOperationMapper.insert(op);
        return op;
    }
}
