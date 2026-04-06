# 通知上下文 (Notification Context) - DDD 详细设计

## 1. 概述

通知上下文负责向用户发送各类通知，包括邮件、短信、站内消息和推送通知。该上下文监听其他上下文的领域事件，并根据用户偏好发送相应的通知。

## 2. 领域模型

### 2.1 聚合根 (Aggregate Root)

#### Notification（通知聚合根）

```java
package com.trading.notification.domain.model.aggregate;

@Entity
@Table(name = "notifications")
public class Notification extends AggregateRoot<NotificationId> {
    
    private NotificationId notificationId;
    private UserId userId;
    private NotificationType type;
    private NotificationChannel channel;
    private NotificationPriority priority;
    private String title;
    private String content;
    private Map<String, String> metadata;
    private NotificationStatus status;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    
    // 工厂方法：创建通知
    public static Notification create(
            UserId userId,
            NotificationType type,
            NotificationChannel channel,
            NotificationPriority priority,
            String title,
            String content,
            Map<String, String> metadata) {
        
        Notification notification = new Notification();
        notification.notificationId = NotificationId.generate();
        notification.userId = userId;
        notification.type = type;
        notification.channel = channel;
        notification.priority = priority;
        notification.title = title;
        notification.content = content;
        notification.metadata = metadata != null ? metadata : new HashMap<>();
        notification.status = NotificationStatus.PENDING;
        notification.retryCount = 0;
        notification.maxRetries = 3;
        notification.scheduledAt = LocalDateTime.now();
        notification.createdAt = LocalDateTime.now();
        
        notification.registerEvent(new NotificationCreatedEvent(
            notification.notificationId, userId, type, channel
        ));
        
        return notification;
    }
    
    // 领域行为：发送通知
    public void send() {
        if (this.status != NotificationStatus.PENDING && 
            this.status != NotificationStatus.RETRY) {
            throw new IllegalStateException("Notification is not in sendable state");
        }
        
        this.status = NotificationStatus.SENDING;
        
        registerEvent(new NotificationSendingEvent(this.notificationId));
    }
    
    // 领域行为：发送成功
    public void markAsSent() {
        if (this.status != NotificationStatus.SENDING) {
            throw new IllegalStateException("Notification is not in sending state");
        }
        
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        
        registerEvent(new NotificationSentEvent(this.notificationId, this.channel));
    }
    
    // 领域行为：发送失败
    public void markAsFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.retryCount++;
        
        if (this.retryCount >= this.maxRetries) {
            this.status = NotificationStatus.FAILED;
            registerEvent(new NotificationFailedEvent(
                this.notificationId, errorMessage, this.retryCount
            ));
        } else {
            this.status = NotificationStatus.RETRY;
            // 计算下次重试时间（指数退避）
            long delayMinutes = (long) Math.pow(2, this.retryCount);
            this.scheduledAt = LocalDateTime.now().plusMinutes(delayMinutes);
            
            registerEvent(new NotificationRetryScheduledEvent(
                this.notificationId, this.scheduledAt, this.retryCount
            ));
        }
    }
    
    // 领域行为：标记为已读
    public void markAsRead() {
        if (this.status != NotificationStatus.SENT) {
            throw new IllegalStateException("Only sent notifications can be marked as read");
        }
        
        this.status = NotificationStatus.READ;
        this.readAt = LocalDateTime.now();
        
        registerEvent(new NotificationReadEvent(this.notificationId));
    }
    
    // 领域行为：取消通知
    public void cancel() {
        if (this.status == NotificationStatus.SENT || 
            this.status == NotificationStatus.READ) {
            throw new IllegalStateException("Cannot cancel sent or read notification");
        }
        
        this.status = NotificationStatus.CANCELLED;
        
        registerEvent(new NotificationCancelledEvent(this.notificationId));
    }
    
    // 查询方法
    public boolean isReadyToSend() {
        return (this.status == NotificationStatus.PENDING || 
                this.status == NotificationStatus.RETRY) &&
               LocalDateTime.now().isAfter(this.scheduledAt);
    }
    
    public boolean shouldRetry() {
        return this.status == NotificationStatus.RETRY && 
               this.retryCount < this.maxRetries;
    }
}
```

