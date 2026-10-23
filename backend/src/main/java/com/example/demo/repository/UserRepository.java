package com.example.demo.repository;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.name = :name, u.telefono = :telefono, u.partitaIva = :partitaIva WHERE u.email = :email")
    int updateProfileFields(
            @Param("email") String email,
            @Param("name") String name,
            @Param("telefono") String telefono,
            @Param("partitaIva") String partitaIva
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.password = :encodedPassword WHERE u.email = :email")
    int updateUserPassword(
            @Param("email") String email,
            @Param("encodedPassword") String encodedPassword
    );
}