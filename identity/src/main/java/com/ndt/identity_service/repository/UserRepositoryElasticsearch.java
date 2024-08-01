package com.ndt.identity_service.repository;

import com.ndt.identity_service.entity.UserDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface UserRepositoryElasticsearch extends ElasticsearchRepository<UserDocument, String> {
    @Query("{\"bool\": {\"must\": {\"match\": {\"lastName\": \"?0\"}}}}")
    List<UserDocument> findByLastName(String fieldValue);

    @Query("{\"fuzzy\": {\"address\": {\"value\": \"?0\", \"fuzziness\": \"auto\"}}}")
    List<UserDocument> findByLastNameFuzzy(String address);
}