#### NotificationPreference（通知偏好聚合根）

```java
package com.trading.notification.domain.model.aggregate;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends AggregateRoot<NotificationPreferenceId> {
    
    private NotificationPreferenceId preferenceId;
    private UserId userId;
    private Map<NotificationType, Set<NotificationChannel>> channelPreferences;
    private boolean enableEmailNotification;
    private boolean enableSmsNotification;
    private boolean enablePushNotification;
    private boolean enableInAppNotification;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private LocalDateTime updatedAt;
    
    // 工厂方法：创建默认偏好
    public static NotificationPreference createDefault(UserId userId) {
        NotificationPreference preference = new NotificationPreference();
        preference.preferenceId = NotificationPreferenceId.generate();
        preference.userId = userId;
        preference.channelPreferences = new HashMap<>();
        preference.enableEmailNotification = true;
        preference.enableSmsNotification = true;
        preference.enablePushNotification = true;
        preference.enableInAppNotification = true;
        preference.quietHoursStart = LocalTime.of(22, 0);
        preference.quietHoursEnd = LocalTime.of(8, 0);
        preference.updatedAt = LocalDateTime.now();
        
        // 设置默认偏好
        preference.setDefaultPreferences();
        
        return preference;
    }
    
    // 领域行为：更新渠道偏好
    public void updateChannelPreference(
            NotificationType type, 
            Set<NotificationChannel> channels) {
        
        this.channelPreferences.put(type, channels);
        this.updatedAt = LocalDateTime.now();
        
        registerEvent(new NotificationPreferenceUpdatedEvent(
            this.preferenceId, type, channels
        ));
    }
    
    // 领域行为：启用/禁用渠道
    public void setChannelEnabled(NotificationChannel channel, boolean enabled) {
        switch (channel) {
            case EMAIL:
                this.enableEmailNotification = enabled;
                break;
            case SMS:
                this.enableSmsNotification = enabled;
                break;
            case PUSH:
                this.enablePushNotification = enabled;
                break;
            case IN_APP:
                this.enableInAppNotification = enabled;
                break;
        }
        
        this.updatedAt = LocalDateTime.now();
    }
    
    // 领域行为：设置免打扰时间
    public void setQuietHours(LocalTime start, LocalTime end) {
        this.quietHoursStart = start;
        this.quietHoursEnd = end;
        this.updatedAt = LocalDateTime.now();
    }
    
    // 查询方法：检查是否应该发送通知
    public boolean shouldSendNotification(
            NotificationType type, 
            NotificationChannel channel,
            NotificationPriority priority) {
        
        // 检查渠道是否启用
        if (!isChannelEnabled(channel)) {
            return false;
        }
        
        // 检查用户偏好
        Set<NotificationChannel> preferredChannels = channelPreferences.get(type);
        if (preferredChannels != null && !preferredChannels.contains(channel)) {
            return false;
        }
        
        // 高优先级通知忽略免打扰时间
        if (priority == NotificationPriority.HIGH || 
            priority == NotificationPriority.URGENT) {
            return true;
        }
        
        // 检查免打扰时间
        return !isInQuietHours();
    }
    
    private boolean isChannelEnabled(NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return enableEmailNotification;
            case SMS:
                return enableSmsNotification;
            case PUSH:
                return enablePushNotification;
            case IN_APP:
                return enableInAppNotification;
            default:
                return false;
        }
    }
    
    private boolean isInQuietHours() {
        LocalTime now = LocalTime.now();
        
        if (quietHoursStart.isBefore(quietHoursEnd)) {
            // 正常情况：如 22:00 - 08:00 (次日)
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        } else {
            // 跨午夜情况：如 08:00 - 22:00
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        }
    }
    
    private void setDefaultPreferences() {
        // 订单通知：所有渠道
        channelPreferences.put(NotificationType.ORDER_CREATED, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        channelPreferences.put(NotificationType.ORDER_FILLED, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.PUSH, NotificationChannel.IN_APP));
        
        // 账户通知：邮件和站内
        channelPreferences.put(NotificationType.DEPOSIT_COMPLETED, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
        channelPreferences.put(NotificationType.WITHDRAWAL_COMPLETED, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.IN_APP));
        
        // 风险通知：所有渠道
        channelPreferences.put(NotificationType.RISK_WARNING, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.SMS, 
                   NotificationChannel.PUSH, NotificationChannel.IN_APP));
        
        // KYC通知：邮件和站内
        channelPreferences.put(NotificationType.KYC_APPROVED, 
            Set.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP));
    }
}
```

