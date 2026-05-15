package com.server.game.repository.mongo;


import org.springframework.data.mongodb.repository.MongoRepository;

import com.server.game.resource.modelInfo.GameMapGridCompressInfo;


public interface GameMapGridCompressRepository extends MongoRepository<GameMapGridCompressInfo, Short> {
    // Optional<GameMapGrid> findByName(String name);
    boolean existsByName(String name);
}
