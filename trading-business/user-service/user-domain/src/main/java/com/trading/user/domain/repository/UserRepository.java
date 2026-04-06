package com.trading.user.domain.repository;

import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.valueobject.Email;
import com.trading.user.domain.model.valueobject.UserId;
import com.trading.user.domain.model.valueobject.Username;
import com.trading.user.domain.model.valueobject.UserStatus;

import java.util.List;
import java.util.Optional;

/**
 * 用户仓储接口
 */
public interface UserRepository {
    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(UserId userId);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(Username username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(Email email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(Username username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(Email email);

    /**
     * 根据状态查找用户（分页）
     */
    List<User> findByStatus(UserStatus status, int page, int size);

    /**
     * 查找所有用户（分页）
     */
    List<User> findAll(int page, int size);

    /**
     * 删除用户
     */
    void delete(UserId userId);
}