### 2.2 值对象 (Value Object)

```java
package com.trading.notification.domain.model.valueobject;

public enum NotificationType {
    // 用户相关
    USER_REGISTERED,
    EMAIL_VERIFIED,
    KYC_SUBMITTED,
    KYC_APPROVED,
    KYC_REJECTED,
    
    // 账户相关
    DEPOSIT_COMPLETED,
    WITHDRAWAL_COMPLETED,
    ACCOUNT_SUSPENDED,
    
    // 订单相关
    ORDER_CREATED,
    ORDER_FILLED,
    ORDER_PARTIALLY_FILLED,
    ORDER_CANCELLED,
    ORDER_REJECTED,
    
    // 风险相关
    RISK_WARNING,
    DAILY_LOSS_LIMIT,
    POSITION_LIMIT_WARNING,
    ACCOUNT_SUSPENDED_RISK,
    
    // 清算相关
    SETTLEMENT_COMPLETED,
    SETTLEMENT_REPORT,
    
    // 系统相关
    SYSTEM_MAINTENANCE,
    SYSTEM_ALERT
}

public enum NotificationChannel {
    EMAIL,    // 邮件
    SMS,      // 短信
    PUSH,     // 推送
    IN_APP    // 站内消息
}

public enum NotificationPriority {
    LOW,      // 低优先级
    NORMAL,   // 普通
    HIGH,     // 高优先级
    URGENT    // 紧急
}

public enum NotificationStatus {
    PENDING,    // 待发送
    SENDING,    // 发送中
    SENT,       // 已发送
    READ,       // 已读
    FAILED,     // 失败
    RETRY,      // 待重试
    CANCELLED   // 已取消
}
```

## 3. 领域服务 (Domain Service)

### NotificationTemplateService（通知模板服务）

```java
package com.trading.notification.domain.service;

@Service
public class NotificationTemplateService {
    
    // 根据事件生成通知内容
    public NotificationContent generateContent(
            NotificationType type, 
            Map<String, String> parameters) {
        
        String title;
        String content;
        
        switch (type) {
            case ORDER_FILLED:
                title = "订单成交通知";
                content = String.format(
                    "您的订单 %s 已完全成交。\n" +
                    "交易品种：%s\n" +
                    "成交价格：%s\n" +
                    "成交数量：%s",
                    parameters.get("orderId"),
                    parameters.get("symbol"),
                    parameters.get("price"),
                    parameters.get("quantity")
                );
                break;
                
            case RISK_WARNING:
                title = "风险预警";
                content = String.format(
                    "您的账户触发风险预警：\n%s\n" +
                    "请及时关注账户风险状况。",
                    parameters.get("reason")
                );
                break;
                
            case WITHDRAWAL_COMPLETED:
                title = "提现完成";
                content = String.format(
                    "您的提现申请已完成。\n" +
                    "提现金额：%s\n" +
                    "到账时间：%s",
                    parameters.get("amount"),
                    parameters.get("completedAt")
                );
                break;
                
            default:
                title = "系统通知";
                content = "您有一条新的系统通知";
        }
        
        return new NotificationContent(title, content);
    }
}
```

