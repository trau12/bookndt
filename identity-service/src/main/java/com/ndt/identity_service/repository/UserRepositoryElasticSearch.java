package com.ndt.identity_service.repository;

import com.ndt.identity_service.entity.UserDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserRepositoryElasticSearch extends ElasticsearchRepository<UserDocument, String> {
    @Query("{\"bool\": {\"must\": {\"match\": {\"lastName\": \"?0\"}}}}")
    List<UserDocument> findByLastName(String lastName);

    @Query("{\"match\": {\"lastName\": \"?0\"}}")
    List<UserDocument> findByLastNameFuzzy(String lastName);

}
