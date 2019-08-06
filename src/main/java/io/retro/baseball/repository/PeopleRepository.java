package io.retro.baseball.repository;


import io.retro.baseball.domain.Person;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PeopleRepository extends PagingAndSortingRepository<Person, String> {
    Person findPersonByPlayerId(String playerId);
}