### NotificationDispatchService（通知分发服务）

```java
package com.trading.notification.domain.service;

@Service
public class NotificationDispatchService {
    
    private final EmailService emailService;
    private final SmsService smsService;
    private final PushService pushService;
    private final InAppMessageService inAppMessageService;
    
    // 分发通知到指定渠道
    public void dispatch(Notification notification, UserContact contact) {
        try {
            switch (notification.getChannel()) {
                case EMAIL:
                    sendEmail(notification, contact.getEmail());
                    break;
                case SMS:
                    sendSms(notification, contact.getPhone());
                    break;
                case PUSH:
                    sendPush(notification, contact.getDeviceToken());
                    break;
                case IN_APP:
                    sendInApp(notification);
                    break;
            }
            
            notification.markAsSent();
            
        } catch (Exception e) {
            notification.markAsFailed(e.getMessage());
        }
    }
    
    private void sendEmail(Notification notification, String email) {
        emailService.send(
            email,
            notification.getTitle(),
            notification.getContent()
        );
    }
    
    private void sendSms(Notification notification, String phone) {
        smsService.send(
            phone,
            notification.getContent()
        );
    }
    
    private void sendPush(Notification notification, String deviceToken) {
        pushService.send(
            deviceToken,
            notification.getTitle(),
            notification.getContent()
        );
    }
    
    private void sendInApp(Notification notification) {
        inAppMessageService.create(
            notification.getUserId(),
            notification.getTitle(),
            notification.getContent()
        );
    }
}
```

## 4. 仓储接口 (Repository)

```java
package com.trading.notification.domain.repository;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(NotificationId notificationId);
    List<Notification> findByUserId(UserId userId);
    List<Notification> findPendingNotifications();
    List<Notification> findByStatus(NotificationStatus status);
    Page<Notification> findByUserIdAndStatus(
        UserId userId, NotificationStatus status, Pageable pageable
    );
}

public interface NotificationPreferenceRepository {
    NotificationPreference save(NotificationPreference preference);
    Optional<NotificationPreference> findById(NotificationPreferenceId preferenceId);
    Optional<NotificationPreference> findByUserId(UserId userId);
}
```

## 5. 领域事件 (Domain Event)

```java
package com.trading.notification.domain.event;

public class NotificationCreatedEvent extends DomainEvent {
    private final NotificationId notificationId;
    private final UserId userId;
    private final NotificationType type;
    private final NotificationChannel channel;
}

public class NotificationSentEvent extends DomainEvent {
    private final NotificationId notificationId;
    private final NotificationChannel channel;
}

public class NotificationFailedEvent extends DomainEvent {
    private final NotificationId notificationId;
    private final String errorMessage;
    private final Integer retryCount;
}
```

## 6. 应用层 (Application Layer)

