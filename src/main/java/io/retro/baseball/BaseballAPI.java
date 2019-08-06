package io.retro.baseball;

import io.micrometer.core.annotation.Timed;
import io.retro.baseball.domain.Person;
import io.retro.baseball.repository.PeopleReadListener;
import io.retro.baseball.repository.PeopleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
public class BaseballAPI {

    private static final Logger LOG = LoggerFactory.getLogger(BaseballAPI.class);

    private final PeopleRepository peopleRepository;

    @Autowired
    public BaseballAPI(PeopleRepository peopleRepository) {
        this.peopleRepository = peopleRepository;
    }

    @GetMapping("/player/{playerId}")
    public Person player(@PathVariable String playerId) {
        return peopleRepository.findPersonByPlayerId(playerId);
    }

    @GetMapping("/players")
    @Timed(value = "all.players")
    public List<Person> players() {
        Instant start = Instant.now();
        List<Person> players = new ArrayList<>();
        this.peopleRepository.findAll().forEach(players::add);
        Instant finish = Instant.now();
        System.out.println("read-all " + players.size() + " duration=" + Duration.between(start, finish).toMillis() + " ms");
        return players;
    }

    @GetMapping(value = "/players/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Person> fromIterable(@RequestParam(value = "calm", defaultValue = "0") Integer calm) {
        return Flux.fromIterable(this.peopleRepository.findAll())
            .delayElements(Duration.ofMillis(calm));
    }

    @GetMapping(value = "/players/chunk", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Person> create(@RequestParam(value = "calm", defaultValue = "0") Integer calm) {
        return Flux.<Person>create(sink -> Mono.fromRunnable(() -> {
            chunk(new PeopleReadListener<Person>() {
                public void onChunk(List<Person> chunk) {
                    for(Person s : chunk) {
                        sink.next(s);
                    }
                }
                public void complete() {
                    sink.complete();
                }
            });
            return;
        }).subscribeOn(Schedulers.elastic()).subscribe()).delayElements(Duration.ofMillis(calm));
    }

    void chunk(PeopleReadListener<Person> listener) {
        final int pageLimit = 100;
        int pageNumber = 0;
        Page<Person> page = peopleRepository.findAll(PageRequest.of(pageNumber, pageLimit));
        while (page.hasNext()) {
            LOG.debug("Chunking page " + page.getNumber() + " with " + page.getNumberOfElements() + " elements");
            listener.onChunk(page.getContent());
            page = peopleRepository.findAll(PageRequest.of(++pageNumber, pageLimit));
        }
        // process last page
        if(page.hasContent()) {
            LOG.debug("Chunking LAST page " + page.getNumber() + " with " + page.getNumberOfElements() + " elements");
            listener.onChunk(page.getContent());
        }
        LOG.debug("Chunking complete");
        listener.complete();
    }
}


