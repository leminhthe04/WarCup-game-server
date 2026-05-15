package com.server.game.repository.mongo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.modelInfo.MinionInfo;


public interface MinionDBRepository extends MongoRepository<MinionInfo, Short> {
    Optional<MinionInfo> findByName(String name);
    boolean existsByName(String name);
}
