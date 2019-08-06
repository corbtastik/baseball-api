package io.retro.baseball.repository;

import java.util.List;

public interface PeopleReadListener<T> {
    void onChunk(List<T> chunk);
    void complete();
}
