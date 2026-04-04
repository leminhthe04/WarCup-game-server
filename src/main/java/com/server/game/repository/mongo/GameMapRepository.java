package com.server.game.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.modelInfo.GameMapInfo;


public interface GameMapRepository extends MongoRepository<GameMapInfo, Short> {
    Optional<GameMapInfo> findByName(String name);
    boolean existsByName(String name);
}
