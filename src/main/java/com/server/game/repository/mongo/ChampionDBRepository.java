package com.server.game.repository.mongo;

import java.util.Optional;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.modelInfo.ChampionInfo;


public interface ChampionDBRepository extends MongoRepository<ChampionInfo, Short> {
    Optional<ChampionInfo> findByName(String name);
    boolean existsByName(String name);
}
