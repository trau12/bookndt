package com.ndt.identity_service.repository;

import com.ndt.identity_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @Query(value = "Select * from user where first_name = :firstName", nativeQuery = true)
    List<User> findAllByFirstName(@Param("firstName") String firstName);

    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);

}
