package com.sha.perceptdrive.repository;

import com.sha.perceptdrive.entities.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Integer> {

    List<User> findUserById(final Integer id);
    List<User> findUserByPhone(final String phone);
}