```java
package com.trading.notification.application.service;

@Service
@Transactional
public class NotificationApplicationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateService templateService;
    private final NotificationDispatchService dispatchService;
    private final UserService userService;
    private final DomainEventPublisher eventPublisher;
    
    // 创建并发送通知
    public void sendNotification(SendNotificationCommand command) {
        UserId userId = UserId.of(command.getUserId());
        
        // 获取用户偏好
        NotificationPreference preference = preferenceRepository
            .findByUserId(userId)
            .orElseGet(() -> NotificationPreference.createDefault(userId));
        
        NotificationType type = NotificationType.valueOf(command.getType());
        NotificationChannel channel = NotificationChannel.valueOf(command.getChannel());
        NotificationPriority priority = NotificationPriority.valueOf(command.getPriority());
        
        // 检查是否应该发送
        if (!preference.shouldSendNotification(type, channel, priority)) {
            log.info("Notification skipped due to user preference");
            return;
        }
        
        // 生成通知内容
        NotificationContent content = templateService.generateContent(
            type, command.getParameters()
        );
        
        // 创建通知
        Notification notification = Notification.create(
            userId,
            type,
            channel,
            priority,
            content.getTitle(),
            content.getContent(),
            command.getParameters()
        );
        
        notification = notificationRepository.save(notification);
        eventPublisher.publish(notification.getDomainEvents());
        
        // 异步发送
        sendNotificationAsync(notification.getNotificationId());
    }
    
    // 异步发送通知
    @Async
    public void sendNotificationAsync(NotificationId notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        
        if (!notification.isReadyToSend()) {
            return;
        }
        
        // 获取用户联系方式
        UserContact contact = userService.getUserContact(notification.getUserId());
        
        // 发送通知
        notification.send();
        notificationRepository.save(notification);
        
        dispatchService.dispatch(notification, contact);
        
        notificationRepository.save(notification);
        eventPublisher.publish(notification.getDomainEvents());
    }
    
    // 定时任务：重试失败的通知
    @Scheduled(fixedRate = 60000)  // 每分钟执行
    public void retryFailedNotifications() {
        List<Notification> retryNotifications = notificationRepository
            .findByStatus(NotificationStatus.RETRY);
        
        for (Notification notification : retryNotifications) {
            if (notification.isReadyToSend()) {
                sendNotificationAsync(notification.getNotificationId());
            }
        }
    }
    
    // 监听领域事件并发送通知
    @EventListener
    public void handleOrderFilledEvent(OrderFullyFilledEvent event) {
        SendNotificationCommand command = new SendNotificationCommand();
        command.setUserId(event.getUserId().getValue());
        command.setType(NotificationType.ORDER_FILLED.name());
        command.setChannel(NotificationChannel.EMAIL.name());
        command.setPriority(NotificationPriority.NORMAL.name());
        
        Map<String, String> params = new HashMap<>();
        params.put("orderId", event.getOrderId().toString());
        params.put("symbol", event.getSymbol().toString());
        params.put("price", event.getFillPrice().toString());
        params.put("quantity", event.getFilledQuantity().toString());
        command.setParameters(params);
        
        sendNotification(command);
    }
}
```

## 7. 项目结构

```
notification-service/
├── notification-interfaces/
│   ├── rest/
│   │   ├── NotificationController.java
│   │   └── NotificationPreferenceController.java
│   └── event/
│       └── DomainEventListener.java
├── notification-application/
│   ├── service/
│   │   └── NotificationApplicationService.java
│   ├── command/
│   │   └── SendNotificationCommand.java
│   └── scheduler/
│       └── NotificationRetryScheduler.java
├── notification-domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   ├── Notification.java
│   │   │   └── NotificationPreference.java
│   │   └── valueobject/
│   │       ├── NotificationType.java
│   │       ├── NotificationChannel.java
│   │       └── NotificationStatus.java
│   ├── service/
│   │   ├── NotificationTemplateService.java
│   │   └── NotificationDispatchService.java
│   ├── repository/
│   │   ├── NotificationRepository.java
│   │   └── NotificationPreferenceRepository.java
│   └── event/
│       └── NotificationEvents.java
└── notification-infrastructure/
    ├── persistence/
    │   ├── NotificationRepositoryImpl.java
    │   └── NotificationPreferenceRepositoryImpl.java
    ├── external/
    │   ├── EmailService.java
    │   ├── SmsService.java
    │   ├── PushService.java
    │   └── InAppMessageService.java
    └── messaging/
        └── NotificationEventProducer.java
```

## 8. 总结

通知上下文的核心职责：
1. 管理用户通知偏好
2. 监听领域事件并生成通知
3. 通过多种渠道发送通知
4. 处理通知失败和重试
5. 支持免打扰时间和优先级
