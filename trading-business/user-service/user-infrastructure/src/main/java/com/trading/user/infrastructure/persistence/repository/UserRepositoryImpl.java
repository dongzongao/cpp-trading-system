package com.trading.user.infrastructure.persistence.repository;

import com.trading.user.domain.model.aggregate.User;
import com.trading.user.domain.model.valueobject.Email;
import com.trading.user.domain.model.valueobject.UserId;
import com.trading.user.domain.model.valueobject.Username;
import com.trading.user.domain.model.valueobject.UserStatus;
import com.trading.user.domain.repository.UserRepository;
import com.trading.user.infrastructure.persistence.mapper.UserMapper;
import com.trading.user.infrastructure.persistence.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓储实现
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserMapper mapper;

    @Override
    public User save(User user) {
        UserPO po = mapper.toPO(user);
        UserPO saved = jpaRepository.save(po);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpaRepository.findByUsername(username.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpaRepository.existsByUsername(username.getValue());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    public List<User> findByStatus(UserStatus status, int page, int size) {
        return jpaRepository.findByStatus(status.name(), PageRequest.of(page, size))
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> findAll(int page, int size) {
        return jpaRepository.findAll(PageRequest.of(page, size))
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void delete(UserId userId) {
        jpaRepository.deleteById(userId.getValue());
    }
}
